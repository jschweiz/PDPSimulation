package template;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class State {

	private Vehicle vehicule;
	private City currentCity;
	private Map<City, List<Task>> tasksInVehicule;
	private Map<City, List<Task>> tasksRemaining;

	private double costToReach;


	// init state constructor
	public State(Vehicle v, TaskSet taskSet) {
		this.vehicule = v;
		this.currentCity = v.getCurrentCity();
		this.tasksInVehicule = new HashMap<Topology.City,List<Task>>();
		this.tasksRemaining =  new HashMap<Topology.City,List<Task>>();
		this.costToReach = 0;

		for (Task t : taskSet) {
			if (!this.tasksRemaining.containsKey(t.pickupCity)) {
				this.tasksRemaining.put(t.pickupCity, new LinkedList<Task>());
			}
			this.tasksRemaining.get(t.pickupCity).add(t);
		}
	}
	// copy clone state constructor
	public State(Vehicle ve, City c, Map<City, List<Task>> v, Map<City, List<Task>> r, double co) {
		this.vehicule = ve;
		this.currentCity = c;
		this.tasksInVehicule = v;
		this.tasksRemaining = r;
		this.costToReach = co;
	}


	// function for state transition

	public void pickupTask(Task t) {
		// add the task in the vehicle
		if (!this.tasksInVehicule.containsKey(t.deliveryCity)) {
			this.tasksInVehicule.put(t.deliveryCity, new LinkedList<Task>());
		}
		this.tasksInVehicule.get(t.deliveryCity).add(t);

		// remove it from the map as it was taken
		this.tasksRemaining.get(t.pickupCity).remove(t);
		if (this.tasksRemaining.get(t.pickupCity).isEmpty()) {
			this.tasksRemaining.remove(t.pickupCity);
		}
	}

	public void deliverTask(Task t) {
		// remove the task from the vehicle
		this.tasksInVehicule.get(t.deliveryCity).remove(t);
		if (this.tasksInVehicule.get(t.deliveryCity).isEmpty()) {
			this.tasksInVehicule.remove(t.deliveryCity);
		}
	}

	public void move(City dest, double cost) {
		this.currentCity = dest;
		this.costToReach += cost;
	}

	
	// function to get possible transitions

	public List<Transition> getTransitions() {
		List<Transition> list = new LinkedList<Transition>();

		// if vehicule contains a task to this city, then only one action: deliver it
		if (tasksInVehicule.containsKey(currentCity)) {
			list.add(new Transition(Transition.Action.DELIVER, tasksInVehicule.get(currentCity).get(0)));
		} else {
			// if tasks are available, add possible transition to take the task
			if (tasksRemaining.containsKey(currentCity)) {
				for (Task t : tasksRemaining.get(currentCity)) {

					// check if we can pickup the task
					if (t.weight + getVehicleWeight() <= vehicule.capacity()) {
						list.add(new Transition(Transition.Action.PICKUP, t));
					}
				}
			}

			// just add neightbors to possible transitions
			for (City c : currentCity.neighbors()) {
				list.add(new Transition(Transition.Action.MOVE, c,
						currentCity.distanceTo(c) * vehicule.costPerKm()));
			}
		}
		return list;
	}

	public int getVehicleWeight() {
		int tot = 0;
		for (City c : tasksInVehicule.keySet()) {
			for (Task t : tasksInVehicule.get(c)) {
				tot += t.weight;
			}
		}
		return tot;
	}


	// Clone state for transitions

	public State clone() {
		Map<City, List<Task>> l1 = copy(tasksInVehicule);
		Map<City, List<Task>> l2 = copy(tasksRemaining);
		State newState = new State(vehicule, currentCity, l1, l2 ,costToReach);
		return newState;
	}

	static private Map<City, List<Task>> copy(Map<City, List<Task>> p) {
		Map<City, List<Task>> newMap = new HashMap<Topology.City,List<Task>>();
		for (City c : p.keySet()) {
			newMap.put(c, new LinkedList<Task>());
			for (Task t : p.get(c)) {
				newMap.get(c).add(t);
			}
		}
		return newMap;
	}


	public boolean isCompleteState() {
		// no task left on map
		return tasksInVehicule.isEmpty() && tasksRemaining.isEmpty();
	}


	// for priorityqueue

	public static class SortByCost implements Comparator<State> {
		public int compare(State a, State b) {
			return Double.compare(a.getCostToReach(), b.getCostToReach());
		}
	}

	public double getCostToReach() {
		return costToReach;
	}


	// hashmaps and displays

	@Override
	public String toString() {
		return toStringwH() + "["+hashCode()+"]";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof State)) return false;
		State s = (State) o;
		return toStringwH().equals(s.toStringwH());
	}

	@Override
	public int hashCode() {
		return toStringwH().hashCode();
	}

	public String toStringwH() {
		int len = currentCity.toString().length();
		String complete = "";
		for (int i = 0; i< 15 - len; i++) {
			complete += " ";
		}
		String string = "{" + currentCity.toString() + complete+ ";";

		string += "ONMAP:[";
		for (City c: tasksRemaining.keySet()) {
			string += "   AT" + c.toString() + "TO:";
			for (Task t : tasksRemaining.get(c)) {
				string += t.deliveryCity.toString() + ",";
			}
		}

		string += "]   INVEHICLE:[";
		for (City c: tasksInVehicule.keySet()) {
			string += "TO" + c.toString() + ":";
			for (Task t : tasksInVehicule.get(c)) {
				string +=  t.toString() + ",";
			}
		}
		return string + "}";
	}

}