package template;

import java.util.LinkedList;

public class TransitionList  {
	private LinkedList<Transition> list;
	private int cost;

	public TransitionList() {
		this.list = new LinkedList<Transition>();
		this.cost = 0;
	}

	public void add(Transition t) {
		this.list.add(t);
		cost += t.cost;
	}

	public LinkedList<Transition> getList() {
		return list;
	}

	public int getCost() {
		return cost;
	}

	// clone function
	public TransitionList clone() {
		TransitionList newT = new TransitionList();
		for (Transition t : list) {
			newT.add(t);
		}
		return newT;
	}

	//tostring function for printing
	public String toString() {
		String string = "LISTOFTRANS:";
		for (Transition t : list) {
			string += t.toString();
		}
		return string;
	}
}
	
