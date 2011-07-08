// This Class is the thread which will control the
// configuration, launching, monitoring, turn allocation, and
// visualisation of the agent processes.

package presage;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.log4j.*;

import presage.Protocol;
import presage.util.RandomIterator;
import presage.util.StringParseTools;

public class Simulator {
	
	// log4j: instantiate main logger with sim.Simulator name (can't get it automatically), and set the cfg file
	static Logger logger = Logger.getLogger(Simulator.class.getName());
	static java.lang.String log4jDefaultConfigFileName = "log4jConfig.default.properties";
	static java.lang.String log4jConfigFileName = "log4jConfig.properties";
	
	// $$$$$$$$$$$$ TOP LEVEL VARIABLES $$$$$$$$$$$

	public static String inputFolder;

	public static int experimentLength;

	// int lastLogged = 0;
	public static int runNumber;

	public static Boolean verbose;
	
	public static Integer threadDelay;

	public static int cycle, noDotsPrinted = 0;

	public static String tempOutputFolderName = "tempoutput/";

	public static String outputFolderName = "output/";
	
	public static String inputFoldersPath = "inputFiles/";

	static String classFilePath = "build/";

	public static ControlPanelStatic controlPanel;

	public static MethodScriptExecuter methodExecuter;

	public static PluginManager pluginManager;

	public static Network network;

	public static InetSocketAddress serverAddress;

	public static PhysicalWorld pworld;

	public static Random RandomGenerator;

	public static Map<String, Participant> participants = Collections.synchronizedMap(new TreeMap<String, Participant>());

	// sets of private index ids for referencing the players Treemap.
	static private SortedSet<String> participantIdSet = new TreeSet<String>();

	static private SortedSet<String> activeParticipantIdSet = new TreeSet<String>();
	
	// for the network class, a TreeMap of Names linked to their network index.
	static private TreeMap<String, Integer> nameToindexMap = new TreeMap<String, Integer>();

	static private TreeMap<Integer, String> indexTonameMap = new TreeMap<Integer, String>();
	
	private ArrayList<String> params = new ArrayList<String>();

	static private TreeMap<String, UUID> participantKeys = new TreeMap<String, UUID>(); 
	
	// $$$$$$$$$$$$$$$$ THE METHODS $$$$$$$$$$$$$$$

	public static String generateKey(String partId) {
		UUID uuid = UUID.randomUUID();
		participantKeys.put(partId, uuid);
		return uuid.toString();
	}
	
	public static boolean validateKey(String partId, String key) {
		UUID uuid = participantKeys.get(partId);
		return UUID.fromString(key).equals(uuid);
	}
	
