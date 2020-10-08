package reactive;

public class Cts {

    // State
    public static final String STATESTRING = "In %s (%s available)";
    public static final String PACKETSTRING = "packet to %s";
    public static final String NOPACKETSTRING = "no packet";

    // AvailableAction
    public static final String PICKUPSTRING = "PICKUPTO->%s";
    public static final String GOTOSTRING = "GOTO->%s";
 
    // OfflineLearningModel
	public static final String SPACE = "                      ";
	public static final String ARROW = "--------------------> ";
	public static final String DEBUGSTRING1 = "Calculation for %s";
	public static final String DEBUGSTRING2 = SPACE + SPACE
			+ "with action %s gives subsum=%d and reward=%f";
    public static final String DEBUGSTRING3 = SPACE  + "CHOICE MADE : %s";
    public static final String INITSTRING = ARROW + "Initializing datastructures";
    public static final String TRAINSTRING = ARROW + "Training model offline...";
    public static final String FINISHEDSTRING = ARROW + "Printing model results...";
    public static final String MAPSTRING = "%s best action is %s and V= %f";
    public static final String MAPWRITESTRING = "%s best action is %s\n";
    
    // ReactiveAgent
	public static final String RECOMMENDEDACTIONSTRING = "Vehicule in %s has recommended action: %s";
	public static final String SUMMARYSTRING = 
			"The total profit after %d actions is %d (average profit: %f) (tasks delivered: %d)";
    
}