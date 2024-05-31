package tools;

import java.util.*;
import java.util.logging.*;
import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class QLearner extends Artifact {

  private Lab lab; // the lab environment that will be learnt 
  private int stateCount; // the number of possible states in the lab environment
  private int actionCount; // the number of possible actions in the lab environment
  private HashMap<Integer, double[][]> qTables; // a map for storing the qTables computed for different goals

  private static final Logger LOGGER = Logger.getLogger(QLearner.class.getName());

  public void init(String environmentURL) {

    // the URL of the W3C Thing Description of the lab Thing
    this.lab = new Lab(environmentURL);

    this.stateCount = this.lab.getStateCount();
    LOGGER.info("Initialized with a state space of n="+ stateCount);

    this.actionCount = this.lab.getActionCount();
    LOGGER.info("Initialized with an action space of m="+ actionCount);

    qTables = new HashMap<>();
  }

/**
* Computes a Q matrix for the state space and action space of the lab, and against
* a goal description. For example, the goal description can be of the form [z1level, z2Level],
* where z1Level is the desired value of the light level in Zone 1 of the lab,
* and z2Level is the desired value of the light level in Zone 2 of the lab.
* For exercise 11, the possible goal descriptions are:
* [0,0], [0,1], [0,2], [0,3], 
* [1,0], [1,1], [1,2], [1,3], 
* [2,0], [2,1], [2,2], [2,3], 
* [3,0], [3,1], [3,2], [3,3].
*
*<p>
* HINT: Use the methods of {@link LearningEnvironment} (implemented in {@link Lab})
* to interact with the learning environment (here, the lab), e.g., to retrieve the
* applicable actions, perform an action at the lab during learning etc.
*</p>
* @param  goalDescription  the desired goal against the which the Q matrix is calculated (e.g., [2,3])
* @param  episodesObj the number of episodes used for calculating the Q matrix
* @param  alphaObj the learning rate with range [0,1].
* @param  gammaObj the discount factor [0,1]
* @param epsilonObj the exploration probability [0,1]
* @param rewardObj the reward assigned when reaching the goal state
**/
  @OPERATION
  public void calculateQ(Object[] goalDescription , Object episodesObj, Object alphaObj, Object gammaObj, Object epsilonObj, Object rewardObj) {
    
    // ensure that the right datatypes are used
    Integer episodes = Integer.valueOf(episodesObj.toString());
    Double alpha = Double.valueOf(alphaObj.toString());
    Double gamma = Double.valueOf(gammaObj.toString());
    Double epsilon = Double.valueOf(epsilonObj.toString());
    Integer reward = Integer.valueOf(rewardObj.toString());


    // Convert goalDescription elements to Integer
    Integer goalZ1 = ((Number) goalDescription[0]).intValue();
    Integer goalZ2 = ((Number) goalDescription[1]).intValue();
    List<Integer> intGoalDescription = Arrays.asList(goalZ1, goalZ2);


    // Initialize the Q-Table
    double[][] qTable = initializeQTable();
    
    // Initialize GUI
    QLearningVisualizer visualizer = new QLearningVisualizer("Q-Learning Dashboard");
    int step = 0;


    // Main Q-Learning loop
    for (int e = 0; e < episodes; e++) {
        // Randomize initial state by performing a random action
        lab.performAction((int) (Math.random() * actionCount));
        int state = lab.readCurrentState();
      
        for (int stepCount = 0; stepCount < 100; stepCount++) { // assuming a max of 1000 steps per episode
            // Choose action using epsilon-greedy policy
            int action;
            if (Math.random() < epsilon) {
                action = (int) (Math.random() * actionCount);
            } else {
                action = getMaxQAction(qTable, state);
            }

            // Perform action and get reward and next state
            lab.performAction(action);
            int nextState = lab.readCurrentState();
            double immediateReward = getImmediateReward(nextState, goalDescription, reward);

            // Update Q-Value
            double oldQValue = qTable[state][action];
            double maxQValueNextState = getMaxQValue(qTable, nextState);
            qTable[state][action] = oldQValue + alpha * (immediateReward + gamma * maxQValueNextState - oldQValue);

            // Update Q-Value
            updateQTable(qTable, state, action, immediateReward, nextState, alpha, gamma);

            // Update visualization
            List<Integer> currentStateDesc = lab.getStateDescription(state);
            visualizer.update(step, currentStateDesc, intGoalDescription, action, immediateReward);
            visualizer.updateQTable(qTable);
            step++;

            // Transition to next state
            state = nextState;
            LOGGER.info("Transition to state: " + state);
            
            // Check if goal state is reached
            if (isGoalState(nextState, goalDescription)) {
                LOGGER.info("Goal reached: " + goalDescription);
                break;
            }
        }
    }

    int goalHash = Arrays.hashCode(goalDescription);
    qTables.put(goalHash, qTable);

    printQTable(qTable);
    //visualizer.updateQTable(qTable);
}


public void calculateQ(String goalDescriptionStr, int episodes, double alpha, double gamma, double epsilon, int reward) {
    Object[] goalDescription = parseGoalDescription(goalDescriptionStr);
    calculateQ(goalDescription, episodes, alpha, gamma, epsilon, reward);
}

private Object[] parseGoalDescription(String goalDescriptionStr) {
    String[] parts = goalDescriptionStr.replace("[", "").replace("]", "").split(",");
    Object[] goalDescription = new Object[parts.length];
    for (int i = 0; i < parts.length; i++) {
        goalDescription[i] = Integer.parseInt(parts[i].trim());
    }
    return goalDescription;
}

  private int getMaxQAction(double[][] qTable, int state) {
      double maxQ = Double.NEGATIVE_INFINITY;
      int action = -1;
      for (int a = 0; a < qTable[state].length; a++) {
          if (qTable[state][a] > maxQ) {
              maxQ = qTable[state][a];
              action = a;
          }
      }
      return action;
  }


  private void updateQTable(double[][] qTable, int state, int action, double reward, int nextState, double alpha, double gamma) {
    double oldQValue = qTable[state][action];
    double maxQValueNextState = getMaxQValue(qTable, nextState);
    qTable[state][action] = oldQValue + alpha * (reward + gamma * maxQValueNextState - oldQValue);
  }

  private double getMaxQValue(double[][] qTable, int state) {
      double maxQ = Double.NEGATIVE_INFINITY;
      for (double q : qTable[state]) {
          if (q > maxQ) {
              maxQ = q;
          }
      }
      return maxQ;
  }

  private boolean isGoalState(int state, Object[] goalDescription) {
    List<Integer> currentState = lab.getStateDescription(state);
    boolean isGoal = currentState.get(0).equals(goalDescription[0]) && currentState.get(1).equals(goalDescription[1]);
    if (isGoal) {
        LOGGER.info("State " + state + " matches goal state: " + Arrays.toString(goalDescription));
    }
    return isGoal;
}




private double getImmediateReward(int state, Object[] goalDescription, int reward) {
  if (isGoalState(state, goalDescription)) {
      LOGGER.info("Goal state reached: " + Arrays.toString(goalDescription) + " at state: " + state);
      return reward;
  }
  return -1;
}

/**
* Returns information about the next best action based on a provided state and the QTable for
* a goal description. The returned information can be used by agents to invoke an action 
* using a ThingArtifact.
*
* @param  goalDescription  the desired goal against the which the Q matrix is calculated (e.g., [2,3])
* @param  currentStateDescription the current state e.g. [2,2,true,false,true,true,2]
* @param  nextBestActionTag the (returned) semantic annotation of the next best action, e.g. "http://example.org/was#SetZ1Light"
* @param  nextBestActionPayloadTags the (returned) semantic annotations of the payload of the next best action, e.g. [Z1Light]
* @param nextBestActionPayload the (returned) payload of the next best action, e.g. true
**/
  @OPERATION
  public void getActionFromState(Object[] goalDescription, Object[] currentStateDescription,
      OpFeedbackParam<String> nextBestActionTag, OpFeedbackParam<Object[]> nextBestActionPayloadTags,
      OpFeedbackParam<Object[]> nextBestActionPayload) {
        Integer goalZ1 = ((Number) goalDescription[0]).intValue();
    Integer goalZ2 = ((Number) goalDescription[1]).intValue();
    int goalHash = Arrays.hashCode(goalDescription);

    double[][] qTable = qTables.get(goalHash);
    if (qTable == null) {
        failed("Q-Table for goal state not found.");
        return;
    }


    int currentState = lab.getStateIndex(currentStateDescription);
    int bestAction = getMaxQAction(qTable, currentState);

    switch (bestAction) {
        case 0:
            nextBestActionTag.set("http://example.org/was#SetZ1Light");
            nextBestActionPayloadTags.set(new Object[]{"Z1Light"});
            nextBestActionPayload.set(new Object[]{true});
            break;
        case 1:
            nextBestActionTag.set("http://example.org/was#SetZ1Light");
            nextBestActionPayloadTags.set(new Object[]{"Z1Light"});
            nextBestActionPayload.set(new Object[]{false});
            break;
        case 2:
            nextBestActionTag.set("http://example.org/was#SetZ2Light");
            nextBestActionPayloadTags.set(new Object[]{"Z2Light"});
            nextBestActionPayload.set(new Object[]{true});
            break;
        case 3:
            nextBestActionTag.set("http://example.org/was#SetZ2Light");
            nextBestActionPayloadTags.set(new Object[]{"Z2Light"});
            nextBestActionPayload.set(new Object[]{false});
            break;
        case 4:
            nextBestActionTag.set("http://example.org/was#SetZ1Blinds");
            nextBestActionPayloadTags.set(new Object[]{"Z1Blinds"});
            nextBestActionPayload.set(new Object[]{true});
            break;
        case 5:
            nextBestActionTag.set("http://example.org/was#SetZ1Blinds");
            nextBestActionPayloadTags.set(new Object[]{"Z1Blinds"});
            nextBestActionPayload.set(new Object[]{false});
            break;
        case 6:
            nextBestActionTag.set("http://example.org/was#SetZ2Blinds");
            nextBestActionPayloadTags.set(new Object[]{"Z2Blinds"});
            nextBestActionPayload.set(new Object[]{true});
            break;
        case 7:
            nextBestActionTag.set("http://example.org/was#SetZ2Blinds");
            nextBestActionPayloadTags.set(new Object[]{"Z2Blinds"});
            nextBestActionPayload.set(new Object[]{false});
            break;
        default:
            failed("Invalid action index: " + bestAction);
      }
         

    }

    /**
    * Print the Q matrix
    *
    * @param qTable the Q matrix
    */
  void printQTable(double[][] qTable) {
    System.out.println("Q matrix");
    for (int i = 0; i < qTable.length; i++) {
      System.out.print("From state " + i + ":  ");
     for (int j = 0; j < qTable[i].length; j++) {
      System.out.printf("%6.2f ", (qTable[i][j]));
      }
      System.out.println();
    }
  }

  /**
  * Initialize a Q matrix
  *
  * @return the Q matrix
  */
 private double[][] initializeQTable() {
    double[][] qTable = new double[this.stateCount][this.actionCount];
    for (int i = 0; i < stateCount; i++){
      for(int j = 0; j < actionCount; j++){
        qTable[i][j] = 0.0;
      }
    }
    return qTable;
  }
}

