package template;

import java.util.HashSet;
import java.util.Set;

import logist.task.TaskSet;

public class CPMaker {

    private static double EXPLORATION_FACTOR = 0.5;

    public CPMaker() {
        return;
    }

    // DONE
    public void runSLS(VariableSet X, DomainSet D, ConstraintSet C) {
        VariableSet A = selectInitialSolution(X, D, C);
        VariableSet A_old = null;
        do {
            A_old = A;
            Set<VariableSet> N = chooseNeightbours(A_old, X, D, C);
            A = localChoice(N, A_old);
        } while (conditionIsMet(X, D, C));

    }

    // TO DO
    public VariableSet selectInitialSolution(VariableSet X, DomainSet D, ConstraintSet C) {
        return X;
    }

    // TO DO
    public boolean conditionIsMet(VariableSet X, DomainSet D, ConstraintSet C) {
        return false;
    }

    // TO COMPLETE
    public Set<VariableSet> chooseNeightbours(VariableSet A_old, VariableSet X, DomainSet D, ConstraintSet C) {

        Set<VariableSet> N = new HashSet<VariableSet>();

        int vi = A_old.getRandomAppropriateVehicle();

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
        int lenght = A_old.computeNumberOfTaskVehicle(vi);

        if (lenght >= 2) {
            for (int tIdX1 = 1; tIdX1 < lenght - 1; tIdX1++) {
                for (int tIdX2 = tIdX1 + 1; tIdX2 < lenght; tIdX2++) {
                    VariableSet A = changingTaskOrder(A_old, vi, tIdX1, tIdX2);
                    N.add(A);
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
        VariableSet A = null;
        

        // return A with probability p (exploration factor)
        return (Math.random() < EXPLORATION_FACTOR? A: A_old);
    }
    
}
