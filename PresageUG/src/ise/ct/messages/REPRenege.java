package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class REPRenege extends Message {
	
	public REPRenege(String myId, Conversation conv, String playerId) {
		super(REPRenege.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { playerId });
	}

	public String getPlayerID() {
		return (String)contents[0];
	}
	
}
