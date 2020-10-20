package template;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class State {

	// variables defining the state 
	private City currentCity;
	private Map<City, List<Task>> tasksInVehicule;
	private Map<City, List<Task>> tasksRemaining;

	// variables not defining the state
	private static Vehicle vehicule;
	private double costToReach; // to sort the priorityqueue
	private int currentVehicleWeight; // used to check weight easily
	private int numberOfTasksRemaining;

	private double estimatedCostThroughState;


	// init state constructor
	public State(Vehicle v, TaskSet taskSet) {
		vehicule = v;
		this.currentCity = v.getCurrentCity();
		this.tasksInVehicule = new HashMap<Topology.City,List<Task>>();
		this.tasksRemaining =  new HashMap<Topology.City,List<Task>>();
		this.costToReach = 0;
		this.currentVehicleWeight = 0;
		this.numberOfTasksRemaining = 0;
		this.estimatedCostThroughState = 0;

		for (Task t : taskSet) {
			if (!this.tasksRemaining.containsKey(t.pickupCity)) {
				this.tasksRemaining.put(t.pickupCity, new LinkedList<Task>());
			}
			this.tasksRemaining.get(t.pickupCity).add(t);
			this.numberOfTasksRemaining++;
		}
	}
	// copy clone state constructor
	public State(City c, Map<City, List<Task>> v, Map<City, List<Task>> r,
			double co, int w, int tr) {
		this.estimatedCostThroughState = 0;
		this.currentCity = c;
		this.tasksInVehicule = v;
		this.tasksRemaining = r;
		this.costToReach = co;
		this.currentVehicleWeight = w;
		this.numberOfTasksRemaining = tr;
	}


	// function for state transition

	public void pickupTask(Task t) {
		// add the task in the vehicle
		if (!this.tasksInVehicule.containsKey(t.deliveryCity)) {
			this.tasksInVehicule.put(t.deliveryCity, new LinkedList<Task>());
		}
		this.tasksInVehicule.get(t.deliveryCity).add(t);
		this.currentVehicleWeight += t.weight;

		// remove it from the map as it was taken
		this.tasksRemaining.get(t.pickupCity).remove(t);
		if (this.tasksRemaining.get(t.pickupCity).isEmpty()) {
			this.tasksRemaining.remove(t.pickupCity);
		}
		this.numberOfTasksRemaining--;
	}

	public void deliverTask(Task t) {
		// remove the task from the vehicle
		this.tasksInVehicule.get(t.deliveryCity).remove(t);
		if (this.tasksInVehicule.get(t.deliveryCity).isEmpty()) {
			this.tasksInVehicule.remove(t.deliveryCity);
		}
		this.currentVehicleWeight -= t.weight;
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
					if (t.weight + this.currentVehicleWeight <= vehicule.capacity()) {
						list.add(new Transition(Transition.Action.PICKUP, t));
					}
				}
			}

			// just add neightbors to possible transitions
			for (City c : currentCity.neighbors()) {
				list.add(new Transition(Transition.Action.MOVE, c, currentCity.distanceTo(c) * vehicule.costPerKm()));
			}
		}
		return list;
	}


	// Clone state for transitions

	public State clone() {
		Map<City, List<Task>> l1 = copyMap(tasksInVehicule);
		Map<City, List<Task>> l2 = copyMap(tasksRemaining);
		State newState = new State(currentCity, l1, l2 ,costToReach,
				currentVehicleWeight, numberOfTasksRemaining);
		return newState;
	}

	static private Map<City, List<Task>> copyMap(Map<City, List<Task>> p) {
		Map<City, List<Task>> newMap = new HashMap<Topology.City,List<Task>>();
		for (City c : p.keySet()) {
			newMap.put(c, new LinkedList<Task>());
			for (Task t : p.get(c)) {
				newMap.get(c).add(t);
			}
		}
		return newMap;
	}


	// no task left on map
	public boolean allPacketsAreDelivered() {
		return tasksInVehicule.isEmpty() && this.numberOfTasksRemaining == 0;
	}


	// for priorityqueue

	public static class SortByCost implements Comparator<State> {
		public int compare(State a, State b) {
			return Double.compare(a.getCostToReach(), b.getCostToReach());
		}
	}

	public static class SortByEstimatedCost implements Comparator<State> {
		public int compare(State a, State b) {
			return Double.compare(a.costThroughState(), b.costThroughState());
		}
	}

	public void setCostToReach(double c) {
		this.costToReach = c;
	}

	
	// g(n) : cost to reach this state
	public double getCostToReach() {
		return costToReach;
	}

	// h(n) : heuristic. Estimate cost to reach a goal state from current state
	public double minCostToGoal() { 
		return findValueMST();
	}

	// f(n) : estimate cost to go from initial to a goal state passing through this state
	public double costThroughState() {
		// return this.getCostToReach() + this.minCostToGoal();
		if (this.estimatedCostThroughState != 0)
			return this.estimatedCostThroughState;
		this.estimatedCostThroughState = this.getCostToReach() + this.minCostToGoal();
		return this.estimatedCostThroughState;
	}


	// hashmaps and displays

	@Override
	public String toString() {
		return toStringwH() + "[" + hashCode() + "]";
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
		String string = "{" + currentCity.toString() + complete + ";";

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

	// Find cost of least significant tree (lower bound of h(n))
	public double findValueMST(){
		HashSet<City> graph = new HashSet<City>();

		// Identify subgraph of cities yet to be visited
		graph.add(currentCity);
		for (Map.Entry<City, List<Task>> entry : tasksInVehicule.entrySet()) {
			if (!entry.getValue().isEmpty())
				graph.add(entry.getKey());
		}
		for (List<Task> remainingTaskList : tasksRemaining.values()) {
			for (Task t : remainingTaskList){
				graph.add(t.pickupCity);
				graph.add(t.deliveryCity);
			}
		}

		// Map int to city
		int numberNodes = graph.size();
		List<City> nodes = new ArrayList<City>(graph);
		UnionFind unionfind = new UnionFind(numberNodes);
		List<Edge> edges = new ArrayList<Edge>();
		
		// Sort edges in ascending order
		for (int i = 0; i < numberNodes-1; i++) {
			for (int j = i+1; j < numberNodes; j++) {
				double distance = nodes.get(i).distanceTo(nodes.get(j));
				edges.add(new Edge(i, j, distance));
			}
		}
		edges.sort(new Edge.SortDistance());

		double totalCost = 0;
		for (Edge e : edges){
			int a = e.getA();
			int b = e.getB();
			if (unionfind.find(a) != unionfind.find(b)){
				totalCost += e.getDistance();
				unionfind.union(a, b);
			}
		}
		return totalCost;
	}

}
