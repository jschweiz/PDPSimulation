package auction;

import java.util.ArrayList;
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

    public Bider(Topology topology, TaskDistribution distribution, Agent agent){
		this.topology = topology;
		this.distribution = distribution;
        this.agent = agent;
        
        this.wonTasks = new ArrayList<Task>();
        this.wonVs = null;
        this.proposedVs = null;
        CPMaker.setParameters(-1, 4000, 5, -1, -1);
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

        VariableSet startPoint = (wonVs != null) ? wonVs.copyPlusTask(t) : null;
        proposedVs = CPMaker.run(agent.vehicles(), wonTasks, startPoint);
        
        double newCost = proposedVs.getCost();
        System.out.println(proposedVs);
        
        double currentCost = (wonVs != null) ? wonVs.getCost() : 0;
        double bid = Double.max(0, newCost - currentCost);

        wonTasks.remove(t); // always last
        return (long) Math.round(bid);
    }

    public VariableSet getVS() {
        VariableSet vs = CPMaker.run(agent.vehicles(), null, wonVs);
        System.out.println(vs.getTrueCost());
        return vs;
    }
}
