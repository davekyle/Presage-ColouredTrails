package ise.ct;

import java.util.TreeMap;

import presage.ConvKey;

public class VoteInfo {
	private String TVRequesterID;
	private Coord coord;	
	private int noOfVoters;
	private int votesReceived;
	private int votesInFavour;
	private int timeOut;
	private TreeMap<ConvKey, Boolean> voters;
	//allows agents to respond to a vote request in the cycle they received the vote notification
	//or the next cycle. Otherwise their vote is ignored.
	private static final int DEFAULT_TIMEOUT = 4;	
	
	public VoteInfo(String _TVRequesterID, Coord _coord, int _noOfVoters){
		this.TVRequesterID = _TVRequesterID;
		this.coord = _coord;
		this.noOfVoters = _noOfVoters;
		this.votesReceived = 0;
		this.votesInFavour = 0;
		this.timeOut = DEFAULT_TIMEOUT;
		this.voters = new TreeMap<ConvKey, Boolean>();		//boolean indicates if player has voted or not
	}
	
	private void incrementVotesReceived(boolean _voteInFavour){
		this.votesReceived++;
		
		if(_voteInFavour){
			this.votesInFavour++;
		}
	}
	
	public int getVotesReceived(){
		return this.votesReceived;
	}
	
	public int getVotesInFavour(){
		return this.votesInFavour;
	}
	
	public String getVoteRequesterID(){
		return this.TVRequesterID;
	}
	
	public Coord getCoord(){
		return this.coord;
	}
	
	public void addVote(ConvKey key, boolean decision){
		//if the player has not voted register his vote
		if(!this.voters.get(key)){
			incrementVotesReceived(decision);
		}
	}
	
	public boolean getVoteEnded(){
		if(this.votesReceived < this.noOfVoters)
			return false;
		else if(this.votesReceived == this.noOfVoters)
			return true;
		else
			return false;
	}
	
	public boolean getVoteResult(){
		if(this.votesInFavour > this.votesReceived/2)
			return true;
		else
			return false;
	}
	
	public void addNewVoter(ConvKey key){
		this.voters.put(key, false);
	}
	
	public ConvKey[] getVotersConversations(){
		return (ConvKey[])voters.keySet().toArray(new ConvKey[voters.size()]);
	}
	
	public boolean getTimeOut(){
		this.timeOut--;
		
		if(this.timeOut == 0)
			return true;
		else
			return false;
	}
}
