package reactive;

import logist.topology.Topology.City;
import static reactive.Cts.PICKUPSTRING;
import static reactive.Cts.GOTOSTRING;

public class AvailableAction {
    private final City destination;
    private final boolean isDelivery;

    public AvailableAction(City destination, boolean packet){
        this.destination = destination;
        this.isDelivery = packet;
    }

    // Getters 
    public City getDestination() {
        return destination;
    }

    public boolean isDelivery() {
        return isDelivery;
    }

    // to display easily
    @Override
    public String toString() {
        if (isDelivery) return String.format(PICKUPSTRING, destination);
        return String.format(GOTOSTRING, destination);
    }

    // hashCode and equals necessary to put into HashMaps
    @Override
    public int hashCode() {
        if (isDelivery)
            return 1;
        else 
            return destination.hashCode();
    }

    @Override
    public boolean equals(Object s){
        if (s != null && (s instanceof AvailableAction)) {
            AvailableAction a = (AvailableAction)s;
            if (this.isDelivery) {
                return a.isDelivery;
            }
            return destination.equals(a.destination);
        }
        return false;
    }
}
