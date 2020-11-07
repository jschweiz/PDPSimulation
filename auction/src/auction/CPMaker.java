package auction;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;

public class CPMaker {

    // Strings
    private static String SUMMARY_STRING = "Run with \t p = %f \t MAX_ITER = %d \t MAX_TIME = %d sec \t DEPTH_SEARCH = %d";
    private static String SUB_ITERATION_STRING = "Iteration %d Cost : %f";
    private static String ITERATION_STRING
            = "************************ ITERATION %d \t vehicles : %d\t Cost : %f ******************************";

    // General parameters
    // general parameters for model
    private static double P = 0.3;
    private static int MAX_ITERATIONS = 4000;
    private static long MAX_TIME_SEC = 30;
    private static long DEPTH_SEARCH = 8;
    public static int DEBUG = 0;
    private static boolean BETA = false;

    // exploration factors
    private static double TAKE_BEST_THRESHOLD = 0.4;
    private static double TAKE_BEST_VARIABILITY = 0.35;
    private static double EXPLORE_THRESHOLD_DEFAULT = 0;
    private static double EXPLORE_THRESHOLD_STABLE = 0.005;
    private static double EXPLORE_THRESHOLD_BOTTOM = 0.01;

    // object variables
    private static long START_TIME_MILLIS;
    private static int COUNTER = 1;
    private static Set<VariableSet> localMinima = new HashSet<VariableSet>();

    /**
     * Set the static parameters of the plan maker.
     * 
     * @param p          This is the probability to choose the best neighbour
     * @param maxIter    This is the max number of iterations after which the plan
     *                   maker returns
     * @param maxTimeSec This is the max number of seconds after which the plan
     *                   maker returns
     * @param debug      This is the LOG level (-1, 0 and 1) to choose how much is printed
     * @return Nothing
     */
    public static void setParameters(double p, int maxIter, long maxTimeSec, int depthSearch, int debug) {
        if (p >= 0) P = p;
        if (maxIter > 0) MAX_ITERATIONS = maxIter;
        if (maxTimeSec > 0) MAX_TIME_SEC = maxTimeSec;
        if (depthSearch > 0) DEPTH_SEARCH = depthSearch;
        if (debug >= 0 && debug <= 2) DEBUG = debug;
        System.out.println(String.format(SUMMARY_STRING, P, MAX_ITERATIONS, MAX_TIME_SEC, DEPTH_SEARCH));
    }

    public static void initialize() {
        localMinima.clear();
    }

     /**
     * Run the SLS algorithm to find an (optimal) repartition of tasks accross vehicles.
     * 
     * @param vehicles   List of available vehicles
     * @param maxIter    List of tasks that need to be picked and delivered
     * @return VariableSet (optimal) repartition of tasks accross vehicles
     */
    public static VariableSet run(List<Vehicle> vehicles, List<Task> tasks){
        initialize();
        long time_start = System.currentTimeMillis();
        VariableSet vs = runSLS(vehicles, tasks);
        long time_stop = System.currentTimeMillis();
        
        if(DEBUG > 0) System.out.println("Plan computed in : " + (time_stop-time_start) + " ms");
        if(DEBUG > 0) System.out.println("Final state : \n" + vs);

        return vs;
    }
   
    public static VariableSet runSLS(List<Vehicle> vehicles, List<Task> tasks) {
        VariableSet A = selectInitialSolution(vehicles, tasks);
        VariableSet A_old = null;
        do {
            A_old = A;
            Set<VariableSet> N = chooseNeighbours(A_old);
            A = localChoice(N, A_old);
            printReport(A, A_old);
        } while (!conditionIsMet(A, A_old));
        localMinima.add(A);
        A = findBest(localMinima);
        // System.out.println("Final minimum has cost " + A.getCost());
        return A;
    }

    private static VariableSet selectInitialSolution(List<Vehicle> vehicles,  List<Task> tasks) {
        // initialize variables for algo
        START_TIME_MILLIS = System.currentTimeMillis();
        COUNTER = 0;
        // create a basic environment of variables by giving all tasks to a vehicle
        return new VariableSet(vehicles, tasks);
    }

    private static boolean conditionIsMet(VariableSet A, VariableSet A_old) {
        COUNTER++;
        // Condition on number of iterations
        if (COUNTER > MAX_ITERATIONS)
            return true;

        // Condition on time
        long elapsedTimeSec = (System.currentTimeMillis() - START_TIME_MILLIS) / 1000;
        if (elapsedTimeSec > MAX_TIME_SEC) 
            return true;

        return false;
    }

