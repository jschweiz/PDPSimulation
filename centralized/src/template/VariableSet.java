package template;

import logist.simulation.Vehicle;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logist.task.Task;
import logist.topology.Topology.City;

public class VariableSet {

    private static int NULL = -1;
    private static List<Vehicle> vehicleList;

    private int[] nextTaskV;
    private int[] nextTaskT;

    private int[] time;
    private int[] vehicle;

    private double[] travelDistVehicle;
    private double cost = -1;

    // construct initial variable

    public VariableSet(List<Vehicle> vehicles, List<Task> tasks) {

        int numTasks = tasks.size();
        int numVehicles = vehicles.size();

        // initialize all variable arrays
        this.nextTaskV = new int[numVehicles];
        this.nextTaskT = new int[numTasks * 2];
        this.time = new int[numTasks * 2];
        this.vehicle = new int[numTasks * 2];
        this.travelDistVehicle = new double[numVehicles];

        // intialize all the task and tasksteps variables
        TaskStep.NUM_TASKS = numTasks;
        TaskStep.TASK_LIST = tasks;

        // save the vehicle list
        vehicleList = vehicles;

        // variables for init
        int vehicleNumber = 0;
        Vehicle currVehicle = vehicles.get(vehicleNumber);
        int stepsOfVehicle = 1;
        TaskStep prevAction = null;

        // Basic check : is there a task that cannot fit in any vehicle ? 
        for (Task t : tasks) {
            boolean ok = false;
            for (Vehicle v : vehicles) {
                if (t.weight < v.capacity())
                    ok = true;
            }
            if (!ok){
                System.err.println("Tast " + t + " does not fit in any vehicle");
                return;
            }
        }

        // Task to vehicle assignement
        for (int taskNum = 0; taskNum < numTasks; taskNum++) {
            Task t = tasks.get(taskNum);

            if (t.weight <= currVehicle.capacity()) {
                // No worry of usedCapacity, as the task is added 
                // As "Pickup and deliver immediately (one task at a time)"
                // create pickup and deliver taskSteps
                TaskStep pickup = new TaskStep(t, taskNum, true);
                TaskStep delivery = new TaskStep(t, taskNum, false);

                // fill the vehicleMap 
                vehicle[pickup.getMapId()] = vehicleNumber;
                vehicle[delivery.getMapId()] = vehicleNumber;

                // fill the timeMap
                time[pickup.getMapId()] = stepsOfVehicle;
                stepsOfVehicle++;
                time[delivery.getMapId()] = stepsOfVehicle;
                stepsOfVehicle++;

                // if first task of vehicle
                if (stepsOfVehicle == 3) 
                    nextTaskV[vehicleNumber] = pickup.getMapId();
                else 
                    nextTaskT[prevAction.getMapId()] = pickup.getMapId();
                nextTaskT[pickup.getMapId()] = delivery.getMapId();
                prevAction = delivery;

            } else {
                // change vehicle
                vehicleNumber++;
                currVehicle = vehicleList.get(vehicleNumber);
                stepsOfVehicle = 1;

                // try again to put the same task in the next vehicle
                taskNum--;
            }
        }
        nextTaskT[prevAction.getMapId()] = NULL;

        // fill unused vehicles with no tasks
        for (int i = vehicleNumber + 1; i < numVehicles; i++) {
            nextTaskV[i] = NULL;
        }

        computeCost(true);
    }


    // helper functions for CPMaker

    public int getRandomAppropriateVehicle() {
        int n = getNumberVehicles();
        int chosenVehicle = new Random().nextInt(n);
        // Potential infinite loop --> expected to finish after n iterations
        while (this.nextTaskV[chosenVehicle] == NULL) {
            chosenVehicle = new Random().nextInt(n);
        }
        return chosenVehicle;
    }

    public int getNumberVehicles() {
        return vehicleList.size();
    }

    public TaskStep getFirstStepOf(int v) {
        return TaskStep.fromId(nextTaskV[v]);
    }

    public int getVehicleCapacity(int vi) {
        return vehicleList.get(vi).capacity();
    }

    public int computeNumberOfTaskVehicle(int v) {
        int t = nextTaskV[v];
        int length = 0;
        do {
            t = nextTaskT[t];
            length++;
        } while (t != NULL);
        return length;
    }


    // CHANGING FUNCTIONS

    public void changingVehicle(int v1, int v2) {
        // take first task pickup of vehicle
        int pickup = nextTaskV[v1];
        int delivery = removeFirstTaskFromVehicle(v1);
        nextTaskT[delivery] = nextTaskV[v2];
        nextTaskT[pickup] = delivery;
        nextTaskV[v2] = pickup;
        updateTime(v1);
        updateTime(v2);
        vehicle[pickup] = v2;
        vehicle[delivery] = v2;

        costHandleChangingTasks(v1);
        costHandleChangingTasks(v2);
    }

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

