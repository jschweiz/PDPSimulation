package template;

import java.util.HashMap;

import static template.Cts.INITSTRING;
import static template.Cts.TRAINSTRING;
import static template.Cts.FINISHEDSTRING;
import static template.Cts.DEBUGSTRING1;
import static template.Cts.DEBUGSTRING2;
import static template.Cts.DEBUGSTRING3;
import static template.Cts.MAPSTRING;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class OfflineLearningModel {

	HashMap<State,AvailableAction> A;
	HashMap<State,Double> V;

	private static int ITERATIONS = 100;
	private static double INITIALVVALUE = 100000;

	// for debug purposes
	private boolean DEBUG = false;

	public OfflineLearningModel(boolean debug) {
		this.DEBUG = debug;
	}

	public void trainModel(Topology topology, TaskDistribution td, Agent agent, double gamma) {
		System.out.println(INITSTRING);
		initializeStates(topology);

		System.out.println(TRAINSTRING);
		train(td, gamma, agent.vehicles().get(0).costPerKm());

		System.out.println(FINISHEDSTRING);
		printMap(V,A);
	}

	private void initializeStates(Topology topology) {
		this.V = new HashMap<State, Double>();
		this.A = new HashMap<State, AvailableAction>();

		for (City here: topology) {
			for (City packetTo: topology) {
				this.V.put(new State(here, packetTo), INITIALVVALUE);
			}
			this.V.put(new State(here, null), INITIALVVALUE);
		}

		for (State state: this.V.keySet()) {
			this.A.put(state, state.getAvailableActions().getFirst());
		}
		if (DEBUG) printMap(V, A);
	}

	private void train(TaskDistribution td, double gamma, int costPerKm) {
		int subsum;
		double Q, Q_, reward;
		AvailableAction temporaryBestChoice;

		for (int i=0; i<ITERATIONS; i++) { // iterate 
	
			for(State state : this.V.keySet()) {
				
				Q = Double.NEGATIVE_INFINITY;
				temporaryBestChoice = null;

				if (DEBUG && i%10 == 0) System.out.println(String.format(DEBUGSTRING1, state));

				for(AvailableAction action : state.getAvailableActions()) {
					reward = R(state, action, td, costPerKm);
					subsum = 0;
					for(State nextState : this.V.keySet()){ 
						subsum += T(state, action, nextState, td) * V.get(nextState);
					}
					Q_ = reward + gamma * subsum;

					if (DEBUG && i%10 == 0) System.out.println(String.format(DEBUGSTRING2, action, subsum, reward));

					if (Q_ > Q) {
						Q = Q_;
						temporaryBestChoice = action;
					}
				}

				if (DEBUG && i%10 == 0) System.out.println(String.format(DEBUGSTRING3, temporaryBestChoice));
				this.V.put(state, Q);
				this.A.put(state, temporaryBestChoice);
			}
		}
	}

	private double R(State state, AvailableAction action, TaskDistribution td, int costPerKm) {
		double r = -state.getCity().distanceTo(action.getDestination()) * costPerKm;
		if (action.isDelivery() && state.getPackageDestination().equals(action.getDestination())) {
			r = td.reward(state.getCity(), state.getPackageDestination());
		}
		return r;
	}

	private double T(State state, AvailableAction action, State nextState, TaskDistribution td){
		City destination = action.getDestination();
		if (!destination.equals(nextState.getCity()))
			return 0;
		return td.probability(destination, nextState.getPackageDestination());
	}

	public AvailableAction getBestActionChoice(State currentState) {
		return this.A.get(currentState);
	}

	private static void printMap(HashMap<State, Double> V, HashMap<State, AvailableAction> A) {
		for (State state : V.keySet()) {
			if (!A.get(state).isDelivery() && state.getPackageDestination() != null && state.isValidPacket())
				System.out.println(String.format(MAPSTRING, state, A.get(state), V.get(state)));
		}
	}
}
