package ise.ct.participants;

import ise.ct.CTParticipant;
import ise.ct.CTWorld;
import ise.ct.Coord;
import ise.ct.VoteInfo;
import ise.ct.actions.MakeNeutralAction;
import ise.ct.messages.TVNotificationMessage;
import ise.ct.messages.TVRequestMessage;
import ise.ct.messages.TVResultMessage;
import ise.ct.messages.TVVoteMessage;

import java.util.UUID;

import org.apache.log4j.Logger;

import presage.ConvKey;
import presage.Conversation;
import presage.Message;
import presage.Simulator;

public class BallotBox extends CTParticipant {
	
	Logger logger = Logger.getLogger(this.getClass().getName());

	public BallotBox(String[] args) {
		super(args);
	}

	@Override
	public void physicallyAct() {
		//decrement time of active voting and check if they excited the timeOut limit
		String[] activeVotesKeys = (String[])beliefs.keySet().toArray(new String[beliefs.size()]);
		for(int i=0; i<beliefs.size(); i++){
			//makes sure that only objects of type VoteInfo are accessed
			if(beliefs.get(activeVotesKeys[i]).getClass().getName().equals(VoteInfo.class.getName())){
				//decreases vote time remaining and checks if time out is reached
				if(((VoteInfo)beliefs.get(activeVotesKeys[i])).getTimeOut()){
					Coord coord = ((VoteInfo)beliefs.get(activeVotesKeys[i])).getCoord();
					logger.info("Vote for tile: (" +coord.x+","+coord.y + ") time out. Send result ignoring delayed players.");
					sendResult(activeVotesKeys[i]);
				}
			}
		}
	}

	@Override
	public void proActiveBehaviour() {}
	
	public void onActivation() {
		super.onActivation();	
	}
	
	public void vote(Message msg, Conversation conv){
		
		if(conv.state == "initial"){	//received a new request for voting
			boolean declined;
			conv.changeStateTo("voterequest");	
			declined = handleVoteRequest(msg, conv);
			if(declined)
				conv.changeStateTo("end");
			else
				conv.changeStateTo("wait");		//need to be in wait state to be able to receive a TVVoteMessage
		}
		else if(conv.state == "wait"){	//received a vote
			boolean voteFinished = handleWait(msg, conv);
			if(voteFinished){
				sendResult(((TVVoteMessage)msg).getVoteKey());
			}
				
		}else 	//something bad happened
			if(conversations.get(conv.myKey)!=null)
				conv.changeStateTo("end");
	}
	
	public boolean handleVoteRequest(Message msg, Conversation conv){
		
		String TVKey;
		int noOfVoters = ((CTWorld)Simulator.pworld).getNumberOfPlayers();
		
		UUID uuid = UUID.randomUUID();
		TVKey = uuid.toString();	//unique key used to identify the current voting
		
		TVRequestMessage voteRequestMsg = (TVRequestMessage)msg;
		int tileColour = ((CTWorld)Simulator.pworld).getWorldState(voteRequestMsg.getCoord());
		
		logger.info("Vote request from " + conv.theirId + " for tile (" + voteRequestMsg.getCoord().x +"," + voteRequestMsg.getCoord().x + ")"   );
		
		//if requesting a vote for a tile with valid coordinates which is not an auction house colour
		if( ((CTWorld)Simulator.pworld).coordValid( voteRequestMsg.getCoord()) &&
			(!((CTWorld)Simulator.pworld).getAuctionColours().contains(tileColour))	){
			VoteInfo info = new VoteInfo(voteRequestMsg.getFrom(), voteRequestMsg.getCoord(), noOfVoters);
			beliefs.put(TVKey, info);	//create a new record of type VoteInfo for the new vote request
			sendNotifications(TVKey, voteRequestMsg.getFrom(), voteRequestMsg.getCoord(),conv);	
			return false;
		}
		else	//if requesting a vote for a tile with invalid coordinates or its colour is an auction house colour, decline the vote request
		{	
			logger.info("Vote request from " + conv.theirId + " for tile (" + voteRequestMsg.getCoord().x +"," + voteRequestMsg.getCoord().y + ") declined due to invalid coordinates or it is an Auction House colour");
			TVNotificationMessage declineNotificationMsg = new TVNotificationMessage(myId, conv, "", TVKey, voteRequestMsg.getCoord(), true);
			outbox.enqueue(declineNotificationMsg);
			return true;
			
		}
		
	}

