package presage;

import java.awt.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.InetSocketAddress;

import org.apache.log4j.*;

import presage.util.Queue;
import presage.util.StringParseTools;

public abstract class Participant {
	
	Logger logger = Logger.getLogger(this.getClass().getName());

	public String myId;

	protected String myKey;
	
	protected String myRolesString;

	public ArrayList<String> myRoles = new ArrayList<String>();
	
	public Queue inbox = new Queue("inbox");

	public Queue outbox = new Queue("outbox");

	// ************AGENT BELIEF STRUCTURES************//
	// This is for LOOSE beliefs, ie those not solely associated with a
	// conversation
	protected TreeMap<String, Object> beliefs = new TreeMap<String, Object>();

	// Set up the Conversation System
	protected ConvKeyGen convKeyGen;

	protected TreeMap<ConvKey, Conversation> conversations = new TreeMap<ConvKey, Conversation>();

	// *************Agent Intention Stucture**************//

	// Will store intentions, intentions are of the form <method, convKey>
	// By making it a SortedSet we stop duplicate intentions forming.
	// private IntentionComparator ic = new IntentionComparator();
	protected HashSet<Intention> intentions = new HashSet<Intention>();

	// **************DataBase Setup************************//
	// declare all the connections and tables here,
	// even if the final agent never uses them.

	// public Connection databaseConnection;

	protected TreeMap<String, ContactInfo> contactsList;
	protected TreeMap<String, Boolean> contactConnectedList;
	
	protected String contactsTable;

	protected Color myColor;

	protected Color rangeColor;

	// Networking
	public int wirelessRange;

	protected InetSocketAddress myAddress;

	protected ContactInfo myBCard;

	// Any Participant extending Participant must use super(myId, myRoles); to
	// call this constructor.
	public Participant(String _myId, String _myRoles, String _myColor,
			String _wRange) {

		// e.g. myId = consumer0001
		myId = _myId;

		// e.g. myRolesString = <producer><consumer><bank><auctionhouse>
		myRolesString = _myRoles;
		// remove the first and last characters
		// e.g. myRolesE = producer><consumer><bank><auctionhouse
		String myRolesE = _myRoles.substring(1, _myRoles.length() - 1);

		String[] roles = StringParseTools.readTokens(myRolesE, "><");
		for (int i = 0; i < roles.length; i++) {
			myRoles.add(roles[i]);
		}

		/*
		// Set the color for any visualisation
		String[] temp = StringParseTools.readTokens(_myColor, ";");
		myColor = new Color((new Integer(temp[0])).intValue(), (new Integer(
				temp[1])).intValue(), (new Integer(temp[2])).intValue());
		
		wirelessRange = new Integer(_wRange).intValue();

		float[] colorArray = myColor.getRGBColorComponents(null);
		
		rangeColor = new Color(colorArray[0], colorArray[1], colorArray[2],
				(float) 0.06);// 0.05
		*/
		
		// All Participants have a table containing their contacts and the roles
		// of their contacts.
		contactsTable = myId + "_contacts";

		initContactDatabase();
		// type=heap

		convKeyGen = new ConvKeyGen();
		// ConversationCount = 0;

		// Participants have a address equal to the platform. 
		myAddress = Simulator.serverAddress;
		logger.info(myId + ", " + myAddress.toString());

		myBCard = new ContactInfo(myId, myAddress.getHostName(), myAddress
				.getPort(), myRolesString);
	}

	// //////////////////////////////////////
	// INTENTION Handling
	// //////////////////////////////////////

	public void handleIntentions() {
		printIntentions();
		executeIntentions();
	}

	@SuppressWarnings("unchecked")
	public void executeIntentions() {
		Iterator<Intention> iterator = ((HashSet<Intention>)intentions.clone()).iterator();
		Intention anIntention;

		while (iterator.hasNext()) {
			anIntention = iterator.next();

			// Test and see if its time to execute the intention!
			if (anIntention.executionTime <= Simulator.cycle) {
				logger.trace("Executing Intention - " + anIntention);
				intentions.remove(anIntention);
				try {
					Class<?> c = this.getClass();
					Method m = c.getDeclaredMethod(anIntention.method, new Class[] { Object[].class });
					m.invoke(this,
							new Object[] { anIntention.variables });

				} catch (NoSuchMethodException e2) {
					logger.fatal("executeIntentions: NoSuchMethodException - ", e2);
				} catch (IllegalAccessException e3) {
					logger.fatal("executeIntentions: IllegalAccessException - ", e3);
				} catch (InvocationTargetException e4) {
					logger.fatal("executeIntentions: InvocationTargetException - ", e4);
				}

			}
		}
	} // ends method executeIntentions

