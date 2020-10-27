package template;

import logist.simulation.Vehicle;

import java.util.HashMap;
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

        int numTasks = tasks.size();
        int numVehicles = vehicles.size();

        // initialize all variable arrays
        this.nextTaskV = new int[numVehicles];
        this.nextTaskT = new int[numTasks * 2];
        this.time = new int[numTasks * 2];
        this.vehicle = new int[numTasks];

        // intialize all the task and tasksteps vairables
        TaskStep.NUM_TASKS = numTasks;
        List<Task> taskList = new LinkedList<Task>();
        for (Task t: tasks) taskList.add(t);
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

                // fill the vehicleMap 
                vehicle[taskNum] = vehicleNumber; //.put(t, vehicle);

                // create pickup and delivert taskSteps
                TaskStep pickup = new TaskStep(t, taskNum, true);
                TaskStep delivery = new TaskStep(t, taskNum, false);

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
            if (TaskStep.isPickup(t)) {
                length++;
            }
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
    }

    // TO DO: VERIFY THAT WE DONT EXCHANGE PICKUP AND DELIVERY OF SAME TASK
    public void changingTaskOrder(int vi, int tIdX1, int tIdX2) {
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

    public boolean validChange(int tIdX1, int tIdX2) {
        if (TaskStep.isPickup(tIdX1)) {
            int deliver = TaskStep.getDeliveryId(tIdX1);
            int next = tIdX1;
            while (next != tIdX2) {
                if (next == deliver) {
                    return false;
                }
                next = nextTaskT[next];
            }
        }
        if (!TaskStep.isPickup(tIdX2)) {
            int pickup = TaskStep.getPickupId(tIdX2);
            int next = tIdX1;
            while (next != tIdX2) {
                if (next == pickup) {
                    return false;
                }
                next = nextTaskT[next];
            }
        }
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
        }
        int tj;
        do {
            tj = nextTaskT[ti];
            if (tj != NULL) {
                time[tj] = time[ti] + 1;
                ti = tj;
            }
        } while (tj != NULL);
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
            s+= i + " -> " + vehicle[i] + "\n";
        }
        return s + delim;
    }


    
}
