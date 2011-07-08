package ise.ct.participants;

import java.util.Iterator;
import java.util.Set;

import ise.ct.CTParticipant;
import ise.ct.CTWorld;
import ise.ct.Coord;
import ise.ct.actions.MoveAction;
import ise.ct.messages.*;

import org.apache.log4j.*;

import presage.Conversation;
import presage.Message;
import presage.Simulator;


public class RandomAgent extends CTParticipant {
	
	/*****************************************************************
	 * RandomAgent roles:
	 * <player> - required. Defaults are:
	 * * Moves randomly without regard for IPVs
	 * * Always votes yes
	 * * Always waits until auction price is one chip before bidding
	 * * Always asks for "letoff" in Arbitrations
	 * * Always pays (if possible) in Arbitrations against it
	 * * Only trades his colour chips for the other agent's colour
	 * * Always asks for an equal amount of chips as the other agent does
	 * ---
	 * <speedracer> (done) - RandomAgent heads towards goal without regard for IPVs
	 * ---
	 * <finer> (done) - RandomAgent always fines offending agent in Arbitrations
	 * <demander> (done) - RandomAgent always demands [chipDemandAmount] chips of the offending agent's colour in Arbitrations
	 * <randomIPV> (done) - RandomAgent makes a random decision on what to do in Arbitrations (overrides others)
	 * ---
	 * <voteno> (done) - Overrides RandomAgent always voting yes in votes
	 * <voterandomly> (done) - RandomAgent votes randomly yes/no (overrides others)
	 * ---
	 * <bidearly> (done) - RandomAgent bids immediately in auctions
	 * <bidrandomly> (done) - RandomAgent bids at a random level in auctions (overrides others)
	 * ---
	 * <traderefuser> (done) - RandomAgent never accepts any chiprequests
	 * <tradereneger> (done) - RandomAgent never transfers any chips, even if it agreed to (overrides others)
	 *****************************************************************/

	Logger logger = Logger.getLogger(this.getClass().getName());
	static int numberOfCommsPhases = 0;
	CTWorld myWorld;
	//String iWant = "iWant";
	String theyWantString = "theyWant";
	String theirColourString = "theirColour";
	String bidThresholdString = "bidThreshold";
	String startPriceString = "startPrice";
	Integer chipDemandAmount = 2;
	
	public RandomAgent(String[] args) {
		super(args);
	}
	
	public void onActivation() {
		super.onActivation();
		cleanRoles();
	}
	
	public void cleanRoles(){
		if ((myRoles.contains("finer")) && (myRoles.contains("demander"))){
			myRoles.remove("finer");
			myRoles.remove("demander");
			myRoles.add("randomIPV");
			logger.warn(myId + " removed conflicting roles \"finer\" and \"demander\" and added \"randomIPV\"");
		}
		if ((myRoles.contains("finer")) && (myRoles.contains("demander")) && (myRoles.contains("randomIPV"))){
			myRoles.remove("finer");
			myRoles.remove("demander");
			logger.warn(myId + " removed conflicting roles \"finer\" and \"demander\" and left \"randomIPV\"");
		}
		if ((myRoles.contains("finer")) && (myRoles.contains("randomIPV"))){
			myRoles.remove("finer");
			logger.warn(myId + " removed conflicting role \"finer\" and left \"randomIPV\"");
		}
		if ((myRoles.contains("demander")) && (myRoles.contains("randomIPV"))){
			myRoles.remove("demander");
			logger.warn(myId + " removed conflicting role \"demander\" and left \"randomIPV\"");
		}
		
		if ((myRoles.contains("voteno")) && (myRoles.contains("voterandomly"))){
			myRoles.remove("voteno");
			logger.warn(myId + " removed conflicting role \"voteno\" and left \"voterandomly\"");
		}
		
		if ((myRoles.contains("bidearly")) && (myRoles.contains("voterandomly"))){
			myRoles.remove("bidearly");
			logger.warn(myId + " removed conflicting role \"bidearly\" and left \"bidrandomly\"");
		}
		if ((myRoles.contains("traderefuser")) && (myRoles.contains("tradereneger"))){
			myRoles.remove("traderefuser");
			logger.warn(myId + " removed conflicting role \"traderefuser\" and left \"tradereneger\"");
		}
	}
	
