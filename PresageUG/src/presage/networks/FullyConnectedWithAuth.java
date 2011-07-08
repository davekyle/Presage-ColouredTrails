package presage.networks;

import org.apache.log4j.*;

import presage.Message;
import presage.Network;
import presage.Participant;
import presage.Simulator;


public class FullyConnectedWithAuth extends Network {

	Logger logger = Logger.getLogger(this.getClass().getName());
	
	public FullyConnectedWithAuth() {
		super();
		logger.info(" Initialising...");	
	}
	
	
	public void execute() {

	}

	public boolean sendMessage(String host, int port, Message msg) {
		// Just ignore port and host as its going to be wrong and
		// uninitialised!!
		
		// Validate the key of the sender
			if (!Simulator.validateKey(msg.getFrom(), msg.getSenderKey())) {
				logger.warn("MESSAGE WITH INVALID KEY - MESSAGE DROPPED)");
				return false;
			}
		
		// Strip senders key now we have authenticated
			msg.setSenderKey("");
			
			enqueuePacket(msg);
			return true;
	}

	// adds a packet to a participants inbox.
	private void enqueuePacket(Message msg) {
		synchronized (Simulator.participants) {
			((Participant) Simulator.participants.get(msg.getTo())).inbox.enqueue(msg);
		}
	}

}
