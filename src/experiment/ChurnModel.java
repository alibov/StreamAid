package experiment;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import utils.distributions.Distribution;
import entites.NodeAvailability;

public class ChurnModel {
  public enum Type {
    none, sessionLengthInterArrival, sessionLengthAddOnFailure, sessionLengthOffLength, availabilityFile, eventBased
  }
  
  public static class Event {
    public enum Type {
      addition, departure
    }
    
    public final Type type;
    public final int amount;
    public final int time;
    
    public Event(final Type type, final int amount, final int time) {
      this.type = type;
      this.amount = amount;
      this.time = time;
    }
    
    public String toXml() {
      return "<event type=\"" + type + "\" time=\"" + time + "\" amount=\"" + amount + "\"/>";
    }
  }
  
  public Map<Integer, Event> eventMap = new TreeMap<Integer, ChurnModel.Event>();
  public final Type type;
  private Distribution sessionLengthDistribution = null;
  private Distribution interArrivalDistribution = null;
  private Distribution offLengthDistribution = null;
  private Map<Integer, NodeAvailability> nodeAvailability = null;
  private String availabilityFile;
  public Random r;
  
  public ChurnModel(final Type type) {
    this.type = type;
  }
  
  public void setRandom(final Random r) {
    this.r = r;
  }
  
  public Distribution getSessionLengthDistribution() {
    return sessionLengthDistribution;
  }
  
  public void setSessionLengthDistribution(final Distribution sessionLengthDistribution) {
    if (this.sessionLengthDistribution != null) {
      throw new RuntimeException("sessionLengthDistribution can be set only once");
    }
    if (type.equals(Type.none) || type.equals(Type.availabilityFile)) {
      throw new RuntimeException("can't set sessionLengthDistribution for type " + type);
    }
    this.sessionLengthDistribution = sessionLengthDistribution;
  }
  
  public Distribution getInterArrivalDistribution() {
    return interArrivalDistribution;
  }
  
  public void setInterArrivalDistribution(final Distribution interArrivalDistribution) {
    if (this.interArrivalDistribution != null) {
      throw new RuntimeException("interArrivalDistribution can be set only once");
    }
    if (!type.equals(Type.sessionLengthInterArrival)) {
      throw new RuntimeException("can't set interArrivalDistribution for type " + type);
    }
    this.interArrivalDistribution = interArrivalDistribution;
  }
  
  public Distribution getOffLengthDistribution() {
    return offLengthDistribution;
  }
  
  public void setOffLengthDistribution(final Distribution offLengthDistribution) {
    if (this.offLengthDistribution != null) {
      throw new RuntimeException("offLengthDistribution can be set only once");
    }
    if (!type.equals(Type.sessionLengthOffLength)) {
      throw new RuntimeException("can't set offLengthDistribution for type " + type);
    }
    this.offLengthDistribution = offLengthDistribution;
  }
  
  public Map<Integer, NodeAvailability> getNodeAvailability() {
    return nodeAvailability;
  }
  
  public void setNodeAvailability(final Map<Integer, NodeAvailability> nodeAvailability) {
    if (this.nodeAvailability != null) {
      throw new RuntimeException("nodeAvailability can be set only once");
    }
    if (!type.equals(Type.availabilityFile)) {
      throw new RuntimeException("can't set nodeAvailability for type " + type);
    }
    this.nodeAvailability = nodeAvailability;
  }
  
  public void setAvailabilityFile(final String availabilityFile) {
    if (this.availabilityFile != null) {
      throw new RuntimeException("availabilityFile can be set only once");
    }
    if (!type.equals(Type.availabilityFile)) {
      throw new RuntimeException("can't set availabilityFile for type " + type);
    }
    this.availabilityFile = availabilityFile;
  }
  
  public String toXml(final String prefix) {
    final StringBuilder sb = new StringBuilder();
    sb.append(prefix + "<churnModel type=\"" + type + "\">\n");
    switch (type) {
      case sessionLengthAddOnFailure:
        sb.append(prefix + "\t<sessionLengthDistribution>\n");
        sb.append(prefix + "\t\t" + sessionLengthDistribution.toXml());
        sb.append(prefix + "\t</sessionLengthDistribution>\n");
        break;
      case sessionLengthOffLength:
        sb.append(prefix + "\t<sessionLengthDistribution>\n");
        sb.append(prefix + "\t\t" + sessionLengthDistribution.toXml());
        sb.append(prefix + "\t</sessionLengthDistribution>\n");
        sb.append(prefix + "\t<offLengthDistribution>\n");
        sb.append(prefix + "\t\t" + offLengthDistribution.toXml());
        sb.append(prefix + "\t</offLengthDistribution>\n");
        break;
      case availabilityFile:
        sb.append(prefix + "\t<availabilityFile value=\"" + availabilityFile + "\" />\n");
        break;
      case none:
        break;
      case sessionLengthInterArrival:
        sb.append(prefix + "\t<sessionLengthDistribution>\n");
        sb.append(prefix + "\t\t" + sessionLengthDistribution.toXml());
        sb.append(prefix + "\t</sessionLengthDistribution>\n");
        sb.append(prefix + "\t<interArrivalDistribution>\n");
        sb.append(prefix + "\t\t" + interArrivalDistribution.toXml());
        sb.append(prefix + "\t</interArrivalDistribution>\n");
        break;
      case eventBased:
        for (final Event event : eventMap.values()) {
          sb.append(prefix + "\t" + event.toXml() + "\n");
        }
        break;
      default:
        throw new RuntimeException("unhandled case for " + type);
    }
    sb.append(prefix + "</churnModel>");
    return sb.toString();
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((availabilityFile == null) ? 0 : availabilityFile.hashCode());
    result = prime * result + ((interArrivalDistribution == null) ? 0 : interArrivalDistribution.hashCode());
    result = prime * result + ((nodeAvailability == null) ? 0 : nodeAvailability.hashCode());
    result = prime * result + ((offLengthDistribution == null) ? 0 : offLengthDistribution.hashCode());
    result = prime * result + ((sessionLengthDistribution == null) ? 0 : sessionLengthDistribution.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    final ChurnModel other = (ChurnModel) obj;
    if (availabilityFile == null) {
      if (other.availabilityFile != null) {
        return false;
      }
    } else if (!availabilityFile.equals(other.availabilityFile)) {
      return false;
    }
    if (interArrivalDistribution == null) {
      if (other.interArrivalDistribution != null) {
        return false;
      }
    } else if (!interArrivalDistribution.equals(other.interArrivalDistribution)) {
      return false;
    }
    if (nodeAvailability == null) {
      if (other.nodeAvailability != null) {
        return false;
      }
    } else if (!nodeAvailability.equals(other.nodeAvailability)) {
      return false;
    }
    if (offLengthDistribution == null) {
      if (other.offLengthDistribution != null) {
        return false;
      }
    } else if (!offLengthDistribution.equals(other.offLengthDistribution)) {
      return false;
    }
    if (sessionLengthDistribution == null) {
      if (other.sessionLengthDistribution != null) {
        return false;
      }
    } else if (!sessionLengthDistribution.equals(other.sessionLengthDistribution)) {
      return false;
    }
    if (type != other.type) {
      return false;
    }
    return true;
  }
  
  public void addEvent(final Event event) {
    if (!type.equals(Type.eventBased)) {
      throw new RuntimeException("can't add event for type " + type);
    }
    if (eventMap.containsKey(event.time)) {
      throw new RuntimeException("can't add two events for time " + event.time);
    }
    eventMap.put(event.time, event);
  }
}