	//executed when a vote is received from a player
	public boolean handleWait(Message msg, Conversation conv){
		
		TVVoteMessage voteMsg = (TVVoteMessage)msg;
		String TVKey = voteMsg.getVoteKey();
		boolean decision = voteMsg.getVote();	//true if player voted in favour
		
		VoteInfo voteInfo = (VoteInfo)beliefs.get(TVKey);	//copy vote information from beliefs
		voteInfo.addVote(conv.myKey, decision);				//register agent's vote
		beliefs.put(TVKey, voteInfo);						//replace the old vote information with the updated
		
		//remove comment to see what each player voted. Need to make votesInFavour and votesReceived public in VoteInfo class first:
		//logger.info(voteMsg.getFrom()+" voted "+voteMsg.getVote()+" Votes in favour: " + voteInfo.votesInFavour + "/"+voteInfo.votesReceived);
		
		return voteInfo.getVoteEnded();
	}
	
	//notifies all the players that they need to vote and for what they need to vote
	public void sendNotifications(String TVKey, String voteRequester, Coord coord, Conversation requesterConv){
		
		//iterate through all players and notify them about the vote
		for(int i=0; i < connectedContacts().size(); i++)
			if (connectedContacts().get(i).roles().contains("player")){
				//if player is vote requester then don't begin a new conversation since there is already one
				if(connectedContacts().get(i).name().equals(voteRequester)){
					
					TVNotificationMessage voteNotificationMsg = new TVNotificationMessage(myId, requesterConv, voteRequester, TVKey, coord, false);
					outbox.enqueue(voteNotificationMsg);	
					
					VoteInfo tempInfo = (VoteInfo)beliefs.get(TVKey);	//copy vote information from beliefs
					tempInfo.addNewVoter(requesterConv.myKey);			//add the new voter
					beliefs.put(TVKey, tempInfo);						//replace the old vote information with the updated
					
					
				}
				//for any other player do the following
				else	
				{
					Conversation notification = new Conversation(connectedContacts().get(i).name(), "vote", convKeyGen.getKey());
					
					TVNotificationMessage voteNotificationMsg = new TVNotificationMessage(myId, notification, voteRequester, TVKey, coord, false);
					outbox.enqueue(voteNotificationMsg);	
					
					VoteInfo tempInfo = (VoteInfo)beliefs.get(TVKey);	//copy vote information from beliefs
					tempInfo.addNewVoter(notification.myKey);			//add the new voter
					beliefs.put(TVKey, tempInfo);						//replace the old vote information with the updated
					
					notification.changeStateTo("wait");					//need to be in wait state to be able to receive a TVVoteMessage
					conversations.put(notification.myKey, notification);
				}	
			}
	}
	
	//notifies all the players about the outcome of the voting
	public void sendResult(String TVKey){
		int noOfVoters = ((VoteInfo)beliefs.get(TVKey)).getVotesReceived();
		int noOfPlayers = ((CTWorld)Simulator.pworld).getNumberOfPlayers();
		boolean inFavour = ((VoteInfo)beliefs.get(TVKey)).getVoteResult();	//returns true if majority of players voted in favour
		Conversation resultConv;
		ConvKey convKeys[] = ((VoteInfo)beliefs.get(TVKey)).getVotersConversations();	//array of ConvKeys of all conversations of votingAgent with players
		Coord coord = ((VoteInfo)beliefs.get(TVKey)).getCoord();
		
		//iterate through all conversations with players associated with the current voting
		for(int i=0; i < noOfPlayers; i++){
			resultConv = conversations.get(convKeys[i]);		
			TVResultMessage resultMsg = new TVResultMessage(myId, resultConv, TVKey, inFavour, false);
			
			//does not send result to players which did not vote. If this is removed they still can't get the result since the toConvKey is not
			//instantiated because they didn't respond to the vote notification.
			if(resultMsg.getToConvKey().isInstantiated())
				outbox.enqueue(resultMsg);
			
			resultConv.changeStateTo("end");			
		}	
		
		logger.info("Voting outcome: -(" + inFavour + ")- with votes in favour: "+((VoteInfo)beliefs.get(TVKey)).getVotesInFavour()+"/"+ noOfVoters);
		
		if(inFavour)	//if vote outcome is in favour of changing the tile's colour, issue an action to make the tile's colour neutral
			Simulator.pworld.act(new MakeNeutralAction(myId,myKey,coord));
		
		beliefs.remove(TVKey);		//since voting is over its information are no longer needed, therefore remove them
	}
}
