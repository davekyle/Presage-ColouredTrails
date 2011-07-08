package ise.ct.messages;

import presage.Conversation;
import presage.Message;
import ise.ct.Coord;

public class TVRequestMessage extends Message {

	public TVRequestMessage(String myId, Conversation conv, Coord coord) {
		super(TVRequestMessage.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Coord(coord) });
	}
	
	public Coord getCoord() {
		return (Coord)contents[0];
	}
}
