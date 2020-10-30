package template;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;

public class CPMaker {

    private static double EXPLORATION_FACTOR = 0.5;
    private static int COUNTER = 0;
    private static int ITERATIONS = 30;
    private static boolean COUTING_ITERATIONS = true;

    // DONE
    public VariableSet runSLS(List<Vehicle> vehicles, List<Task> tasks) {
        VariableSet A = selectInitialSolution(vehicles, tasks);
        VariableSet A_old = null;
        int counter = 0;
        do {
            A_old = A;
            System.out.println("\n\n\n\n***********************************************************************************************");
            System.out.println("************************************ITERATION "+counter +"******************************************");
            System.out.println("***********************************************************************************************");
            System.out.println(A_old);
            Set<VariableSet> N = chooseNeightbours(A_old);
            A = localChoice(N, A_old);
            // System.out.println(A_old.compare(A));
            counter++;
        } while (!conditionIsMet(A, A_old));
        return A;
    }

    // DONE
    public VariableSet selectInitialSolution(List<Vehicle> vehicles,  List<Task> tasks) {
        // create a basic environment of variables by giving all tasks to a vehicle
        return new VariableSet(vehicles, tasks);
    }

    // TO COMPLETE
    public boolean conditionIsMet(VariableSet A, VariableSet A_old) {
        if (COUTING_ITERATIONS) {
            if (COUNTER > ITERATIONS) {
                return true;
            }
            COUNTER++;
            return false;
        } else {
            // compute something else
            return false;
        }
    }

    // DONE
    public Set<VariableSet> chooseNeightbours(VariableSet A_old) {

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
    public VariableSet changingTaskOrder(VariableSet A, int vi, int tIdX1, int tIdX2) {
        VariableSet A1 = A.copy();
        A1.changingTaskOrder(vi, tIdX1, tIdX2);
        return A1;
    }
    

    // DONE
    public VariableSet changingVehicle(VariableSet A, int vi, int vj) {
        VariableSet A1 = A.copy();
        A1.changingVehicle(vi, vj);
        return A1;
    }


    // TO DO : thinking required 
    public VariableSet localChoice(Set<VariableSet> N, VariableSet A_old) {

        // select best A among the N
        int n = N.size();
        int rand = new Random().nextInt(n);
        

        // return A with probability p (exploration factor)
        // return (Math.random() < EXPLORATION_FACTOR? A: A_old);
        int count = 0;
        for (VariableSet v : N) {
            if (count == rand) {
                return v;
            }
            count++;
        }
        return null;
    }
    
}
