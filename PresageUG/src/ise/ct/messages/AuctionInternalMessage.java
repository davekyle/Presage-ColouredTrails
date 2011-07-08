package ise.ct.messages;

import presage.Message;
import presage.ConvKey;

public class AuctionInternalMessage extends Message {

	public AuctionInternalMessage(String myId, ConvKey auctionKey) {
		super(AuctionInternalMessage.class.getCanonicalName(),
				myId,
				myId,
				"auctionInternalFunc",
				auctionKey,
				auctionKey,
				new Object[] { }
		);
	}
	
}