    private static Set<VariableSet> chooseNeighbours(VariableSet A_old) {
        Set<VariableSet> N = new HashSet<VariableSet>();
        int vi;

        // applying changing vehicle operator
        List<Set<VariableSet>> treeExploration = new LinkedList<Set<VariableSet>>();

        Set<VariableSet> initialSet = new HashSet<VariableSet>();
        initialSet.add(A_old);
        treeExploration.add(initialSet);
        
        for (int depth = 1; depth <= DEPTH_SEARCH; depth++) {
            Set<VariableSet> neighbors = new HashSet<VariableSet>();
            for (VariableSet vs : treeExploration.get(depth - 1)) {
                vi = vs.getRandomAppropriateVehicle();
                neighbors.addAll(applyChangeVehicleOperator(vs, vi));
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
    private static Set<VariableSet> applyChangeVehicleOperator(VariableSet A_old, int vi) {
        Set<VariableSet> N = new HashSet<VariableSet>();
        if (!BETA) {
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


    private static VariableSet changingTaskOrder(VariableSet A, int vi, int tIdX1, int tIdX2) {
        VariableSet A1 = A.copy();
        A1.changingTaskOrder(vi, tIdX1, tIdX2);
        return A1;
    }
    

    // DONE
    private static VariableSet changingVehicle(VariableSet A, int vi, int vj) {
        VariableSet A1 = A.copy();
        A1.changingVehicle(vi, vj);
        return A1;
    }

    private static VariableSet changingVehicle(VariableSet A, int vi, int vj, int pickup) {
        VariableSet A1 = A.copy();
        A1.changingVehicle(vi, vj, pickup);
        return A1;
    }

    private static VariableSet localChoice(Set<VariableSet> N, VariableSet A_old) {
        // return A with probability p
        VariableSet bestNeighbor = findBest(N);

        // facilitate moving to a solution with less vehicles
        double takeBestThreshold = TAKE_BEST_THRESHOLD;
        int diffVehicle = A_old.getNumberUsedVehicles() - bestNeighbor.getNumberUsedVehicles();
        if (diffVehicle < 0) { // more vehicles
            takeBestThreshold -= TAKE_BEST_VARIABILITY;
        } else if (diffVehicle > 0) { // less vehicles
            takeBestThreshold += TAKE_BEST_VARIABILITY;
        }

        // encorage exploration when stable or stuck in local minimum
        double explorationThreshold = EXPLORE_THRESHOLD_DEFAULT;
        double diffCost = A_old.getCost() - bestNeighbor.getCost();
        if ( diffCost == 0 ) { // stable
            explorationThreshold = EXPLORE_THRESHOLD_STABLE;
        } else if ( diffCost <= 0) { // for sure a local min
            explorationThreshold = EXPLORE_THRESHOLD_BOTTOM;
        }

        // choose neightbor according to proba
        double rand = new Random().nextDouble();
        if (rand < takeBestThreshold) {
            return bestNeighbor;
        } else if (rand > 1 - explorationThreshold) {
            localMinima.add(A_old);
            return getRandom(N);
        } else {
            return A_old;
        }    
    }

    private static VariableSet findBest(Set<VariableSet> N) {
        double minValue = Double.MAX_VALUE;
        VariableSet minArg = null;

        for (VariableSet vs : N) {
            double cost = vs.getCost();
            if (cost < minValue) {
                minValue = cost;
                minArg = vs;
            }
        }
        return minArg;
    }

    private static VariableSet getRandom(Set<VariableSet> N) {
        int randIndex = new Random().nextInt(N.size());
        int count = 0;
        for (VariableSet v : N) {
            if (count == randIndex) {
                return v;
            }
            count++;
        }
        return null;
    }

    private static void printReport(VariableSet A, VariableSet A_old) {
        if (CPMaker.DEBUG > 0) {
            System.out.println(String.format(ITERATION_STRING, COUNTER, A.getNumberUsedVehicles(), A.getCost()));
            if (CPMaker.DEBUG > 1) System.out.println(A_old);
        } 
        // else if (COUNTER % 100 == 0) {
        //     System.out.println(String.format(SUB_ITERATION_STRING, COUNTER, A.getCost())); 
        // }
    }
    
}