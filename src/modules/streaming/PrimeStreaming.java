package modules.streaming;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkMessage;
import messages.PrimeReportMessage;
import messages.PrimeRequestMessage;
import modules.P2PClient;
import modules.overlays.GroupedOverlayModule;
import modules.overlays.OverlayModule;
import modules.player.StreamListener;
import utils.Common;
import entites.MDCVideoStreamChunk;
import entites.PrimeNodeReport;
import entites.VideoStreamChunk;
import experiment.frameworks.NodeAddress;

public class PrimeStreaming extends StreamingModule implements StreamListener {
  // Mapping between son to the information that was sent to him
  private Map<NodeAddress, PrimeNodeReport> outboxHistory = new TreeMap<NodeAddress, PrimeNodeReport>();
  // Mapping between parent to the information was got by him
  private Map<NodeAddress, PrimeNodeReport> inboxHistory = new TreeMap<NodeAddress, PrimeNodeReport>();
  // Mapping between a child to its playtime in order to send only relevant
  // reports
  private Map<NodeAddress, Long> sonsPlaytime = new TreeMap<NodeAddress, Long>();
  // Number of description in current cycle
  private int numOfDescriptionsInCurrentCycle = 0;
  // Exponential moving average
  private double ema = 0.0;
  // Global current max timestamp
  private Long currMaxTimestamp = Long.valueOf(-1);
  // Global previous max timestamp
  private Long prevMaxTimestamp = Long.valueOf(-1);
  private boolean firstDescriptionReceived = false;
  private int cycleCoefficent = 1;
  // Mapping between timestamp and its number of sending to children
  private final Map<Long, Integer[]> sourceSentHistory = new TreeMap<Long, Integer[]>();
  
  public PrimeStreaming(final P2PClient client, final OverlayModule<?> overlay, final Random r) {
    super(client, overlay, r);
    if (!(overlay instanceof GroupedOverlayModule)) {
      throw new RuntimeException("Overlay is not applicable.");
    }
  }
  
  @Override public void handleMessage(final messages.Message message) {
    super.handleMessage(message);
    if (message instanceof PrimeReportMessage) {
      final PrimeNodeReport inboxMessage;
      if (inboxHistory.containsKey(message.sourceId)) {
        inboxMessage = inboxHistory.get(message.sourceId);
      } else {
        inboxMessage = new PrimeNodeReport();
      }
      inboxMessage.update((PrimeNodeReport) (((PrimeReportMessage<?>) message).payload));
      inboxHistory.put(message.sourceId, inboxMessage);
      if (inboxHistory.get(message.sourceId).getLatestTimestamp() > currMaxTimestamp) {
        currMaxTimestamp = inboxHistory.get(message.sourceId).getLatestTimestamp();
      }
    } else if (message instanceof PrimeRequestMessage) {
      handleRequestMessage(message);
    } else if (message instanceof ChunkMessage) {
      numOfDescriptionsInCurrentCycle++;
      final ChunkMessage crm = (ChunkMessage) message;
      // for hop that didn't receive any description yet
      if (!firstDescriptionReceived) {
        firstDescriptionReceived = true;
        if (client.player.getVs() == null) {
          client.player.initVS(crm.chunk.index);
        }
      }
      client.player.getVs().updateChunk(crm.chunk);
    }
  }
  
