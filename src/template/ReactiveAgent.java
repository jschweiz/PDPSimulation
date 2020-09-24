package template;

import static template.Cts.RECOMMENDEDACTIONSTRING;
import static template.Cts.SUMMARYSTRING;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveAgent implements ReactiveBehavior {

	private OfflineLearningModel model;

	private Agent agent;
	private int numActions;
	private int tasksDelivered;

	// For debugging purposes
	private static boolean DEBUG = true;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		double discount =  agent.readProperty("discount-factor", Double.class, 0.95);

		this.agent = agent;
		this.numActions = 0;
		this.tasksDelivered = 0;
		
		this.model = new OfflineLearningModel(DEBUG);
		this.model.trainModel(topology, td, agent, discount);
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		
		// Find the current state
		City potentialDelivery = (availableTask == null) ? null : availableTask.deliveryCity;
		State currentState = new State(vehicle.getCurrentCity(), potentialDelivery);

		// Give advice based on the state
		AvailableAction bestAction = this.model.getBestActionChoice(currentState);
		if (DEBUG) System.out.println(String.format(RECOMMENDEDACTIONSTRING, currentState, bestAction));

		// Choose action based on advice
		Action action = null;
		if (bestAction.isDelivery()) {
			action = new Pickup(availableTask);
		} else {
			action = new Move(bestAction.getDestination());
		}
		
		if (numActions >= 1) {
			System.out.println(String.format(SUMMARYSTRING, numActions, agent.getTotalProfit(),
					agent.getTotalProfit() / agent.getTotalDistance(), tasksDelivered));
		}
		// update values
		if (action instanceof Pickup) tasksDelivered++;
		numActions++;
		
		return action;
	}
}
