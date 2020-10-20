package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;


public class PlanMaker {

	public static boolean DEBUG = false;
	private static String SEPARATOR = "==========================================================================================";
	private static String PRINT1 = "\n\n" + SEPARATOR + "\nCURRENT STATE: %s reached by %s";
	

	// FUNCTION TO RUN ALGORITHMS

	private static List<TransitionList> runBFS(State initialState) {

		Queue<State> Q = new LinkedList<State>();
		Set<State> C = new HashSet<State>();

		Map<State,TransitionList> pathTo = new HashMap<State,TransitionList>();
		List<TransitionList> validPaths = new LinkedList<TransitionList>();

		Q.add(initialState);
		pathTo.put(initialState, new TransitionList());

		while (!Q.isEmpty()) {
			// get first element
			State state = Q.poll();
			TransitionList pathToState = pathTo.get(state);
			if (DEBUG) System.out.println(String.format(PRINT1, state, pathToState));
			
			// if state is final (= all packets have been delivered)
			if (state.allPacketsAreDelivered()) {
				validPaths.add(pathToState);
			}

			// if state not visited yet
			if (!C.contains(state)) {
				C.add(state);

				// get all possible transitions
				List<Transition> possibleTransitions = state.getTransitions();
				if (DEBUG) printTransitions(possibleTransitions);

				for (Transition t : possibleTransitions) {
					State nextState = t.getNextState(state);

					// add to list to visit
					Q.add(nextState);

					// clone the path to this state and add transition to next
					TransitionList pathToNextState = pathToState.clone();
					pathToNextState.add(t);
					pathTo.put(nextState, pathToNextState);
				}

				if (DEBUG) printStates(Q, C, pathTo);
			}
		}
		return validPaths;
	}

	private static List<Transition> findBestPath(List<TransitionList> validPaths) {
		TransitionList bestPath = null;
		int bestCost = Integer.MAX_VALUE;
		for (TransitionList tl : validPaths) {
			int interCost = tl.getCost();
			if (interCost < bestCost) {
				bestCost = interCost;
				bestPath = tl;
			}
		}
		if (bestPath == null) {
			return null;
		}
		return bestPath.getList();
	}


	// FUNCTION TO GENERATE PLANS

	public static Plan processBFSPlan(Vehicle vehicle, TaskSet tasks) {
		State initialState = new State(vehicle, tasks);

		long startTime = System.nanoTime();

		// run BFS to find less costly path to final state
		List<TransitionList> validPaths = runBFS(initialState);
		List<Transition> bestPath = findBestPath(validPaths);

		long duration = System.nanoTime() - startTime;
		long heapSize = Runtime.getRuntime().totalMemory(); 

		System.out.println("Calculation time: " + duration/1000000
				+ "ms using " + heapSize/1000000 + " mb");

		if (bestPath == null) return null;
		
		// tranform list of actions to plan
		Plan plan = new Plan(vehicle.getCurrentCity());
		for (Transition t : bestPath) {
			switch(t.getAction()) {
				case PICKUP:
					plan.appendPickup(t.getTask());
					break;
				case DELIVER:
					plan.appendDelivery(t.getTask());
					break;
				case MOVE:
					plan.appendMove(t.getDestination());
					break;
			}
		}
		return plan;
	}

	private static List<Transition> runASTAR(State initialState) {
		PriorityQueue<State> Q = new PriorityQueue<State>(new State.SortByEstimatedCost());
		Set<State> C = new HashSet<State>();
		Map<State, Double> C_costs = new HashMap<State, Double>(); // Cost of state last time it was visited

		Map<State,TransitionList> pathTo = new HashMap<State,TransitionList>();

		Q.add(initialState);
		pathTo.put(initialState, new TransitionList());

		while (!Q.isEmpty()) {
			// get first element
			State state = Q.poll();
			TransitionList pathToState = pathTo.get(state);
			if (DEBUG) System.out.println(String.format(PRINT1, state, pathToState));
			
			// if state is final (= all packets have been delivered)
			if (state.allPacketsAreDelivered()) {
				System.out.println("Visited " + pathTo.size() + " states.");
				return pathToState.getList();
			}

			// if state not visited yet
			if (!C.contains(state) || state.getCostToReach() < C_costs.get(state) ) {
				C.add(state);
				C_costs.put(state, state.getCostToReach());

				// get all possible transitions
				List<Transition> possibleTransitions = state.getTransitions();
				if (DEBUG) printTransitions(possibleTransitions);

				for (Transition t : possibleTransitions) {
					State nextState = t.getNextState(state);

					// add to list to visit
					Q.add(nextState);

					// clone the path to this state and add transition to next
					TransitionList pathToNextState = pathToState.clone();
					pathToNextState.add(t);
					pathTo.put(nextState, pathToNextState);
				}

				if (DEBUG) printStates(Q, C, pathTo);
			}
		}
		return null;
	}

	public static Plan processASTARPlan(Vehicle vehicle, TaskSet tasks) {
		State initialState = new State(vehicle, tasks);
		long startTime = System.nanoTime();

		// run BFS to find less costly path to final state
		List<Transition> bestPath = runASTAR(initialState);

		long duration = System.nanoTime() - startTime;
		long heapSize = Runtime.getRuntime().totalMemory(); 

		System.out.println("Calculation time: " + duration/1000000
				+ "ms using " + heapSize/1000000 + " mb");

		if (bestPath == null) return null;
		
		// tranform list of actions to plan
		Plan plan = new Plan(vehicle.getCurrentCity());
		for (Transition t : bestPath) {
			switch(t.getAction()) {
				case PICKUP:
					plan.appendPickup(t.getTask());
					break;
				case DELIVER:
					plan.appendDelivery(t.getTask());
					break;
				case MOVE:
					plan.appendMove(t.getDestination());
					break;
			}
		}
		return plan;
	}

	public static Plan processNAIVEPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}


	// Setters

	public static void setDEBUG(boolean b) {
		DEBUG = b;
	}

	// PRINTING FOR DEBUG

	public static void printTransitions(List<Transition> list) {
		System.out.println("\nPOSSIBLE TRANSITIONS");
		for (Transition t : list) {
			System.out.println("-----> " + t);
		}
	}

	public static void printStates(Queue<State> Q, Set<State> C, Map<State,TransitionList> P) {
		System.out.println("\nC STATES:");
		for (State s : C) {
			System.out.println("------------> " + s);
		}

		System.out.println("\nQ STATES:");
		for (State s : Q) {
			System.out.println("------------> " + s);
		}

		System.out.println("\nPATHTO STATES:");
		for (State s : P.keySet()) {
			System.out.println("------------> " + s + "       " +  P.get(s));
		}
	}
}
