package ingredients;

import interfaces.AlgorithmComponent;
import interfaces.NodeConnectionAlgorithm;

import java.util.Arrays;
import java.util.Random;

import logging.TextLogger;
import modules.P2PClient;
import utils.Utils;

abstract public class AbstractIngredient<T extends NodeConnectionAlgorithm> implements AlgorithmComponent {
  protected P2PClient client;
  protected T alg;
  protected Random r;
  private long lastRound = Long.MIN_VALUE;
  private String lastStackTrace = "";
  
  // private int confNumber;
  public AbstractIngredient(final Random r) {
    this.r = r;
  }
  
  public void setClientAndComponent(final P2PClient client, final T alg) {
    this.alg = alg;
    this.client = client;
    addListener();
    if (r != null) {
      TextLogger.log(client.network.getAddress(), getMessageTag() + " initialized with " + Utils.getRandomSeed(r) + "\n");
    } else {
      TextLogger.log(client.network.getAddress(), getMessageTag() + " initialized with null\n");
    }
  }
  
  public String getMessageTag() {
    return alg.getMessageTag() + "-" + this.getClass().getSimpleName();
  }
  
  protected void assertServerStatus(final String error) {
    if (!client.isServerMode()) {
      throw new RuntimeException(error);
    }
  }
  
  @Override public void nextCycle() {
    if (Utils.getRound() == lastRound) {
      throw new RuntimeException("next cycle called twice!\n first here: \n" + lastStackTrace);
    }
    lastRound = Utils.getRound();
    lastStackTrace = Arrays.toString(Thread.currentThread().getStackTrace());
  }
  
  public void deactivate() {
    TextLogger.log(client.network.getAddress(), "deactivating " + getMessageTag() + "\n");
    client.network.removeListener(this, getMessageTag());
    alg.ingredients.remove(this);
  }
  
  public void setServerMode() {
    // Do nothing
  }
  
  public void removeListener() {
    client.network.removeListener(this, getMessageTag());
  }
  
  public void addListener() {
    client.network.addListener(this, getMessageTag());
  }
}
