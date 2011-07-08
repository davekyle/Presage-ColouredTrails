package presage;
import java.util.*;

public class Conversation {

	public String theirId;

	public ConvKey myKey;

	public ConvKey theirKey;

	// public ArrayList messages;
	public String type;

	public String state;

	public Integer timeout;
	
	public Integer nextTimeout;

	public boolean initiator;
	
	// remember .0 is the parent! so we start at 1.
	public int childConvKey = 1;

	// The keys in beliefs are strings
	// eg "bids" or "price" therefore no comparator is needed.
	public TreeMap<String, Object> beliefs = new TreeMap<String, Object>();

	public Protocol protocol;
	
	public static final int DEFAULT_TIMEOUT = 3;
	
	public Conversation(String convWith, ConvKey myConvKey,
			ConvKey theirConvKey, String convType, boolean myConvRole) {
		theirId = convWith;
		myKey = myConvKey;
		theirKey = theirConvKey;
		type = convType;
		initiator = myConvRole;
		state = "initial";
		protocol = Protocol.getProtocol(convType);
		this.timeout = DEFAULT_TIMEOUT;
	}

	public Conversation(String theirId, String protocolName, ConvKey key) {
		this.theirId = theirId;
		this.myKey = key;
		this.theirKey = ConvKey.NullConvKey;
		this.type = protocolName;
		this.protocol = Protocol.getProtocol(protocolName);
		this.initiator = true;
		this.state = "initial";
		this.timeout = DEFAULT_TIMEOUT;
	}
	

	public void changeStateTo(String newState) {
		state = newState;
	}

	public void resetTimeout(){
		if (timeout != null)
			nextTimeout = Simulator.cycle + timeout;
	}
	
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public void setNoTimeout() {
		timeout = null;
		nextTimeout = null;
	}

	public boolean isTimedOut(int time) {
		if (nextTimeout == null) {
			return false;
		} else if (nextTimeout.intValue() < time) {
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		return "<" + theirId + ", (" + myKey.toString() + ", "
				+ theirKey.toString() + "), " + type + ", " + state + ", "
				+ initiator + ">";
	}
	
} // ends class Conversation
