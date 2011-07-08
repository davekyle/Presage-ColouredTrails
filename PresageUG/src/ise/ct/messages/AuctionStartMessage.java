package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class AuctionStartMessage extends Message {

	public AuctionStartMessage(String myId, Conversation conv, int chipCol, int chipNo, int packagePrice) {
		super(AuctionStartMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Integer(chipCol), new Integer(chipNo), new Integer(packagePrice) }
		);
	}
	
	public Integer getChipColour() {
		return (Integer) contents[0];
	}
	
	public Integer getChipNumber() {
		return (Integer) contents[1];
	}
	
	public Integer getPackagePrice() {
		return (Integer) contents[2];
	}
	
}
