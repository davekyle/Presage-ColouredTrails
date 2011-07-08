package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class AuctionYellMessage extends Message {

	public AuctionYellMessage(String myId, Conversation conv, String myKey) {
		super(AuctionYellMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new String(myKey) }
		);
	}
	
	public String getUniqueKey() {
		return (String) contents[0];
	}
	
}

