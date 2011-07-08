/**
 * Created on 20-Jan-2005
 * Brendan Neville
 * Information Systems and Networks Research Group
 * Electrical and Electronic Engineering
 * Imperial College London
 */


package presage;

import org.apache.log4j.*;

public abstract class Message {
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	protected String performative, to, from, convType;
	protected Object[] contents;
	protected ConvKey toConvKey, fromConvKey;

	protected String senderKey;
	
	/**MEGA CONSTUCTOR*/
	public Message(
		String perf,
		String msgTo,
		String msgFrom,
		String cType,
		ConvKey toCKey,
		ConvKey fromCKey,
		Object[] msgContents) {

		performative = perf;
		to = msgTo;
		from = msgFrom;
		convType = cType;
		toConvKey = toCKey;
		fromConvKey = fromCKey;
		contents = msgContents;
	}
	
	public String getPerformative() {
		return performative;
	}

	public String getTo() {
		return to;
	}

	public String getFrom() {
		return from;
	}

	public String getConvType() {
		return convType;
	}

	public Object[] getContents() {
		return contents;
	}

	public ConvKey getToConvKey() {
		return toConvKey;
	}

	public ConvKey getFromConvKey() {
		return fromConvKey;
	}

	public void setSenderKey(String key){
		senderKey = key;
	}
	
	public String getSenderKey(){
		return senderKey;
	}
	
	/** Prints out the Message in a style dictated by the string flag*/
	public void printMsg(String flag) {
		
		// techPrint();
		String string = "";
		if (flag.equals("sent")) {
				string = from
					+ ": ("
					+ performative
					+ ") message sent to("
					+ to
					+ ") in a ("
					+ convType
					+ ") Conversation with ConvIds("
					+ toConvKey.major
					+ "."
					+ toConvKey.minor
					+ ","
					+ fromConvKey.major
					+ "."
					+ fromConvKey.minor
					+ ") contents(";
		} else if (flag.equals("received")) {
				string = to
					+ ": (" 
					+ performative
					+ ") message received from ("
					+ from
					+ ") in a ("
					+ convType
					+ ") Conversation with ConvIds("
					+ toConvKey.major
					+ "."
					+ toConvKey.minor
					+ ","
					+ fromConvKey.major
					+ "."
					+ fromConvKey.minor
					+ ") contents(";
		}
		if (contents.length > 0){
		String contentString = contents[0].toString();
		for (int counter = 1; counter < contents.length; counter++) {
			contentString += ", " + contents[counter].toString();
		}
		string = string + contentString + ");";
		} else {
			string = string + "empty);";
		}
		logger.debug(string);
	} // ends printMessage()

	/** Prints out the Message in a style dictated by the string flag*/
	public void printErrMsg() {
		
		String errString = "";
			errString = 
				from
					+ ": ("
					+ performative
					+ ") to("
					+ to
					+ ") in a ("
					+ convType
					+ ") Conversation with ConvIds("
					+ toConvKey.major
					+ "."
					+ toConvKey.minor
					+ ","
					+ fromConvKey.major
					+ "."
					+ fromConvKey.minor
					+ ") contents(";
			
		if (contents.length > 0){
		String contentString = contents[0].toString();
		for (int counter = 1; counter < contents.length; counter++) {
			contentString += ", " + contents[counter].toString();
		}
		errString = errString + contentString + ");";
		} else {
			errString = errString + "empty);";
		}
		logger.warn(errString);
	} // ends printMessage()
	
	
} // ends Message
