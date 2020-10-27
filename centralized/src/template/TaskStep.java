package template;

import java.util.List;

import logist.task.Task;

public class TaskStep {

    public static int NUM_TASKS = 0;
    public static List<Task> TASK_LIST = null;
    public static int NULL = -1;

    public Task t;
    public int id;
    public boolean isPickup;

    public TaskStep(Task t, int id, boolean isPickup) {
        this.t = t;
        this.id = id;
        this.isPickup = isPickup;
    }

    public int getMapId() {
        return this.id + (isPickup?0:NUM_TASKS);
    }

    public static TaskStep fromId(int id) {
        if (id == NULL) return null;
        int n = NUM_TASKS;
        int rid = id%n;
        if (rid<0) rid += n;
        boolean isPickup = (id/n == 0);
        return new TaskStep(TASK_LIST.get(rid), rid, isPickup);
    }

    public static boolean isPickup(int id) {
        int n = NUM_TASKS;
        return (id/n == 0);
    }

    public static int getDeliveryId(int i) {
        return i + NUM_TASKS;
    }

    public static int getPickupId(int i) {
        return i - NUM_TASKS;
    }


    // hashcode, equals and tostring
    public int hashCode() {
        return t.hashCode() + (isPickup?1:0) + this.id;
    }
    public boolean equals(Object o) {
        if (!(o instanceof TaskStep)) return false;
        TaskStep s = (TaskStep)o;
        return this.isPickup == s.isPickup && this.id == s.id;
    }
    public String toString() {
        return "(task:" + id + ";" + (isPickup?"PICK":"DELIV") + ")";
    }
    
}
