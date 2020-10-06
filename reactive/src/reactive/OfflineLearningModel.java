package reactive;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import cern.colt.list.adapter.DoubleListAdapter;

import static reactive.Cts.INITSTRING;
import static reactive.Cts.TRAINSTRING;
import static reactive.Cts.FINISHEDSTRING;
import static reactive.Cts.DEBUGSTRING1;
import static reactive.Cts.DEBUGSTRING2;
import static reactive.Cts.DEBUGSTRING3;
import static reactive.Cts.MAPSTRING;
import static reactive.Cts.MAPWRITESTRING;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class OfflineLearningModel {

	private HashMap<State,AvailableAction> policy;
	private HashMap<State,Double> potentialRewards;
	private TaskDistribution td;
	private Agent agent;
	private double gamma;

	private static int ITERATIONS = 10000;
	private static double INITIALVVALUE = 100000;

	// for debug purposes
	private boolean DEBUG = false;

	public OfflineLearningModel(boolean debug) {
		this.DEBUG = debug;
	}
 
	public OfflineLearningModel(boolean debug, int iterations) {
		this.DEBUG = debug;
		ITERATIONS = iterations;
	}

	public void trainModel(Topology topology, TaskDistribution td, Agent agent, double gamma) {
		this.gamma = gamma;
		this.td = td;
		this.agent = agent;
		System.out.println(INITSTRING);
		initializeStates(topology);

		System.out.println(TRAINSTRING);
		train(td, gamma, agent.vehicles().get(0).costPerKm());

		System.out.println(FINISHEDSTRING);
		printMap(potentialRewards,policy);

		writeModelToFile(gamma);
	}

	private void initializeStates(Topology topology) {
		this.potentialRewards = new HashMap<State, Double>();
		this.policy = new HashMap<State, AvailableAction>();

		for (City here: topology) {
			for (City packetTo: topology) {
				this.potentialRewards.put(new State(here, packetTo), INITIALVVALUE);
			}
			this.potentialRewards.put(new State(here, null), INITIALVVALUE);
		}

		for (State state: this.potentialRewards.keySet()) {
			this.policy.put(state, state.getAvailableActions().getFirst());
		}
		if (DEBUG) printMap(potentialRewards, policy);
	}

	private void train(TaskDistribution td, double gamma, int costPerKm) {
		int subsum;
		double Q, Q_, reward;
		AvailableAction temporaryBestChoice;

		for (int i=0; i<ITERATIONS; i++) { // iterate 
	
			for(State state : this.potentialRewards.keySet()) {
				
				Q = Double.NEGATIVE_INFINITY;
				temporaryBestChoice = null;

				if (DEBUG && i%10 == 0) System.out.println(String.format(DEBUGSTRING1, state));

				for(AvailableAction action : state.getAvailableActions()) {
					reward = R(state, action, td, costPerKm);
					subsum = 0;
					for(State nextState : this.potentialRewards.keySet()){ 
						subsum += T(state, action, nextState, td) * potentialRewards.get(nextState);
					}
					Q_ = reward + gamma * subsum;

					if (DEBUG && i%10 == 0) System.out.println(String.format(DEBUGSTRING2, action, subsum, reward));

					if (Q_ > Q) {
						Q = Q_;
						temporaryBestChoice = action;
					}
				}

				if (DEBUG && i%10 == 0) System.out.println(String.format(DEBUGSTRING3, temporaryBestChoice));
				this.potentialRewards.put(state, Q);
				this.policy.put(state, temporaryBestChoice);
			}
		}
	}

	private double R(State state, AvailableAction action, TaskDistribution td, int costPerKm) {
		double r = -state.getCity().distanceTo(action.getDestination()) * costPerKm;
		if (action.isDelivery() && state.getPackageDestination().equals(action.getDestination())) {
			r += td.reward(state.getCity(), state.getPackageDestination());
		}
		return r;
	}

	private double T(State state, AvailableAction action, State nextState, TaskDistribution td){
		City destination = action.getDestination();
		if (!destination.equals(nextState.getCity()))
			return 0;
		return td.probability(destination, nextState.getPackageDestination());
	}

	public AvailableAction getBestActionChoice(State currentState, long reward) {
		Double V = Double.MIN_VALUE;
		AvailableAction best= null;
		for (AvailableAction a : currentState.getAvailableActions()) {
			double subsum = 0;
			for(State nextState : this.potentialRewards.keySet()){ 
				subsum += T(currentState, a, nextState, td) * potentialRewards.get(nextState);
			} 
			double r = gamma*subsum;
			r -= currentState.getCity().distanceTo(a.getDestination()) * agent.vehicles().get(0).costPerKm();
			if (a.isDelivery() && currentState.getPackageDestination().equals(a.getDestination())) {
				r += reward;
			}

			if (r > V){
				V = r;
				best = a;
			}
		}
		return best;
	}

	private static void printMap(HashMap<State, Double> potentialRewards, HashMap<State, AvailableAction> policy) {
		for (State state : potentialRewards.keySet()) {
			if (!policy.get(state).isDelivery() && state.getPackageDestination() != null && state.isValidPacket())
				System.out.println(String.format(MAPSTRING, state, policy.get(state), potentialRewards.get(state)));
		}
	}

	private void writeModelToFile(double gamma)  {
		try {
		FileWriter myWriter = new FileWriter("logs/" + gamma + "model.txt");
		for (State state : potentialRewards.keySet()) {
			myWriter.write(String.format(MAPWRITESTRING, state, policy.get(state)));
		}
		myWriter.close();
		} catch (IOException e) {
		System.out.println("An error occurred.");
		e.printStackTrace();
		}
	}
}