	public void printIntentions() {
		/** Prints out all the player's currentIntentions */
		Intention currentIntention;
		Iterator<Intention> iterator = intentions.iterator();
		if (intentions.size() != 0) {
			logger.trace(myId + ": " + intentions.size() + " intentions: ");
			while (iterator.hasNext()) {
				currentIntention = (Intention) iterator.next();
				logger.trace("  " + currentIntention.toString() + " ");
			}
		}
		else {
				logger.trace(myId + " has no intentions");
		}
	} // ends printIntentions

	/*
	 * CONTACT DATABASE METHODS
	 */

	protected void initContactDatabase() {
		
		contactsList = new TreeMap<String,ContactInfo>();
		contactConnectedList = new TreeMap<String, Boolean>();

	}

	protected void removeContact(String theirId) {

		logger.trace(myId + ": Removing Contact " + theirId
				+ " from contacts table...");

		contactsList.remove(theirId);
		contactConnectedList.remove(theirId);
	}

	protected void addNewContact(ContactInfo info, boolean connected) {

		logger.debug(myId + ": Attempting to add new contact ("
				+ info.name() + "," + info.roles() + "," + info.host() + ","
				+ info.port() + ")");

		contactsList.put(info.name(), info);
		contactConnectedList.put(info.name(), new Boolean(connected));
	}

	protected void addNewContact(ContactInfo info) {

		this.addNewContact(info, true);
	}
	

	public Object[] getHostandPort(String theirId) {
		try {

			ContactInfo cInfo = (ContactInfo)contactsList.get(theirId);

			Object[] info = new Object[2];

			info[0] = cInfo.host();
			info[1] = new Integer(cInfo.port());

			return info;

		} catch (Exception e) {
			logger.fatal(myId + " - getHostandPort(" + theirId + ") ", e);
			return null;
		}
	}

	protected ArrayList<ContactInfo> searchContactsByRole(String contactRole) {
		
		try {
			Iterator<String> iter = contactsList.keySet().iterator();
			ContactInfo current;
			ArrayList<ContactInfo> results = new ArrayList<ContactInfo>();
			
			while (iter.hasNext()) {
				current = contactsList.get(iter.next());
				if (current.roles().contains(contactRole)) {
					results.add(current);
				}
			}

			return results;
		} catch (Exception e) {
			logger.fatal("ERROR: ", e);
			return null;
		}
	}

	protected boolean contactHasRole(String contact, String role){
		
		try {
			ContactInfo info = (ContactInfo)contactsList.get(contact);
			return info.roles().contains(role);
		} catch (Exception e) {
			logger.fatal("ERROR: ", e);
		}
		
		return false;
	}
	
	
	protected ArrayList<ContactInfo> searchContactsByRole(String contactRole,
			boolean connected) {

		try {
			Iterator<String> iter = contactsList.keySet().iterator();
			boolean isConnected;
			ContactInfo info;
			ArrayList<ContactInfo> results = new ArrayList<ContactInfo>();
			
			while (iter.hasNext()) {
				info = contactsList.get(iter.next());
				isConnected = contactConnectedList.get(info.name());
				if ((isConnected) && (info.roles().contains(contactRole))) {
					results.add(info);
				}
			}

			return results;
		} catch (Exception e) {
			logger.fatal("ERROR: ", e);
			return null;
		}
	}

	protected ArrayList<ContactInfo> connectedContacts() {
		try {
			Iterator<String> iter = contactsList.keySet().iterator();
			boolean isConnected;
			ContactInfo info;
			ArrayList<ContactInfo> results = new ArrayList<ContactInfo>();
			
			while (iter.hasNext()) {
				info = contactsList.get(iter.next());
				isConnected = contactConnectedList.get(info.name());
				if ((isConnected)) {
					results.add(info);
				}
			}

			return results;
		} catch (Exception e) {
			logger.fatal("ERROR: ", e);
			return null;
		}
	}
	
