package interfaces;

import ingredients.AbstractIngredient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logging.TextLogger;
import messages.Message;
import modules.P2PClient;
import modules.network.NetworkModule;
import modules.network.NetworkModule.SendInfo;
import utils.Utils;

public abstract class NodeConnectionAlgorithm implements AlgorithmComponent {
  public int secondsUp = 0;
  public final NetworkModule network;
  public List<AbstractIngredient<? extends NodeConnectionAlgorithm>> ingredients = new LinkedList<AbstractIngredient<? extends NodeConnectionAlgorithm>>();
  public List<AbstractIngredient<? extends NodeConnectionAlgorithm>> ingredientsToRemove = new LinkedList<AbstractIngredient<? extends NodeConnectionAlgorithm>>();
  public List<AbstractIngredient<? extends NodeConnectionAlgorithm>> ingredientsToAdd = new LinkedList<AbstractIngredient<? extends NodeConnectionAlgorithm>>();
  protected Random r;
  private boolean inCycle = false;
  public int confNumber = 0;
  private long lastRound = Long.MIN_VALUE;
  private long lastTime = Long.MIN_VALUE;
  private String lastStackTrace = "";
  
  public void setConfNumber(final int confNumber) {
    network.removeListener(this, getMessageTag());
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> i : ingredients) {
      i.removeListener();
    }
    this.confNumber = confNumber;
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> i : ingredients) {
      i.addListener();
    }
    network.addListener(this, getMessageTag());
  }
  
  public NodeConnectionAlgorithm(final NetworkModule node, final Random r) {
    this.r = r;
    TextLogger.log(node.getAddress(), getMessageTag() + " initialized with " + Utils.getRandomSeed(r) + "\n");
    network = node;
    node.addListener(this, getMessageTag());
  }
  
  public void deactivate() {
    TextLogger.log(network.getAddress(), "deactivating " + getMessageTag() + "\n");
    network.removeListener(this, getMessageTag());
    while (!ingredients.isEmpty()) {
      ingredients.get(0).deactivate();
    }
  }
  
  @Override public void nextCycle() {
    if (Utils.getRound() == lastRound) {
      System.err.println("movie time: " + Utils.getMovieTime());
      System.err.println("lastTime: " + lastTime);
      throw new RuntimeException("next cycle called twice!\n first here: \n" + lastStackTrace);
    }
    lastRound = Utils.getRound();
    lastTime = Utils.getMovieTime();
    lastStackTrace = Arrays.toString(Thread.currentThread().getStackTrace());
    secondsUp++;
    for (final SendInfo si : network.sendInfo.values()) {
      si.time++;
    }
    if (!ingredientsToAdd.isEmpty()) {
      ingredients.addAll(ingredientsToAdd);
      ingredientsToAdd.clear();
    }
    if (!ingredientsToRemove.isEmpty()) {
      ingredients.removeAll(ingredientsToRemove);
      ingredientsToRemove.clear();
    }
    inCycle = true;
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b : ingredients) {
      b.nextCycle();
    }
    inCycle = false;
  }
  
  @Override public void handleMessage(final Message message) {
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b : ingredients) {
      b.handleMessage(message);
    }
  }
  
  public String getMessageTag() {
    return confNumber + "-" + this.getClass().getSimpleName();
  }
  
  public void setServerMode() {
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b : ingredients) {
      b.setServerMode();
    }
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b : ingredientsToAdd) {
      b.setServerMode();
    }
  }
  
  protected void assertServerStatus(final String error) {
    if (!network.isServerMode()) {
      throw new RuntimeException(error);
    }
  }
  
  public <T extends NodeConnectionAlgorithm> void addIngredient(final AbstractIngredient<T> b, final P2PClient client) {
    if (inCycle) {
      ingredientsToAdd.add(b);
    } else {
      ingredients.add(b);
    }
    b.setClientAndComponent(client, (T) this);
  }
  
  public AbstractIngredient<? extends NodeConnectionAlgorithm> getIngredient(
      final Class<? extends AbstractIngredient<? extends NodeConnectionAlgorithm>> class1) {
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b2 : ingredients) {
      if (class1.isAssignableFrom(b2.getClass())) {
        return b2;
      }
    }
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b2 : ingredientsToAdd) {
      if (class1.isAssignableFrom(b2.getClass())) {
        return b2;
      }
    }
    return null;
  }
  
  public List<AbstractIngredient<? extends NodeConnectionAlgorithm>> getIngredients(
      final Class<? extends AbstractIngredient<? extends NodeConnectionAlgorithm>> class1) {
    final List<AbstractIngredient<? extends NodeConnectionAlgorithm>> retVal = new LinkedList<AbstractIngredient<? extends NodeConnectionAlgorithm>>();
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b2 : ingredients) {
      if (class1.isAssignableFrom(b2.getClass())) {
        retVal.add(b2);
      }
    }
    for (final AbstractIngredient<? extends NodeConnectionAlgorithm> b2 : ingredientsToAdd) {
      if (class1.isAssignableFrom(b2.getClass())) {
        retVal.add(b2);
      }
    }
    return retVal;
  }
}
