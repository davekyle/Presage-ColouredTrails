package ise.ct.participants;

import ise.ct.messages.NSAgentRegisteredMessage;
import ise.ct.messages.NSRegisterMessage;
import ise.ct.messages.NSReplyMessage;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.*;

import presage.ContactInfo;
import presage.Conversation;
import presage.Message;
import presage.Participant;
import presage.Simulator;

public class Nameserver extends Participant {
	
	Logger logger = Logger.getLogger(this.getClass().getName());

	public Nameserver(String[] args) {
		super(args[1], args[2], args[3], args[4]);
	}

	@Override
	public void physicallyAct() {}

	@Override
	public void proActiveBehaviour() {}
	
	public void nsregistration(Message msg, Conversation conversation)
	{
		ArrayList<ContactInfo> contactsInfo = connectedContacts();
		Iterator<ContactInfo> iterContacts = contactsInfo.iterator();
		ContactInfo currentContact;
		NSRegisterMessage registerMsg = (NSRegisterMessage)msg;
		if (!contactKnown(msg.getFrom()))
		{
			// announce to everyone
			while (iterContacts.hasNext()) {
				
				currentContact = iterContacts.next();
				
				Conversation broadcastConversation = new Conversation(currentContact.name(), "nsbroadcast", convKeyGen.getKey());

				conversations.put(broadcastConversation.myKey, broadcastConversation);

				outbox.enqueue(new NSAgentRegisteredMessage(myId, broadcastConversation, (registerMsg.getContactInfo())));
				
				broadcastConversation.changeStateTo("end");
				broadcastConversation.setTimeout(new Integer(Simulator.cycle + 7));
			}
			addNewContact(registerMsg.getContactInfo());
			
		}
		outbox.enqueue(new NSReplyMessage(myId, conversation, contactsInfo ));		
		logger.debug("Received hello from " + msg.getFrom());
		conversation.changeStateTo("end");
				
	}

}
