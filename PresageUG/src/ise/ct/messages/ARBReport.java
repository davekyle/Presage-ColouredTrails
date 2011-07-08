package ise.ct.messages;

import presage.Conversation;
import presage.Message;
import ise.ct.Coord;

public class ARBReport extends Message {

	public ARBReport(String myId, Conversation conv, String theirId, Coord tile ) {
		super(ARBReport.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new String(theirId), new Coord(tile) });
	}
	
	public String getTheirId()
	{
		return (String)contents[0];
	}
	
	public Coord getTile()
	{
		return (Coord)contents[1];
	}
	
}
