package reactive;

import static reactive.Cts.RECOMMENDEDACTIONSTRING;
import static reactive.Cts.SUMMARYSTRING;

import java.io.FileWriter;
import java.io.IOException;

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

public class DummyAgent implements ReactiveBehavior {

	private Topology topology;

	// For debugging purposes
	private static boolean DEBUG = false;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		
		// Choose action based on advice
		Action action = null;

		// if task available take it
		if (availableTask != null) {
			action = new Pickup(availableTask);
		} else { // otherwise go to closest city
			double minDist = Integer.MAX_VALUE;
			City minCity = null;
			for (City c : vehicle.getCurrentCity().neighbors()) {
				double dist = vehicle.getCurrentCity().distanceTo(c);
				if ( dist < minDist) {
					minDist = dist;
					minCity = c;
				}
			}
			action = new Move(minCity);
		}
		
		return action;
	}
}
