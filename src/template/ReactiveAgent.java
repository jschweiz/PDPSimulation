package template;

import java.security.DrbgParameters.NextBytes;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

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

	private Random random;
	private double pPickup;
	private int numActions;
	private int tasksDelivered;
	private Agent myAgent;

	private Double discount;
	private Topology topology;
	private TaskDistribution td;
	private Agent agent;
	private LinkedList<State> states;

	HashMap<State, Double> trained;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		this.discount = agent.readProperty("discount-factor", Double.class, 0.5);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.tasksDelivered = 0;
		this.myAgent = agent;
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		int numCities = topology.size();
		this.states = new LinkedList<State>();
		for (City city: topology) {
			this.states.add(new State(city));
		}

		this.trained = train();
	}

	private HashMap<State, Double> train() {
		HashMap<State, Double> V = new HashMap<State,Double>();
		int subsum;
		double Q = Double.NEGATIVE_INFINITY, Q_;

		// Arbitrary initialisation
		for(State state : this.states)
			V.put(state, random.nextDouble() * 100000);


		for (int i=0; i<100; i++) {
			printV(V);
			for(State state : this.states) {
				for(AvailableAction action : state.getAvailableActions()) {
					subsum = 0;
					for(State nextState : this.states){ 
						subsum += T(state, action, nextState) * V.get(nextState);
					}
					Q_ = R(state, action) + this.discount * subsum;

					if (Q_ > Q)
						Q = Q_;
				}
				V.put(state, Q);
			}
		}
		printP();
		printR();
		return V;
	}

	private double R(State state, AvailableAction action) {
		double r = 0;
		City origin = state.getCity(), destination;
		double sumProba = 0;
		if(action.isPickup()) {
			for(State nextState : this.states){
				destination = nextState.getCity();
				r += td.probability(origin, destination) * (td.reward(origin, destination) - origin.distanceTo(destination) * this.agent.vehicles().get(0).costPerKm());
			}
			for(City city : this.topology){
				sumProba += this.td.probability(origin, city);
			}
			r /= sumProba;

		}
		else {
			destination = action.getNextState().getCity();
			r = -origin.distanceTo(destination) * this.agent.vehicles().get(0).costPerKm();
		}
		return r;
	}

	private double T(State state, AvailableAction action, State nextState){
		double t = 0;

		if(action.isPickup()) {
			t = this.td.probability(state.getCity(), nextState.getCity());
		}
		else {
			t = (action.getNextState().equals(nextState)) ? 1 : 0;
		}
		return t;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = null;

		State currentState = new State(vehicle.getCurrentCity());
		double Q;
		double maxQ = Double.NEGATIVE_INFINITY;
		LinkedList<AvailableAction> actions = currentState.getAvailableActions();
		if (availableTask == null)
			actions.remove(new AvailableAction(null));

		State nextState;
		System.out.println("Vehicule in "+currentState+". Possible actions are: ");
		for (AvailableAction possibleAction : currentState.getAvailableActions()){
			System.out.println("-"+possibleAction);
			nextState = (possibleAction.isPickup()) ? new State(availableTask.deliveryCity) : possibleAction.getNextState();
			Q = R(currentState, possibleAction) + discount * trained.get(nextState);
			if (possibleAction.isPickup())
				Q += availableTask.reward;
			if (Q > maxQ){
				maxQ = Q;
				if (possibleAction.isPickup()) {
					action = new Pickup(availableTask);
				} else {
					action = new Move(nextState.getCity());
				}
			}
		}

		// if (availableTask == null || random.nextDouble() > pPickup) {
		// 	City currentCity = vehicle.getCurrentCity();
		// 	action = new Move(currentCity.randomNeighbor(random));
		// 	System.out.println("Flemme de prendre le paquet");
		// } else {
		// 	action = new Pickup(availableTask);
		// 	System.out.println("Ok je vais le livrer");
		// 	tasksDelivered++;
		// }

		
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "
					+myAgent.getTotalProfit()+" (average profit: "
					+(myAgent.getTotalProfit() / myAgent.getTotalDistance())+") (tasks delivered: "
					+tasksDelivered+")");
		}
		if (action instanceof Pickup) 
			tasksDelivered++;
		numActions++;
		
		return action;
	}

	public void printV(HashMap<State, Double> map) {
		System.out.println("Map is:");
		for (State s : map.keySet()) {
			System.out.println("City: " + s.getCity().toString() + " has a value of: " + map.get(s));
		}
	}

	public void printR() {
		DecimalFormat df = new DecimalFormat("#.#####");
		df.format(0.912385);		
		System.out.println("Reward map is:");
		for (City from: topology) {
			for (City to: topology) {
				System.out.printf(df.format(td.reward(from, to)) + " ");
			}
			System.out.println("");
		}
	}

	public void printP() {
		DecimalFormat df = new DecimalFormat("#.##");
		df.format(0.912385);
		System.out.println("Probability map is:");
		for (City from: topology) {
			for (City to: topology) {
				if (from.equals(to))
					System.out.printf(df.format(td.probability(from, to)) + "    ");
				else
					System.out.printf(df.format(td.probability(from, to)) + " ");
			}
			System.out.println("");
		}
	}


}
