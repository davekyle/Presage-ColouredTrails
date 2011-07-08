package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class ARBRelinquishResponse extends Message {
	
	public ARBRelinquishResponse(String myId, Conversation conv, boolean response) {
		super(ARBRelinquishResponse.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] {new Boolean(response)});
	}
	
	public boolean getResponse(){
		return ((Boolean)contents[0]).booleanValue();
	}
	
}
