package ise.ct.messages;

import presage.Conversation;

public class BankReceipt extends presage.Message {
	
	public BankReceipt(String myId, Conversation conv, Boolean success) {
		super(BankReceipt.class.getCanonicalName(),
				conv.theirId,
				myId,
				conv.type,
				conv.theirKey,
				conv.myKey,
				new Object[] { new Boolean(success) });
	}
	
	public boolean getSuccess()
	{
		return (Boolean)contents[0];
	}

}
