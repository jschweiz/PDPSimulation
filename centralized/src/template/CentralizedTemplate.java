package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
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

        
        // test

        System.out.println("Testing VariableSet structure");
        VariableSet A = new VariableSet(vehicles, tasks);
        // System.out.println(A);

        CPMaker pm = new CPMaker();

        System.out.println(pm.runSLS(vehicles, tasks));

        // VariableSet abis = pm.changingVehicle(A,0,1);

        // System.out.println(A.compare(A));

        // System.out.println("There is " + N.size() + " possible new states");

        // System.out.println(del + ":" + del.getMapId() + "     -----    " + pic);

        // System.out.println("=================>" + A.validChange(del.getMapId(), pic.getMapId()));

        // for (VariableSet v : N) {
        //     System.out.println(v);
        // }

        
        // VariableSet newA = pm.changingTaskOrder(set, 0, 1, 3);
        // TaskStep pick0 = new TaskStep(null, 0, true);
        // TaskStep deliv0 = new TaskStep(null, 0, false);
        // TaskStep pick1 = new TaskStep(null, 1, true);
        // TaskStep deliv1 = new TaskStep(null, 1, false);
        // TaskStep pick3 = new TaskStep(null, 3, true);
        // TaskStep deliv3 = new TaskStep(null, 3, false);

        // System.out.println(pick0 + "" + pick1 +  set.validChange(pick0.getMapId(),pick1.getMapId()));
        // System.out.println(pick0 + "" + deliv1 +  set.validChange(pick0.getMapId(),deliv1.getMapId()));
        // System.out.println(deliv0 + "" + pick1 +  set.validChange(deliv0.getMapId(),pick1.getMapId()));
        // System.out.println(deliv0 + "" + pick3 +  set.validChange(deliv0.getMapId(),pick3.getMapId()));
        // System.out.println(deliv0 + "" + pick1 +  set.validChange(deliv0.getMapId(),pick1.getMapId()));
        // set.changingTaskOrder(0, deliv0.getMapId(), pick3.getMapId());
        // System.out.println(set);

        // System.out.println(set);

        return null;



        
// //		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
//         Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

//         List<Plan> plans = new ArrayList<Plan>();
//         plans.add(planVehicle1);
//         while (plans.size() < vehicles.size()) {
//             plans.add(Plan.EMPTY);
//         }
        
//         long time_end = System.currentTimeMillis();
//         long duration = time_end - time_start;
//         System.out.println("The plan was generated in " + duration + " milliseconds.");
        
//         return plans;
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