	public void proActiveBehaviour() {}
	
	// Own fn to call when sending chips
	public void transferChips(String destination, Integer chipColour, Integer chipAmount){
	//	CTWorld myWorld = (CTWorld)Simulator.pworld;
		Set<String> playerNames = myWorld.getPlayerNames();
		Integer myColour = myWorld.getPlayerColour(myId);
		if (myRoles.contains("tradereneger")){
			logger.trace(myId + " reports: I'm reneging on a deal with " + destination + " for " + chipAmount + " of " + chipColour);
		}
		else if (!playerNames.contains(destination)){
			logger.warn(myId + " reports: Invalid destination for transferChips: " + destination);
		}
		//If chipcolour is mycolour or if i have chipamount of chipcolour chips
		else if ((chipColour == myColour)
				|| (myWorld.getPlayerChips(myId, myKey, chipColour) >= chipAmount)) {
			logger.trace(myId + " reports: started a transferChips with " + destination + " for " + chipAmount + " of " + chipColour);
		//	make a new conversation with bank
			Conversation conversation = new Conversation(myWorld.getBankId(), "banksend", convKeyGen.getKey());
			conversations.put(conversation.myKey, conversation);
			conversation.setNoTimeout();
		//	make new banktransfer message, state initial
			BankTransfer message = new BankTransfer(myId, conversation, chipColour, chipAmount, destination);
			conversation.changeStateTo("initial");
			outbox.enqueue(message);
			logger.trace(myId + " reports that it made a banktransfer to " + destination + " for " + chipAmount + " of " + chipColour);
		//	set convstate to reply (deal with it in banksend fn)
			conversation.changeStateTo("reply");
		} else logger.warn(myId + " reports: Insufficient amount of chips of colour " + chipColour + " - needed " + chipAmount);
	}
	
	public void doCheckForInfractions(){
		//CTWorld myWorld = (CTWorld)Simulator.pworld;
		String currentPhase = myWorld.getCurrentPhase();
		if (!(currentPhase.equals(CTWorld.COMM_PHASE))){
			numberOfCommsPhases = 0;
		} else {
			if (numberOfCommsPhases == 0){
				//now in a movephase
				checkForInfractions();
				numberOfCommsPhases = numberOfCommsPhases + 1;
			} else numberOfCommsPhases = numberOfCommsPhases + 1;
		}
	}
	
	public void checkForInfractions(){
	//	CTWorld myWorld = (CTWorld)Simulator.pworld;
		Coord coord = new Coord();
		Integer myColour = myWorld.getPlayerColour(myId);
		Set<String> playerNames = myWorld.getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();
		String myName = myId;
		String name = "";
		//for all players
		while (iterator.hasNext()){
		//	get player pos
			name = iterator.next();
			coord = myWorld.getPlayerPos(name);
		//	if worldstate(playerpos) is mycolour
			if ((myWorld.getWorldState(coord) == myColour) && !(name == myName)){
		//		send arbreport
				Conversation conversation = new Conversation(myWorld.getArbiterId(), "arbitrationreport", convKeyGen.getKey());
				conversations.put(conversation.myKey, conversation);
				conversation.setNoTimeout();
				conversation.changeStateTo("initial");
				ARBReport arbReportMsg = new ARBReport(myId, conversation, name, coord);
				outbox.enqueue(arbReportMsg);
				conversation.changeStateTo("wait");
				Integer theirColour = myWorld.getPlayerColour(name);
				conversation.beliefs.put(theirColourString, theirColour);
				logger.trace(myId + " reports that " + name + " (colour " + theirColour + " is on one of their tiles (" + coord.x + "," + coord.y + "). Convkey " + conversation.myKey);
			}
		}
		//		deal with it in another fn
	}
	
