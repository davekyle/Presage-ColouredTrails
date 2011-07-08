package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class TVVoteMessage extends Message {

	public TVVoteMessage(String myId, Conversation conv, String TVKey, boolean vote) {
		super(TVVoteMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { vote, TVKey });
	}
	
	public boolean getVote() {
		return (Boolean)contents[0];
	}
	
	public String getVoteKey(){
		return (String)contents[1];
	}
}
