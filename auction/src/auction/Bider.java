package auction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.agent.Agent;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

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
        CPMaker.setParameters(-1, 2000, 5, -1, -1);
    }

    public void auctionResult(Task previous, int winner, Long[] bids) {
        bidsOfAgents.add(bids);
        updateTaskOfAgents(previous, winner, bids.length);
        if (winner == agent.id()) {
			addTask(previous);
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

    /**
     * ESSENTIAL
     * Taskset.TASK_LIST is same the object as wonTasks
     */
    public long proposeTask(Task t) {
        wonTasks.add(t); // always first
        System.out.println("Bid #" + bidNumber);
        bidNumber++;

        VariableSet startPoint = (wonVs != null) ? wonVs.copyPlusTask(t) : null;
        proposedVs = CPMaker.run(agent.vehicles(), wonTasks, startPoint);
        
        double newCost = proposedVs.getCost();
        System.out.println(proposedVs);
        
        double currentCost = (wonVs != null) ? wonVs.getCost() : 0;
        double bid = Double.max(0, newCost - currentCost);

        wonTasks.remove(t); // always last
        return (long) Math.round(bid) + 10;
    }

    public VariableSet getVS() {
        // VariableSet vs = CPMaker.run(agent.vehicles(), wonTasks, null);
        VariableSet vs = CPMaker.run(agent.vehicles(), null, wonVs);
        System.out.println(vs);

        // VariableSet vsRand = CPMaker.randomShake(vs, 10);
        // System.out.println("modif \n" + vsRand);
        return vs;
    }
}
