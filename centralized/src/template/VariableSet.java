package template;

import logist.simulation.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import logist.task.Task;
import logist.task.TaskSet;

public class VariableSet {

    public static int NULL = -1;

    private int[] nextTaskV;
    private int[] nextTaskT;

    private int[] time;
    private int[] vehicle;

    private static List<Vehicle> vehicleList;

    // construct initial variable 
    public VariableSet(List<Vehicle> vehicles, TaskSet tasks) {

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////
        Task t1 = new Task(1, null, null, 10, 5);
        Task t2 = new Task(2, null, null, 10, 2);
        Task t3 = new Task(3, null, null, 10, 3);
        Task t4 = new Task(3, null, null, 10, 3);
        Task t5 = new Task(3, null, null, 10, 3);
        LinkedList<Task> tt = new LinkedList<Task>();
        tt.add(t1);
        tt.add(t2);
        tt.add(t3);
        tt.add(t4);
        tt.add(t5);
        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        int numTasks = 5;//tasks.size();
        int numVehicles = vehicles.size();

        // initialize all variable arrays
        this.nextTaskV = new int[numVehicles];
        this.nextTaskT = new int[numTasks * 2];
        this.time = new int[numTasks * 2];
        this.vehicle = new int[numTasks*2];

        // intialize all the task and tasksteps vairables
        TaskStep.NUM_TASKS = numTasks;
        List<Task> taskList = new LinkedList<Task>();

        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////
        for (Task t: tt/*tasks*/) taskList.add(t);
         ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        TaskStep.TASK_LIST = taskList;

        // save the vehicle list
        vehicleList = vehicles;

        // variables for init
        int vehicleNumber = 0;
        Vehicle currVehicle = vehicles.get(vehicleNumber);
        int usedCapacity = 0;
        int stepsOfVehicle = 1;
        TaskStep prevAction = null;

        for (int taskNum = 0; taskNum < numTasks; taskNum++) {

            Task t = taskList.get(taskNum);

            if (usedCapacity + t.weight <= currVehicle.capacity()) {
                // add this task to the vehicle

                // create pickup and delivert taskSteps
                TaskStep pickup = new TaskStep(t, taskNum, true);
                TaskStep delivery = new TaskStep(t, taskNum, false);

                // fill the vehicleMap 
                vehicle[pickup.getMapId()] = vehicleNumber; //.put(t, vehicle);
                vehicle[delivery.getMapId()] = vehicleNumber; //.put(t, vehicle);

                // fill the timeMap
                time[pickup.getMapId()] = stepsOfVehicle; // timeMap.put(pickup, stepsOfVehicle);
                stepsOfVehicle++;
                time[delivery.getMapId()] = stepsOfVehicle; // timeMap.put(delivery, stepsOfVehicle);
                stepsOfVehicle++;

                // if first task of vehicle
                if (stepsOfVehicle == 3) {
                    nextTaskV[vehicleNumber] = pickup.getMapId(); // vehicleNextTaskMap.put(vehicle.id(), pickup);
                    nextTaskT[pickup.getMapId()] = delivery.getMapId(); // taskStepNextTaskMap.put(pickup, delivery);
                    prevAction = delivery;
                } else {
                    nextTaskT[prevAction.getMapId()] = pickup.getMapId(); // taskStepNextTaskMap.put(prevAction, pickup);
                    nextTaskT[pickup.getMapId()] = delivery.getMapId(); //taskStepNextTaskMap.put(pickup, delivery);
                    prevAction = delivery;
                }

            } else {
                if (t.weight > currVehicle.capacity()) throw new UnknownError();
                // change vehicle 
                vehicleNumber++;
                usedCapacity = 0;
                stepsOfVehicle = 1;
                currVehicle = vehicleList.get(vehicleNumber);
            }
        }


        nextTaskT[prevAction.getMapId()] = NULL; // taskStepNextTaskMap.put(prevAction, null);

        for (int i = vehicleNumber + 1; i < numVehicles; i++) {
            // fill vehicles with no tasks
            nextTaskV[i] = NULL; // vehicleNextTaskMap.put(vehicles.get(i).id(), null);
        }
    }



    public int getRandomAppropriateVehicle() {
        int n = getNumberVehicles();
        int chosenVehicle = new Random().nextInt(n);
        // POTENTIAL INFINITE LOOP BE CAREFULL
        while (this.nextTaskV[chosenVehicle] == NULL) {
            chosenVehicle = new Random().nextInt(n);
        }
        return chosenVehicle;
    }

    public int getNumberVehicles() {
        return vehicleList.size();
    }

    public TaskStep getTaskSetOfVehicle(int v) {
        return TaskStep.fromId(nextTaskV[v]);
    }

    public int getCapacityVehicle(int vi) {
        return vehicleList.get(vi).capacity();
    }

    public int computeNumberOfTaskVehicle(int v) {
        int t = nextTaskV[v];
        int length = 0;
        do {
            t = nextTaskT[t]; // this.taskStepNextTaskMap.get(t);
            length++;
        } while (t != NULL);
        return length;
    }


