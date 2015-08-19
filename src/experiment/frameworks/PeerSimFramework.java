package experiment.frameworks;

import java.util.Properties;

import modules.network.peersim.ExperimentPlayer;
import modules.network.peersim.MyGeneralNode;
import modules.network.peersim.NodeAdder;
import modules.network.peersim.OneNodeInitialiser;
import modules.network.peersim.PeersimNode;
import modules.network.peersim.UniformRandomFIFOTransport;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.IdleProtocol;
import peersim.core.Network;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;
import utils.Common;
import utils.Utils;

/**
 * A framework running experiments using the Peersim P2P simulator
 *
 * @author Alexander Libov
 *
 */
public class PeerSimFramework implements P2PFramework {
  private final double dropRate;
  private final long maxDelay;
  private final long minDelay;
  
  public PeerSimFramework(final double dropRate, final long minDelay, final long maxDelay) {
    this.dropRate = dropRate;
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
  }
  
  @Override public void initFramework() {
    Utils.time = new Utils.Timeable() {
      @Override public long getTime() {
        return CommonState.getTime();
      }
    };
    final Properties p = new Properties();
    p.setProperty("SECOND", "1000");
    p.setProperty("MINUTE", "60*SECOND");
    p.setProperty("HOUR", "60*MINUTE");
    p.setProperty("simulation.logtime", "MINUTE");
    p.setProperty("simulation.endtime", "PLAYBACKSECONDS*SECOND");
    p.setProperty("network.size", String.valueOf(Common.currentConfiguration.nodes));
    p.setProperty("network.node", MyGeneralNode.class.getName());
    p.setProperty("PLAYBACKSECONDS", String.valueOf(Common.currentConfiguration.playbackSeconds));
    // ################### protocols ===========================
    p.setProperty("protocol.link", IdleProtocol.class.getName());
    p.setProperty("protocol.avg", PeersimNode.class.getName());
    p.setProperty("protocol.avg.linkable", "link");
    p.setProperty("protocol.avg.transport", "tr");
    p.setProperty("protocol.avg.step", "" + Common.currentConfiguration.cycleLength);
    p.setProperty("protocol.urt", UniformRandomFIFOTransport.class.getName());
    p.setProperty("protocol.urt.mindelay", "MINDELAY");
    p.setProperty("protocol.urt.maxdelay", "MAXDELAY");
    p.setProperty("MINDELAY", "" + minDelay); // in milliseconds
    p.setProperty("MAXDELAY", "" + maxDelay); // in milliseconds
    p.setProperty("protocol.tr.transport", "urt");
    p.setProperty("protocol.tr", UnreliableTransport.class.getName());
    p.setProperty("protocol.tr.drop", "DROP");
    // drop is a probability, 0<=DROP<=1
    p.setProperty("DROP", String.valueOf(dropRate));
    String protocols = "avg";
    switch (Common.currentConfiguration.churnModel.type) {
      case sessionLengthAddOnFailure:
      case sessionLengthInterArrival:
      case sessionLengthOffLength:
      case eventBased:
        p.setProperty("protocol.add", NodeAdder.class.getName());
        p.setProperty("protocol.add.protocol", "avg");
        p.setProperty("protocol.add.at", "0");
        protocols += " add";
        break;
      case availabilityFile:
        p.setProperty("protocol.add", ExperimentPlayer.class.getName());
        p.setProperty("protocol.add.protocol", "avg");
        p.setProperty("protocol.add.step", "" + Common.currentConfiguration.cycleLength);
        protocols += " add";
        break;
      case none: // do nothing
        break;
      default:
        throw new RuntimeException("unhandled case for " + Common.currentConfiguration.churnModel.type);
    }
    // ################### initialization ======================
    p.setProperty("init.lin", OneNodeInitialiser.class.getName());
    p.setProperty("init.lin.protocol", "avg");
    p.setProperty("init.sch", "CDScheduler");
    p.setProperty("init.sch.protocol", protocols);
    // p.setProperty("init.sch.randstart","");
    Configuration.setConfig(p);
  }
  
  @Override public void commenceSingleRun(final int run) {
    MyGeneralNode.resetIDs();
    CommonState.initializeRandom(Common.currentSeed);
    EDSimulator.nextExperiment();
    Network.reset();
  }
  
  @Override public boolean isSimulator() {
    return true;
  }
  
  @Override public int getMaxNodes() {
    return Integer.MAX_VALUE;
  }
  
  @Override public String toXml(final String prefix) {
    final StringBuilder sb = new StringBuilder();
    sb.append(prefix + "<framework value=\"" + getClass().getSimpleName() + "\" dropRate=\"" + dropRate + "\" minDelay=\""
        + minDelay + "\" maxDelay=\"" + maxDelay + "\" />");
    return sb.toString();
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(dropRate);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + (int) (maxDelay ^ (maxDelay >>> 32));
    result = prime * result + (int) (minDelay ^ (minDelay >>> 32));
    return result;
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PeerSimFramework other = (PeerSimFramework) obj;
    if (Double.doubleToLongBits(dropRate) != Double.doubleToLongBits(other.dropRate)) {
      return false;
    }
    if (maxDelay != other.maxDelay) {
      return false;
    }
    if (minDelay != other.minDelay) {
      return false;
    }
    return true;
  }
}