	public String getPunishmentByRole(){
		//Let-off by default
		String result = ARBPunishment.LET_OFF;
		if (myRoles.contains("finer")){
			result = ARBPunishment.FINE;
		} else if (myRoles.contains("demander")){
			result = ARBPunishment.DEMAND_CHIPS;
		} else if (myRoles.contains("randomIPV")){
			int temp = Simulator.RandomGenerator.nextInt(3);
			switch (temp) {
				case 0 : result = ARBPunishment.LET_OFF; break;
				case 1 : result = ARBPunishment.FINE; break;
				case 2 : result = ARBPunishment.DEMAND_CHIPS; break;
			}
		}
		return result;
	}
	
	//@Override
	// Function to be called when contacted by arbiter investigating on your behalf
	public void arbitrationreport(Message msg, Conversation conv){
		conv.setNoTimeout();
		//CTWorld myWorld = (CTWorld)Simulator.pworld;
		//if state is initial, something went wrong
		if (conv.state.equals("initial")){
		//	set state to end
			conv.changeStateTo("end");
		}
		//if state is wait
		else if (conv.state.equals("wait")){
		//	received an arbrelinquishresponse
			ARBRelinquishResponse message = (ARBRelinquishResponse)msg;
		//	if response is true
			if (message.getResponse()){
		//		change convstate to end
				conv.changeStateTo("end");
				logger.trace(myId + " reports that someone relinquished chips to it in Conv: " + conv.myKey);
			}
		//	if response is false
			else {
				String punishment = getPunishmentByRole();
		//		send arbpunishment msg by role
				if (!(punishment.equals(ARBPunishment.DEMAND_CHIPS))){
					ARBPunishment reply = new ARBPunishment(myId, conv, punishment);
					outbox.enqueue(reply);
					logger.trace(myId + " reports that it punished someone with: " + punishment + " in Conv " + conv.myKey);
				} else {
					Integer chipColour = (Integer)conv.beliefs.get(theirColourString);
					ARBPunishment reply = new ARBPunishment(myId, conv, punishment, chipColour, chipDemandAmount);
					outbox.enqueue(reply);
					logger.trace(myId + " reports that it punished someone with: " + punishment + " for " + chipDemandAmount + " of " + chipColour + " in Conv " + conv.myKey);
				}
		//		change convstate to awaitpunishconf
				conv.changeStateTo("awaitpunishconf");
			}
		}
		//if state is awaitpunishconf
		else if (conv.state.equals("awaitpunishconf")){
		//	received an arbpunishmentgiven(punishment)
			ARBPunishmentGiven message = (ARBPunishmentGiven)msg;
		//	change convstate to end
			String punishment = message.getPunishment();
			conv.changeStateTo("end");
			logger.trace(myId + " reports that it got confirmation that someone was " + punishment + " in Conv " + conv.myKey);
		}
	}
	