    // CHANGING FUNCTIONS

    public void changingVehicle(int v1, int v2) {
        // take first task pickup of vehicle
        int pickup = nextTaskV[v1];
        int delivery = removeTaskFromVehicle(v1);
        nextTaskT[delivery] = nextTaskV[v2];
        nextTaskT[pickup] = delivery;
        nextTaskV[v2] = pickup;
        updateTime(v1);
        updateTime(v2);
        vehicle[pickup] = v2;
        vehicle[delivery] = v2;
    }

    // TO DO: VERIFY THAT WE DONT EXCHANGE PICKUP AND DELIVERY OF SAME TASK
    public void changingTaskOrder(int vi, int tIdX1, int tIdX2) {
        // System.out.println("System is currently inverting" + tIdX1 + " and "+ tIdX2);
        // we are using different index origin (0 instead of 1 like pseudocode)
        tIdX1++;
        tIdX2++;

        int tPre1 = NULL;
        int t1 = nextTaskV[vi];
        int count = 1;
        boolean special = false;
        if (count >= tIdX1) {
            special = true;
        }
        while (count < tIdX1) {
            tPre1 = t1;
            t1 = nextTaskT[t1];
            count++;
        }
        int tPost1 = nextTaskT[t1];
        int tPre2 = t1;
        int t2 = nextTaskT[tPre2];
        count++;
        while(count < tIdX2) {
            tPre2 = t2;
            t2 = nextTaskT[t2];
            count++;
        }
        int tPost2 = nextTaskT[t2];
        if (tPost1 == t2) {
            if (special) {
                nextTaskV[vi] = t2;
            } else {
                nextTaskT[tPre1] = t2;
            }
            nextTaskT[t2] = t1;
            nextTaskT[t1] = tPost2;
        } else {
            if (special) {
                nextTaskV[vi] = t2;
            } else {
                nextTaskT[tPre1] = t2;
            }
            nextTaskT[tPre2] = t1;
            nextTaskT[t2] = tPost1;
            nextTaskT[t1] = tPost2;
        }
        updateTime(vi);
    }

    public int getVehicleCapacityAt(int v, int t0) {
        int t = nextTaskV[v];
        int currWeight = 0;//TaskStep.getWeight(t);
        while (t != t0) {
            currWeight += TaskStep.getWeight(t);
            t = nextTaskT[t];
        }
        return currWeight;
    }

    public boolean validChange(int tIdX1, int tIdX2, int v) {
        int counter = 0;
        int t = nextTaskV[v];
        while (counter < tIdX1) {
            t = nextTaskT[t];
            counter++;
        }
        int val1 = t;
        while (counter < tIdX2) {
            t = nextTaskT[t];
            counter++;
        }
        int val2 = t;
        tIdX1 = val1;
        tIdX2 = val2;


        // carefull if they are inversed: next processing only works if tIdX1 is before tIdX2
        int time1 = time[tIdX1];
        int time2 = time[tIdX2];
        if (time1>time2) {
            int tid = tIdX1;
            tIdX1 = tIdX2;
            tIdX2 = tid;
        }


        // find deliver or pickup of tasks
        int deliverOfT1 = TaskStep.isPickup(tIdX1) ? TaskStep.getDeliveryId(tIdX1) : NULL;
        int pickupOfT2 = !TaskStep.isPickup(tIdX2) ? TaskStep.getPickupId(tIdX2) : NULL;


        int vehicleCapacity = vehicleList.get(v).capacity();

        
        // System.out.println("VALID ?" + TaskStep.fromId(tIdX1)+ " and " + TaskStep.fromId(tIdX2) + " (capacity:" + vehicleCapacity + ')');

        int next = tIdX1;
        int currentCapacity = getVehicleCapacityAt(v, tIdX1) + TaskStep.getWeight(tIdX2);

        // System.out.println("Capacity after taking " + TaskStep.fromId(tIdX2) + " is " + currentCapacity + "(vehicleis:" + getVehicleCapacityAt(v, tIdX1)+")");

        while (next != tIdX2) {

            if (deliverOfT1 != NULL && next == deliverOfT1) return false;
            if (pickupOfT2 != NULL && next == pickupOfT2) return false;
            
            if (currentCapacity > vehicleCapacity) return false;

            next = nextTaskT[next];
            currentCapacity += TaskStep.getWeight(next);

            // System.out.println("Capacity after taking " + TaskStep.fromId(next) + " is " + currentCapacity);
        }

        // final check for capacity
        currentCapacity +=  -TaskStep.getWeight(tIdX2) + TaskStep.getWeight(tIdX1);
        // System.out.println("Capacity before tasking " + TaskStep.fromId(tIdX1) + " is " + currentCapacity + "\n\n");
        if (currentCapacity > vehicleCapacity) return false;

        return true;
    }



    // CHANGING HELPER FUNCTIONS

