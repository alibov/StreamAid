package messages;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

import java.io.Serializable;

import utils.Common;

/**
 * message class. all messages sent in the system must inherit from this class
 * 
 * @author Alexander Libov
 * 
 */
public abstract class Message implements Serializable, Sizeable {
  /**
	 * 
	 */
  private static final long serialVersionUID = 1872768435984476742L;
  public final String tag;
  public NodeAddress sourceId;
  public final NodeAddress destID;
  
  @Override public long getSimulatedSize() {
    return Integer.SIZE + NodeAddress.SIZE * 2;
  }
  
  public Message(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    this.tag = tag;
    this.sourceId = sourceId;
    this.destID = destID;
  }
  
  @Override public String toString() {
    return tag + "-" + this.getClass().getSimpleName() + ", " + Common.currentConfiguration.getNodeGroup(sourceId.toString())
        + "->" + Common.currentConfiguration.getNodeGroup(destID.toString()) + "," + sourceId + "->" + destID + ", "
        + getContents();
  }
  
  abstract protected String getContents();
  
  abstract public boolean isOverheadMessage();
  
  public void updateSourceID(final NodeAddress newSource) {
    sourceId = newSource;
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((destID == null) ? 0 : destID.hashCode());
    result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
    result = prime * result + ((tag == null) ? 0 : tag.hashCode());
    result = prime * result + ((getContents() == null) ? 0 : getContents().hashCode());
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
    final Message other = (Message) obj;
    return toString().equals(other.toString());
  }
}
