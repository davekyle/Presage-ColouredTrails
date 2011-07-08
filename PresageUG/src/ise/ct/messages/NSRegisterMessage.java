package ise.ct.messages;

import presage.ContactInfo;
import presage.Conversation;
import presage.Message;

public class NSRegisterMessage extends Message {

	public NSRegisterMessage(String myId, Conversation conv, ContactInfo ci) {
		super(NSRegisterMessage.class.getCanonicalName(),
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