	//@Override
	// Function to be called when contacted by arbiter and under investigation
	public void arbitrationinvestigation(Message msg, Conversation conv){
		conv.setNoTimeout();
	//	CTWorld myWorld = (CTWorld)Simulator.pworld;
		//if convstate is initial
		if (conv.state.equals("initial")){
			ARBRelinquishRequest message = (ARBRelinquishRequest)msg;
			logger.trace(myId + " reports that it is now under investigation in conv " + conv.myKey);
		//	if I have a chip (so no problem)
			if (myWorld.getPlayerChips(myId, myKey, message.getColour())>0){
		//		send an ARBRelinquishResponse with a true value
				ARBRelinquishResponse reply = new ARBRelinquishResponse(myId, conv, true);
				outbox.enqueue(reply);
		//		change convstate to end
				conv.changeStateTo("end");
				logger.trace(myId + " reports it tried to relinquish a chip in conv " + conv.myKey);
			}
		//	if I don't have a chip
			else {
		//		send an ARBRelinquishResponse with a false value
				ARBRelinquishResponse reply = new ARBRelinquishResponse(myId, conv, false);
				outbox.enqueue(reply);
		//		change convstate to awaitpunishment
				conv.changeStateTo("awaitpunishment");
				logger.trace(myId + " reports that it couldn't relinquish a chip and is awaiting punishment in conv " + conv.myKey);
			}
		}
		//
		//if convstate is awaitpunishment
		else if (conv.state.equals("awaitpunishment")){
			ARBPunishment message = (ARBPunishment)msg;
		//	if arbpunishment msg is LETOFF or FINE
			if ((message.getPunishment().equals(ARBPunishment.LET_OFF))
					|| (message.getPunishment().equals(ARBPunishment.FINE))) {
		//		change convstate to end
				conv.changeStateTo("end");
				logger.trace(myId + " reports that it recieved a " + message.getPunishment() + " in conv " + conv.myKey);
			}
		//	if arbpunishment msg is CHIPs
			else if  (message.getPunishment().equals(ARBPunishment.DEMAND_CHIPS)){
				Integer numberOfChips = message.getNumberOfChips();
				Integer chipColour = message.getChipColour();
				Integer myColour = myWorld.getPlayerColour(myId);
		//		if I can pay
				if ((myWorld.getPlayerChips(myId, myKey, chipColour) > numberOfChips) || (chipColour.equals(myColour))){
		//			send arbpay msg
					ARBPay reply = new ARBPay(myId, conv);
					outbox.enqueue(reply);
		//			change convstate to end
					conv.changeStateTo("end");
					logger.trace(myId + " reports that it is paying " + numberOfChips + " of colour " + chipColour + " in conv " + conv.myKey);
				}
		//		if I can't pay
				else {
		//			send arbnonpayment msg
					ARBNonPayment reply = new ARBNonPayment(myId, conv);
					outbox.enqueue(reply);
		//			(should recieve an arbpunishment msg with FINE)
					logger.trace(myId + " reports that it cannot pay in conv " + conv.myKey);
				}
			}
		}
	}
	
	public boolean getVoteByRole(){
		//Vote yes by default
		boolean result = true;
		if (myRoles.contains("voteno")){
			result = false;
		} else if (myRoles.contains("voterandomly")){
			//Random number from 0 (inclusive) to 2 (exclusive)
			if (Simulator.RandomGenerator.nextInt(2) == 0){
				result = false;
			} else {
				result = true;
			}
		}
		return result;
	}

	
	//@Override
	// Function to be called when receiving a votenotification
	public void vote(Message msg, Conversation conv){
		conv.setNoTimeout();
		logger.trace(myId + " reports: Received vote msg");
		//if it is a new vote
		if (conv.state.equals("initial")){
			TVNotificationMessage message = (TVNotificationMessage)msg;
			String TVKey = message.getVoteKey();
		//	change convstate to wait
			conv.changeStateTo("wait");
		//	vote according to role
			Boolean vote = getVoteByRole();
			TVVoteMessage reply = new TVVoteMessage(myId, conv, TVKey, vote);
			outbox.enqueue(reply);
			logger.trace(myId + " reports: voted " + vote + " in vote " + TVKey);
		}
		//if its not a new vote
		else if (conv.state.equals("wait")){
		//	get the result
			TVResultMessage message = (TVResultMessage)msg;
		//	if vote finished (accepted or declined)
			if (message.getVoteAccepted() || message.getDeclined()){
		//		change convstate to end
				conv.changeStateTo("end");
				logger.trace(myId + " reports: vote " + message.getVoteKey() + " concluded: " + message.getVoteAccepted());
			}
		}
		//if something broke
		else {
			conv.changeStateTo("end");
			logger.trace(myId + " reports: broken vote");
		}
	}
	
	
	//@Override
	// Function to be called when sent stuff by bank
	public void bankreceive(Message msg, Conversation conv){
		BankTransfer message = (BankTransfer)msg;
		logger.trace(myId + " reports it received " + message.getAmount() + " of " + message.getColour() + " from the bank. From = " + message.getFrom() + ". Id = " + message.getId() + ". Conv " + conv.myKey);
	}
	
