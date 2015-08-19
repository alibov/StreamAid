package bandits.reward;

import interfaces.NodeConnectionAlgorithm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeMap;

import messages.Message;
import messages.RewardMessage;
import modules.P2PClient;
import utils.Utils;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instance;
import weka.core.Instances;

public class InfoRepTreeGather extends RewardIngredient {
  private final ArrayList<RewardIngredient> rewards;
  private final int timeout;
  int playSeconds = 0;
  double totalWeightedReward;
  int totalPlaySeconds;
  Instances data;
  // Classifier pullClassifier;
  // Classifier pushPullClassifier;
  Classifier classifier;
  TreeMap<Long, ArrayList<Double>> perRoundReward = new TreeMap<Long, ArrayList<Double>>();
  TreeMap<Long, Integer> perRoundSeconds = new TreeMap<Long, Integer>();
  
  public InfoRepTreeGather(final ArrayList<RewardIngredient> rewards, final int timeout) {
    super(null);
    this.rewards = rewards;
    this.timeout = timeout;
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    for (final RewardIngredient reward : rewards) {
      reward.setServerMode();
    }
    /* pullClassifier = initClassifier("pull100.arff"); pushPullClassifier =
     * initClassifier("pushpull100.arff"); */
    classifier = initClassifier("all100.arff");
  }
  
  private Classifier initClassifier(final String file) {
    try {
      Classifier retVal;
      final BufferedReader br = new BufferedReader(new FileReader(file));
      data = new Instances(br);
      data.setClassIndex(data.numAttributes() - 1);
      /* REPTree rt = new REPTree(); rt.setMaxDepth(-1); rt.setMinNum(2.0);
       * rt.setMinVarianceProp(0.001); rt.setNoPruning(false);
       * rt.setNumFolds(3); rt.setSeed(1); classifier = rt; */
      final LinearRegression lr = new LinearRegression();
      retVal = lr;
      retVal.buildClassifier(data);
      br.close();
      return retVal;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final NodeConnectionAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    for (final RewardIngredient reward : rewards) {
      alg.addIngredient(reward, client);
      reward.startNewRound();
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (Utils.getMovieTime() <= 0 || client.player.getVs() == null) {
      return;
    }
    if (!client.isServerMode()) {
      playSeconds++;
      if (Utils.getRound() % timeout != 0) {
        return;
      }
      final ArrayList<Double> rewardsList = new ArrayList<Double>();
      for (final RewardIngredient reward : rewards) {
        rewardsList.add(reward.getReward());
        reward.startNewRound();
      }
      client.network.send(new RewardMessage(getMessageTag(), client.network.getAddress(), client.network.getServerNode(),
          rewardsList, client.player.currentConfNumber, playSeconds, Utils.getRound()));
      playSeconds = 0;
    } else {
      // final StreamingModule sm = ((AdaptiveStreaming)
      // alg).streamingModules.getLast();
      // classifier = sm instanceof PushPull ? pushPullClassifier :
      // pullClassifier;
      if (Utils.getRound() % timeout == 2 && !perRoundSeconds.isEmpty()) {
        System.out.print("perRoundReward, " + perRoundSeconds.lastEntry().getKey() + ", ");
        for (int i = 0; i < perRoundReward.lastEntry().getValue().size(); ++i) {
          final double rewardVal = perRoundReward.lastEntry().getValue().get(i) / perRoundSeconds.lastEntry().getValue();
          System.out.print(rewardVal + ", ");
        }
        System.out.println(perRoundSeconds.lastEntry().getValue());
      }
    }
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof RewardMessage) {
      if (client.player.currentConfNumber != ((RewardMessage) message).currentConfNumber) {
        // return;
      }
      final RewardMessage msg = (RewardMessage) message;
      Utils.checkExistence(perRoundSeconds, msg.round, 0);
      perRoundSeconds.put(msg.round, perRoundSeconds.get(msg.round) + msg.playSeconds);
      if (!perRoundReward.containsKey(msg.round)) {
        perRoundReward.put(msg.round, new ArrayList<Double>());
        for (int rewardID = 0; rewardID < msg.rewardsList.size(); ++rewardID) {
          perRoundReward.get(msg.round).add(0.0);
        }
      }
      final ArrayList<Double> arr = perRoundReward.get(msg.round);
      for (int rewardID = 0; rewardID < msg.rewardsList.size(); ++rewardID) {
        arr.set(rewardID, arr.get(rewardID) + msg.rewardsList.get(rewardID) * msg.playSeconds);
      }
    }
  }
  
  @Override public void startNewRound() {
    for (final RewardIngredient reward : rewards) {
      reward.startNewRound();
    }
    for (final ArrayList<Double> arr : perRoundReward.values()) {
      arr.clear();
    }
    perRoundSeconds.clear();
  }
  
  @Override public double getReward() {
    final Instance inst = new Instance(data.numAttributes());
    inst.setDataset(data);
    int i = 0;
    double averageCI = 0.0;
    double averageCR = 0.0;
    double averageCRA = 0.0;
    int rounds = 0;
    // int skip = 3;
    int skip = 0;
    for (final Long round : perRoundReward.keySet()) {
      final ArrayList<Double> arr = perRoundReward.get(round);
      if (!arr.isEmpty()) {
        skip--;
        if (skip >= 0) {
          continue;
        }
        averageCI += arr.get(2) / perRoundSeconds.get(round);
        averageCRA += arr.get(1) / perRoundSeconds.get(round);
        averageCR += arr.get(0) / perRoundSeconds.get(round);
        rounds++;
      }
      for (int rewardID = 0; rewardID < arr.size(); ++rewardID) {
        inst.setValue(i, arr.get(rewardID) / perRoundSeconds.get(round));
        ++i;
      }
    }
    for (; i < inst.numAttributes() - 1; ++i) {
      inst.setValue(i, inst.value(i - 3));
    }
    try {
      // return ((0.1 * averageCRA) + (0.9 * averageCR)) / rounds;
      return classifier.classifyInstance(inst);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
