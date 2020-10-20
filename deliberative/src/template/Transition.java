package template;

import logist.task.Task;
import logist.topology.Topology.City;

public class Transition  {

	public enum Action { PICKUP, DELIVER, MOVE }

	double cost;
	Action action;
	City destination;
	Task task;

	// init for move transitions
	public Transition(Action a, City dest, double cost) {
		this.action = a;
		this.destination = dest;
		this.task = null;
		this.cost = cost;
	}

	// init for pickup and deliver transitions
	public Transition(Action a, Task t) {
		this.action = a;
		this.destination = null;
		this.task = t;
		this.cost = 0;
	}

	// return a new state by cloning it and making changes according to the transition
	public State getNextState(State s) {
		State newState = s.clone();

		switch (action) {
			case PICKUP:
				newState.pickupTask(this.task);
				break;
			case DELIVER:
				newState.deliverTask(this.task);
				break;
			case MOVE:
				newState.move(this.destination, cost);
				break;
		}
		return newState;
	}


	// tostring function for printing
	public String toString() {
		if ( action == Action.MOVE) {
			return "(MOVETO" + this.destination.toString() + ":"+ this.cost+")";
		}
		return "(" + action.toString() + ":" + task.deliveryCity.toString()+":"+ this.cost+")";
	}


	// getters
	
	public Action getAction() {
		return action;
	}

	public Task getTask() {
		return task;
	}

	public City getDestination() {
		return destination;
	}

	
}
	
