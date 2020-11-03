package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
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
public class CentralizedTemplate implements CentralizedBehavior {

    // general parameters for model
    public static final int DEBUG = 0;
    public static final double P = 0.3;
    public static final int MAX_ITERATIONS = 5000;
    public static final int DEPTH_SEARCH = 8;

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        // convert TaskSet to List<Task> as used in the code structure
        int nTasks = tasks.size();
        List<Task> taskList = new ArrayList<Task>(nTasks);
        for (Task t : tasks) 
            taskList.add(t);

        System.out.println("Starting computing best plan...");

        CPMaker.setParameters(P, MAX_ITERATIONS, timeout_plan / 1000 - 1, DEBUG); // take 1 sec of margin
        VariableSet finalState = CPMaker.runSLS(vehicles, taskList);

        long time_stop = System.currentTimeMillis();
        System.out.println("Plan computed in : " + (time_stop-time_start) + " ms");

        System.out.println("Final state : ");
        System.out.println(finalState);
        
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

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
