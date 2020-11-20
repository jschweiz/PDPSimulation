package common;

import java.util.List;

import logist.task.Task;
import logist.topology.Topology.City;

public class TaskStep {

    private static List<Task> TASK_LIST = null;
    private static int NULL = -1;
    private static String TO_STRING_PICK = "(task:%d;PICK)";
    private static String TO_STRING_DELI = "(task:%d;DELI)";

    private Task t;
    private int id;
    private boolean isPickup;

    // functions on a taskstep
    public TaskStep(Task t, int id, boolean isPickup) {
        this.t = t;
        this.id = id;
        this.isPickup = isPickup;
    }

    // even : pickup
    // odd : delivery
    public int getMapId() {
        return isPickup ? 2*id : 2*id+1;
    }
    
    public String toString() {
        String s = isPickup ? TO_STRING_PICK : TO_STRING_DELI;
        return String.format(s, id);
    }

    public boolean isPickup() {
        return isPickup;
    }

    public Task getTask() {
        return t;
    }

    public int getId() {
        return id;
    }


    // STATIC FUNCTIONS: simplify between TaskSteps and their ids to deal with arrays

    public static void setTaskList(List<Task> taskList) {
        TASK_LIST = taskList;
    }
    

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
        return id%2 == 0;
    }
    public static int getWeight(int id) {
        int w = TASK_LIST.get(getRealTaskId(id)).weight;
        return isPickup(id) ? w : -w;
    }
    public static int getRealTaskId(int id) {
        return id/2;
    }

    // functions to switch between id of pickup and delivery TaskSteps
    public static int getDeliveryId(int i) {
        return i + 1;
    }
    public static int getPickupId(int i) {
        return i - 1;
    }
}
