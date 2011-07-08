 package ise.ct.messages;

import ise.ct.Coord;
import presage.Conversation;
import presage.Message;

public class ARBRelinquishRequest extends Message {
	
	public ARBRelinquishRequest(String myId, Conversation conv, int colour, Coord tile) {
		super(ARBRelinquishRequest.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] {new Integer(colour), new Coord(tile)});
	}

	public int getColour(){
		return (Integer)contents[0];
	}
	
	public Coord getTile(){
		return (Coord)contents[1];
	}
}
