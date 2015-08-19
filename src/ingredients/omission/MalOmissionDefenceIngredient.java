package ingredients.omission;

import java.util.Random;

import messages.Message;
import messages.OmissionDefense.BlameMessage;
import messages.OmissionDefense.DefendMessage;
import messages.OmissionDefense.OmissionRequestMessage;
import utils.Common;
import experiment.frameworks.NodeAddress;

public class MalOmissionDefenceIngredient extends OmissionDefenceIngredient {
  public MalOmissionDefenceIngredient(final int numOfCycles, final int thresholdForMisses, final int roundsForDiffusion,
      final int roundsForAnswer, final Random r) {
    super(numOfCycles, thresholdForMisses, roundsForDiffusion, roundsForAnswer, r);
  }
  
  @Override public String getMessageTag() {
    return alg.getMessageTag() + "-" + "OmissionDefenceBehavior";
  }
  
  @Override public void nextCycle() {
    // Do nothing
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof OmissionRequestMessage) {
      final OmissionRequestMessage request = (OmissionRequestMessage) message;
      final NodeAddress nodeToCheck = request.nodeToCheck;
      final int myGroup = Common.currentConfiguration.getNodeGroup(client.network.getAddress().toString());
      final int nodeToCheckGroup = Common.currentConfiguration.getNodeGroup(nodeToCheck.toString());
      // The case where nodeToCheck is a colluding node, and therefore I should
      // lie for his good.
      if (myGroup == nodeToCheckGroup) {
        client.network.send(new DefendMessage(getMessageTag(), client.network.getAddress(), message.sourceId, nodeToCheck));
        return;
      }
      // The case where nodeToCheck is not from my group and I should lie so the
      // verifying neighbor will disconnect from him.
      client.network.send(new BlameMessage(getMessageTag(), client.network.getAddress(), message.sourceId, nodeToCheck));
    }
  }
}
