package ise.ct;

import ise.ct.messages.NSAgentRegisteredMessage;
import ise.ct.messages.NSRegisterMessage;
import ise.ct.messages.NSReplyMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import presage.ContactInfo;
import presage.ConvKey;
import presage.Conversation;
import presage.Intention;
import presage.Message;
import presage.Participant;
import presage.Simulator;
import presage.util.Queue;

public abstract class CTParticipant extends Participant {

	Logger logger = Logger.getLogger(this.getClass().getName());
	
	public CTParticipant(String[] args) {	
		super(args[1], args[2], args[3], args[4]);
		
	}
	
	public void onActivation() {
		Map<String, Object> regResult = Simulator.pworld.register(myId, myRoles);
		myKey = Simulator.generateKey(myId);
		myBCard.setMetadata("colour", regResult.get("colour"));

		if (!contactKnown("Nameserver")) {
			CTWorld world = (CTWorld)Simulator.pworld;
			if (world.getNameserverInfo() != null) {
				addNewContact(world.getNameserverInfo());
			} else {
				logger.fatal("Unable to find Nameserver");
				System.exit(1);
			}
		}
		
		Conversation conversation = new Conversation("Nameserver", "nsregistration", 
				convKeyGen.getKey());
		conversations.put(conversation.myKey, conversation);		

		outbox.enqueue(new NSRegisterMessage(myId, conversation, myBCard));		
		conversation.changeStateTo("wait");
		conversation.setTimeout(new Integer(Simulator.cycle + 7));		
	}
	
	public void nsregistration(Message msg, Conversation conversation)
	{
		// registration reply, we can unpack contact info
		ArrayList<ContactInfo> arrayList = ((NSReplyMessage)msg).getContactList();
		Iterator<ContactInfo> iterator = arrayList.iterator();
		while (iterator.hasNext()) {
			addNewContact(iterator.next());
		}
		conversation.changeStateTo("end");
	}
	
	public void nsbroadcast(Message msg, Conversation conversation)
	{
		addNewContact(((NSAgentRegisteredMessage)msg).getContactInfo());
		conversation.changeStateTo("end");
	}
	
	public String getPlayerForColour(int colour) {
		Set<String> names = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iter = names.iterator();
		String result = null;
		String currentName;
		int currentColour;
		
        if (((CTWorld)Simulator.pworld).getAuctionColours().contains(colour)) { 
        	result = ((CTWorld)Simulator.pworld).getAuctionId(); 
        } else { 
        	while ((iter.hasNext())&&(result==null)) {
        		currentName = iter.next();
        		currentColour = ((CTWorld)Simulator.pworld).getPlayerColour(currentName);
        		if (currentColour==colour) result = currentName;
        	}
        }
        return result;
	}
	
	public void proActiveBehaviour() {
		if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.RESET_PHASE) ) {
			conversations = new TreeMap<ConvKey, Conversation>();
			intentions = new HashSet<Intention>();
			inbox = new Queue("inbox");
			outbox = new Queue("outbox");
		}
	}
	
}
