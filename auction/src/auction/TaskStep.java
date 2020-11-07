package auction;

import java.util.List;

import logist.task.Task;
import logist.topology.Topology.City;

public class TaskStep {

    public static int NUM_TASKS = 0;
    public static List<Task> TASK_LIST = null;
    public static int NULL = -1;
    public static String TO_STRING_PICK = "(task:%d;PICK)";
    public static String TO_STRING_DELI = "(task:%d;DELI)";

    public Task t;
    public int id;
    public boolean isPickup;

    // functions on a taskstep
    public TaskStep(Task t, int id, boolean isPickup) {
        this.t = t;
        this.id = id;
        this.isPickup = isPickup;
    }

    public int getMapId() {
        return this.id + (isPickup ? 0 : NUM_TASKS);
    }
    
    public String toString() {
        String s = isPickup ? TO_STRING_PICK : TO_STRING_DELI;
        return String.format(s, id);
    }


    // STATIC FUNCTIONS: simplify between TaskSteps and their ids to deal with arrays

    public static TaskStep fromId(int id) {
        if (id == NULL) return null;
        int rid = getRealTaskId(id);
        return new TaskStep(TASK_LIST.get(rid), rid, isPickup(id));
    }

    public static City getInvolvedCity(int id) {
        if (id == NULL) return null;
        int rid = getRealTaskId(id);
        Task t = TASK_LIST.get(rid);
        return isPickup(id) ? t.pickupCity : t.deliveryCity;
    }


    // get info on a specific id
    public static boolean isPickup(int id) {
        return id/NUM_TASKS == 0;
    }
    public static int getWeight(int id) {
        int w = TASK_LIST.get(getRealTaskId(id)).weight;
        return isPickup(id) ? w : -w;
    }
    public static int getRealTaskId(int id) {
        if (id == NULL) return -1;
        return id >= NUM_TASKS ? id - NUM_TASKS : id;
    }

    // functions to switch between id of pickup and delivery TaskSteps
    public static int getDeliveryId(int i) {
        return i + NUM_TASKS;
    }
    public static int getPickupId(int i) {
        return i - NUM_TASKS;
    }
}
