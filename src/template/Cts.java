package template;

public class Cts {

    // State
    public static final String STATESTRING = "In {0} ({1} available)";
    public static final String PACKETSTRING = "packet to {0}";
    public static final String NOPACKETSTRING = "no packet";

    // AvailableAction
    public static final String PICKUPSTRING = "PICKUPTO->{0}";
    public static final String GOTOSTRING = "GOTO->{0}";

    // OfflineLearningModel
	public static final String SPACE = "                      ";
	public static final String ARROW = "--------------------> ";
	public static final String DEBUGSTRING1 = "Calculation for {0}";
	public static final String DEBUGSTRING2 = SPACE + SPACE
			+ "with action {0} gives subsum={1} and reward={2}";
    public static final String DEBUGSTRING3 = SPACE  + "CHOICE MADE : {0}";
    public static final String INITSTRING = ARROW + "Initializing datastructures";
    public static final String TRAINSTRING = ARROW + "Training model offline...";
    public static final String FINISHEDSTRING = ARROW + "Printing model results...";
    public static final String MAPSTRING = "{0} best action is {1} and V= {2}";
    
    // ReactiveAgent
	public static final String RECOMMENDEDACTIONSTRING = "Vehicule in {0} has recommended action: {1}";
	public static final String SUMMARYSTRING = 
			"The total profit after {0} actions is {1} (average profit: {2}) (tasks delivered: {3}";
    
}