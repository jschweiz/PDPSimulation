package template;

import java.security.DrbgParameters.NextBytes;
import java.util.HashMap;
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

	HashMap<City, Double> trained;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		this.discount = agent.readProperty("discount-factor", Double.class,
				0.5);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.tasksDelivered = 0;
		this.myAgent = agent;
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		int numCities = topology.size();
		this.trained = train();
	}

	private HashMap<City, Double> train() {
		HashMap<City, Double> V = new HashMap<City,Double>();
		int subsum;
		double Q, Q_;

		// Arbitrary initialisation
		for(City state : topology)
			V.put(state, 1.0);


		for (int i=0; i<1000; i++) {
			for(City state : topology){
				// Q if action == "take packet"
				subsum = 0;
				for(City nextState : topology) 
					subsum += T(state, null, nextState) * V.get(nextState);
				Q = R(state, null) + this.discount * subsum;

				// Q if action == "Go to one of the neighboring cities"
				for(City action : state.neighbors()) {
					subsum = 0;
					for(City nextState : topology) 
						subsum += T(state, action, nextState) * V.get(nextState);
					Q_ = R(state, action) + this.discount * subsum;

					if (Q_ > Q)
						Q = Q_;
				}
				V.put(state, Q);
			}
		}
		return V;
	}

	private double R(City state, City action) {
		double r = 0;
		if(action == null) {
			for(City nextState : this.topology){
				r += td.probability(state, nextState) * (td.reward(state, nextState) - state.distanceTo(nextState) * this.agent.vehicles().get(0).costPerKm());
			}
		}
		else {
			r = -state.distanceTo(action) * this.agent.vehicles().get(0).costPerKm();
		}
		return r;
	}

	private double T(City state, City action, City nextState){
		double t = 0;

		if(action == null) {
			t = this.td.probability(state, nextState);
		}
		else {
			t = (action.equals(nextState)) ? 1 : 0;
		}
		return t;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = null;

		City currentCity = vehicle.getCurrentCity();
		double Q;
		double maxQ = Double.NEGATIVE_INFINITY;
		for (City nextCity : currentCity.neighbors()){
			Q = R(currentCity, nextCity) + discount * trained.get(nextCity);
			if (Q > maxQ){
				maxQ = Q;
				action = new Move(nextCity);
			}
		}

		if (availableTask != null) {
			Q = R(currentCity, availableTask.deliveryCity) + availableTask.reward + discount * trained.get(availableTask.deliveryCity);
			if (Q > maxQ){
				maxQ = Q;
				action = new Pickup(availableTask);
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
		numActions++;
		
		return action;
	}
}
