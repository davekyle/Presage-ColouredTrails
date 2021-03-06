package presage.networks;

import org.apache.log4j.*;

import presage.Message;
import presage.Network;
import presage.Participant;
import presage.Simulator;


public class FullyConnected extends Network {

	Logger logger = Logger.getLogger(this.getClass().getName());
	
	public FullyConnected() {
		super();
		logger.info(" Initialising...");	
	}
	
	
	public void execute() {

	}

	public boolean sendMessage(String host, int port, Message msg) {
		// Just ignore port and host as its going to be wrong and
		// uninitialised!!
		
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