  private void handleRequestMessage(final messages.Message message) {
    final TreeSet<VideoStreamChunk> chunks = new TreeSet<VideoStreamChunk>();
    final PrimeNodeReport request = (PrimeNodeReport) ((PrimeRequestMessage<?>) message).payload;
    if (request.size() == 0) {
      throw new RuntimeException("empty request message received!");
    }
    if (request.playTime != -1) {
      sonsPlaytime.put(message.sourceId, request.playTime);
    }
    for (final Long timestamp : request.keySet()) {
      VideoStreamChunk vsc = null;
      vsc = client.player.getVs().getChunk(timestamp);
      if (vsc == null) {
        // Father don't have this chunk any more.
        continue;
      }
      MDCVideoStreamChunk MDCvsc;
      for (final Integer description : request.get(timestamp)) {
        if (client.isServerMode()) {
          if (descriptionAlreadySent(timestamp, description)) {
            MDCvsc = getRarestDescription(vsc);
          } else {
            MDCvsc = getMDChunk(vsc, description);
          }
          updateDescriptionsSentBySource(MDCvsc);
        } else {
          MDCvsc = getMDChunk(vsc, description);
        }
        chunks.add(MDCvsc);
      }
    }
    if (chunks.isEmpty()) {
      return;
    }
    for (final VideoStreamChunk chunk : chunks) {
      if (chunk.index < fromChunk || chunk.index > latestChunk) {
        // continue;
        System.out.println("sending chunk " + chunk.index + " out of protocol bounds: " + fromChunk + "-" + latestChunk);
      }
      network.send(new ChunkMessage(getMessageTag(), network.getAddress(), message.sourceId, chunk));
    }
  }
  
  private static MDCVideoStreamChunk getMDChunk(final VideoStreamChunk vsc, final Integer description) {
    final TreeSet<Integer> ts = new TreeSet<Integer>();
    ts.add(description);
    return vsc.getMDChunk(ts);
  }
  
  private void updateDescriptionsSentBySource(final MDCVideoStreamChunk mdcvsc) {
    if (sourceSentHistory.get(mdcvsc.index) == null) {
      sourceSentHistory.put(mdcvsc.index, new Integer[Common.currentConfiguration.descriptions]);
      Arrays.fill(sourceSentHistory.get(mdcvsc.index), 0);
    }
    sourceSentHistory.get(mdcvsc.index)[mdcvsc.getDescriptions().iterator().next()]++;
  }
  
  private MDCVideoStreamChunk getRarestDescription(final VideoStreamChunk vsc) {
    int minValue = Integer.MAX_VALUE;
    int minDescription = -1;
    final Integer[] sendingHistory = sourceSentHistory.get(vsc.index);
    for (int i = 0; i < Common.currentConfiguration.descriptions; i++) {
      if (sendingHistory[i] < minValue) {
        minValue = sendingHistory[i];
        minDescription = i;
      }
    }
    return getMDChunk(vsc, minDescription);
  }
  
  private boolean descriptionAlreadySent(final Long timestamp, final Integer description) {
    return !((sourceSentHistory.get(timestamp) == null) || sourceSentHistory.get(timestamp)[description] == 0);
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    updateFields1();
    sendReportToAllMyChildren();
    qualityAdaptaion();
    diffusionPhase();
    swarmingPhase();
    updateFields2();
  }
  
  private void updateFields1() {
    final Set<NodeAddress> sons = ((GroupedOverlayModule<?>) mainOverlay).getNodeGroup(GroupedOverlayModule.sonsGroupName);
    final Set<NodeAddress> fathers = ((GroupedOverlayModule<?>) mainOverlay).getNodeGroup(GroupedOverlayModule.fathersGroupName);
    final Map<NodeAddress, Long> newSonsPlaytime = new TreeMap<NodeAddress, Long>();
    final Map<NodeAddress, PrimeNodeReport> newOutboxHistory = new TreeMap<NodeAddress, PrimeNodeReport>();
    final Map<NodeAddress, PrimeNodeReport> newInboxHistory = new TreeMap<NodeAddress, PrimeNodeReport>();
    for (final NodeAddress son : sons) {
      final Long sonPlayTime = sonsPlaytime.get(son);
      if (sonPlayTime != null) {
        newSonsPlaytime.put(son, sonPlayTime);
      }
      final PrimeNodeReport outbox = outboxHistory.get(son);
      if (outbox != null) {
        newOutboxHistory.put(son, outbox);
      }
    }
    for (final NodeAddress father : fathers) {
      final PrimeNodeReport inbox = inboxHistory.get(father);
      if (inbox != null) {
        newInboxHistory.put(father, inbox);
      }
    }
    outboxHistory = newOutboxHistory;
    inboxHistory = newInboxHistory;
    sonsPlaytime = newSonsPlaytime;
  }
  
