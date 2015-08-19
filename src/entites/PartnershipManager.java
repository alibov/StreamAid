package entites;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import modules.P2PClient;
import experiment.frameworks.NodeAddress;

public class PartnershipManager {
  final private Integer numOfSubstreams;
  // final private Integer maxNumOfPartners;
  final private Integer Ts; // Ts
  final private Integer Tp; // Tp
  final Integer bitmapTimeout;
  private Long maximalChunkInPartners;
  // DEBUG change to private
  public final BitmapListener bml;
  P2PClient client;
  private final Map<NodeAddress, BitmapStatistics> partnersBitmaps;
  private final Map<NodeAddress, Integer> partnersRemainingTimeToCatchUp;
  private final Map<NodeAddress, Integer> partnersRemainingTimeToAdvance;
  private final Boolean[] parents;
  final Integer timeForPartnerToBeSufficient;
  
  public PartnershipManager(final P2PClient client, final Integer numOfSubstreams, final Integer numOfMaxPartners,
      final Integer Ts, final Integer Tp, final Integer bitmapTimeOut, final Integer bitMapCyclesForPartnerToBeSufficient,
      final Boolean[] parents) {
    if (client == null || numOfSubstreams == null || numOfMaxPartners == null || Ts == null || Tp == null || bitmapTimeOut == null
        || bitMapCyclesForPartnerToBeSufficient == null) {
      throw new IllegalArgumentException("arguments mustn't be null");
    }
    if (numOfSubstreams < 0 || numOfMaxPartners < 0 || Ts < 0 || Tp < 0 || bitmapTimeOut < 0
        || bitMapCyclesForPartnerToBeSufficient < 0) {
      throw new IllegalArgumentException("integer arguments mustn't be negative");
    }
    this.numOfSubstreams = numOfSubstreams;
    timeForPartnerToBeSufficient = bitMapCyclesForPartnerToBeSufficient * bitmapTimeOut;
    // this.maxNumOfPartners = numOfMaxPartners;
    partnersBitmaps = new HashMap<NodeAddress, BitmapStatistics>();
    partnersRemainingTimeToCatchUp = new HashMap<NodeAddress, Integer>();
    partnersRemainingTimeToAdvance = new HashMap<NodeAddress, Integer>();
    this.Ts = Ts;
    this.Tp = Tp;
    this.parents = parents;
    bitmapTimeout = bitmapTimeOut;
    maximalChunkInPartners = Long.MIN_VALUE;
    bml = new BitmapListener(numOfSubstreams);
    client.player.addVSlistener(bml);
    this.client = client;
  }
  
  private boolean doesPartnerHoldTs(final NodeAddress partner, final Integer substream, final Boolean computeWithAbs) {
    final Long patnersSubstreamChunk = partnersBitmaps.get(partner).bitmap[substream];
    final Long[] currBitmap = bml.getFirstPartOfBitmap();
    Long deviation = 0L;
    int index = 0;
    for (final Long chunk : currBitmap) {
      if (parents[index]) {
        Long distance = 0L;
        if (computeWithAbs) {
          distance = Math.abs(chunk - patnersSubstreamChunk);
        } else {
          distance = chunk - patnersSubstreamChunk;
        }
        deviation = deviation < distance ? distance : deviation;
      }
      index++;
    }
    return deviation < Ts;
  }
  
  private boolean doesPartnerHoldTp(final NodeAddress partner, final Integer substream) {
    return maximalChunkInPartners - partnersBitmaps.get(partner).bitmap[substream] < Tp;
  }
  
