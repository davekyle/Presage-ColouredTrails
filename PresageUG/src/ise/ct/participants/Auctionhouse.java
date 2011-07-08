package ise.ct.participants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import presage.ConvKey;
import presage.ContactInfo;
import presage.Conversation;
import presage.Message;
import ise.ct.CTParticipant;
import presage.Simulator;
import ise.ct.actions.TransferChipsAction;
import ise.ct.messages.*;
import ise.ct.CTWorld;
import ise.ct.Auction;

import org.apache.log4j.*;

public class Auctionhouse extends CTParticipant {
	
	ArrayList<Integer> auctionhouseColours;
	
	int auctionhouseInterval, intervalCount;
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	
//	 -------------------------------------------------------------------------------------
//	 -------------------------------------------------------------------------------------
	public Auctionhouse(String[] args) {
		super(args);
	
		auctionhouseColours = null;
		auctionhouseInterval = 10; 		/* default value */
		intervalCount = 0;
	}
	
	@SuppressWarnings("unchecked")
	public void onActivation() {
		Map<String, Object> regResult = Simulator.pworld.register(myId, myRoles);
		if (regResult.containsKey("colours")) {
			auctionhouseColours = (ArrayList<Integer>) regResult.get("colours");
		} else {
			logger.warn(myId + " has not been given its set of auction colours");
			auctionhouseColours = new ArrayList<Integer>();
		}
		if (regResult.containsKey("interval"))
			auctionhouseInterval = (Integer) regResult.get("interval");
		else
			logger.warn(myId + " has not been given its interval value and defaults to " + auctionhouseInterval);
		myKey = Simulator.generateKey(myId);
		
		if (!contactKnown("Nameserver")) {	
			CTWorld world = (CTWorld)Simulator.pworld;
			if (world.getNameserverInfo() != null) {
				addNewContact(world.getNameserverInfo());
			} else {
				logger.fatal("Unable to find Nameserver");
				System.exit(1);
			}
		}		
		/* register with Nameserver to get all other Player's contacts */
		Conversation conversation = new Conversation("Nameserver", "nsregistration", 
				convKeyGen.getKey());
		conversations.put(conversation.myKey, conversation);		

		outbox.enqueue(new NSRegisterMessage(myId, conversation, myBCard));		
		conversation.changeStateTo("wait");
		conversation.setTimeout(new Integer(Simulator.cycle + 7));		
	}
	
	@Override
	public void physicallyAct() {}

	public void proActiveBehaviour() {
	
		if (Simulator.cycle >= 15 && auctionhouseColours.size() > 0) {
			if (intervalCount <= 0 && conversations.size() < 10) {
				if (auctionhouseInterval > 0) intervalCount = auctionhouseInterval;
				else intervalCount = Simulator.RandomGenerator.nextInt(16) + 5;
				
				ConvKey auctionKey = convKeyGen.getKey();
				Conversation conv = new Auction("", "auction", auctionKey); /* theirKey by default empty */
				conversations.put(auctionKey, conv);
	
				Integer auctionChipColour = auctionhouseColours.get( Simulator.RandomGenerator.nextInt(auctionhouseColours.size()) );
				Integer auctionChipAmount = Simulator.RandomGenerator.nextInt(3) + 1;
				Integer auctionPackagePrice = auctionChipAmount * 4;
				conv.beliefs.put("chipColour", auctionChipColour);
				conv.beliefs.put("chipAmount", auctionChipAmount);
				conv.beliefs.put("packagePrice", auctionPackagePrice);
				
				ArrayList<ContactInfo> allPlayers = searchContactsByRole("player");
				Iterator<ContactInfo> iterAllPlayers = allPlayers.iterator();
	
				while (iterAllPlayers.hasNext()) {
					conv.theirId = iterAllPlayers.next().name();
					outbox.enqueue(new AuctionStartMessage(myId , conv, auctionChipColour, auctionChipAmount, auctionPackagePrice));
				}
				conv.theirId = "";
	
				conv.setTimeout(1); 	
				conv.resetTimeout();		/* makes auction stall 1 cycle */
				conv.changeStateTo("stall");		/* allows players to notify participation by yelling */
			} else {
				intervalCount--;
			}
		}
	}

