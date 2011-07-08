package ise.ct.messages;

import presage.ContactInfo;
import presage.Conversation;
import presage.Message;

public class NSAgentRegisteredMessage extends Message {

	public NSAgentRegisteredMessage(String myId, Conversation conv, ContactInfo ci) {
		super(NSAgentRegisteredMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { ci });
	}
	
	public ContactInfo getContactInfo() {
		return (ContactInfo)contents[0];
	}
}
