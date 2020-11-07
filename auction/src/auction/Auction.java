package auction;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Auction implements AuctionBehavior {
	// General variables 
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private long timeout_bid;

	// Bid relative variables
	private List<Task> wonTasks;
	private Bider bider;
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		// Get timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);

		// Save parameters
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;

		// Bid-relative
		wonTasks = new LinkedList<Task>();
		bider = new Bider(topology, distribution, agent);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			wonTasks.add(previous);
			bider.addTask(previous);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		long bid = bider.proposeTask(task);
		return bid;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		VariableSet finalState = bider.getVS();
		List<Plan> plans = convertVariableSet(vehicles, finalState);
		return plans;
	}

	public List<Plan> convertVariableSet(List<Vehicle> vehicles, VariableSet vs) {
        List<Plan> plans = new ArrayList<Plan>();

        for (int i = 0; i < vehicles.size(); i++) {
            List<TaskStep> executedTaskStep = vs.getTaskStepVehicle(i);
            List<Action> actions = new ArrayList<Action>();
            City currentCity = vehicles.get(i).getCurrentCity();

            for (TaskStep ts : executedTaskStep) {
                City destination = TaskStep.getInvolvedCity(ts.getMapId());
                for (City c : currentCity.pathTo(destination)) {
                    actions.add(new Action.Move(c));
                }
                if (ts.isPickup) actions.add(new Action.Pickup(ts.t));
                else actions.add(new Action.Delivery(ts.t));

                currentCity = destination;
            }

            Plan plan = new Plan(vehicles.get(i).getCurrentCity(), actions);
            plans.add(plan);
        }
        return plans;
    }
}