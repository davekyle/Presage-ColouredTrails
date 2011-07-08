package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class ARBNonPayment extends Message {
	
	public ARBNonPayment(String myId, Conversation conv) {
		super(ARBNonPayment.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] {});
	}
	
}