	//@Override
	// Function to be called when bank is sending stuff for you (will show success/fail)
	public void banksend(Message msg, Conversation conv){
		conv.setNoTimeout();
		BankReceipt message = (BankReceipt)msg;
		// convstate should be reply
		if (conv.state.equals("reply")){
		// make sure it succeeded (shouldnt fail)
			if (!(message.getSuccess())){
				logger.warn(myId + " reports: Failed a banksend");
			} else logger.trace(myId + " reports: Successful banksend in conv " + conv.myKey);
			conv.changeStateTo("end");
		}
	}
	
	public Integer getBidThresholdByRole(Integer startPrice){
		//Bid at 1 by default
		Integer result = 1;
		if (myRoles.contains("bidearly")){
			result = startPrice;
		} else if (myRoles.contains("bidrandomly")){
			result = Simulator.RandomGenerator.nextInt(startPrice+1);
		}
		return result;
	}
	
	//@Override
	// Function to be called when receiving an auction broadcast
	public void auction(Message msg, Conversation conv){
		conv.setNoTimeout();
		//if convstate is initial
		if (conv.state.equals("initial")){
		//	send auctionyellmessage
				AuctionYellMessage reply = new AuctionYellMessage(myId, conv, myKey);
				outbox.enqueue(reply);
				AuctionStartMessage message = (AuctionStartMessage)msg;
				Integer startPrice = 0;
				Integer chipColour = message.getChipColour();
				Integer chipAmount = message.getChipNumber();
				conv.beliefs.put(startPriceString, startPrice);
				logger.trace(myId + " reports that it is participating in an auction (" + conv.myKey + ") to buy " + chipAmount + " of " + chipColour);
		//	change convstate to participate
				conv.changeStateTo("participate");
		}
		//if convstate is participate
		else if (conv.state.equals("participate")) {
		//	if msg is auctionpricemessage
			if (msg instanceof AuctionPriceMessage){
		//		read auctionpricemessage
				AuctionPriceMessage message = (AuctionPriceMessage)msg;
				Integer price = message.getPackagePrice();
				Integer startPrice = (Integer)conv.beliefs.get(startPriceString);
				Integer chipColour = message.getChipColour();
				Integer chipAmount = message.getChipNumber();
				Integer threshold = 0; // initialise
				if (price > startPrice){
					// first pricemsg
					startPrice = price;
					conv.beliefs.put(startPriceString, price);
					threshold = getBidThresholdByRole(startPrice);
					conv.beliefs.put(bidThresholdString, threshold);
					logger.trace(myId + " reports that it is participating in an auction (" + conv.myKey + ") to buy " + chipAmount + " of " + chipColour + " and set the startPrice to " + startPrice + " and the threshold to "+ threshold);
				}
				threshold = (Integer)conv.beliefs.get(bidThresholdString);
		//		if price is threshold
				if (price.equals(threshold)){
		//			send auctionyellmessage
					AuctionYellMessage reply = new AuctionYellMessage(myId, conv, myKey);
					outbox.enqueue(reply);
					logger.trace(myId + " reports that it bid on an auction (" + conv.myKey + ") to get " + chipAmount + " of " + chipColour + " because the price is at the threshold of " + threshold);
				}
			}
		//	if msg is auctionendmessage
			if (msg instanceof AuctionEndMessage){
				AuctionEndMessage message = (AuctionEndMessage)msg;
				if (message.getWinner().equals(myId)) {
					Integer threshold = (Integer)conv.beliefs.get(bidThresholdString);
					logger.trace(myId + " reports: it won an auction (" + conv.myKey + ") for " + message.getChipNumber() + 
							" chip(s) of colour " + message.getChipColour() + " by spending the threshold of " + threshold);
				}
		//		change convstate to end
				conv.changeStateTo("end");
			}
		}
	}
	
