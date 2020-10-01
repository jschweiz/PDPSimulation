package template;

import java.util.LinkedList;

import static template.Cts.PACKETSTRING;
import static template.Cts.NOPACKETSTRING;
import static template.Cts.STATESTRING;
import logist.topology.Topology.City;

public class State {

    private final City currentCity;
    private final City packageDestination;
    private final LinkedList<AvailableAction> availableActions;

    public State(City currCity, City deliverTo) {
        this.currentCity = currCity;
        this.packageDestination = deliverTo;
        availableActions = new LinkedList<AvailableAction>();

        for (City action : currCity.neighbors())
            availableActions.add(new AvailableAction(action, false));
        
        if (deliverTo != null)
            availableActions.add(new AvailableAction(packageDestination, true));
    }
    
    // Getters
    public LinkedList<AvailableAction> getAvailableActions() {
        return availableActions;
    }

    public City getCity() {
        return currentCity;
    }

    public City getPackageDestination() {
        return packageDestination;
    }

    public boolean isValidPacket() {
        return !this.currentCity.equals(this.packageDestination);
    }

    @Override
    public String toString() { // to print easily
        String packetString = (packageDestination == null) ? NOPACKETSTRING
                : String.format(PACKETSTRING, packageDestination);
        return String.format(STATESTRING, currentCity, packetString);
    }

    // hashCode and equals necessary for putting state in HashMaps
    @Override
    public int hashCode() {
        if (packageDestination == null)
            return currentCity.hashCode();
        return currentCity.hashCode() * packageDestination.hashCode();
    }

    @Override
    public boolean equals(Object s) {
        if (s != null && (s instanceof State)) {
            State o = (State)s;

            if (!currentCity.equals(o.currentCity))
                return false;

            if (packageDestination == null)
                return o.packageDestination == null;

            return packageDestination.equals(o.packageDestination);
        }
        return false;
    }
}
