package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class ARBPunishment extends Message {
	
	public static final String LET_OFF = "letOff";
	public static final String DEMAND_CHIPS = "demandChips";
	public static final String FINE = "fine";
	
	public ARBPunishment(String myId, Conversation conv, String punishment, Integer chipColour, Integer numberOfChips ) {
		super(ARBPunishment.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new String(punishment), new Integer(chipColour), new Integer(numberOfChips) });
	}
	
	public ARBPunishment(String myId, Conversation conv, String punishment) {
		super(ARBPunishment.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new String(punishment), 0, 0 });
	}
	
	public String getPunishment()
	{
		return (String)contents[0];
	}
	
	public Integer getChipColour(){
		return (Integer)contents[1];
	}
	
	public Integer getNumberOfChips(){
		return (Integer)contents[2];
	}
	
}