	/**
	 * stores all the contacts that are in an agent's contact list in an arraylist for ease of use. Compliments of Mario and Panyiotis (and Finlay)
	 *
	 * @deprecated Due to database removal this is now unnecessary - use connectedContacts() instead
	 */
	@Deprecated
	protected ArrayList<ContactInfo> connectedContactsList() {

		return connectedContacts();
		
	}

	protected int countConnectedContacts(String contactRole) {
		return searchContactsByRole(contactRole, true).size();

	}

	protected int countConnectedContacts() {
	
		return connectedContacts().size();
	}

	protected ContactInfo searchContactsByName(String contactName) {

		// Simulator.println(myId + ":searchContactsByName: " + contactName);

		return contactsList.get(contactName);
	}

	protected ArrayList<ContactInfo> returnAllContacts() {
		
		return new ArrayList<ContactInfo>(contactsList.values());
	}

	protected boolean contactKnown(String contactName) {
		return contactsList.containsKey(contactName);
	}

	public boolean isRole(String role) {
		if (myRoles.contains(role)) {
			return true;
		} else {
			return false;
		}
	}

	public void updateRoles(String theirId, String roles) {

		ContactInfo info, newInfo;
		info = contactsList.get(theirId);
		newInfo = new ContactInfo(info.name(), info.host(), info.port(), roles);
		contactsList.remove(theirId);
		contactsList.put(theirId, newInfo);

		logger.trace("Updated roles assigned to peer(" + theirId + ")");

	}
	
	public String getPlayerRole(String playerId){
		
		return contactsList.get(playerId).roles();
	}

	public boolean getConnectedStatus(String contactName) {

		return contactConnectedList.get(contactName);
	}

	public void updateConnectedStatus(String theirId, boolean connected) {

		contactConnectedList.remove(theirId);
		contactConnectedList.put(theirId, connected);

	} // ends updateRoles

	/*
	 * CONVERSATION CODE
	 */

	public void handleConversations() {
		// First Step print out all the Conversations in conversations
		printConvs();
		// Second remove any conversations which have ended.
		removeEndedConvs();
		// and Finally handle any timeouts
		handleTimeouts();
	} // ends method handleConversations

	public void printConvs() {
		/** Prints out all the players current conversations */
		Conversation currentConv;
		Iterator<ConvKey> iterator = conversations.keySet().iterator();
		if (conversations.size() != 0){
			logger.trace(myId + ": " + conversations.size()
					+ " conversations: ");
			while (iterator.hasNext()) {
				currentConv = (Conversation) conversations.get(iterator.next());
				logger.trace("  <" + currentConv.myKey.toString() + ", "
								+ currentConv.type + ", "
								+ currentConv.state + ", "
								+ currentConv.nextTimeout + "> ");
			}
		}
		else {
			logger.trace(myId + " has no conversations");
		}
	} // ends printConvs

	public void removeEndedConvs() {
		Conversation currentConv;
		Iterator<ConvKey> iterator = conversations.keySet().iterator();
		Set<ConvKey> removeSet = new TreeSet<ConvKey>();
		while (iterator.hasNext()) {
			ConvKey key = (ConvKey) iterator.next();
			currentConv = (Conversation) conversations.get(key);
			if (currentConv.state.equals("end")) {
				// then add key to list of conversations to be deleted
				removeSet.add(key);
			}
		}
		// now do the removing
		iterator = removeSet.iterator();
		while (iterator.hasNext()) {
			conversations.remove(iterator.next());
		}
	} // ends removeEndedConvs

	public void handleTimeouts() {

		Conversation currentConv;
		TreeSet<ConvKey> keySetCopy = new TreeSet<ConvKey>(conversations.keySet());
		Iterator<ConvKey> iterator = keySetCopy.iterator();
		while (iterator.hasNext()) {
			try {
				currentConv = (Conversation) conversations.get(iterator.next());
				if (currentConv.isTimedOut(Simulator.cycle)) { // if its
					// timedout
					// then handle by calling appropriate method
					logger.trace("<" + currentConv.myKey.toString() + ", "
							+ currentConv.type + ", "
							+ currentConv.state + ", "
							+ currentConv.nextTimeout + "> ");
					try {
						Class<?> c = this.getClass();
						Method m = c.getDeclaredMethod(currentConv.type + "_timeout",
								new Class[] { Conversation.class });
						m.invoke(this, new Object[] { currentConv });
	
					} catch (NoSuchMethodException e2) {
						logger.trace(myId + ": does not implement " + currentConv.type + "_timeout");
					} catch (IllegalAccessException e3) {
						logger.fatal("handleTimeouts: IllegalAccessException", e3);
					} catch (InvocationTargetException e4) {
						logger.fatal(myId + " handleTimeouts: InvocationTargetException", e4);
					}
				}
			} catch (Exception e) {
				logger.fatal("This shouldn't happen anymore....", e);
			}
		}
	} // ends handleTimeouts

