package template;

import java.util.LinkedList;

import logist.topology.Topology.City;

public class State {
    private City state;
    private LinkedList<AvailableAction> availableActions;

    public State(City state) {
        this.state = state;
        availableActions = new LinkedList<AvailableAction>();

        for (City action : state.neighbors())
            availableActions.add(new AvailableAction(action));
            
        availableActions.add(new AvailableAction(null));
    }

    public LinkedList<AvailableAction> getAvailableActions() {
        return availableActions;
    }

    public City getCity() {
        return state;
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public boolean equals(Object s) {
        if (s != null && (s instanceof State)) {
            return state.equals(((State)s).getCity());
        }
        return false;
    }
}