        costHandleChangingTasks(vi);
    }


    // OTHER CHANGING FUNCTION HELPERS

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
        if (tIdX1 > tIdX2)
            return false;

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

        int vehicleCapacity = getVehicleCapacity(v);
        int next = tIdX1;
        int currentCapacity = getVehicleCapacityAt(v, tIdX1) + TaskStep.getWeight(tIdX2);

        // iterate on the tasks in v from tIdX1 to tIdX2
        while (next != tIdX2) {

            if (deliverOfT1 != NULL && next == deliverOfT1) return false;   // if deliverOfT1 comes before tIdX2
            if (pickupOfT2 != NULL && next == pickupOfT2) return false;     // if pickupOfT2 comes before tIdX2
            
            if (currentCapacity > vehicleCapacity) return false;            // check that weight < capacity if exchange

            next = nextTaskT[next];
            currentCapacity += TaskStep.getWeight(next);
        }

        // final check for capacity
        currentCapacity +=  -TaskStep.getWeight(tIdX2) + TaskStep.getWeight(tIdX1);
        if (currentCapacity > vehicleCapacity) return false;

        return true;
    }

    public int removeFirstTaskFromVehicle(int vi) {
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
        int timer = 1;

        while (ti != NULL) {
            time[ti] = timer;
            timer++;
            ti = nextTaskT[ti];
        }

        // if (ti != NULL) {
        //     time[ti] = 1;
        //     int tj;
        //     do {
        //        tj = nextTaskT[ti];
        //         if (tj != NULL) {
        //             time[tj] = time[ti] + 1;
        //             ti = tj;
        //         }
        //     } while (tj != NULL);
        // }
    }

    // COST FUNCTION
    public double getCost() {
        return cost;
    }

    /**
     * Compute cost and replace the attribute of the instance
     * @param fromScratch This says whether travelDistVehicle should be computed from scratch
     * @return double. Cost of the VariableSet
     */
    public double computeCost(boolean fromScratch) {
        if (fromScratch)
            computeTravelDistanceList();
        double newCost = 0;
        for (int vehicleNumber = 0; vehicleNumber < nextTaskV.length; vehicleNumber++) {
            newCost += travelDistVehicle[vehicleNumber] * vehicleList.get(vehicleNumber).costPerKm(); 
        }
        cost = newCost;
        return newCost;
    }

    public void computeTravelDistanceList() {
        for (int i = 0; i < nextTaskV.length; i++) {
            travelDistVehicle[i] = getTravelDistanceVehicle(i);
        }
    }

    public double getTravelDistanceVehicle(int vehicleNumber) {
        Vehicle vehicle = vehicleList.get(vehicleNumber);
        double distVehicle = 0;

        int t1 = nextTaskV[vehicleNumber];
        if (t1 == NULL) 
            return 0;

        // distance to first task of vehicle
        distVehicle += vehicle.getCurrentCity().distanceTo(TaskStep.getInvolvedCity(t1));

        // distance between tasks
        int t2;
        while ((t2 = nextTaskT[t1]) != NULL) {
            City cityT1 = TaskStep.getInvolvedCity(t1);
            City cityT2 = TaskStep.getInvolvedCity(t2);
            distVehicle += cityT1.distanceTo(cityT2);

            t1 = t2;
        }
        return distVehicle;
    }

    /**
     * Update the travelDistVehicle[vehicleNumber] and the cost.
     * @param vehicleNumber This is the vehicle number whose order of tasks has changed
     * @return Nothing
     */
    public void costHandleChangingTasks(int vehicleNumber) {
        Vehicle vehicle = vehicleList.get(vehicleNumber);
        this.cost -= travelDistVehicle[vehicleNumber] * vehicle.costPerKm();
        travelDistVehicle[vehicleNumber] = getTravelDistanceVehicle(vehicleNumber);
        this.cost += travelDistVehicle[vehicleNumber] * vehicle.costPerKm();
    }


    // CLONE FUNCTIONS

    public VariableSet(int[] nextTaskT, int[] nextTaskV, int[] time, int[] vehicle, double[] travelDistList, double cost) {
        this.nextTaskV = nextTaskV;
        this.nextTaskT = nextTaskT;
        this.time = time;
        this.vehicle = vehicle;
        this.travelDistVehicle = travelDistList;
        this.cost = cost;
    }

    public VariableSet copy() {
        return new VariableSet(nextTaskT.clone(), nextTaskV.clone(), time.clone(), vehicle.clone(), travelDistVehicle.clone(), cost);
    }


    // TO STRING FUNCTIONS

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
        s += "VEHICLE:\n";
        for (int i = 0; i < vehicle.length; i++) {
            if (vehicle[i] != c.vehicle[i]) {
                s+= TaskStep.fromId(i) + ":::  " +  vehicle[i] + " -> " + c.vehicle[i] + "\n";
            }
        }
        return s + delim;
    }


    // Hashcode and equals function, for use in HashSet

    @Override
    public int hashCode() {
        // Arrays.hashCode(a) == Arrays.hashCode(b) if Arrays.equals(a, b)
        return Arrays.hashCode(nextTaskV) + 29*Arrays.hashCode(nextTaskT) + 7*Arrays.hashCode(time) + 31*Arrays.hashCode(vehicle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) 
            return true;
        if (o == null)
            return false;
        if (!(o instanceof VariableSet))
            return false;
        VariableSet other = (VariableSet)o;

        if (!Arrays.equals(this.nextTaskV, other.nextTaskV)
            || !Arrays.equals(this.nextTaskT, other.nextTaskT)
            || !Arrays.equals(this.time, other.time)
            || !Arrays.equals(this.vehicle, other.vehicle))
            return false;

        return true;
    }

}