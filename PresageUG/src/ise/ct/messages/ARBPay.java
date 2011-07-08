package ise.ct.messages;

import presage.Conversation;
import presage.Message;

public class ARBPay extends Message {
	
	public ARBPay(String myId, Conversation conv) {
		super(ARBPay.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] {});
	}
	
}
