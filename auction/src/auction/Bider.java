package auction;

import java.util.LinkedList;
import java.util.List;

import logist.agent.Agent;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

public class Bider {
	private Topology topology;
	private TaskDistribution distribution;
    private Agent agent;
    
    private List<Task> tasks;
    private double currentCost;

    public Bider(Topology topology, TaskDistribution distribution, Agent agent){
		this.topology = topology;
		this.distribution = distribution;
        this.agent = agent;
        
        this.tasks = new LinkedList<Task>();
        CPMaker.setParameters(-1, 4000, 5, -1, -1);
        currentCost = 0;
    }

    public void addTask(Task t) {
        tasks.add(t);
    }

    public long proposeTask(Task t) {
        tasks.add(t);
        VariableSet vs = CPMaker.run(agent.vehicles(), tasks, null);
        double newCost = vs.getCost();
        System.out.println(vs);
        tasks.remove(t);

        double bid = Double.max(0, newCost - currentCost);
        return (long) Math.round(bid);
    }

    public VariableSet getVS() {
        return CPMaker.run(agent.vehicles(), tasks, null);
    }
    
}