  private Boolean isInsufficient(final NodeAddress partner) {
    for (int substream = 0; substream < numOfSubstreams; substream++) {
      if (doesPartnerHoldTs(partner, substream, false) && doesPartnerHoldTp(partner, substream)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   *
   * adds new Partners to the manager (with their Bitmaps).
   *
   * @param partner
   *          - the partner
   * @param bitmap
   *          = the patner's bitmap
   * @throws IllegalArgumentException
   *           - if partnersBitmaps is null or contains null or illegal bitmaps
   */
  public void updateBitmapToPartner(final NodeAddress partner, final Long[] bitmap) {
    if (partner == null || bitmap == null) {
      throw new IllegalArgumentException("arguments mustn't be null");
    }
    // check that the bitmap structures is valid
    if (bitmap.length != 2 * numOfSubstreams) {
      throw new IllegalArgumentException("partnersBitmaps mustn't contain illegal bitmap (legal = 2*numOfStreams)");
    }
    //
    if (partnersBitmaps.get(partner) == null) {
      partnersRemainingTimeToCatchUp.put(partner, timeForPartnerToBeSufficient);
      partnersRemainingTimeToAdvance.put(partner, timeForPartnerToBeSufficient);
    }
    partnersBitmaps.put(partner, new BitmapStatistics(bitmap, partnersBitmaps.get(partner)));
    maximalChunkInPartners = maximalChunkInPartners < partnersBitmaps.get(partner).maxChunk ? partnersBitmaps.get(partner).maxChunk
        : maximalChunkInPartners;
  }
  
  /**
   * clears all unsuitable partners according to coolstreaming+ definition of
   * unsuitable parents and partners.
   *
   * @return the cleared partners
   */
  public Collection<NodeAddress> clearUnsuitablePartners() {
    final Collection<NodeAddress> toBeRemoved = new HashSet<NodeAddress>();
    for (final Entry<NodeAddress, BitmapStatistics> keyvalue : partnersBitmaps.entrySet()) {
      // checks if got bitmaps fromthat partner
      if (keyvalue.getValue().TTL <= 0) {
        toBeRemoved.add(keyvalue.getKey());
      }
      //
      // DEBUG
      if (client.network.getAddress().getName().equals("0")) {
        System.out.print("p: " + keyvalue.getKey().getName() + " is advancing: "
            + partnersBitmaps.get(keyvalue.getKey()).isBitmapChanged() + " rem: "
            + partnersRemainingTimeToAdvance.get(keyvalue.getKey()) + " is suff: " + !isInsufficient(keyvalue.getKey()) + " rem: "
            + partnersRemainingTimeToCatchUp.get(keyvalue.getKey()) + " TTL " + keyvalue.getValue().TTL + " parenthood: ");
        for (int substream = 0; substream < numOfSubstreams; substream++) {
          if (getSuitableNodesForParenthoodInSubstream(substream, false).contains(keyvalue.getKey())) {
            System.out.print("t ");
          } else {
            System.out.print("f ");
          }
        }
      }
      //
      //
      // checks if the partner stuck
      if (!partnersBitmaps.get(keyvalue.getKey()).isBitmapChanged()) {
        partnersRemainingTimeToAdvance.put(keyvalue.getKey(), partnersRemainingTimeToAdvance.get(keyvalue.getKey()) - 1);
        if (partnersRemainingTimeToAdvance.get(keyvalue.getKey()) == 0) {
          toBeRemoved.add(keyvalue.getKey());
        }
      } else {
        partnersRemainingTimeToCatchUp.put(keyvalue.getKey(), timeForPartnerToBeSufficient);
      }
      // checks if the partner is insufficient
      if (isInsufficient(keyvalue.getKey())) {
        partnersRemainingTimeToCatchUp.put(keyvalue.getKey(), partnersRemainingTimeToCatchUp.get(keyvalue.getKey()) - 1);
        if (partnersRemainingTimeToCatchUp.get(keyvalue.getKey()) == 0) {
          toBeRemoved.add(keyvalue.getKey());
        }
      } else {
        partnersRemainingTimeToCatchUp.put(keyvalue.getKey(), timeForPartnerToBeSufficient);
      }
    }
    for (final NodeAddress node : toBeRemoved) {
      partnersBitmaps.remove(node);
    }
    return toBeRemoved;
  }
  
  /**
   * tells the partnershipManager that a cycle has been over.
   */
  public void nextCycle() {
    for (final BitmapStatistics bms : partnersBitmaps.values()) {
      bms.nextCycle();
    }
  }
  
  /**
   * gets all suitable partners for parenthood in a certain substream according
   * to coolstreaming+ definitions.
   *
   * @param substream
   *          - a certain substream
   * @return the suitable partners
   * @throws IllegalArgumentException
   *           - if substream is above the number of substreams
   */
  public Collection<NodeAddress> getSuitableNodesForParenthoodInSubstream(final Integer substream, final Boolean alreadyAParent) {
    if (substream == null) {
      throw new IllegalArgumentException("arguments mustn't be null");
    }
    if (substream < 0) {
      throw new IllegalArgumentException("substream mustn't be negative");
    }
    if (substream >= numOfSubstreams) {
      throw new IllegalArgumentException("substream is above the number of substreams");
    }
    final Set<NodeAddress> suitableForSubstream = new HashSet<NodeAddress>();
    for (final NodeAddress partner : partnersBitmaps.keySet()) {
      // DEBUG
      if (client.network.getAddress().getName().equals("9") && partner.getName().equals("4")) {
        System.out.println(bml.getLatestChunk() + Tp + "," + partnersBitmaps.get(partner).bitmap[substream]);
      }
      if (doesPartnerHoldTp(partner, substream) && doesPartnerHoldTs(partner, substream, true)) {
        suitableForSubstream.add(partner);
      }
    }
    return suitableForSubstream;
  }
  
  /**
   * gets all current partners.
   *
   * @return the partners
   */
  public Collection<NodeAddress> getCurrentPartners() {
    return partnersBitmaps.keySet();
  }
  
  /**
   * @return the current number of partners
   */
  public Integer getNumOfPartners() {
    return partnersBitmaps.size();
  }
  
  /**
   * @return removes a partner from the PartnerShipManager
   */
  public void removePartner(final NodeAddress partner) {
    partnersBitmaps.remove(partner);
  }
  
  public BitmapListener getBml() {
    return bml;
  }
  
  /**
   * produces a bitmap. joins the first part of the bitmap (which is calculated
   * within the partnership manager) and the given second part.
   *
   * @param secondPartOfBitmap
   *          - the second part of the bitmap.
   * @return a bitmap
   */
  public Long[] produceBitmap(final Long[] secondPartOfBitmap) {
    if (secondPartOfBitmap == null) {
      throw new IllegalArgumentException("arguments mustn't be null");
    }
    if (secondPartOfBitmap.length != numOfSubstreams) {
      throw new IllegalArgumentException("secondPartOfBitmap's size isn't equal to numOfSubstreams");
    }
    final Long[] result = Arrays.copyOf(bml.getFirstPartOfBitmap(), numOfSubstreams * 2);
    System.arraycopy(secondPartOfBitmap, 0, result, numOfSubstreams, numOfSubstreams);
    return result;
  }
  
  @Override public String toString() {
    String str = new String();
    str += "[ ";
    for (final NodeAddress l : partnersBitmaps.keySet()) {
      str += l.getName() + ", ";
    }
    str += " ]";
    return str;
  }
  
  class BitmapStatistics {
    public Long[] bitmap;
    public Long[] lastBitmap;
    public Long maxChunk;
    public Integer length;
    public Integer TTL;
    public Integer remainingTimeForRemainingBehind;
    
    public BitmapStatistics(final Long[] bitmap, final BitmapStatistics lastBitmap) {
      this.bitmap = bitmap;
      length = bitmap.length;
      maxChunk = findMaxChunk();
      TTL = timeForPartnerToBeSufficient;
      if (lastBitmap == null) {
        this.lastBitmap = null;
      } else {
        this.lastBitmap = lastBitmap.bitmap;
      }
    }
    
    public Boolean isBitmapChanged() {
      if (lastBitmap == null) {
        return true;
      }
      for (int i = 0; i < bitmap.length; i++) {
        if (!bitmap[i].equals(lastBitmap[i])) {
          return true;
        }
      }
      return false;
    }
    
    public void nextCycle() {
      TTL--;
    }
    
    private Long findMaxChunk() {
      Long maxVal = Long.MIN_VALUE;
      for (final Long val : bitmap) {
        maxVal = maxVal < val ? val : maxVal;
      }
      return maxVal;
    }
  }
}
