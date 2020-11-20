package common;

import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.task.Task;
import logist.topology.Topology.City;

public class Unused {
    
    // /**
    //  * ESSENTIAL
    //  * Taskset.TASK_LIST is same the object as wonTasks
    //  */
    // private class Computation {
    //     VariableSet v;
    //     List<Task> t;
    //     List<Vehicle> ve;
    //     VariableSet r;
    //     public Computation(VariableSet v, List<Task> t, List<Vehicle> ve) {
    //         this.v = v;
    //         this.t = t;
    //         this.ve = ve;
    //         this.r = null;
    //     }
    //     public Computation copy() {
    //         VariableSet vi = v == null ? null : v.copy();
    //         List<Task> ti = null;
    //         if (t != null) {
    //             ti = new LinkedList<>();
    //             ti.addAll(t);
    //         }
    //         List<Vehicle> vei = new LinkedList<>();
    //         vei.addAll(ve);
    //         return new Computation(vi, ti, vei);
    //     }
        
    // }

    // public void getResult(Computation c) {
    //     c.r = CPMaker.run(c.ve, c.t, c.v);
    // }



    // int numThreads = 1;
    // List<Computation> comp = new LinkedList<Bider.Computation>();
    // comp.add(new Computation(startPoint, wonTasks, agent.vehicles()));
    // for (int i = 1; i < numThreads; i++) {
    //     comp.add(comp.get(0).copy());
    // }

    // // compute them
    // comp.stream()
    //     .parallel()
    //     .forEach((e) -> this.getResult(e));

    // List<Double> costsFound = new LinkedList<>();
    // double min = Double.MAX_VALUE;
    // Computation minC = null;
    // for (Computation c : comp) {
    //     double val = c.r.getCost();
    //     costsFound.add(val);
    //     if (val < min) {
    //         min = val;
    //         minC = c;
    //     }
    // }

}