package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class AuctionEndMessage extends Message {

	public AuctionEndMessage(String myId, Conversation conv, String winner, int chipCol, int chipNo) {
		super(AuctionEndMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new String(winner), new Integer(chipCol), new Integer(chipNo) }
		);
	}
	
	public String getWinner() {
		return (String) contents[0];
	}
	
	public Integer getChipColour() {
		return (Integer) contents[1];
	}
	
	public Integer getChipNumber() {
		return (Integer) contents[2];
	}
	
}
