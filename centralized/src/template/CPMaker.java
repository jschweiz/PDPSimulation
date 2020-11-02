package template;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;

public class CPMaker {
    private static double P = 0.4;
    private static int MAX_ITERATIONS = 30;
    private static long MAX_TIME_SEC = 30;

    private static long startTimeMillis;
    private static int COUNTER = 1;

    /**
     * Set the static parameters of the plan maker.
     * 
     * @param p          This is the probability to choose the best neighbour
     * @param maxIter    This is the max number of iterations after which the plan
     *                   maker returns
     * @param maxTimeSec This is the max number of seconds after which the plan
     *                   maker returns
     * @return Nothing
     */
    public static void setParameters(double p, int maxIter, Long maxTimeSec) {
        if (p >= 0)
            P = p;
        if (maxIter > 0)
            MAX_ITERATIONS = maxIter;
        if (maxTimeSec > 0)
            MAX_TIME_SEC = maxTimeSec;

        String summary = String.format("Run with \t p = %f \t MAX_ITER = %d \t MAX_TIME = %d sec", P, MAX_ITERATIONS,
                MAX_TIME_SEC);
        System.out.println(summary);

    }

    public static VariableSet runSLS(List<Vehicle> vehicles, List<Task> tasks) {
        VariableSet A = selectInitialSolution(vehicles, tasks);
        VariableSet A_old = null;
        COUNTER = 0;
        startTimeMillis = System.currentTimeMillis();
        do {
            A_old = A;
            Set<VariableSet> N = chooseNeightbours(A_old);
            A = localChoice(N, A_old);
            COUNTER++;
            if (CentralizedTemplate.DEBUG > 0) {
                System.out.println("************************ ITERATION " + COUNTER + "\t vehicles : " + A.getNumberUsedVehicles() + "\t Cost : " + A.getCost() + " ******************************");
                
                if (CentralizedTemplate.DEBUG > 1)
                    System.out.println(A_old);
            } else if (COUNTER % 100 == 0) {
                System.out.println("Iteration " + COUNTER + " Cost : " + A.getCost()); 
            }
        } while (!conditionIsMet(A, A_old));
        return A;
    }

    public static VariableSet selectInitialSolution(List<Vehicle> vehicles,  List<Task> tasks) {
        // create a basic environment of variables by giving all tasks to a vehicle
        return new VariableSet(vehicles, tasks);
    }

    public static boolean conditionIsMet(VariableSet A, VariableSet A_old) {
        // Condition on number of iterations
        if (COUNTER > MAX_ITERATIONS)
            return true;

        // Condition on time
        long currentTimeMillis = System.currentTimeMillis();
        long elapsedTimeSec = (currentTimeMillis - startTimeMillis) / 1000;
        if (elapsedTimeSec > MAX_TIME_SEC) 
            return true;

        return false;
    }

    public static Set<VariableSet> chooseNeightbours(VariableSet A_old) {
        Set<VariableSet> N = new HashSet<VariableSet>();
        int vi = A_old.getRandomAppropriateVehicle();

        // applying changing vehicle operator
        List<Set<VariableSet>> treeExploration = new LinkedList<Set<VariableSet>>();

        Set<VariableSet> initialSet = new HashSet<VariableSet>();
        initialSet.add(A_old);
        treeExploration.add(initialSet);
        
        for (int depth = 1; depth <= CentralizedTemplate.DEPTH_SEARCH; depth++) {
            Set<VariableSet> neighbors = new HashSet<VariableSet>();
            for (VariableSet vs : treeExploration.get(depth - 1)) {
                vi = vs.getRandomAppropriateVehicle();
                neighbors.addAll(applyChangeVehicleOperator(vs, vi, true));
            }
            treeExploration.add(neighbors);
        }

        // Merge all levels of the tree (except root=A_old)
        for (int i = 1; i < treeExploration.size(); i++) {
            N.addAll(treeExploration.get(i));
        }
        N.remove(A_old);
        
        vi = A_old.getRandomAppropriateVehicle();
        // applying changing task order operator, only if more than 2 tasks (4 TaskStep)
        int length = A_old.computeNumberOfTaskVehicle(vi);
        if (length >= 4) {
            for (int tIdX1 = 1; tIdX1 < length - 1; tIdX1++) {
                for (int tIdX2 = tIdX1 + 1; tIdX2 < length; tIdX2++) {
                    if (A_old.validChange(tIdX1, tIdX2, vi)) {
                        VariableSet A = changingTaskOrder(A_old, vi, tIdX1, tIdX2);
                        N.add(A);
                    }
                }
            }
        }

        return N;
    }

    /**
     * 
     * @param A_old Parent VariableSet 
     * @param N List of neighboring states
     * @param vi Change tasks in this vehicle
     * @param firstOnly true : only the first task of vi is moved in other vehicles. Otherwise, tries to move every task of vi
     */
    public static Set<VariableSet> applyChangeVehicleOperator(VariableSet A_old, int vi, boolean firstOnly) {
        Set<VariableSet> N = new HashSet<VariableSet>();
        if (firstOnly) {
            for (int vj = 0; vj < A_old.getNumberVehicles(); vj++) {
                if (vi == vj) continue;
                
                TaskStep t = A_old.getFirstStepOf(vi);
                // possible to put the task in the vehicle (in front of all) 
                if ( t.t.weight < A_old.getVehicleCapacity(vj)) {
                    VariableSet A = changingVehicle(A_old, vi, vj);
                    N.add(A);
                }
            }
        } else {
            List<Integer> pickupsIds = A_old.computePickupsIdVehicle(vi);
            for (int pickId : pickupsIds) {
                for (int vj = 0; vj < A_old.getNumberVehicles(); vj++) {
                    if (vi == vj) continue;
                    
                    TaskStep t = TaskStep.fromId(pickId);
                    // possible to put the task in the vehicle (in front of all) 
                    if ( t.t.weight < A_old.getVehicleCapacity(vj)) {
                        VariableSet A = changingVehicle(A_old, vi, vj, pickId);
                        N.add(A);
                    }
                }
            }
        }
        return N;
    }


    // DONE
    public static VariableSet changingTaskOrder(VariableSet A, int vi, int tIdX1, int tIdX2) {
        VariableSet A1 = A.copy();
        A1.changingTaskOrder(vi, tIdX1, tIdX2);
        return A1;
    }
    

    // DONE
    public static VariableSet changingVehicle(VariableSet A, int vi, int vj) {
        VariableSet A1 = A.copy();
        A1.changingVehicle(vi, vj);
        return A1;
    }

    public static VariableSet changingVehicle(VariableSet A, int vi, int vj, int pickup) {
        VariableSet A1 = A.copy();
        A1.changingVehicle(vi, vj, pickup);
        return A1;
    }


    public static VariableSet localChoice(Set<VariableSet> N, VariableSet A_old) {
        // return A with probability p

        VariableSet bestNeighbor = findBest(N);
        if (new Random().nextDouble() < CPMaker.P)
            return bestNeighbor;
        else    
            return A_old;
    }


    public static VariableSet findBest(Set<VariableSet> N) {
        boolean first = true;
        double minValue = -1;
        VariableSet minArg = null;

        for (VariableSet vs : N) {
            double cost = vs.getCost();

            // initialize minValue and minArg
            if (first) {    
                minValue = cost;
                minArg = vs;
                first = false;
            }

            if (cost < minValue) {
                minValue = cost;
                minArg = vs;
            }
        }
        return minArg;
    }
    
}
