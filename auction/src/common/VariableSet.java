package common;

import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.task.Task;
import logist.topology.Topology.City;

public class VariableSet {
    private static int NULL = -1;
    private static List<Vehicle> vehicleList;

    private int numTasks;
    private int numVehicles;
    private int[] nextTaskV;
    private int[] nextTaskT;

    private int[] time;
    private int[] vehicle;

    private double[] travelDistVehicle;
    private double cost = Double.MAX_VALUE;
    private Random rand = new Random();

    // construct initial variableSet

    public VariableSet(List<Vehicle> vehicles, List<Task> tasks) {
        numTasks = tasks.size();
        numVehicles = vehicles.size();

        // initialize all variable arrays
        this.nextTaskV = new int[numVehicles];
        this.nextTaskT = new int[numTasks * 2];
        this.time = new int[numTasks * 2];
        this.vehicle = new int[numTasks * 2];
        this.travelDistVehicle = new double[numVehicles];

        // intialize all the task and tasksteps variables
        TaskStep.setTaskList(tasks);

        // save the vehicle list
        vehicleList = vehicles;

        // variables for init
        int vehicleNumber = 0;
        Vehicle currVehicle = vehicles.get(vehicleNumber);
        int stepsOfVehicle = 1;
        TaskStep prevAction = null;

        if (CPMaker.DEBUG > 0)
            System.out.println("Building initial solution");

        // Basic check : is there a task that cannot fit in any vehicle ?
        for (Task t : tasks) {
            boolean ok = false;
            for (Vehicle v : vehicles) {
                if (t.weight < v.capacity())
                    ok = true;
            }
            if (!ok) {
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

        if (CPMaker.DEBUG > 0)
            System.out.println("Initial solution built");
        computeCost();
    }

    // CHANGING FUNCTIONS

    /**
     * Change first task of v1 and put it in front of v2
     * 
     * @param v1 Vehicle number whose fist task is removed
     * @param v2 Vehicle number in which the task is added (in front of all the
     *           others)
     */
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

        updateCostVehicle(v1);
        updateCostVehicle(v2);
    }

    /**
     * Change pickup task of v1 and put it in front of v2
     * 
     * @param v1     Vehicle number whose first task is removed
     * @param v2     Vehicle number in which the task is added (in front of all the
     *               others)
     * @param pickup Id of the task to remove in v1
     */
    public void changingVehicle(int v1, int v2, int pickup) {
        // take first task pickup of vehicle
        int delivery = TaskStep.getDeliveryId(pickup);
        removeTaskFromVehicle(v1, pickup);
        nextTaskT[delivery] = nextTaskV[v2];
        nextTaskT[pickup] = delivery;
        nextTaskV[v2] = pickup;
        updateTime(v1);
        updateTime(v2);
        vehicle[pickup] = v2;
        vehicle[delivery] = v2;

        updateCostVehicle(v1);
        updateCostVehicle(v2);
    }

    /**
     * Changing task order according to algorithm given in td
     * 
     * @param vi    Vehicle for which tasks will be exchanged
     * @param tIdX1 TaskStep id that will be put in tIdX2 place
     * @param tIdX2 TaskStep id that will be put in tIdX1 place
     */
    public void changingTaskOrder(int vi, int tIdX1, int tIdX2) {
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
        while (count < tIdX2) {
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
        updateCostVehicle(vi);
    }

    // OTHER CHANGING FUNCTION HELPERS

    /**
     * Checks if exchanging tasks is possible regarding task order and capacity or
     * vehicle
     * 
     * @param tIdX1 Task to exchange
     * @param tIdX2 Task to exchange
     * @param v     Vehicle concerned
     * @return
     */
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

        // carefull if they are inversed: next processing only works if tIdX1 is before
        // tIdX2
        int time1 = time[tIdX1];
        int time2 = time[tIdX2];
        if (time1 > time2) {
            int tid = tIdX1;
            tIdX1 = tIdX2;
            tIdX2 = tid;
        }

        // find deliver or pickup of tasks
        int deliverOfT1 = TaskStep.isPickup(tIdX1) ? TaskStep.getDeliveryId(tIdX1) : NULL;
        int pickupOfT2 = !TaskStep.isPickup(tIdX2) ? TaskStep.getPickupId(tIdX2) : NULL;

        int vehicleCapacity = getVehicleCapacity(v);
        int next = tIdX1;
        int currentCapacity = getVehicleWeightBefore(v, tIdX1) + TaskStep.getWeight(tIdX2);

        // iterate on the tasks in v from tIdX1 to tIdX2
        while (next != tIdX2) {

            if (deliverOfT1 != NULL && next == deliverOfT1)
                return false; // if deliverOfT1 comes before tIdX2
            if (pickupOfT2 != NULL && next == pickupOfT2)
                return false; // if pickupOfT2 comes before tIdX2
            if (currentCapacity > vehicleCapacity)
                return false; // check that weight < capacity if exchange

            next = nextTaskT[next];
            currentCapacity += TaskStep.getWeight(next);
        }

        // final check for capacity
        currentCapacity += -TaskStep.getWeight(tIdX2) + TaskStep.getWeight(tIdX1);
        if (currentCapacity > vehicleCapacity)
            return false;

        return true;
    }

    private int removeFirstTaskFromVehicle(int vi) {
        int pickup = nextTaskV[vi];
        int delivery = TaskStep.getDeliveryId(pickup);

        int next = nextTaskT[pickup];

        if (next == delivery) { // both pickup and delivery are subsequent
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

    private void removeTaskFromVehicle(int vi, int pickup) {
        int delivery = TaskStep.getDeliveryId(pickup);

        // Put under form of LinkedList (easier to remove indices)
        int t = nextTaskV[vi];
        if (t == NULL)
            return;
        LinkedList<Integer> ll = new LinkedList<Integer>();
        while (t != NULL) {
            ll.add(t);
            t = nextTaskT[t];
        }

        // remove pickup and delivery
        ll.remove((Object) pickup);
        ll.remove((Object) delivery);

        if (ll.isEmpty()) {
            nextTaskV[vi] = NULL;
            return;
        }

        t = ll.removeFirst();
        nextTaskV[vi] = t;
        while (!ll.isEmpty()) {
            nextTaskT[t] = ll.removeFirst();
            t = nextTaskT[t];
        }
        nextTaskT[t] = NULL;

    }

    // INITIAL COST CALCULATION FUNCTIONS

    /**
     * Compute cost and replace the attribute of the instance
     * 
     * @return double. Cost of the VariableSet
     */
    private double computeCost() {
        for (int i = 0; i < nextTaskV.length; i++) {
            travelDistVehicle[i] = getTravelDistanceVehicle(i);
        }
        double newCost = 0;
        for (int vehicleNumber = 0; vehicleNumber < nextTaskV.length; vehicleNumber++) {
            newCost += travelDistVehicle[vehicleNumber] * vehicleList.get(vehicleNumber).costPerKm();
        }
        cost = newCost;
        return newCost;
    }

    /**
     * Compute the total distance traveled by a vehicle
     * 
     * @param vehicleNumber
     * @return
     */
    private double getTravelDistanceVehicle(int vehicleNumber) {
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

    // UPDATE FUNCTIONS

    /**
     * Update the travelDistVehicle[vehicleNumber] and the cost.
     * 
     * @param vehicleNumber This is the vehicle number whose order of tasks has
     *                      changed
     * @return Nothing
     */
    private void updateCostVehicle(int vehicleNumber) {
        Vehicle vehicle = vehicleList.get(vehicleNumber);
        this.cost -= travelDistVehicle[vehicleNumber] * vehicle.costPerKm();
        travelDistVehicle[vehicleNumber] = getTravelDistanceVehicle(vehicleNumber);
        this.cost += travelDistVehicle[vehicleNumber] * vehicle.costPerKm();
    }

    /**
     * Update the time table for a vehicle
     * 
     * @param vi vehicle for which time table will be updated
     */
    private void updateTime(int vi) {
        int ti = nextTaskV[vi];
        int timer = 1;

        while (ti != NULL) {
            time[ti] = timer;
            timer++;
            ti = nextTaskT[ti];
        }
    }

    // GET FUNCTIONS

    /**
     * Computes the ordered of TaskStep executed by vehicleNumber in the current
     * VariableSet instance
     * 
     * @param vehicleNumber Id of the vehicle
     * @return Nothing.
     */
    public List<TaskStep> getTaskStepVehicle(int vehicleNumber) {
        List<TaskStep> taskStepOrdered = new ArrayList<TaskStep>();
        int t = nextTaskV[vehicleNumber];
        while (t != NULL) {
            taskStepOrdered.add(TaskStep.fromId(t));
            t = nextTaskT[t];
        }
        return taskStepOrdered;
    }

    /**
     * Useful to see if a solution involve one or several vehicles
     * 
     * @return int Number of vehicle with at least one task
     */
    public int getNumberUsedVehicles() {
        int counter = 0;
        for (int i = 0; i < nextTaskV.length; i++) {
            if (nextTaskV[i] != NULL)
                counter++;
        }
        return counter;
    }

    /**
     * Get the current capacity of vehicle just before a specific taskstep
     * 
     * @param v  Vehicle concerned
     * @param t0 Task number in the order of the tasks of vehicle v
     * @return int : weight of vehicle just before taskstep
     */
    private int getVehicleWeightBefore(int v, int t0) {
        int t = nextTaskV[v];
        int currWeight = 0;
        while (t != t0) {
            currWeight += TaskStep.getWeight(t);
            t = nextTaskT[t];
        }
        return currWeight;
    }

    /**
     * @return int random Index of a vehicle with at least a task on board
     */
    public int getRandomAppropriateVehicle() {
        int n = getNumberVehicles();
        int chosenVehicle = rand.nextInt(n);
        int tries = 0; // only try a finite number of time to avoid infinite loop
        while (this.nextTaskV[chosenVehicle] == NULL) {
            chosenVehicle = rand.nextInt(n);
            if (tries > 1000)
                return -1;
            tries++;
        }
        return chosenVehicle;
    }

    /**
     * Counts the number of tasksteps in vehicle
     * 
     * @param v Vehicle
     * @return number of tasksteps
     */
    public int computeNumberOfTaskVehicle(int v) {
        int t = nextTaskV[v];
        int length = 0;
        do {
            t = nextTaskT[t];
            length++;
        } while (t != NULL);
        return length;
    }

    /**
     * Finds the ids of all pickup tasks
     * 
     * @param v Vehicle number
     * @return List<Integer> ids of all the pickup TaskSteps in the vehicle number v
     */
    public List<Integer> computePickupsIdVehicle(int v) {
        List<Integer> pickupsId = new ArrayList<Integer>();
        int t = nextTaskV[v];
        while (t != NULL) {
            if (TaskStep.isPickup(t)) {
                pickupsId.add(t);
            }
            t = nextTaskT[t];
        }
        return pickupsId;
    }

    public double getCost() {
        return cost;
    }

    public double getTrueCost() {
        return computeCost();
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

    // CLONE FUNCTIONS

    private VariableSet(int numVehicles, int numTasks, int[] nextTaskT, int[] nextTaskV, int[] time, int[] vehicle,
            double[] travelDistList, double cost) {
        this.numVehicles = numVehicles;
        this.numTasks = numTasks;
        this.nextTaskV = nextTaskV;
        this.nextTaskT = nextTaskT;
        this.time = time;
        this.vehicle = vehicle;
        this.travelDistVehicle = travelDistList;
        this.cost = cost;
    }

    private VariableSet(int updateVehicle, int numVehicles, int numTasks, int[] nextTaskT, int[] nextTaskV, int[] time,
            int[] vehicle, double[] travelDistList, double cost) {
        this.numVehicles = numVehicles;
        this.numTasks = numTasks;
        this.nextTaskV = nextTaskV;
        this.nextTaskT = nextTaskT;
        this.time = time;
        this.vehicle = vehicle;
        this.travelDistVehicle = travelDistList;
        this.cost = cost;

        updateTime(updateVehicle);
        computeCost();
    }

    public VariableSet copy() {
        return new VariableSet(numVehicles, numTasks, nextTaskT.clone(), nextTaskV.clone(), time.clone(),
                vehicle.clone(), travelDistVehicle.clone(), cost);
    }

    public VariableSet copyPlusTask(Task t) {
        // Clone and initialize variables
        int newNumTasks = numTasks + 1;
        int[] newNextTaskV = nextTaskV.clone();
        double[] newTravelDistVehicle = travelDistVehicle.clone();
        int[] newNextTaskT = new int[newNumTasks * 2];
        int[] newTime = new int[newNumTasks * 2];
        int[] newVehicle = new int[newNumTasks * 2];

        for (int i = 0; i < numTasks * 2; i++) {
            newNextTaskT[i] = nextTaskT[i];
            newVehicle[i] = vehicle[i];
        }

        // add task to taskStep
        TaskStep tsPickup = new TaskStep(t, numTasks, true);
        TaskStep tsDelivery = new TaskStep(t, numTasks, false);
        int pickup = tsPickup.getMapId();
        int delivery = tsDelivery.getMapId();

        // check capacities of vehicles
        int v = -1;
        for (int i = 0; i < numVehicles; i++) {
            if (vehicleList.get(i).capacity() >= t.weight) {
                v = i;
                break;
            }
        }

        // no vehicle has the required capacity for the task
        if (v == -1) {
            System.err.println("No vehicle has enough capacity to carry this task");
            return null;
        }
        newNextTaskT[delivery] = newNextTaskV[v];
        newNextTaskT[pickup] = delivery;
        newNextTaskV[v] = pickup;
        newVehicle[pickup] = v;
        newVehicle[delivery] = v;

        return new VariableSet(v, numVehicles, newNumTasks, newNextTaskT, newNextTaskV, newTime, newVehicle,
                newTravelDistVehicle, cost);
    }

    public VariableSet copyMinusLastTask() {
        // Clone and initialize variables
        int newNumTasks = numTasks - 1;
        int[] newNextTaskV = nextTaskV.clone();
        double[] newTravelDistVehicle = travelDistVehicle.clone();
        int[] newNextTaskT = new int[newNumTasks * 2];
        int[] newTime = new int[newNumTasks * 2];
        int[] newVehicle = new int[newNumTasks * 2];

        int rid = TaskStep.getListTaskSize() - 1;
        int pickupId = TaskStep.getMapId(rid, true);
        int deliverId = TaskStep.getMapId(rid, false);

        int v = vehicle[pickupId]; // vehicle holding the task

        for (int i = 0; i < newNumTasks * 2; i++) {
            if (nextTaskT[i] == pickupId) {
                if (nextTaskT[nextTaskT[i]] == deliverId)
                    newNextTaskT[i] = nextTaskT[nextTaskT[nextTaskT[i]]];
                else
                    newNextTaskT[i] = nextTaskT[nextTaskT[i]];
            } else if (nextTaskT[i] == deliverId)
                newNextTaskT[i] = nextTaskT[nextTaskT[i]];
            else
                newNextTaskT[i] = nextTaskT[i];

            newVehicle[i] = vehicle[i];
        }

        return new VariableSet(v, numVehicles, newNumTasks, newNextTaskT, newNextTaskV, newTime, newVehicle,
                newTravelDistVehicle, cost);
    }

    // TO STRING FUNCTIONS

    public String toString() {
        String delim = "===========================================================================\n";
        String s = delim;
        s += "VEHICLE CHAINS:\n";
        for (int v = 0; v < vehicleList.size(); v++) {
            s += "   --- vehicle number" + v + ":  ";
            int t = nextTaskV[v];
            while (t != NULL) {
                TaskStep tid = TaskStep.fromId(t);
                String act = (tid.isPickup()) ? "P" : "D";
                s += TaskStep.getInvolvedCity(t) + " (" + act + tid.getId() + ") --> ";
                t = nextTaskT[t];
            }
            s += "NULL";
            s += "  travels " + travelDistVehicle[v] + " km";
            s += "\n";
        }
        return s + "Cost : " + cost + "\n" + delim;
    }

    public String toString_() {
        String delim = "===========================================================================\n";
        String s = delim;
        s += "VEHICLE CHAINS:\n";
        for (int v = 0; v < vehicleList.size(); v++) {
            s += "   --- vehicle number" + v + ":  ";
            int t = nextTaskV[v];
            while (t != NULL) {
                s += TaskStep.fromId(t) + " --> ";
                t = nextTaskT[t];
            }
            s += "NULL";
            s += "  travels " + travelDistVehicle[v] + " km";
            s += "\n";
        }
        return s + "Cost : " + cost + "\n" + delim;
    }

    public String toStringDetails() {
        String delim = "===========================================================================\n";
        String s = delim;
        s += "NEXT TASK TASKSTEP:\n";
        for (int i = 0; i < nextTaskT.length; i++) {
            s += TaskStep.fromId(i) + " -> " + TaskStep.fromId(nextTaskT[i]) + "\n";
        }
        s += "NEXT TASK VEHICLE:\n";
        for (int i = 0; i < nextTaskV.length; i++) {
            s += i + " -> " + TaskStep.fromId(nextTaskV[i]) + "\n";
        }
        s += "TIME :\n";
        for (int i = 0; i < time.length; i++) {
            s += TaskStep.fromId(i) + " -> " + time[i] + "\n";
        }
        s += "VEHICLE:\n";
        for (int i = 0; i < vehicle.length; i++) {
            s += TaskStep.fromId(i) + " -> " + vehicle[i] + "\n";
        }
        return s + delim;
    }

    // Hashcode and equals function, for use in HashSet

    @Override
    public int hashCode() {
        // Arrays.hashCode(a) == Arrays.hashCode(b) if Arrays.equals(a, b)
        return Arrays.hashCode(nextTaskV) + 29 * Arrays.hashCode(nextTaskT) + 7 * Arrays.hashCode(time)
                + 31 * Arrays.hashCode(vehicle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof VariableSet))
            return false;
        VariableSet other = (VariableSet) o;

        if (!Arrays.equals(this.nextTaskV, other.nextTaskV) || !Arrays.equals(this.nextTaskT, other.nextTaskT)
                || !Arrays.equals(this.time, other.time) || !Arrays.equals(this.vehicle, other.vehicle))
            return false;

        return true;
    }
}