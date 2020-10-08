package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import logist.plan.Plan;
import logist.task.TaskSet;


public class PlanMaker {

	public static String SEPARATOR = "==========================================================================================";

	private static Map<State, TransitionList> runBFS(State initialState) {

		PriorityQueue<State> Q = new PriorityQueue<State>(new State.SortByCost());
		Set<State> C = new HashSet<State>();

		Map<State,TransitionList> pathTo = new HashMap<State,TransitionList>();
		Map<State, TransitionList> validPaths = new HashMap<State,TransitionList>();

		Q.add(initialState);
		pathTo.put(initialState, new TransitionList());

		while (!Q.isEmpty()) {
			// get first element
			State state = Q.poll();

			
			// System.out.println( "\n\n" +SEPARATOR );
			// System.out.println("CURRENT STATE: " + state + " reached by" + pathTo.get(state));


			List<Transition> possibleTransitions = state.getTransitions();
			// System.out.println("CURRENT STATE: " + state + " reached by" + pathTo.get(state));


			// final node
			if (possibleTransitions.isEmpty() || state.isCompleteState()) {
				System.out.println("FOUNNNNNNNNNNNNNNNNDCOMPLEEEEEETE");
				validPaths.put(state, pathTo.get(state));
				return validPaths;
			}
			if (!C.contains(state)) {
				C.add(state);


				// System.out.println("\nPOSSIBLE TRANSITIONS");
				// for (Transition t : possibleTransitions) {
				// 	System.out.println("-----> " + t);
				// }

				for (Transition t : possibleTransitions) {
					State nextState = t.getNextState(state);

					// add to list to visit
					Q.add(nextState);

					// add how to reach this state
					// System.out.println(pathTo.containsKey(state));
					TransitionList pathToState = pathTo.get(state).clone() ;
					pathToState.add(t);

					pathTo.put(nextState, pathToState);
				}

				// System.out.println("\nC STATES:");
				// for (State s : C) {
				// 	System.out.println("------------> " + s);
				// }

				// System.out.println("\nQ STATES:");
				// for (State s : Q) {
				// 	System.out.println("------------> " + s);
				// }

				// System.out.println("\nPATHTO STATES:");
				// for (State s : pathTo.keySet()) {
				// 	System.out.println("------------> " + s + "       " +  pathTo.get(s));
				// }
				
			}
		}
		return validPaths;
	}

	private static LinkedList<Transition> getLessCostlyPath(Map<State, TransitionList> validPaths) {
		double minCost = Double.MAX_VALUE;
		State minState = null;

		for (State s : validPaths.keySet()) {
			if (s.getCostToReach() < minCost) {
				minCost = s.getCostToReach();
				minState = s;
			}
		}
		TransitionList bestTransitions = validPaths.get(minState);
		return bestTransitions.getList();
	}

	public static Plan calculatesBestPlan(Vehicle vehicle, TaskSet tasks) {
		
		State initialState = new State(vehicle, tasks);

		// run BFS
		Map<State, TransitionList> validPaths = runBFS(initialState);
		// find less costly path
		LinkedList<Transition> bestPath = getLessCostlyPath(validPaths);
		
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
}
