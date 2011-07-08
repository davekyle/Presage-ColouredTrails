package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class CERequestMessage extends Message {

	public CERequestMessage(String myId, Conversation conv, int chipColour, int noChips ) {
		super(CERequestMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Integer(chipColour), new Integer(noChips) });
	}
	
	public Integer getChipColour() {
		return (Integer)contents[0];
	}
	
	public Integer getChipAmount() {
		return (Integer)contents[1];
	}
}