	public void auction(Message msg, Conversation conversation) {
			if (conversation.state.equals("sell")) {
				AuctionYellMessage yellMsg = (AuctionYellMessage) msg;
				Auction thisAuction = (Auction) conversation;
																/* first message processed wins */
				if (thisAuction.winnerID.equals("no winner")) {	
					Integer auctionChipColour = (Integer) thisAuction.beliefs.get("chipColour");
					Integer auctionChipAmount = (Integer) thisAuction.beliefs.get("chipAmount");
					Integer auctionPackagePrice = (Integer) thisAuction.beliefs.get("packagePrice");
					
					thisAuction.winnerID = yellMsg.getFrom();
					String winnerKey = yellMsg.getUniqueKey();
					CTWorld ctWorld = (CTWorld) Simulator.pworld;
					
					/* Delivery: transfer from Auctionhouse to player */
					ctWorld.act( new TransferChipsAction(myId, myKey, thisAuction.winnerID, 
							auctionChipColour, auctionChipAmount) );
					
					/* Payment: ransfer from player to Auctionhouse */
					ctWorld.act( new TransferChipsAction(thisAuction.winnerID, winnerKey, myId, 
							ctWorld.getPlayerColour(thisAuction.winnerID), auctionPackagePrice) );
				}
				
			} else if (conversation.state.equals("register")) {
				AuctionYellMessage yellMsg = (AuctionYellMessage) msg;
				Auction thisAuction = (Auction) conversation;
				
				/* register yelling player as auction participant */
				thisAuction.participants.put(yellMsg.getFrom(), yellMsg.getFromConvKey());
				
			} else if (conversation.state.equals("stall")) {
				/* Do nothing, since should not receive messages during stall state of auction */
			} else if (conversation.state.equals("end")) {
				/* Do nothing, since auction already ended */
			} else
				logger.warn(myId + " is in an undefined state: " + conversation.state);	
	}
	
	public void auction_timeout(Conversation conversation) {
		if (conversation.state.equals("stall")) { 	/* timeout wakes auction up */
			if ( ((Auction) conversation).participants.size() == 0 )
				conversation.changeStateTo("register");
			else 
				conversation.changeStateTo("sell");
			conversation.setNoTimeout();
			inbox.enqueue(new AuctionInternalMessage(myId, conversation.myKey));
		}		
	}
	
	public void auctionInternalFunc(Message msg, Conversation conversation) {
		Auction thisAuction = (Auction) conversation;
		Integer chipColour = (Integer)thisAuction.beliefs.get("chipColour");
		Integer chipAmount = (Integer)thisAuction.beliefs.get("chipAmount");
		logger.debug(myId + " is running internal function");
		
		if (thisAuction.state.equals("sell")) {
			if ( thisAuction.winnerID.equals("no winner") ) {
			/* case of auction continue with decremented price, unless price is already 1 */
				Integer newPackagePrice = (Integer) thisAuction.beliefs.get("packagePrice") - 1;
				
				if (newPackagePrice > 0) {
					thisAuction.beliefs.put("packagePrice", newPackagePrice);
					
					Iterator iter = thisAuction.participants.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry entry = (Map.Entry)iter.next();
						thisAuction.theirId = (String) entry.getKey();
						thisAuction.theirKey = (ConvKey) entry.getValue();
						outbox.enqueue( new AuctionPriceMessage(myId, thisAuction, 
								chipColour, chipAmount, newPackagePrice) );
					}
					thisAuction.theirId = "";
					thisAuction.theirKey = ConvKey.NullConvKey;
					
					conversation.setTimeout(1);	
					conversation.resetTimeout();
					conversation.changeStateTo("stall");
				} else {					
					Iterator iter = thisAuction.participants.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry entry = (Map.Entry)iter.next();
						thisAuction.theirId = (String) entry.getKey();
						thisAuction.theirKey = (ConvKey) entry.getValue();
						outbox.enqueue( new AuctionEndMessage(myId, thisAuction, 
								thisAuction.winnerID, chipColour, chipAmount) );
					}
					thisAuction.theirId = "";
					thisAuction.theirKey = ConvKey.NullConvKey;
					
					thisAuction.changeStateTo("end");
					thisAuction.setNoTimeout();
				}
			} else {
			/* case of a winner exists -> auction shoud end */
				Iterator iter = thisAuction.participants.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry)iter.next();
					thisAuction.theirId = (String) entry.getKey();
					thisAuction.theirKey = (ConvKey) entry.getValue();
					outbox.enqueue( new AuctionEndMessage(myId, thisAuction, 
							thisAuction.winnerID, chipColour, chipAmount) );
				}
				thisAuction.theirId = "";
				thisAuction.theirKey = ConvKey.NullConvKey;
				
				thisAuction.changeStateTo("end");
				thisAuction.setNoTimeout();
			}
		} else if (thisAuction.state.equals("register")) {
		/* begin auction only if one or more participants were registered, else end auction */
			if (thisAuction.participants.size() > 0) {
			/* start with 1st price */
				Integer firstAskingPrice = (Integer)thisAuction.beliefs.get("packagePrice");
				
				Iterator iter = thisAuction.participants.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry)iter.next();
					thisAuction.theirId = (String) entry.getKey();
					thisAuction.theirKey = (ConvKey) entry.getValue();
					outbox.enqueue( new AuctionPriceMessage(myId, thisAuction, 
							chipColour, chipAmount, firstAskingPrice) );
				}
				thisAuction.theirId = "";
				thisAuction.theirKey = ConvKey.NullConvKey;
				
				conversation.setTimeout(1);	
				conversation.resetTimeout();
				conversation.changeStateTo("stall");
			} else {
			/* case when no participants recorded -> end auction immediately */
				thisAuction.changeStateTo("end");
				thisAuction.setNoTimeout();
			}
		} else if (thisAuction.state.equals("stall")) {
			
		} else if (thisAuction.state.equals("end")) {
			
		} else logger.warn(myId + " is in an undefined state: " + conversation.state);
	}
	
}