  private void updateFields2() {
    numOfDescriptionsInCurrentCycle = 0;
    prevMaxTimestamp = currMaxTimestamp;
  }
  
  private void sendReportToAllMyChildren() {
    if (client.player.getVs() == null) {
      return;
    }
    PrimeNodeReport availableDescriptions;
    final Set<NodeAddress> sons = ((GroupedOverlayModule<?>) mainOverlay).getNodeGroup(GroupedOverlayModule.sonsGroupName);
    for (final NodeAddress son : sons) {
      if (!sonsPlaytime.containsKey(son)) {
        availableDescriptions = client.player.getVs().getAvailableDescriptions(client.player.getVs().windowOffset);
      } else {
        availableDescriptions = client.player.getVs().getAvailableDescriptions(sonsPlaytime.get(son));
      }
      sendReportToOneChild(availableDescriptions, son);
    }
  }
  
  private void sendReportToOneChild(final PrimeNodeReport availableDescriptions, final NodeAddress son) {
    PrimeNodeReport reportToSend;
    PrimeNodeReport alreadySent;
    if (!outboxHistory.containsKey(son)) {
      reportToSend = availableDescriptions;
      outboxHistory.put(son, availableDescriptions);
    } else {
      alreadySent = outboxHistory.get(son);
      // TODO collapse these next two lines to one funciton call?
      reportToSend = alreadySent.getDelta(availableDescriptions);
      alreadySent.update(reportToSend);
      // TODO do you really need this?
      outboxHistory.put(son, alreadySent);
    }
    if (reportToSend.size() != 0) {
      network.send(new PrimeReportMessage<PrimeNodeReport>(getMessageTag(), network.getAddress(), son, reportToSend));
    }
  }
  
  private void qualityAdaptaion() {
    if (client.player.getVs() == null || network.isServerMode()) {
      return;
    }
    final double coefficient = 2.0 / (cycleCoefficent + 1);
    ema = numOfDescriptionsInCurrentCycle * coefficient + ema * (1 - coefficient);
    final double currentEMA = ema / Common.currentConfiguration.descriptions;
    final double oneDescriptionUp = client.player.getVs().threshold + 1.0 / Common.currentConfiguration.descriptions;
    final double oneDescriptionDown = client.player.getVs().threshold - 1.0 / Common.currentConfiguration.descriptions;
    if (currentEMA < client.player.getVs().threshold) {
      client.player.getVs().threshold = Math.max(oneDescriptionDown, 0.0);
    } else if (currentEMA >= oneDescriptionUp) {
      client.player.getVs().threshold = Math.min(oneDescriptionUp, 1.0);
    }
    cycleCoefficent++;
  }
  
  private void diffusionPhase() {
    // Don't do any diffusion in either the first 2 cycles or when the parents
    // has nothing new (both prevMax and currMax aren't -1)
    if (prevMaxTimestamp == -1 || currMaxTimestamp == -1 || prevMaxTimestamp == currMaxTimestamp) {
      return;
    }
    final Set<NodeAddress> fathers = ((GroupedOverlayModule<?>) mainOverlay).getNodeGroup(GroupedOverlayModule.fathersGroupName);
    final PrimeNodeReport alreadyRequested = new PrimeNodeReport();
    for (final NodeAddress father : fathers) {
      if (!inboxHistory.keySet().contains(father)) {
        continue;
      }
      if (client.player.getVs() == null
          || inboxHistory.get(father).getLatestTimestamp() > client.player.getVs()
          .getAvailableDescriptions(client.player.getVs().windowOffset).getLatestTimestamp()) {
        final PrimeNodeReport availableDescriptionsAtFather = inboxHistory.get(father);
        PrimeNodeReport requestToSend = (client.player.getVs() == null) ? availableDescriptionsAtFather : (client.player.getVs()
            .getAvailableDescriptions(prevMaxTimestamp)).getDelta(availableDescriptionsAtFather);
        requestToSend = alreadyRequested.getDelta(requestToSend);
        if (requestToSend.size() == 0) {
          continue;
        }
        requestToSend = requestToSend.getRandomSubset(prevMaxTimestamp, currMaxTimestamp, r);
        if (requestToSend.size() != 0) {
          if (client.player.getVs() != null) {
            requestToSend.playTime = client.player.getVs().windowOffset;
          }
          alreadyRequested.update(requestToSend);
          network.send(new PrimeRequestMessage<PrimeNodeReport>(getMessageTag(), network.getAddress(), father, new PrimeNodeReport(
              requestToSend)));
        }
      }
    }
  }
  
