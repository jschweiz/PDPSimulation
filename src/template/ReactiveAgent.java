package template;

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

	private double p[][];
	private int r[][];

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.5);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.tasksDelivered = 0;
		this.myAgent = agent;
		
		int numCities = topology.size();
		this.p = new double[numCities][numCities];
		this.r = new int[numCities][numCities];
		construct_p_r(this.p, this.r, topology, td, agent);

		printDouble("P", this.p);
		printInt("R", this.r);
	}

	public static void printInt(String name, int [][] p) {
		System.out.println("Printing matrix " + name);
		for (int [] d: p) {
			for(int dd: d) {
				System.out.print(" " + dd);
			}
			System.out.println("");
		}
	}

	public static void printDouble(String name, double [][] p) {
		System.out.println("Printing matrix " + name);
		for (double [] d: p) {
			for(double dd: d) {
				System.out.print(" " + dd);
			}
			System.out.println("");
		}
	}

	public static void construct_p_r(double [][] p, int [][] r, Topology topo, TaskDistribution td, Agent agent) {
		int numCities = topo.size();
		int iterator = 0;
		for (City from: topo) {
			for (City to: topo) {
				p[iterator/numCities][iterator%numCities] = td.probability(from, to);
				//double travelPrice = from.distanceTo(to) * agent.vehicles().get(0).costPerKm();
				r[iterator/numCities][iterator%numCities] = td.reward(from, to);// - travelPrice;
				System.out.println(td.reward(from, to));
				iterator++;
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
			System.out.println("Flemme de prendre le paquet");
		} else {
			action = new Pickup(availableTask);
			System.out.println("Ok je vais le livrer");
			tasksDelivered++;
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "
					+myAgent.getTotalProfit()+" (average profit: "
					+(myAgent.getTotalProfit() / (double)numActions)+") (tasks delivered: "
					+tasksDelivered+")");
		}
		numActions++;
		
		return action;
	}
}
