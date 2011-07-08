package ise.ct;

import presage.ConvKey;
import presage.Conversation;
import java.util.TreeMap;

public class Auction extends Conversation {
	
	public String winnerID;
	
	public TreeMap<String, ConvKey> participants;
	
	
	public Auction(String theirId, String protocolName, ConvKey myKey) {
		super(theirId, protocolName, myKey);
		
		participants = new TreeMap<String, ConvKey>();
		winnerID = new String("no winner");
	}
}
