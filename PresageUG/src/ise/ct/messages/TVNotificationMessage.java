package ise.ct.messages;

import presage.Conversation;
import presage.Message;
import ise.ct.Coord;

public class TVNotificationMessage extends Message {

	public TVNotificationMessage(String myId, Conversation conv, String TVRequesterID, String TVKey, Coord coord, boolean declined) {
		super(TVNotificationMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Coord(coord), TVRequesterID, declined, TVKey });
	}
	
	public Coord getCoord(){
		return (Coord)contents[0];
	}
	
	public String getTVRequesterID(){
		return (String)contents[1];
	}
	
	public boolean getDeclined(){
		return (Boolean)contents[2];
	}
	
	public String getVoteKey(){
		return (String)contents[3];
	}
}
