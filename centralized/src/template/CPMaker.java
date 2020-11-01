package template;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;

public class CPMaker {
    private static double P = 0.4;
    private static int MAX_ITERATIONS = 30;
    private static int MAX_TIME_SEC = 30;

    private static long startTimeMillis;
    private static int COUNTER = 0;
    
    /**
     * Set the static parameters of the plan maker.
     * @param p This is the probability to choose the best neighbour
     * @param maxIter This is the max number of iterations after which the plan maker returns
     * @param maxTimeSec This is the max number of seconds after which the plan maker returns
     * @return Nothing
     */
    public static void setParameters(double p, int maxIter, int maxTimeSec) {
        if (p >= 0)
            P = p;
        if (maxIter > 0)
            MAX_ITERATIONS = maxIter;
        if (maxTimeSec > 0)
            MAX_TIME_SEC = maxTimeSec;

        String summary = String.format("Run with \t p = %f \t MAX_ITER = %d \t MAX_TIME = %d", P, MAX_ITERATIONS, MAX_TIME_SEC);
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
            if (CentralizedTemplate.DEBUG) {
                System.out.println("\n************************************ITERATION "+ COUNTER +" ******************************************");  
                System.out.println(A_old);
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
        for (int vj = 0; vj < A_old.getNumberVehicles(); vj++) {
            if (vi == vj) continue;
            
            TaskStep t = A_old.getFirstStepOf(vi);
            // possible to put the task in the vehicle (in front of all) 
            if ( t.t.weight < A_old.getVehicleCapacity(vj)) {
                VariableSet A = changingVehicle(A_old, vi, vj);
                N.add(A);
            }
        }

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


    public static VariableSet localChoice(Set<VariableSet> N, VariableSet A_old) {
        // return A with probability p
        if (new Random().nextDouble() < CPMaker.P) 
            return findBest(N);
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
