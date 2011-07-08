package presage;

import org.apache.log4j.*;

class CoreMethods {
	
	static Logger logger = Logger.getLogger(CoreMethods.class.getName());

	public static void test(String[] args) {
		// Simulator.println("CoreMethods: It was a Test;");
		logger.debug("It was a Test");
	}

	public static void activateParticipant(String[] args) {
		Simulator.activateParticipant(args[0]);
	}

	public static void deactivateParticipant(String[] args) {
		Simulator.deactivateParticipant(args[0]);
	}

	public static void getPluginManagerGUI(String[] args) {
		Simulator.pluginManager.setVisible(true);
	}
	
}