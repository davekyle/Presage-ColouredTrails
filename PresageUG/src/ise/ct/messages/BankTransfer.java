package ise.ct.messages;

import presage.Conversation;

public class BankTransfer extends presage.Message {
	
	public BankTransfer(String myId, Conversation conv, Integer sendColour, Integer sendAmount, String id ) {
		super(BankTransfer.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Integer(sendColour), new Integer(sendAmount),  new String(id)});
	}
	
	public Integer getColour()
	{
		return (Integer)contents[0];
	}

	public Integer getAmount()
	{
		return (Integer)contents[1];
	}

	public String getId()
	{
		return (String)contents[2];
	}

}
