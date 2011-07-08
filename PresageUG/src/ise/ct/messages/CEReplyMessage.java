package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class CEReplyMessage extends Message {

	public CEReplyMessage(String myId, Conversation conv, boolean terminated, boolean requestAccepted, int chipColour, int noChips ) {
		super(CEReplyMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Boolean(terminated), new Boolean(requestAccepted), new Integer(chipColour), new Integer(noChips) });
	}
	
	public boolean getTerminated()
	{
		return (Boolean)contents[0];
	}
	
	public boolean getAccepted()
	{
		return (Boolean)contents[1];
	}

	public Integer getColour()
	{
		return (Integer)contents[2];
	}
	
	public Integer getNoChips()
	{
		return (Integer)contents[3];
	}
	
}