    public int removeTaskFromVehicle(int vi) {
        int pickup = nextTaskV[vi];
        int delivery = TaskStep.getDeliveryId(pickup);

        int next = nextTaskT[pickup];

        if (next  == delivery) { // both pickup and delivery are subsequent
            nextTaskV[vi] = nextTaskT[delivery];
        } else {

            nextTaskV[vi] = next;
            int lnext = next;

            while (next != delivery) {
                lnext = next;
                next = nextTaskT[next];
            }

            nextTaskT[lnext] = nextTaskT[next];
        }
        return delivery;
    }

    public void updateTime(int vi) {
        int ti = nextTaskV[vi];
        if (ti != NULL) {
            time[ti] = 1;
            int tj;
            do {
               tj = nextTaskT[ti];
                if (tj != NULL) {
                    time[tj] = time[ti] + 1;
                    ti = tj;
                }
            } while (tj != NULL);
        }
    }



    // clone functions
    public VariableSet(int[] nextTaskT, int[] nextTaskV, int[] time, int[] vehicle) {
        this.nextTaskV = nextTaskV;
        this.nextTaskT = nextTaskT;
        this.time = time;
        this.vehicle = vehicle;
    }

    public VariableSet copy() {
        return new VariableSet(nextTaskT.clone(), nextTaskV.clone(), time.clone(), vehicle.clone());
    }


    // utility functions
    public String toString() {
        String delim = "===========================================================================\n";
        String s = delim;
        s += "VEHICLE CHAINS:\n";
        for (int v = 0; v < vehicleList.size(); v++) {
            s+= "   --- vehicle number" + v+ ":  ";
            int t = nextTaskV[v];
            while (t != NULL) {
                s+= TaskStep.fromId(t) + " --> ";
                t = nextTaskT[t];
            }
            s+= "NULL \n";
        }
        return s + delim;
    }

    public String toStringDetails() {
        String delim = "===========================================================================\n";
        String s = delim;
        s += "NEXT TASK TASKSTEP:\n";
        for (int i = 0; i < nextTaskT.length; i++) {
            s+= TaskStep.fromId(i) + " -> " + TaskStep.fromId(nextTaskT[i]) + "\n";
        }
        s += "NEXT TASK VEHICLE:\n";
        for (int i = 0; i < nextTaskV.length; i++) {
            s+= i + " -> " + TaskStep.fromId(nextTaskV[i]) + "\n";
        }
        s += "TIME :\n";
        for (int i = 0; i < time.length; i++) {
            s+= TaskStep.fromId(i) + " -> " + time[i] + "\n";
        }
        s += "VEHICLE:\n";
        for (int i = 0; i < vehicle.length; i++) {
            s+= TaskStep.fromId(i) + " -> " + vehicle[i] + "\n";
        }
        return s + delim;
    }

    public String compare(VariableSet c) {
        String delim = "==============================COMPARISON====================================\n";
        String s = delim;
        s += "NEXT TASK TASKSTEP:\n";
        for (int i = 0; i < nextTaskT.length; i++) {
            if (nextTaskT[i] != c.nextTaskT[i]) {
                s+= TaskStep.fromId(i) + ":::  " +  TaskStep.fromId(nextTaskT[i]) + " -> " + TaskStep.fromId(c.nextTaskT[i]) + "\n";
            }
        }
        s += "NEXT TASK VEHICLE:\n";
        for (int i = 0; i < nextTaskV.length; i++) {
            if (nextTaskV[i] != c.nextTaskV[i]) {
                s+= i + ":::  " +  TaskStep.fromId(nextTaskV[i]) + " -> " + TaskStep.fromId(c.nextTaskV[i]) + "\n";
            }
        }
        // s += "TIME :\n";
        // for (int i = 0; i < time.length; i++) {
        //     if (time[i] != c.time[i]) {
        //         s+= TaskStep.fromId(i) + ":::  " +  time[i] + " -> " + c.time[i] + "\n";
        //     }
        // }
        s += "VEHICLE:\n";
        for (int i = 0; i < vehicle.length; i++) {
            if (vehicle[i] != c.vehicle[i]) {
                s+= TaskStep.fromId(i) + ":::  " +  vehicle[i] + " -> " + c.vehicle[i] + "\n";
            }
        }
        return s + delim;
    }


    
}




        // if () {
        //     int deliver = ;
            
        //     // System.out.println("tIdX1 is pickup with delivery" + deliver);
        //     int next = tIdX1;
        //     while (next != tIdX2) {
        //         if (next == deliver) {
        //             // System.out.println("return false because of tix1");
        //             return false;
        //         }
        //         next = nextTaskT[next];
        //     }
        // }
        // if (!TaskStep.isPickup(tIdX2)) {
        //     int pickup = TaskStep.getPickupId(tIdX2);
        //     int next = tIdX1;
        //     while (next != tIdX2) {
        //         if (next == pickup) {
        //             // System.out.println("return false because of tix2");
        //             return false;
        //         }
        //         next = nextTaskT[next];
        //     }
        // }

        // check if vehicle has enough capacity for it