	public Boolean processMessage(Message msg) {
		if (msg.toConvKey.isInstantiated()
				&& !conversations.containsKey(msg.toConvKey)) {
			logger.error(myId + ": Error Conversation with ConvId("
					+ msg.toConvKey + ") does not exist!");	
		} else {
			Conversation conv;
			if (msg.toConvKey.isInstantiated() 
					&& conversations.containsKey(msg.toConvKey)) {
				// is a response
				conv = conversations.get(msg.toConvKey);
				
				// if it's the first response, we need to update
				// their conversation key
				if (conv.theirKey.equals(ConvKey.NullConvKey))
					conv.theirKey = msg.fromConvKey;
			} else {
				// someone is starting a conversation with me
				conv = new Conversation(msg.from, convKeyGen.getKey(),
						msg.fromConvKey, msg.convType, false);
				conversations.put(conv.myKey, conv);

			}
			// check the message performative is valid for the conversation state
			ProtocolState state = conv.protocol.getState(conv.state);
			if (state != null && state.isValidMessage(msg.performative)) {
				try {
					Class<?> c = this.getClass();
					Method m = c.getMethod(msg.convType,
							Message.class, Conversation.class );
					m.invoke(this, new Object[] { msg, conv });
					return true;
				} catch (InvocationTargetException e1) {
					logger.fatal("Fatal ", e1);
				} catch (NoSuchMethodException e2) {
					logger.trace(myId + ": does not implement " + conv.type + "_timeout");
				} catch (Exception e) {
					logger.fatal("processMessage: ", e);
				}
			} else {
				logger.warn("Ignoring invalid message " + msg + " for conversation " + conv);
			}
		}
		return false;
	} // ends method processMessage

	public void processInbox() {
		while (!inbox.isEmpty()) {
			Message msgReceived = (Message) inbox.dequeue();
			msgReceived.printMsg("received");
			if (myId.equals(msgReceived.to)) {
				processMessage(msgReceived);
			} else {
				logger.warn(myId
						+ ": Error - msg received not intended for me");
			}
		}
	}

	public void emptyOutbox() {
		while (!outbox.isEmpty()) {
			Message msg = (Message) outbox.dequeue();
			sendMessage(msg);
		}
	}

	public boolean sendMessage(Message msg) {

		//Attach my key for network authentication
		msg.setSenderKey(myKey);
		
		Conversation conv = conversations.get(msg.fromConvKey);
		conv.resetTimeout();
		
		Object[] addr = getHostandPort(msg.to);

		boolean sent = Simulator.network.sendMessage((String) addr[0],
				((Integer) addr[1]).intValue(), msg);
		if (sent) {
			msg.printMsg("sent");
		} else {
			logger.debug(myId + ": MESSAGE SENDING FAILED ");
			msg.printMsg("sent");
		}
		return sent;
	}
	
	protected void println(String s) {
		logger.debug(myId + ": " + s);
	}

	/**
	 * This method is called when the agent is activated, it is 
	 * a good place to put extra intitialisation code such as registering 
	 * with the PhysicalWorld etc.
	 */
	public void onActivation() {
		Simulator.pworld.register(myId, myRoles);
		myKey = Simulator.generateKey(myId);
	}

	/**
	 * This method is called when the agent is deactivated, it is 
	 * a good place to put clean up code such as deregistering 
	 * with the PhysicalWorld etc.
	 */
	public void onDeActivation() {}

	abstract public void physicallyAct();

	abstract public void proActiveBehaviour();
	
	public ContactInfo getContactInfo() {
		return myBCard;
	}

} // ends class Participant
