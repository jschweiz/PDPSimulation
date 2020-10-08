package template;

import java.util.LinkedList;

public class TransitionList  {
	private LinkedList<Transition> list;

	public TransitionList() {
		this.list = new LinkedList<Transition>();
	}

	public void add(Transition t) {
		this.list.add(t);
	}

	public LinkedList<Transition> getList() {
		return list;
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
	