  private void swarmingPhase() {
    // Don't do any swarming in either the first 2 cycles.
    if (prevMaxTimestamp == -1 || client.player.getVs() == null) {
      return;
    }
    final Set<NodeAddress> fathers = ((GroupedOverlayModule<?>) mainOverlay).getNodeGroup(GroupedOverlayModule.fathersGroupName);
    final PrimeNodeReport alreadyRequested = new PrimeNodeReport();
    final Map<Long, Integer> howManyMissingDescriptions = new TreeMap<Long, Integer>();
    // TODO initialize, also, why in parameter and not as return value?
    initiateHowManyMissingDescriptions(howManyMissingDescriptions);
    for (final NodeAddress father : fathers) {
      if (!inboxHistory.keySet().contains(father)) {
        continue;
      }
      // Don't do swarming phase for nodes that didn't get any chunks yet.
      if (inboxHistory.get(father).getLatestTimestamp() <= client.player.getVs()
          .getAvailableDescriptions(client.player.getVs().windowOffset).getLatestTimestamp()) {
        final PrimeNodeReport availableDescriptionsAtFather = inboxHistory.get(father);
        PrimeNodeReport requestToSend = client.player.getVs().getMissingDescriptions().getIntersect(availableDescriptionsAtFather);
        requestToSend = alreadyRequested.getDelta(requestToSend);
        updateRequestAccordingThreshold(howManyMissingDescriptions, requestToSend);
        if (requestToSend.size() == 0) {
          continue;
        }
        requestToSend = requestToSend.getRandomSubset(client.player.getVs().windowOffset - 1, prevMaxTimestamp, r);
        if (requestToSend.size() != 0) {
          requestToSend.playTime = client.player.getVs().windowOffset;
          alreadyRequested.update(requestToSend);
          updateHowManyMissingDescriptions(howManyMissingDescriptions, requestToSend);
          network.send(new PrimeRequestMessage<PrimeNodeReport>(getMessageTag(), network.getAddress(), father, new PrimeNodeReport(
              requestToSend)));
        }
      }
    }
  }
  
  private static void updateRequestAccordingThreshold(final Map<Long, Integer> howManyMissingDescriptions,
      final PrimeNodeReport requestToSend) {
    final Set<Long> keys = new TreeSet<Long>(requestToSend.keySet());
    for (final Long timestamp : keys) {
      if (howManyMissingDescriptions.get(timestamp) == 0) {
        requestToSend.remove(timestamp);
      }
    }
  }
  
  private static void updateHowManyMissingDescriptions(final Map<Long, Integer> howManyMissingDescriptions,
      final PrimeNodeReport requestToSend) {
    for (final Long timestamp : requestToSend.keySet()) {
      howManyMissingDescriptions.put(timestamp, Math.max(howManyMissingDescriptions.get(timestamp) - 1, 0));
    }
  }
  
  private void initiateHowManyMissingDescriptions(final Map<Long, Integer> howManyMissingDescriptions) {
    final PrimeNodeReport pr = client.player.getVs().getMissingDescriptions();
    final int threshold = (int) Math.ceil(client.player.getVs().threshold * Common.currentConfiguration.descriptions);
    for (final Long timestamp : pr.keySet()) {
      final Integer missingDescriptions = threshold - (Common.currentConfiguration.descriptions - pr.get(timestamp).size());
      howManyMissingDescriptions.put(timestamp, Math.max(missingDescriptions, 0));
    }
  }
  
  @Override public void onStreamUpdate(final Set<Long> updatedChunks) {
    // TODO test in mTreebone... maybe some code here is needed...
  }
}
