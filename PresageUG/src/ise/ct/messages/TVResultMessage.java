package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class TVResultMessage extends Message {
	public TVResultMessage(String myId, Conversation conv, String TVKey, boolean voteAccepted, boolean declined) {
		super(TVResultMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { voteAccepted, declined, TVKey });
	}
	
	public boolean getVoteAccepted() {
		return (Boolean)contents[0];
	}
	
	public boolean getDeclined(){
		return (Boolean)contents[1];
	}
	
	public String getVoteKey(){
		return (String)contents[2];
	}
}
