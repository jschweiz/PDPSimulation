package auction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import common.CPMaker;
import common.VariableSet;
import logist.agent.Agent;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Bider {
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;

    private List<Task> wonTasks;
    private VariableSet wonVs;
    private VariableSet proposedVs;
    private int bidNumber;

    // history to plot
    private List<Long> totalBenefList;
    private List<Long> marginalCostList;
    private List<Long> costList;
    private List<Long> totalGainList;
    private List<Long[]> bidsOfAgents;
    private HashMap<Integer, List<Task>> tasksOfAgents;

    // risk measure
    private double[] probaToGoTo;
    private double[][] isThisValuable;

    // strategy
    private int currentRevenuFromTasks = 0;
    private int minBidToGetBackPositive;

    public Bider(Topology topology, TaskDistribution distribution, Agent agent) {
        // initialize basic variables
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

        // initialize bidding tournament
        this.bidNumber = 0;
        this.wonTasks = new ArrayList<Task>();
        this.wonVs = null;
        this.proposedVs = null;

        // initialize risk calculation
        int numCities = topology.cities().size();
        this.probaToGoTo = new double[numCities];
        this.isThisValuable = new double[numCities][numCities];
        this.setRiskProbabilities();

        this.currentRevenuFromTasks = 0;
        // initalize histories of all bids / strategy choices
        this.totalBenefList = new ArrayList<>();
        this.marginalCostList = new ArrayList<>();
        this.costList = new ArrayList<>();
        this.totalGainList = new ArrayList<>();
        this.bidsOfAgents = new ArrayList<>();
        this.tasksOfAgents = new HashMap<>();
    }

    // compute the strategy

    public long proposeTask(Task t) {
        wonTasks.add(t); // always first
        System.out.println("Bid #" + bidNumber + " with task " + t);
        bidNumber++;

        long currentRevenu = wonVs == null ? 0 : (long) (this.currentRevenuFromTasks - wonVs.getCost());
        // System.out.println("Current total profit is : " + currentRevenu);

        // compute optiomal plan of previous plan plus the new task
        VariableSet startPoint = (wonVs != null) ? wonVs.copyPlusTask(t) : null;
        proposedVs = CPMaker.run(agent.vehicles(), wonTasks, startPoint);

        // compute marginal cost
        double lastStepCost = (wonVs != null) ? wonVs.getCost() : 0;
        double newPlanCost = proposedVs.getCost();
        double marginalCost = newPlanCost - lastStepCost;
        // System.out.println( "Marginal price is : " + marginalCost);

        // Check if new plan's cost < old plan's cost
        if (marginalCost < -10) {
            // System.out.println("WonVs      : " + wonVs.getCost());
            // System.out.println("proposedVs : " + proposedVs.getCost());
            try {
                wonVs = proposedVs.copyMinusLastTask();
                // System.out.println("WonVS after proposed task removed: " + wonVs.getCost() + "\t" + wonVs.getTrueCost());
            }
            catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Error in task removal : ignored");
            }
        }

        // calcul final bid
        long bid = computePriceWithStrategy(marginalCost, newPlanCost, t);
        // System.out.println("Final bid is : " + bid);

        // note for history
        totalBenefList.add(currentRevenu);
        marginalCostList.add((long) marginalCost);

        wonTasks.remove(t); // always last
        return bid;
    }

    private static double TI = 0.6;

    public long computePriceWithStrategy(double marginalCost, double newPlanCost, Task t) {
        int changeStratTask = 3;

        double m = 0.4;
        double x = 0.7;
        double M = 1;
        long minPrice = 300;

        // adapt factors to the time-beeing
        long benefits = (wonVs == null) ? 0 : (long) (this.currentRevenuFromTasks - wonVs.getCost());

        if (wonTasks.size() == changeStratTask) {
            minBidToGetBackPositive = (int) Math.abs(benefits / (changeStratTask));
        }

        if (wonTasks.size() > changeStratTask && benefits < 0) {
            m = 0.98;
            x = 1.1;
            M = 1.3;
            minPrice = minBidToGetBackPositive;
        }

        if (wonTasks.size() > changeStratTask && benefits >= 0) {
            m = 0.95;
            x = 1.1;
            M = 1.4;
            minPrice = 500;
        }

        // compute risk adversion
        double qualityOfInvestment = measureRisk(t);
        double marginalMultiplicationFactor = computeRiskFactor(qualityOfInvestment, TI, M, m, x);

        // compute adapted marginal cost
        long marginalAdaptedPrice = (long) (Double.max(0, marginalCost) * marginalMultiplicationFactor);

        // compute minimal price
        long minimalBidPrice = (long) (minPrice * Math.pow(marginalMultiplicationFactor, 2));

        // final bid
        long bid = Long.max(minimalBidPrice, marginalAdaptedPrice);
        // String s = "risk-proba: %d , marg-factor: %f, marg-adapted-price: %d,
        // min-price: %d, bid-price: %d";
        // System.out.println(String.format(s,
        // (int)(qualityOfInvestment*100),
        // marginalMultiplicationFactor,
        // marginalAdaptedPrice,
        // minimalBidPrice,
        // bid));

        // if one versus one, adapt to his bids
        if (this.bidsOfAgents.size() > 0 && this.bidsOfAgents.get(0).length == 2) {
            double opponentRisk = measureOpponentStrenght();
            bid *= opponentRisk;
        }

        return bid;
    }

    public double measureOpponentStrenght() {

        // factor regarding market share [0.9; 1.1]
        int numOfTasks = this.bidsOfAgents.size();
        double marketShare = (this.wonTasks.size() - 1) / ((double) numOfTasks) - 0.5;
        double opponentShareFactor = (marketShare*1/5 + 1);

        // // factor regarding bid difference [0.9; 1.1]
        // long bidDifference = 0;
        // for (int i = Integer.max(0, bidNumber - 5); i < numOfTasks; i++) {
        //     bidDifference -= this.bidsOfAgents.get(i)[0];
        // }
        // double average = -bidDifference/Integer.min(numOfTasks, 5);
        // for (int i = Integer.max(0, bidNumber - 5); i < numOfTasks; i++) {
        //     // bidDifference += 1000;
        //     bidDifference += this.bidsOfAgents.get(i)[1];
        // }
        // double averageBidDiff = bidDifference/Integer.min(numOfTasks, 5);
        // double multiplicatorDiff = (averageBidDiff - 0.1 * average) / (2 * average) + 1;
        // double oponentBidFactor = Double.min(1.5, Double.max(0.5, multiplicatorDiff));

        // // mean of the two factors
        // double finality = (opponentShareFactor * oponentBidFactor);// / 2;

        // System.out.println("OpponentStrengh: shareFactor:" + opponentShareFactor
        //         + " averageBidDiff:" + averageBidDiff
        //         + " oponentBidFactor: " + oponentBidFactor
        //         + " finality:" + finality);
        // System.out.println("Opponent share factor: " + opponentShareFactor);
        return opponentShareFactor;
    }

    public double computeRiskFactor(double p, double t, double M, double m, double x) {
        double factor;
        if (p < t) {
            factor = M - (M - x) / t * p;
        } else {
            factor = x + (x - m) / (1 - t) * (t - p);
        }
        return factor;
    }

    // function to handle bid steps

    public void auctionResult(Task previousTask, int winner, Long[] bids) {
        updateTaskOfAgents(previousTask, winner); // add the task to the winner set

        boolean taskWon = winner == agent.id();
        if (taskWon) { // if we won
            updateOurPlan(previousTask);
            // System.out.println("GOT THE BID\n\n");
        } else {
            // System.out.println("DID NOT GET THE BID\n\n");
        }

        // save history
        this.bidsOfAgents.add(bids); // note for history
        this.currentRevenuFromTasks += taskWon ? bids[this.agent.id()] : 0;
        this.totalGainList.add((long) this.currentRevenuFromTasks);
        this.costList.add(this.wonVs != null ? (long) this.wonVs.getCost() : 0);
    }

    private void updateTaskOfAgents(Task t, int winner) {
        if (!tasksOfAgents.containsKey(winner))
            tasksOfAgents.put(winner, new ArrayList<Task>());
        tasksOfAgents.get(winner).add(t);
    }

    private void updateOurPlan(Task t) {
        wonVs = proposedVs;
        wonTasks.add(t);
    }

    // risk analyze

    private void setRiskProbabilities() {
        initializeRisk();

        // proba to go through a city
        for (City from : this.topology.cities()) {
            for (City to : this.topology.cities()) {
                double proba = this.distribution.probability(from, to);
                List<City> path = from.pathTo(to);
                this.probaToGoTo[from.id] += proba;
                for (City c : path)
                    this.probaToGoTo[c.id] += proba;
            }
        }

        // valuation of a task (does it require to go throught rewarding cities) ?
        for (City from : this.topology.cities()) {
            for (City to : this.topology.cities()) {
                for (City c : from.pathTo(to))
                    this.isThisValuable[from.id][to.id] += this.probaToGoTo[c.id];
            }
        }

        // find min and max for nomalization
        normalizeRisk();
    }

    private void initializeRisk() {
        for (City from : this.topology.cities()) {
            this.probaToGoTo[from.id] = 0;
            for (City to : this.topology.cities())
                this.isThisValuable[from.id][to.id] = 0;
        }
    }

    private void normalizeRisk() {
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;
        double val;
        for (City from : this.topology.cities()) {
            for (City to : this.topology.cities()) {
                val = this.isThisValuable[from.id][to.id];
                if (val < minValue && val != 0)
                    minValue = val;
                if (val > maxValue)
                    maxValue = val;
            }
        }

        // normalize
        List<Double> allValues = new ArrayList<>();
        for (City from : this.topology.cities()) {
            for (City to : this.topology.cities()) {
                if (this.isThisValuable[from.id][to.id] != 0) {
                    val = (this.isThisValuable[from.id][to.id] - minValue)
                            / (maxValue - minValue);
                    this.isThisValuable[from.id][to.id] = val;
                    allValues.add(val);
                }
            }
        }

        Collections.sort(allValues);
        TI = allValues.get(allValues.size()/2);
    }

    private double measureRisk(Task t) {
        return this.isThisValuable[t.pickupCity.id][t.deliveryCity.id];
    }

    // function to return the final plan
    public VariableSet getVariableSet() {
        VariableSet vs = CPMaker.run(agent.vehicles(), null, wonVs);
        // System.out.println("WonVS with recompute : " + vs.getCost());
        // System.out.println(vs);
        return vs;
    }

    // function to write in order to plot
    public void writeToFile() {
        try {
            int rd = (new Random()).nextInt(1000);
            System.out.println("Writing in " + rd);
            FileWriter myWriter = new FileWriter("result_investor_" + rd + ".txt");
            myWriter.write("taskid,bidAgent1,bidAgent2,totalBenef,margCost,totalCost,totalRevenu\n");
            for (int i = 0; i < totalBenefList.size(); i++) {
                long v1 = bidsOfAgents.get(i)[this.agent.id()];
                long v2 = bidsOfAgents.get(i).length > 1 ? bidsOfAgents.get(i)[1 - this.agent.id()] : 0;
                myWriter.write(
                        i + "," + v1 + "," + v2 + "," + this.totalBenefList.get(i) + "," + this.marginalCostList.get(i)
                                + ',' + this.costList.get(i) + "," + this.totalGainList.get(i) + "\n");
            }
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