	public void chipoffer(Message msg, Conversation conv) {}

	public void chiprequest(Message msg, Conversation conv){
		conv.setNoTimeout();
		//CTWorld myWorld = (CTWorld)Simulator.pworld;
		int myColour = myWorld.getPlayerColour(myId);
		if (myRoles.contains("traderefuser")){
			String sender = conv.theirId;
		//	send cereplymsg with terminated true
			CEReplyMessage reply = new CEReplyMessage(myId, conv, true, false, 0, 0);
			outbox.enqueue(reply);
			//	change convstate to end
			conv.changeStateTo("end");
			logger.trace(myId + " reports: it refused a chiprequest (" + conv.myKey + ") from " + sender + " because it is a tradrefuser");
		} else {
			//if convstate is initial
			if (conv.state.equals("initial")){
				CERequestMessage message = (CERequestMessage)msg;
				Integer chipColour = message.getChipColour();
				Integer chipAmount = message.getChipAmount();
				String sender = conv.theirId;
				int theirColour = myWorld.getPlayerColour(sender);
				logger.trace(myId + " reports: it is in an initial chiprequest conversation (" + conv.myKey + ") with " + sender + ". They want " + chipAmount + " of " + chipColour);
			//	if colour is mycolour
				if (chipColour.equals(myColour)){
			//		send cereplymsg with amount to request (same as what they wanted)
					CEReplyMessage reply = new CEReplyMessage(myId, conv, false, false, theirColour, chipAmount);
					outbox.enqueue(reply);
			//		change convstate to negotiate
					conv.changeStateTo("negotiate");
					logger.trace(myId + " reports: initial chiprequest (" + conv.myKey + ") " + sender + " asked for " + chipAmount + " of " + chipColour + " so sent a msg asking for " + chipAmount + "x" + theirColour + " (" + reply.getNoChips() + "x" + reply.getColour() + ")");
					//conv.beliefs.put(iWant, chipAmount);
					conv.beliefs.put(theyWantString, chipAmount);
					logger.trace(myId + " reports: initial chiprequest (" + conv.myKey + ") with " + sender + " has beliefs: theyWant " + conv.beliefs.get(theyWantString));
				}
			//	If chipcolour is not mycolour
				else {
			//		send cereplymsg with terminated true
					CEReplyMessage reply = new CEReplyMessage(myId, conv, true, false, 0, 0);
					outbox.enqueue(reply);
			//		change convstate to end
					conv.changeStateTo("end");
					logger.trace(myId + " reports: initial chiprequest (" + conv.myKey + ") " + sender + " asked for chips of colour " + chipColour + " which is not " + myColour + " so refused");
				}
			}
			//if convstate is negotiate (regardless of if they change what they want)
			else if (conv.state.equals("negotiate")){
				CEReplyMessage message = (CEReplyMessage)msg;
			//	if not terminated
				if (!(message.getTerminated())){
					logger.trace(myId + " reports that conv " + conv.myKey + " is in negotiate and not terminated ...");
					Integer chipColour = message.getColour();
					Integer chipAmount = message.getNoChips();
					String theirId = conv.theirId;
					int theirColour = myWorld.getPlayerColour(theirId);
					if ((!(message.getAccepted())) && chipColour.equals(myColour)){
			//			send cereplymsg with accepted true
						CEReplyMessage reply = new CEReplyMessage(myId, conv, false, true, theirColour, chipAmount);
						outbox.enqueue(reply);
						logger.trace(myId + " reports that conv " + conv.myKey + " was not accepted, so accepting with a msg of " + chipAmount + " of " + theirColour);
						transferChips(theirId, chipColour, chipAmount);
						logger.trace(myId + " reports it sent " + chipAmount + " of " + chipColour + " to " + theirId + " in conv " + conv.myKey + ". MyColour is " + myColour + ". Conv was ended");
						conv.changeStateTo("end");
					} else if ((!(message.getAccepted())) && (!(chipColour.equals(myColour)))){
			//			send cereplymsg with terminated true
						CEReplyMessage reply = new CEReplyMessage(myId, conv, true, false, 0, 0);
						outbox.enqueue(reply);
						logger.trace(myId + " reports that conv " + conv.myKey + " was not accepted, and for " + chipAmount + " of " + chipColour + " not for " + myColour + " so terminating");
			//			change convstate to end
						conv.changeStateTo("end");
					} else if (message.getAccepted()){
						Integer amountTheyWant = (Integer)conv.beliefs.get(theyWantString);
						logger.trace(myId + " reports that conv " + conv.myKey + " was accepted... Assuming they didn't change their mind on asking for " + amountTheyWant + " so sending them " + amountTheyWant + " of " + myColour);
						transferChips(theirId, myColour, amountTheyWant);
			//			change convstate to end
						conv.changeStateTo("end");
					}
			//	if terminated, end conversation
				} else {
					logger.trace(myId + " reports that " + conv.myKey + " was terminated");
					conv.changeStateTo("end");
				}
			}
		}
	}
	
