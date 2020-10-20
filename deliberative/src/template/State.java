package template;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {

	// variables defining the state 
	private City currentCity;
	private HashSet<Task> tasksInVehicule;
	private HashSet<Task> tasksRemaining;

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
		this.tasksInVehicule = new HashSet<Task>();
		this.tasksRemaining =  new HashSet<Task>();
		this.costToReach = 0;
		this.currentVehicleWeight = 0;
		this.numberOfTasksRemaining = 0;
		this.estimatedCostThroughState = 0;

		for (Task t : taskSet) {
			tasksRemaining.add(t);
			this.numberOfTasksRemaining++;
		}

		for (Task t : v.getCurrentTasks()) {
			tasksInVehicule.add(t);
			this.currentVehicleWeight += t.weight;
		}
	}
	// copy clone state constructor
	public State(City c, HashSet<Task> v, HashSet<Task> r,
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
		this.tasksInVehicule.add(t);
		this.currentVehicleWeight += t.weight;

		// remove it from the map as it was taken
		this.tasksRemaining.remove(t);
		this.numberOfTasksRemaining--;
	}

	public void deliverTask(Task t) {
		// remove the task from the vehicle
		this.tasksInVehicule.remove(t);
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
		for (Task t : tasksInVehicule) {
			if (t.deliveryCity.equals(currentCity)){
				list.add(new Transition(Transition.Action.DELIVER, t));
				return list;
			}
		}

		for (Task t : tasksRemaining) {
			if (t.pickupCity.equals(currentCity) &&  (t.weight + this.currentVehicleWeight <= vehicule.capacity() ) ) {
				list.add(new Transition(Transition.Action.PICKUP, t));
			}
		}

		for (City c : currentCity.neighbors()) {
			list.add(new Transition(Transition.Action.MOVE, c, currentCity.distanceTo(c) * vehicule.costPerKm()));
		}

		return list;
	}


	// Clone state for transitions

	public State clone() {
		HashSet<Task> l1 = new HashSet<Task>();
		l1.addAll(tasksInVehicule);
		HashSet<Task> l2 = new HashSet<Task>();
		l2.addAll(tasksRemaining);
		State newState = new State(currentCity, l1, l2 ,costToReach,
				currentVehicleWeight, numberOfTasksRemaining);
		return newState;
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
		// return findValueMST();
		return maxDistance();
	}

	// f(n) : estimate cost to go from initial to a goal state passing through this state
	public double costThroughState() {
		// return this.getCostToReach();
		if (this.estimatedCostThroughState != 0)
			return this.estimatedCostThroughState;
		this.estimatedCostThroughState = this.getCostToReach() + this.minCostToGoal();
		return this.estimatedCostThroughState;
	}


	// hashmaps and displays

	@Override
	public String toString() {
		return "nuuuuul";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof State)) return false;
		State s = (State) o;

		if (!s.currentCity.equals(this.currentCity)){
			return false;
		}
		
		if (!s.tasksInVehicule.equals(this.tasksInVehicule))
			return false;

		if (!s.tasksRemaining.equals(this.tasksRemaining))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int h = 0;
		h += currentCity.hashCode();
		
		for (Task t : tasksInVehicule){
			h += t.toString().hashCode();
		}
		for (Task t : tasksRemaining){
			h += 3*t.toString().hashCode();
		}
		return h;
	}

	// Other heuristic h(n)
	public double maxDistance() {

		double maxDistance = 0;
		// Identify subgraph of cities yet to be visited
		for (Task t : tasksInVehicule) {
			double d = currentCity.distanceTo(t.deliveryCity);
			if (d > maxDistance)
				maxDistance = d;
		}
		
		for (Task t : tasksRemaining) {
			double d = t.pickupCity.distanceTo(t.deliveryCity);
			d += currentCity.distanceTo(t.pickupCity);
			if (d > maxDistance)
				maxDistance = d;
		}
		return maxDistance;
	}

	// Find cost of least significant tree (lower bound of h(n))
	public double findValueMST(){
		HashSet<City> graph = new HashSet<City>();

		// Identify subgraph of cities yet to be visited
		graph.add(currentCity);
		for (Task t : tasksInVehicule)
			graph.add(t.deliveryCity);
		
		for (Task t : tasksInVehicule){
			graph.add(t.deliveryCity);
			graph.add(t.pickupCity);
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
