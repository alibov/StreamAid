package experiment.frameworks;

/**
 * Framework for running experiments The setting are taken from the common class
 * 
 * @author Alexander Libov
 * 
 */
public interface P2PFramework {
  public void initFramework();
  
  public void commenceSingleRun(int run) throws Exception;
  
  public boolean isSimulator();
  
  public int getMaxNodes();
  
  public String toXml(String prefix);
  
  @Override public int hashCode();
  
  @Override public boolean equals(Object obj);
}
