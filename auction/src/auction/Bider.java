package auction;

import java.nio.channels.WritePendingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import logist.agent.Agent;
import logist.simulation.Vehicle;
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

    private HashMap<Integer, List<Task>> tasksOfAgents;
    private List<Long[]> bidsOfAgents;

    // risk measure
    private double[] probaToGoTo;
    private double[][] isThisValuable;

    // strategy
    private int moneyFromTasks = 0;

    public Bider(Topology topology, TaskDistribution distribution, Agent agent){
		this.topology = topology;
		this.distribution = distribution;
        this.agent = agent;
        this.bidNumber = 0;
        this.bidsOfAgents = new ArrayList<Long[]>();
        this.tasksOfAgents = new HashMap<Integer, List<Task>>();
        
        this.wonTasks = new ArrayList<Task>();
        this.wonVs = null;
        this.proposedVs = null;
        CPMaker.setParameters(-1, 1000, 5, -1, -1);
        

        int numCities = topology.cities().size();
        this.probaToGoTo = new double[numCities];
        this.isThisValuable = new double[numCities][numCities];
        this.setRiskProbabilities(topology, distribution);

        this.moneyFromTasks = 0;
    }

    
    public double calculateFactor(double p, double t, double M, double m, double x) {
        double factor;
        if (p < t) {
            factor = M - (M - x) / t * p;
        } else {
            factor = x + (x - m) / (x - t) * (t - p);
        }
        return factor;
    }


    /**
     * ESSENTIAL
     * Taskset.TASK_LIST is same the object as wonTasks
     */
    private class Computation {
        VariableSet v;
        List<Task> t;
        List<Vehicle> ve;
        VariableSet r;
        public Computation(VariableSet v, List<Task> t, List<Vehicle> ve) {
            this.v = v;
            this.t = t;
            this.ve = ve;
            this.r = null;
        }
        public Computation copy() {
            VariableSet vi = v == null ? null : v.copy();
            List<Task> ti = null;
            if (t != null) {
                ti = new LinkedList<>();
                ti.addAll(t);
            }
            List<Vehicle> vei = new LinkedList<>();
            vei.addAll(ve);
            return new Computation(vi, ti, vei);
        }
        
    }

    public void getResult(Computation c) {
        c.r = CPMaker.run(c.ve, c.t, c.v);
    }


    public long proposeTask(Task t) {
        wonTasks.add(t); // always first
        System.out.println("Bid #" + bidNumber + " with task " + t);
        bidNumber++;

        
        long benef = 0;
        double m = 0.5;
        double x = 0.8;
        double M = 0.9;
        double ti = 0.7;
        if (bidsOfAgents.size() > 4) {
            m += 0.2;
            x +=  0.2;
            M +=  0.2;
            ti -= 0.1;
        }


        if (wonVs != null) {
            benef = (long) (this.moneyFromTasks - wonVs.getCost());
            System.out.println("Currently, the total benef is :" + benef);
        }


        // current situation
        VariableSet startPoint = (wonVs != null) ? wonVs.copyPlusTask(t) : null;



        // create the computations
        int numThreads = 1;
        List<Computation> comp = new LinkedList<Bider.Computation>();
        comp.add(new Computation(startPoint, wonTasks, agent.vehicles()));
        for (int i = 1; i < numThreads; i++) {
            comp.add(comp.get(0).copy());
        }

        // compute them
        comp.stream()
            .parallel()
            .forEach((e) -> this.getResult(e));

        List<Double> costsFound = new LinkedList<>();
        double min = Double.MAX_VALUE;
        Computation minC = null;
        for (Computation c : comp) {
            double val = c.r.getCost();
            costsFound.add(val);
            if (val < min) {
                min = val;
                minC = c;
            }
        }
        proposedVs = minC.r;

        
        double currentCost = (wonVs != null) ? wonVs.getCost() : 0;
        double newCost = proposedVs.getCost();
        double marginalCost = newCost - currentCost;
        
        System.out.println( "Marginal price: " + marginalCost); // proposedVs +



        double qualityOfInvestment = isGoodInvestment(t);
        double factor = calculateFactor(qualityOfInvestment, ti, M, m, x);



        // compute bid based on strategy
        double minPrice = 500;
        minPrice *= Math.pow(factor, 3);


        double proposedPrice = Math.round(Double.max(0, marginalCost));
        proposedPrice *= factor;



        // calcul final bid
        long bid = (long) Double.max(minPrice, proposedPrice);

        System.out.println("Wanted task ? " + (int)(qualityOfInvestment*100) + "%");
        System.out.println("Factor: " + factor);
        System.out.println("Proposed price: " + proposedPrice);
        System.out.println("Minprice price: " + minPrice);
        System.out.println("Bid price: " + bid);

        
        wonTasks.remove(t); // always last
        return bid;
    }





    public VariableSet getVariableSet() {
        // VariableSet vs = CPMaker.run(agent.vehicles(), wonTasks, null);
        VariableSet vs = CPMaker.run(agent.vehicles(), null, wonVs);
        System.out.println(vs);

        // VariableSet vsRand = CPMaker.randomShake(vs, 10);
        // System.out.println("modif \n" + vsRand);
        return vs;
    }


    // function to handle bid steps
    
    public void auctionResult(Task previous, int winner, Long[] bids) {
        bidsOfAgents.add(bids);
        updateTaskOfAgents(previous, winner, bids.length);
        if (winner == agent.id()) {
            addTask(previous);
            this.moneyFromTasks += bids[this.agent.id()];
            System.out.println("GOT THE BID\n\n");
        } else {
            System.out.println("DID NOT GET THE BID\n\n");
        }
    }

    private void updateTaskOfAgents(Task t, int winner, int numAgents) {
        if (!tasksOfAgents.containsKey(winner))
            tasksOfAgents.put(winner, new ArrayList<Task>());
        tasksOfAgents.get(winner).add(t);
    }

    public void addTask(Task t) {
        wonVs = proposedVs;
        wonTasks.add(t);
    }


    // risk analyze
    
    private void setRiskProbabilities(Topology topo, TaskDistribution td) {
        // initialize
        for (City from : topo.cities()) {
            this.probaToGoTo[from.id] = 0;
            for (City to : topo.cities()) this.isThisValuable[from.id][to.id] = 0;
        }

        // proba to go through a city
        for (City from : topo.cities()) {
            for (City to : topo.cities()) {
                double proba = td.probability(from, to);
                // System.out.println(from.pathTo(to).size());
                List<City> path = from.pathTo(to);
                this.probaToGoTo[from.id] += proba;
                for (City c : path) this.probaToGoTo[c.id] += proba;
            }
        }

        for (City from : topo.cities()) {
            for (City to : topo.cities()) {
                for (City c : from.pathTo(to)) this.isThisValuable[from.id][to.id] += this.probaToGoTo[c.id];
            }
        }

        // find min and max for nomalization
        normalize(isThisValuable);
    }

    private void normalize(double[][] isThisValuable) {
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;
        double val;
        for (City from : this.topology.cities()) {
            for (City to : this.topology.cities()) {
                val = isThisValuable[from.id][to.id];
                if (val < minValue && val != 0) minValue = val;
                if (val > maxValue) maxValue = val;
            }
        }

        // normalize
        for (City from : this.topology.cities()) {
            for (City to : this.topology.cities()) {
                if (isThisValuable[from.id][to.id] != 0) {
                    isThisValuable[from.id][to.id] =  (isThisValuable[from.id][to.id] - minValue)/(maxValue - minValue);
                }
            }
        }
    }

    private double isGoodInvestment(Task t) {
        return this.isThisValuable[t.pickupCity.id][t.deliveryCity.id];
    }
}
