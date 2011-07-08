//Bank Agent
package ise.ct.participants;

import presage.Conversation;
import presage.Message;
import ise.ct.actions.TransferChipsAction;
import ise.ct.messages.BankTransfer;
import ise.ct.messages.BankReceipt;
import ise.ct.CTWorld;
import ise.ct.CTParticipant;
import presage.Simulator;

public class Bank extends CTParticipant {

	private CTWorld theWorld;
	
	public Bank(String[] args) {
		super(args);
	}

	public void onActivation() {
		super.onActivation();
		theWorld = (CTWorld)Simulator.pworld;
	}	
	
	//Perform a chip transfer via the bank agent
	public void banksend(Message msg, Conversation conv) {
		
		Integer colourToSend = ((BankTransfer)msg).getColour(); //Colour of chips to send
		Integer amountToSend = ((BankTransfer)msg).getAmount(); //Number chips to send
		String destId = ((BankTransfer)msg).getId(); //Destination agent ID

		//Check the sender's chip balance for the sending colour
		Integer chipBalance = theWorld.getPlayerChips(conv.theirId, myKey, colourToSend);
		
		//Get the sender's colour
		Integer senderColour = theWorld.getPlayerColour(conv.theirId);

		//Create conversation for replying to the sender
		Conversation reply = new Conversation(conv.theirId, conv.myKey, conv.theirKey, "banksend", false);
		conversations.put(reply.myKey, reply);
		reply.changeStateTo("reply");
		
		if( (chipBalance >= amountToSend) || (colourToSend.equals(senderColour)) ) {
			//Sender has enough chips, so send them
			Simulator.pworld.act(new TransferChipsAction(conv.theirId, myKey, destId, colourToSend, amountToSend));
			
			//Send message back to the sender to say their transfer was successful
			outbox.enqueue(new BankReceipt(myId, reply, true));
			
			//Send a receipt to the receiver to say they successfully received the chips
			Conversation bankReceipt = new Conversation(destId, "bankreceive", convKeyGen.getKey());
			conversations.put(bankReceipt.myKey, bankReceipt);
			outbox.enqueue(new BankTransfer(myId, bankReceipt, colourToSend, amountToSend, conv.theirId));
			bankReceipt.changeStateTo("end");
			
		} else {
			//Sender doesn't have enough chips! Send a message to notify them
			outbox.enqueue(new BankReceipt(myId, reply, false));
		}
		
		reply.changeStateTo("end");
		conv.changeStateTo("end");
		
	}
	
	public void physicallyAct() {}
	public void proActiveBehaviour() {}
	
}