	public int speedMove(){
		int move = 0;
		Coord myLoc = myWorld.getPlayerPos(myId);
		Coord myGoal = myWorld.getPlayerGoal(myId);
		Coord testCoord = new Coord();
		
		if (myGoal.y > myLoc.y){
			//go up
			if (myGoal.x > myLoc.x){
				//go up right
				move = CTWorld.NORTHEAST;
			} else if (myGoal.x < myLoc.x){
				//go up left
				move = CTWorld.NORTHWEST;
			} else if (myGoal.x == myLoc.x){
				//pick one
				testCoord.set(((myLoc.x)-1),((myLoc.y)+1));
				if (myWorld.coordValid(testCoord)){
					move = CTWorld.NORTHWEST;
				} else{
					move = CTWorld.NORTHEAST;
				}
			}
		} else if (myGoal.y < myLoc.y){
			//go down
			if (myGoal.x > myLoc.x){
				//go down right
				move = CTWorld.SOUTHEAST;
			} else if (myGoal.x < myLoc.x){
				// go down left
				move = CTWorld.SOUTHWEST;
			} else if (myGoal.x == myLoc.x){
				// pick one
				testCoord.set(((myLoc.x)-1),((myLoc.y)-1));
				if (myWorld.coordValid(testCoord)){
					move = CTWorld.SOUTHWEST;
				} else{
					move = CTWorld.SOUTHEAST;
				}
			}
		} else if (myGoal.y == myLoc.y){
			if (myGoal.x > myLoc.x){
				// go right
				move = CTWorld.EAST;
			} else if (myGoal.x < myLoc.x){
				//go left
				move = CTWorld.WEST;
			}
		}
		return move;
	}
	
	public void reputation(Message msg, Conversation conv){
		conv.changeStateTo("end");
	}

	@Override
	public void physicallyAct() {
		myWorld = (CTWorld)Simulator.pworld;
		int move;
		doCheckForInfractions();
		if (myWorld.getCurrentPhase().equals(CTWorld.MOVE_PHASE)) {
			if (myRoles.contains("speedracer")){
				move = speedMove();
			} else {
				move = Simulator.RandomGenerator.nextInt(6);
			}
			myWorld.act(new MoveAction(myId, myKey, move));
		} else if (myWorld.getCurrentPhase().equals(CTWorld.INIT_PHASE)) {
			for (int i = 0; i < ((CTWorld)Simulator.pworld).getNumberOfPlayers(); i++) {
				//logger.debug("Colour " + i + " owned by " + getPlayerForColour(i));
			}
		}
	}
}