	public static void pauseForUser() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			in.readLine();
		} catch (IOException e) {
			logger.fatal("Keyboard Input Failed: ", e);
		}
	}

	public static int getNumberParticipants() {
		return participantIdSet.size();
	}

	public static int getNumberParticipants(Class<? extends Participant> participantClass) {
		Participant currentParticipant;
		Iterator<String> iterator = participantIdSet.iterator();
		int i = 0;
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				if (currentParticipant.getClass().equals(participantClass)) {
					i++;
				}
			}
		}
		return i;
	}

	public static int getNumberParticipants(String role) {
		Participant currentParticipant;
		Iterator<String> iterator = participantIdSet.iterator();
		int i = 0;
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				if (currentParticipant.isRole(role)) {
					i++;
				}
			}
		}
		return i;
	}

	public static SortedSet<String> getactiveParticipantIdSet() {
		return activeParticipantIdSet;
	}

	public static SortedSet<String> getactiveParticipantIdSet(String role) {
		SortedSet<String> resultSet = new TreeSet<String>();

		Participant currentParticipant;
		String id;
		Iterator<String> iterator = activeParticipantIdSet.iterator();
		synchronized (Simulator.participants) {
			while (iterator.hasNext()) {
				id = (String) iterator.next();
				currentParticipant = (Participant) Simulator.participants.get(id);
				if (currentParticipant.isRole(role)) {
					resultSet.add(id);
				}
			}
		}
		return resultSet;
	}

	public static SortedSet<String> getactiveParticipantIdSet(Class<? extends Participant> participantClass) {
		SortedSet<String> resultSet = new TreeSet<String>();

		Participant currentParticipant;
		String id;
		Iterator<String> iterator = activeParticipantIdSet.iterator();
		synchronized (Simulator.participants) {
			while (iterator.hasNext()) {
				id = (String) iterator.next();
				currentParticipant = (Participant) Simulator.participants.get(id);
				if (currentParticipant.getClass().equals(participantClass)) {
					resultSet.add(id);
				}
			}
		}
		return resultSet;
	}

	public static SortedSet<String> getParticipantIdSet() {
		return participantIdSet;
	}

	public static SortedSet<String> getParticipantsIdSet(String role) {

		SortedSet<String> resultSet = new TreeSet<String>();

		Participant currentParticipant;
		String id;
		Iterator<String> iterator = participantIdSet.iterator();
		synchronized (Simulator.participants) {
			while (iterator.hasNext()) {
				id = (String) iterator.next();
				currentParticipant = (Participant) Simulator.participants.get(id);
				if (currentParticipant.isRole(role)) {
					resultSet.add(id);
				}
			}
		}
		return resultSet;
	}

	public static SortedSet<String> getParticipantsIdSet(Class<? extends Participant> participantClass) {

		SortedSet<String> resultSet = new TreeSet<String>();

		Participant currentParticipant;
		String id;
		Iterator<String> iterator = participantIdSet.iterator();
		synchronized (Simulator.participants) {
			while (iterator.hasNext()) {
				id = (String) iterator.next();
				currentParticipant = (Participant) Simulator.participants.get(id);
				if (currentParticipant.getClass().equals(participantClass)) {
					resultSet.add(id);
				}
			}
		}
		return resultSet;
	}
	
	public static boolean isParticipantActive(String name) {
		return activeParticipantIdSet.contains(name);
	}

	public static void activateParticipant(String name) {
		if (!participantIdSet.contains(name)) {
			logger.warn("SIMULATOR: WARNING - Cannot Activate Participant, "
							+ name + " not found in participant list");
		} else if (isParticipantActive(name)) {
			logger.warn("SIMULATOR: WARNING - Cannot Activate Participant, "
							+ name + " is already active.");
		} else {
			activeParticipantIdSet.add(name);
			
			Participant currentParticipant;
			synchronized (participants) {
				currentParticipant = (Participant) participants.get(name);
				currentParticipant.onActivation();
			}
			// networkDirty = true;
			logger.info("SIMULATOR: Activated Participant(" + name + ")");
		}

	}

	public static void deactivateParticipant(String name) {
		if (!participantIdSet.contains(name)) {
			logger.warn("SIMULATOR: WARNING - Cannot Deactivate Participant, "
							+ name + " not found in participant list");
		} else if (!isParticipantActive(name)) {
			logger.warn("SIMULATOR: WARNING - Cannot Deactivate Participant, "
							+ name + " is already inactive.");
		} else {
			activeParticipantIdSet.remove(name);
			Participant currentParticipant;
			synchronized (participants) {
				currentParticipant = (Participant) participants.get(name);
				currentParticipant.onDeActivation();
			}
			// networkDirty = true;
			logger.info("SIMULATOR: Deactivated Participant(" + name
					+ ")");
		}

	}

	public static void nameChange(String oldName, String newName) {
		// update nameToindexMap
		Integer index = (Integer) nameToindexMap.get(oldName);
		nameToindexMap.remove(oldName);
		nameToindexMap.put(newName, index);
		// and its sister method
		indexTonameMap.remove(index);
		indexTonameMap.put(index, newName);

		// update participantIdSet
		participantIdSet.remove(oldName);
		participantIdSet.add(newName);
		// if the participant is active then update activeParticipantIdSet
		if (activeParticipantIdSet.contains(oldName)) {
			activeParticipantIdSet.remove(oldName);
			activeParticipantIdSet.add(newName);
		}
		// networkDirty = true;
	}

	public static int getIndex(String name) {
		return ((Integer) nameToindexMap.get(name)).intValue();
	}

	public static String getName(int index) {
		return ((String) indexTonameMap.get(new Integer(index)));
	}

	public static String getRandomActiveParticipantId(String except) {
		// removes the except Id b4 returning a random Id, this way you can
		// remove the requesting peers Id so that it doesn't return it.
		// returns a random Id from the activeParticipantIdSet or null if set is
		// empty
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		iterator.removeElement(except);
		if (iterator.hasNext()) {
			return (String) iterator.next();
		} else {
			return null;
		}
	}

	public static ArrayList<Object> getRandomActiveParticipantId(String except,
			String role, int num) {
		// removes the except Id b4 returning a random Id, this way you can
		// remove the requesting peers Id so that it doesn't return it.
		// returns a random Id from the activeParticipantIdSet or null if set is
		// empty

		ArrayList<Object> names = new ArrayList<Object>();

		RandomIterator<String> iterator = new RandomIterator<String>(
				getactiveParticipantIdSet(role), RandomGenerator);
		iterator.removeElement(except);
		while ((iterator.hasNext()) && (num > 0)) {
			names.add(iterator.next());
			num--;
		}
		return names;
	}
	
	
	public void simulationThread() throws Exception {
		//System.out.println();
		logger.info("Simulation Thread Executing");

		while ((cycle < experimentLength) && (pworld.live())) {

			updateDisplay(); // Text output display			
			
			methodExecuter.runScript();

			controlPanel.execute();

			network.execute();

			// Conversation timeouts are handled, ended conversations removed.
			everyoneHandleConversations();
			
			everyoneProActiveBehaviour();

			// Intentions are executed, executed intentions are removed.
			everyoneHandleIntentions();
			// Everyone processess their inboxes
			everyoneProcessInbox();
			// Everyone empties their outboxes

			// proActiveBehaviour is where probabilistic stuff happens
			// eg the consumer owners make service requests.
			// Or the Producers do any price Setting, economic calculations
			// Or they might move.

			everyonePhysicallyAct();
			everyoneEmptyOutbox();
			
			pworld.execute();

			pluginManager.executePlugins();

			try {
				Thread.sleep(threadDelay);
			} catch (Exception e) {
			}
			
			// increment the cycle
			cycle++;
		}

		//System.out.println();
		// clean up sql stuff
		// sqlCleanup();

		logger.info("Simulation Thread Completed: " + cycle
				+ " Iterations");
		
	} // ends simulationThread


	// Simply prints out progress information.
	public void updateDisplay() {
		if (verbose.booleanValue()) {
			logger.info("Time Period = " + cycle);
		} else { // if not verbose give a percentage bar.
			if (Simulator.cycle == 0)
				logger.info("0%");
			int twoPercentComplete = (int) ((cycle / (float) experimentLength) * 50.0);

			while (noDotsPrinted < twoPercentComplete) {

				logger.info(".");
				noDotsPrinted++;

				if (noDotsPrinted == 12)
					logger.info("25%");
				if (noDotsPrinted == 25)
					logger.info("50%");
				if (noDotsPrinted == 38)
					logger.info("75%");
				if (noDotsPrinted == 49)
					logger.info("100%");
			}
		}
	} // ends updateDisplay

	public void everyoneHandleConversations() {

		Participant currentParticipant;
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				currentParticipant.handleConversations();
				
			}
		}
	}

	public void everyonePhysicallyAct() {

		Participant currentParticipant;
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				logger.trace("Calling PhysicallyAct: " + currentParticipant.myId);
				try {
					currentParticipant.physicallyAct();
				} catch (Exception e) {
					logger.error(currentParticipant.myId + " FAILED MISERABLY");
				}
				
			}
		}
	}

	public void everyoneHandleIntentions() {
		Participant currentParticipant;
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				logger.trace("Calling HandleIntentions: " + currentParticipant.myId);
				try {
					currentParticipant.handleIntentions();
				} catch (Exception e) {
					logger.error(currentParticipant.myId + " FAILED MISERABLY");
				}				
			}
		}
	}

	public void everyoneProcessInbox() {

		Participant currentParticipant;
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				logger.trace("Calling ProcessInbox: " + currentParticipant.myId);
				try {
					currentParticipant.processInbox();
				} catch (Exception e) {
					logger.error(currentParticipant.myId + " FAILED MISERABLY");
				}				
			}
		}
	}

	public void everyoneEmptyOutbox() {
		Participant currentParticipant;
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				currentParticipant.emptyOutbox();
				
			}
		}
	}

	public void everyoneProActiveBehaviour() {
		Participant currentParticipant;
		RandomIterator<String> iterator = new RandomIterator<String>(activeParticipantIdSet, RandomGenerator);
		synchronized (participants) {
			while (iterator.hasNext()) {
				currentParticipant = (Participant) participants
						.get((String) iterator.next());
				logger.trace("Calling ProActiveBehaviour: " + currentParticipant.myId);
				try {
					currentParticipant.proActiveBehaviour();
				} catch (Exception e) {
					logger.error(currentParticipant.myId + " FAILED MISERABLY");
				}					
			}
		}
	} // ends proActiveBehaviour()

	/**
	 * Gives each run a unique id helps identity results.
	 * 
	 * Updates file intRunNumber.txt
	 * 
	 * */
	private void setRunNumber() {
		try {
						
			RandomAccessFile runNumberFile = new RandomAccessFile(
					outputFolderName + "intRunNumber.txt", "rw");
			
			try {
				runNumber = Integer.parseInt(runNumberFile.readLine());
			} catch (NumberFormatException e) {
				// if intRunNumber.txt contains invalid content or blank
				runNumber = 0;
			}
			
			runNumberFile.setLength(0);
			runNumberFile.writeBytes((new Integer(runNumber + 1)).toString());
			runNumberFile.close();

		} catch (IOException e) {
			// logger request should be error not fatal since it continues anyway
			logger.error("Error - Unable to set runNumber, set runNumber to 0", e);
			runNumber = 0;
		}
	}

	public void initialise() {
		

		
		// delete any old temp output files
		try {
			// Get a list of all the files in the directory
			File toF = new File(tempOutputFolderName);
			File[] files = toF.listFiles();
			// Then Delete then all.
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		} catch (Exception e) {
			// logger request should be warn since it continues anyway
			logger.warn("Failed to delete old temp outfiles ", e);
		}

		// Check if output folder exists
		File oFolder = new File(outputFolderName);
		if (!oFolder.exists()) oFolder.mkdir();
		
		// Check if tempoutput folder exists
		File toFolder = new File(tempOutputFolderName);
		if (!toFolder.exists()) toFolder.mkdir();
		
		
		try {
			// Get a list of all the files in the directory
			File toF = new File(tempOutputFolderName);
			File[] files = toF.listFiles();
			// Then Delete then all.
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		} catch (Exception e) {
			// logger request should be warn since it continues anyway
			logger.warn("Failed to delete old temp outfiles ", e);
		}
		
		setRunNumber();

		// set the system variables and create a physical world
		logger.info("Initialising Environment....");

		try {
			RandomAccessFile systemFile = new RandomAccessFile(inputFoldersPath
					+ inputFolder + "/system.csv", "r");
			systemFile.readLine();

			// Get the System Data from file
			String currentLine = systemFile.readLine();
			String[] theArgs = StringParseTools.readTokens(currentLine, ",");

			systemFile.close();

			// Set the System Data
			RandomGenerator = new Random(Integer.parseInt(theArgs[0]));
			experimentLength = Integer.parseInt(theArgs[1]);
			verbose = Boolean.valueOf(theArgs[2]);
			threadDelay = Integer.parseInt(theArgs[6]);

			InetAddress tmp = InetAddress.getLocalHost();
			serverAddress = new InetSocketAddress(tmp.getHostName(),
					(new Integer(theArgs[3])).intValue());

			initialiseParticipants(inputFolder);

			init_name_indexMaps();

			// Create a PhysicalWorld
			Class<?> pwcls = Class.forName(theArgs[4]);
			Constructor<?> pwc = pwcls.getConstructor(new Class[] {});
			pworld = (PhysicalWorld) pwc.newInstance(new Object[] {});

			
			// Create a Simulated Network env.
			Class<?> netcls = Class.forName(theArgs[5]);
			Constructor<?> nc = netcls.getConstructor(new Class[] {});
			network = (Network) nc.newInstance(new Object[] {});
			

		} catch (Exception e) {
			logger.fatal("Error access System I/O file: ", e);
		}

		methodExecuter = new MethodScriptExecuter(inputFolder);

		pluginManager = new PluginManager(inputFolder);

		boolean startPaused = !params.contains("-autorun");
		controlPanel = new ControlPanelStatic(startPaused);
		
		// Hide rarely-used panels
		controlPanel.hideMethodsManager();
		controlPanel.hidePluginManager();

		// now that both the physical world and the network exist!
		// make the initial connections
		network.execute();

	}

	// public void initialiseSQL() {
	//	
	// // testJDBCDriver();
	// // We need to create a database else..
	// maindatabaseConnection = getDatabaseConnection("MainDB");
	//		
	// }

	public void initialiseEnvironmentVariables(String inputFolderName) {

	} // ends initialiseEnvironmentVariables

	public void initialiseParticipant(String[] theArgs) {
		try {
			// theArgs[0] is the class of participant.
			// theArgs[1] is the name
			// theArgs[2] are the roles
			// theArgs[3] is the icon filename
			// after that its specialised to the class of participant.

			Class<?> cls = Class.forName(theArgs[0]);
			Constructor<?> c = cls.getConstructor(new Class[] { String[].class });
			Participant participant = (Participant) c
					.newInstance(new Object[] { theArgs });

			synchronized (participants) {
				participants.put(theArgs[1], participant);
				// add to the Id sets
				// note if you don't want everyone active from time zero
				// comment second line
				participantIdSet.add(theArgs[1]);
				// activeParticipantIdSet.add(theArgs[1]);
				// increment Integer nextPrivateId
				// nextPrivateId = new Integer(nextPrivateId.intValue() +
				// 1);
			}

		} catch (Exception e) {
			logger.fatal("Error: Initialising Participant..... ", e);
		}
	} // ends initialiseParticipant

	public void initialiseParticipants(String inputFolderName) {
		logger.info("	Initialising Participants....");
		try {

			RandomAccessFile participantsFile = new RandomAccessFile(
					inputFoldersPath + inputFolderName + "/participants.csv",
					"r");
			// First readLine discards the coloumn headers
			String currentLine = participantsFile.readLine();
			// Get the first Line of Agent Data
			currentLine = participantsFile.readLine();

			while (!(currentLine == null)) {
				String[] theArgs = StringParseTools
						.readTokens(currentLine, ",");

				initialiseParticipant(theArgs);

				// get next line
				currentLine = participantsFile.readLine();
			}
			participantsFile.close();

		} catch (Exception e) {
			logger.fatal("Error: Initialising Participants..... ", e);
		}
	} // ends initialiseParticipants

	private void init_name_indexMaps() {
		int i = 0;
		String name;
		Iterator<String> iterator = participantIdSet.iterator();
		while (iterator.hasNext()) {
			name = (String) iterator.next();
			nameToindexMap.put(name, new Integer(i));
			indexTonameMap.put(new Integer(i), name);
			i++;
		}	
	}

	public String persistentStringRequest(String que, String expectedAnswer) {

		String f = new String("");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(que + ": ");
		// System.out.print(expectedAnswer);
		// Get the user's response.
		try {
			f = in.readLine();
		} catch (IOException e) {
			logger.fatal("Keyboard Input Failed:", e);
		}
		if (f.equalsIgnoreCase("")) {
			while (f.equalsIgnoreCase("")) {
				System.out.print("You Must " + que + ": ");
				try {
					f = in.readLine();
				} catch (IOException e) {
					logger.fatal("Keyboard Input Failed:", e);
				}
			}
		}
		return (f);
	} // ends persistentStringRequest

	public void setcheckArgs(String[] argsArray) {

		ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(argsArray));
		
		// Check arguments
		
		while ((arguments.size() > 0) && (arguments.get(0).startsWith("-"))) {
			params.add(arguments.get(0));
			arguments.remove(0);
		}
		
		if (arguments.size() != 1) {
			// System.out.println("Missing cmd-line args");
			// System.out.println("Usage: java Simulator <input file repository>
			// " + "\n");
			arguments.add(persistentStringRequest(
					"Enter 'Input File' Folder Name", "testInputs"));
		}

		// Set the args to their respective variables..
		if (arguments.size() == 1) {
			// First argument point simulator to where all the input-files are.
			inputFolder = arguments.get(0);
			// number = Integer.parseInt(arguments[1]);
		}
	}

	public void setInputs(String argument) {
		// First argument point simulator to where all the input-files are.
		inputFolder = argument;
	}	

	private void preThreadMethods() {
		logger.info("Executing Pre-Thread Methods...");
		String progress = "start";
		try {
			RandomAccessFile actionFile = new RandomAccessFile("inputFiles/"
					+ inputFolder + "/premethods.csv", "r");
			// First readLine discards the coloumn headers
			String currentLine = actionFile.readLine();
			// Get the first Line of Agent Data
			currentLine = actionFile.readLine();

			while (!(currentLine == null)) {
				logger.debug(currentLine);
				String[] theArgs = StringParseTools
						.readTokens(currentLine, ",");
				String[] variables = StringParseTools.readTokens(theArgs[2],
						";");
				Class<?> cls = Class.forName(theArgs[0]);
				progress = "Class cls = Class.forName(theArgs[0]);";
				Method m = cls.getMethod(theArgs[1],
						new Class[] { String[].class });
				progress = "Method m = cls.getMethod(theArgs[1], new Class[] { String[].class });";
				m.invoke(null, new Object[] { variables });
				progress = "Object tmp = m.invoke(null, new Object[] { variables });";
				currentLine = actionFile.readLine();
			}
			actionFile.close();

		} catch (Exception e) {
			logger.fatal("Error: Accessing PreMethods I/O file, progress = "
							+ progress, e);
		}
	}

	private void postThreadMethods() {
		logger.info("Executing Post-Thread Methods...");
		try {
			RandomAccessFile actionFile = new RandomAccessFile("inputFiles/"
					+ inputFolder + "/postmethods.csv", "r");
			// First readLine discards the coloumn headers
			String currentLine = actionFile.readLine();
			// Get the first Line of Agent Data
			currentLine = actionFile.readLine();

			while (!(currentLine == null)) {
				logger.info(currentLine);
				String[] theArgs = StringParseTools
						.readTokens(currentLine, ",");

				String[] variables = StringParseTools.readTokens(theArgs[2],
						";");
				Class<?> cls = Class.forName(theArgs[0]);
				Method m = cls.getMethod(theArgs[1],
						new Class[] { String[].class });
				m.invoke(null, new Object[] { variables });

				currentLine = actionFile.readLine();
			}
			actionFile.close();

		} catch (Exception e) {
			logger.fatal("Error: Accessing PostMethods I/O file: ", e);
		}
	}
	
	private static void loggerConfigure(){
		// Checks to see if there is a custom cfg in the project
		// Loads the default config if there isn't
			File l4jcfgFile = new File(log4jConfigFileName);
			if (!l4jcfgFile.exists()) {
				log4jConfigFileName = log4jDefaultConfigFileName;
			}
			PropertyConfigurator.configure(log4jConfigFileName);

	}
	
	public static void main(String[] args) throws Exception {
	
		// Set loggers to read from the cfg file	
		//PropertyConfigurator.configure(log4jConfigFileName);
		loggerConfigure();
		
		// Trick the logger into showing this message regardless of the threshold
		Level tempLevel = logger.getLevel();
		logger.setLevel(Level.ALL);
		logger.info("Starting main logging instance with cfg \"" + log4jConfigFileName + "\" and applying logging threshold");
		logger.setLevel(tempLevel);
		
		Protocol.init();
		
			try {

				Simulator s = new Simulator();

				// check that all the right arguments are there
				// s.setInputs(args[i]);
				s.setcheckArgs(args);

				// Initialise all the owners, producer agents,
				// consumer agents and misc variables here...
				s.initialise();

				// Execute any methods that the experimenter wishes for pre
				// thread
				// i.e time = 0;
				s.preThreadMethods();

				long startTime = System.currentTimeMillis();
				
				// call the iterating thread
				s.simulationThread();

				long duration = System.currentTimeMillis() - startTime;

				if (duration > 3600000) {
					logger.info("Thread completed in "
							+ (double) duration / (double) 3600000 + " hours");
				} else if (duration > 60000) {
					logger.info("Thread completed in "
							+ (double) duration / (double) 60000 + " minutes");
				} else if (duration > 1000) {
					logger.info("Thread completed in "
							+ (double) duration / (double) 1000 + " seconds ");
				} else {
					logger.info("Thread completed in " + duration + "ms");
				}
				
				// Store some metrics. 
				try {
					RandomAccessFile metricFile = new RandomAccessFile(
							outputFolderName + "simMetrics.csv", "rw");
					metricFile.skipBytes((int)metricFile.length());
					metricFile.writeBytes(Simulator.inputFolder + "_" + runNumber + "," + Simulator.cycle + "," + duration + "\n" );
					metricFile.close();
				} catch (IOException e) {
					logger.fatal("Error - Unable to record simMetrics ", e);
				}

				// handle data archiving etc..
				s.postThreadMethods();
				
				// Trick the compiler to output this string regardless
				Level tempLevel2 = logger.getLevel();
				logger.setLevel(Level.ALL);
				logger.info("Thread completed");
				logger.setLevel(tempLevel2);
			} catch (Exception e) {
				logger.fatal("Exception in Main ", e);
			}
		// }
			
			// new SoundPlayer("notify.wav").start();
			
			System.exit(0);
			
//			controlPanel.dispose();
//			pluginManager.killAll();
//			pluginManager.dispose();
//			methodExecuter.dispose();
//			SQLModule.databaseConnection.close();
			
			
	} // ends main

} // ends public class Simulate extends JComponent
