package ise.ct.messages;

import java.util.ArrayList;

import presage.ContactInfo;
import presage.Conversation;
import presage.Message;


public class NSReplyMessage extends Message {

	public NSReplyMessage(String myId, Conversation conv, ArrayList<ContactInfo> ci) {
		super(NSReplyMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { ci });
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<ContactInfo> getContactList()
	{
		return (ArrayList<ContactInfo>)contents[0];
	}
	
}
