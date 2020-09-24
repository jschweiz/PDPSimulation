package template;

import logist.topology.Topology.City;

public class AvailableAction {
    City nextState;
    boolean packetPickup;

    public AvailableAction(City nextState){
        if (nextState == null){
            this.nextState = null;
            this.packetPickup = true;
        } else {
            this.nextState = nextState;
            this.packetPickup = false;
        }
    }

    public State getNextState() {
        return new State(nextState);
    }

    public boolean isPickup() {
        return packetPickup;
    }

    @Override
    public int hashCode() {
        if (packetPickup)
            return 1;
        else 
            return nextState.hashCode();
    }

    @Override
    public String toString() {
        if (packetPickup)
            return "PICKUP";
        return "GOTO" + nextState.toString();
    }


    @Override
    public boolean equals(Object s){
        if (s != null && (s instanceof AvailableAction)) {
            AvailableAction a = (AvailableAction)s;
            if (this.packetPickup) {
                return a.packetPickup;
            }
            return nextState.equals(a.nextState);
        }
        return false;
    }
}
