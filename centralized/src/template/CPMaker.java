package template;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class CPMaker {

    private static double EXPLORATION_FACTOR = 0.5;
    private static int COUNTER = 0;
    private static int ITERATIONS = 30;
    private static boolean COUTING_ITERATIONS = true;

    // DONE
    public VariableSet runSLS(List<Vehicle> vehicles, TaskSet tasks) {
        VariableSet A = selectInitialSolution(vehicles, tasks);
        VariableSet A_old = null;
        int counter = 0;
        do {
            System.out.println("\n\n\n\n***********************************************************************************************");
            System.out.println("************************************ITERATION "+counter +"******************************************");
            System.out.println("***********************************************************************************************");
            A_old = A;
            System.out.println(A_old);
            Set<VariableSet> N = chooseNeightbours(A_old);
            System.out.println("Found " + N.size() + " possible states");
            A = localChoice(N, A_old);
            // System.out.println(A_old.compare(A));
            counter++;
        } while (!conditionIsMet(A, A_old));

        return A;
    }

    // DONE
    public VariableSet selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
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
        System.out.println("Picked vehicle " + vi);

        // applying changing vehicle operator
        for (int vj = 0; vj < A_old.getNumberVehicles(); vj++) {
            if (vi == vj) continue;

            TaskStep t = A_old.getTaskSetOfVehicle(vi);

            /* possible de charger cette tache dans le vehicule*/
            if ( t.t.weight < A_old.getCapacityVehicle(vj)) {
                VariableSet A = changingVehicle(A_old, vi, vj);
                N.add(A);
            }
        }

        // applying changing task order operator
        // compute the number of tasks of the vehicle
        int length = A_old.computeNumberOfTaskVehicle(vi);
        System.out.println("length is" + length);
        if (length >= 2) {
            for (int tIdX1 = 1; tIdX1 < length - 1; tIdX1++) {
                for (int tIdX2 = tIdX1 + 1; tIdX2 < length; tIdX2++) {
                    // System.out.println("Testing exchanging" + TaskStep.fromId(tIdX1) + "            -----       " +  TaskStep.fromId(tIdX2) + "   ");// + A_old.validChange(tIdX1, tIdX2));
                    if (A_old.validChange(tIdX1, tIdX2, vi)) {
                        System.out.println("Works exchanging          " +tIdX1 + "   -----   " +  tIdX2 + "   ");// + A_old.validChange(tIdX1, tIdX2));
                        System.out.println("=================================>TRUE");
                        // System.out.println("Working exchanging" + tIdX1 + ":" + tIdX2);
                        // System.out.println("BEFORE CHANGE" + A_old);
                        VariableSet A = changingTaskOrder(A_old, vi, tIdX1, tIdX2);
                        // System.out.println(A_old.compare(A));
                        // System.out.println("AFTER CHANGE" + A);
                        N.add(A);
                    } else {
                        System.out.println("=================================>FALSE");
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
        VariableSet A = null;
        

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
