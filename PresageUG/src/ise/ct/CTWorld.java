package ise.ct;

import ise.ct.actions.*;
import ise.ct.config.CTConfig;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.load.Persister;

import presage.Action;
import presage.ContactInfo;
import presage.PhysicalWorld;
import presage.Protocol;
import presage.Simulator;

public class CTWorld extends PhysicalWorld {
	
	Logger logger = Logger.getLogger(this.getClass().getName());

	public static final int NORTHEAST = 0;
	public static final int EAST = 1;
	public static final int SOUTHEAST = 2;
	public static final int SOUTHWEST = 3;
	public static final int WEST = 4;
	public static final int NORTHWEST = 5;
	
	// changing these requires changes to lots of things
	public static final int INVALID = -1;
	public static final int NEUTRAL = 0;
	
	
	public static final String REG_PHASE = "reg";
	public static final String INIT_PHASE = "init";
	public static final String COMM_PHASE = "comm";
	public static final String MOVE_PHASE = "move";
	public static final String END_PHASE = "end";
	public static final String RESET_PHASE = "reset";
	public static final String COMPLETED_PHASE = "completed";
	
	public static final int MOVE_FINE = 0;
	public static final int ARBITER_FINE = 1;
	
	private ContactInfo nameserverInfo = null;
	
	private Player auctionhouse = null;		/* later consider chips of auctionhouse within scoring function */
	private ArrayList<Integer> auctionHouseColours = new ArrayList<Integer>();
	
	private String bankId;
	private String auctionId;
	private String arbiterId;
	private String ballotId;
	
	private CTConfig config;
	
	private int worldsizeX;  // must be even
	private int worldsizeY; // must be odd
	
	private int xMin = 0;
	private int xMax;  // = worldsizeX - 1;
	private int yMin = 0;
	private int yMax; // = worldsizeY - 1;
	
	private int[][] worldState; 

	private int currentColour;
	
	private Map<String, Player> playerMap = new TreeMap<String, Player>();

	private String currentPhase = REG_PHASE;
	private int currentPhaseRemaining = 3;
	
	private int numIterationsLeft;
	private int turn;
	private int configTimeout = -1;
	
	private ArrayList<CTConfig> configs = new ArrayList<CTConfig>();
	private int currentConfig = 0;
	
	public CTWorld() {
		super();
		logger.info(" Initialising...");
		
		this.loadConfigFiles();
		this.loadConfig(0);
		configTimeout = config.getTimeout();
		
		// Start numbering agents from 1 not 0
		currentColour = 1;
		
	}

	private void validateConfig(CTConfig config){
		
		int worldsizeX = config.getBoard().getWidth();
		if (worldsizeX%2!=0) worldsizeX++;
		int worldsizeY = config.getBoard().getHeight();
		if (worldsizeY%2!=1) worldsizeY++;
		
		if (config.getGoalDistance() >= (worldsizeX/2) || config.getGoalDistance() >= (worldsizeY/2)){
			logger.fatal("Config Error: Distance from start to goal is too high!");
			System.exit(1);
		}
		
		if (config.getScoring().getOtherChipsWeight() > config.getScoring().getOwnChipsWeight()){
			logger.fatal("Config error: Weighting for OtherChips is greater than OwnChips!");
			System.exit(1);
		}
		
		if ((config.getScoring().getOtherChipsWeight() < 0) || (config.getScoring().getOwnChipsWeight() < 0)){
			logger.fatal("Config error: Weighting for OtherChips or OwnChips is Negative!");
			System.exit(1);
		}
		
		if ((config.getScoring().getArbiterFine() < 0) || (config.getScoring().getMoveFine() < 0)){
			logger.fatal("Config error: ArbiterFine or MoveFine is Negative!");
			System.exit(1);
		}
	}
	
	private void loadConfigFiles(){
		Serializer serializer = new Persister();
		File configDir = new File(Simulator.inputFoldersPath + Simulator.inputFolder + "/");
		File[] files = configDir.listFiles();
		String filename;

		try
		{
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {
					filename = files[i].getName();
					if ( (filename.length() >= 12) && (filename.substring(0,8).equals("ctconfig")) && (filename.substring(filename.length()-4, filename.length()).equals(".xml")) ) {
						CTConfig c;
						c = serializer.read(CTConfig.class, files[i]);
						validateConfig(c);
						configs.add(c);
					}
				}
			}

		} catch (Exception e) {
			logger.fatal("Error reading configuration: ", e);	
		}

	}
	
	private void loadConfig(int number) {
		this.config = configs.get(number);
		
		this.worldsizeX = config.getBoard().getWidth();
		if (worldsizeX%2!=0) worldsizeX++;
		this.xMax = worldsizeX-1;
			
		this.worldsizeY = config.getBoard().getHeight();
		if (worldsizeY%2!=1) worldsizeY++;
		this.yMax = worldsizeY - 1;
		
		worldState = new int[worldsizeX][worldsizeY];
		
		numIterationsLeft = config.getIterations();
	}
	
	public void initialise(){
		Iterator<String> playerName = playerMap.keySet().iterator();
		Player player;
		Coord goal;
		int totalPlayers = getNumberOfPlayers();
		
		for (int i = xMin; i <= xMax; i++){
			for (int j = yMin; j <= yMax; j++){
				if (i % 2 == j % 2)
					// Need to get a random player colour, not 0 
					worldState[i][j] = ( Simulator.RandomGenerator.nextInt(this.getNumberOfPlayers()+config.getAuctionColours()) )+1;			
				else
					worldState[i][j] = INVALID;
			}
		}
		
		positionPlayers();
		
		while(playerName.hasNext()){
			player = playerMap.get(playerName.next());
			goal = calculateGoal(player.pos);
			player.goal = new CoordColour(goal.x, goal.y, getWorldState(goal.x, goal.y));
			worldState[player.pos.x][player.pos.y] = NEUTRAL;
			
			// Need to start at 1
			for (int i=1; i < totalPlayers+1+config.getAuctionColours(); i++){
				player.chips.put(new Integer(i), new Integer(config.getStartingChips()));
			}
		}
		
		if (auctionhouse != null) {
			for (int i=0; i < totalPlayers+1+config.getAuctionColours(); i++){
				auctionhouse.chips.put(new Integer(i), new Integer(0));
			}	
		}
		turn = 0;
	}

	private Coord calculateGoal(Coord startPos){
		
		Coord boardMid = new Coord(getWorldSizeX()/2, getWorldSizeY()/2);
		Coord goal = new Coord(-1,-1);
		
		while ((( (goal.x < xMin) || (goal.x > xMax) || (goal.y < yMin) || (goal.y > yMax) ))){
			int xDir = (startPos.x <= boardMid.x) ? 1 : -1;
			int yDir = (startPos.y <= boardMid.y) ? 1 : -1;
			
			int dx = Simulator.RandomGenerator.nextInt(config.getGoalDistance());
			int dy = config.getGoalDistance() - dx;
			
			goal.set( startPos.x + 2*(xDir * dx) + (xDir * dy), startPos.y + (yDir * dy));
		}
		
		return goal;
		
	}
	
	public int getWorldSizeX(){
		return worldsizeX;
	}

	public int getWorldSizeY(){
		return worldsizeY;
	}

	public int getWorldState(int i, int j){
		return worldState[i][j];
	}
	
	public int getWorldState(Coord a){
		return worldState[a.x][a.y];
	}
	
	public int[][] getWorldState() {
		return worldState;
	}
	
	public ContactInfo getNameserverInfo() {
		return nameserverInfo;
	}
	
	private boolean isPrivilegedAgent(String key) {
		if ( Simulator.validateKey("CTStats_PLUGIN", key))
			return true;
		else if ( Simulator.validateKey(arbiterId, key) )
			return true;
		else if ( Simulator.validateKey(bankId, key) )
			return true;
		else
			return false;
	}
	
	public int getPlayerChips(String id, String playerKey, int colour){
		if ((Simulator.validateKey(id, playerKey))||(isPrivilegedAgent(playerKey)))
			return playerMap.get(id).chips.get(colour);
		else
			return -1;
	}
	
	@SuppressWarnings("unchecked")
	public TreeMap<Integer, Integer> getPlayerChips(String id){
		return (TreeMap<Integer, Integer>)playerMap.get(id).chips.clone();
	}
	
	public Coord getPlayerPos(String id){
		if (playerMap.get(id) != null && playerMap.get(id).pos != null)
			return new Coord(playerMap.get(id).pos);
		else
			return null;
	}
	
	public Coord getPlayerGoal(String id){
		if (playerMap.get(id) != null && playerMap.get(id).goal != null)
			return new Coord(playerMap.get(id).goal);
		else
			return null;
	}
	
	public int getPlayerColour(String id){
		if ((id.equals(bankId)) || (id.equals(auctionId)) || (id.equals(arbiterId)) || (id.equals(ballotId))) {
			logger.error("Someone tried to getPlayerColour on " + id);
			return -1;
		}
		return playerMap.get(id).colour;
	}
	
    public Set<String> getPlayerNames(){
        return new TreeSet<String>(playerMap.keySet());
    }

	public int getNumberOfPlayers(){
		return playerMap.size();
	}
	
	private void transitionPhase() {	
		if (isEndGame()){
			if (currentPhase.equals(END_PHASE)) {
				currentPhase = RESET_PHASE;
				currentPhaseRemaining = 1;
				numIterationsLeft--;
				if (numIterationsLeft <= 0) {
					if (currentConfig >= (configs.size()-1))
						currentPhase = COMPLETED_PHASE;
					else {
						currentConfig++;
						loadConfig(currentConfig);
					}
				}
			} else if (currentPhase.equals(RESET_PHASE)) {
				currentPhase = INIT_PHASE;
				currentPhaseRemaining = 1;	
				configTimeout = config.getTimeout();
			} else {
				currentPhase = END_PHASE;
			}
		} else {
			currentPhaseRemaining--;
			if (currentPhaseRemaining == 0) {
				if (currentPhase.equals(REG_PHASE)) {
					currentPhase = INIT_PHASE;
					currentPhaseRemaining = 1;
				} else if (currentPhase.equals(INIT_PHASE) || currentPhase.equals(MOVE_PHASE)) {
					currentPhase = COMM_PHASE;
					currentPhaseRemaining = 10;
				} else if (currentPhase.equals(COMM_PHASE)) {
					currentPhase= MOVE_PHASE;
					currentPhaseRemaining = 1;
					turn++;
				}
			}
		}
	}
	
	public void execute() {
		super.execute();
		
		transitionPhase();
		if (currentPhase.equals(INIT_PHASE)) {
			initialise();
		} else if (currentPhase.equals(END_PHASE)) {
			recordScores();
			outputScores();
			outputWinner();
			recordDetailedScores();
		}
			
		Iterator <Player> iter = playerMap.values().iterator();
		while (iter.hasNext()) {
			Player p = iter.next();
			p.setMoved(false);
		}
		
	}

	protected Object move(Action action) {
		MoveAction act = (MoveAction)action;
		if (!currentPhase.equals(MOVE_PHASE)) {
			logger.warn("Player " + act.getPlayerID() + " denied move outside of move phase");
			return null;
		}
		Player player = playerMap.get(act.getPlayerID());

		if (Simulator.validateKey(act.getPlayerID(), act.getPlayerKey())) {
			
			if (player.hasMoved()) {
				logger.warn("Player " + act.getPlayerID() + " has already moved this turn");
				return null;
			}
			
			int newX = player.pos.x; 
			int newY = player.pos.y;
			int newColour;
			
			switch(act.getDirection()){
			case NORTHEAST:
				newY++;	newX++;	break;
			case EAST:
				newX+=2; break;
			case SOUTHEAST:
				newX++;	newY--; break;
			case SOUTHWEST:
				newY--; newX--; break;
			case WEST:
				newX-=2; break;
			case NORTHWEST:
				newX--;	newY++;	break;
			}
			
			if (!( (newX < xMin) || (newX > xMax) || (newY < yMin) || (newY > yMax) )) {
				
				newColour = getWorldState(newX, newY);
				
				if (auctionHouseColours.contains(newColour)){
					Integer numChips = player.chips.get(newColour);
					if (numChips != null) { // frd05 fix for Integer's int part being null
						player.pos.set(newX, newY);
						if (numChips > 0) {
							player.chips.put(newColour, player.chips.get(newColour) - 1 );			
							player.setMoved(true);
						} else {
							// Fine for moving onto auction house tiles
							player.addFines(MOVE_FINE);
						}
					}
				} else {
					player.pos.set(newX, newY);
					player.setMoved(true);
				}
			}
		}
			
		return null;
	}
	
	protected Object transferChips(Action action){
		TransferChipsAction act = (TransferChipsAction)action;
		
		if (auctionhouse != null && act.getPlayerID().equals(auctionhouse.guid)) {
		/* case of Auction house sending to player */
			if ( Simulator.validateKey(act.getPlayerID(), act.getPlayerKey()) ) {
				Player toPlayer = playerMap.get(act.getDestID());
				int noChips = Math.abs(act.getNumChips());
				logger.trace("auctionhouse transfer: " + noChips + " of colour " + act.getChipColour() + " to " + toPlayer.guid + ". It had " + auctionhouse.chips.get(act.getChipColour()) + " before the transfer and the player had " + toPlayer.chips.get(act.getChipColour()));
				auctionhouse.chips.put(act.getChipColour(),
						auctionhouse.chips.get(act.getChipColour()) - noChips); /* keep track of how many chips were auctioned off */
				toPlayer.chips.put(act.getChipColour(),
						toPlayer.chips.get(act.getChipColour()) + noChips);	
				logger.trace("auctionhouse transfer:  It had " + auctionhouse.chips.get(act.getChipColour()) + " after the transfer and the player had " + toPlayer.chips.get(act.getChipColour()));
			}
		} else if (auctionhouse != null && act.getDestID().equals(auctionhouse.guid)) {
		/* case of player sending to auction house */
			if ( Simulator.validateKey(act.getPlayerID(), act.getPlayerKey()) ) {
				int noChips = Math.abs(act.getNumChips());
				logger.trace("auctionhouse recieving " + noChips + " of colour " + act.getChipColour() + " from " + act.getPlayerID());
				auctionhouse.chips.put(act.getChipColour(),
						auctionhouse.chips.get(act.getChipColour()) + noChips);
			}
		} else {
				
			Player fromPlayer = playerMap.get(act.getPlayerID());
			if ((Simulator.validateKey(act.getPlayerID(), act.getPlayerKey())||(isPrivilegedAgent(act.getPlayerKey())))) {
				Player toPlayer = playerMap.get(act.getDestID());
				
				int noChips = Math.abs(act.getNumChips());
				
				if (fromPlayer.colour == act.getChipColour()){
					//Implicit chip creation here
					if (fromPlayer.chips.get(fromPlayer.colour) >= act.getNumChips())
						fromPlayer.chips.put(fromPlayer.colour, fromPlayer.chips.get(fromPlayer.colour) - noChips);
					else
						fromPlayer.chips.put(fromPlayer.colour, 0);
					
					toPlayer.chips.put(act.getChipColour(),
							toPlayer.chips.get(act.getChipColour()) + noChips);	
					
				} else {
					//Normal chip transfer
					if (fromPlayer.chips.get(act.getChipColour()) >= noChips) {
						fromPlayer.chips.put(act.getChipColour(),
								fromPlayer.chips.get(act.getChipColour()) - noChips);	
						toPlayer.chips.put(act.getChipColour(),
								toPlayer.chips.get(act.getChipColour()) + noChips);	
					}
				}
				
			}
		}
		return null;
	}
	
	protected Object createChips(Action action){
		
		CreateChipsAction act = (CreateChipsAction)action;

		logger.warn("Agent ("+act.getPlayerID()+") attempting chip creation - this is a deprecated action");
		return null;
	}
	
	protected Object makeNeutral(Action action){
		MakeNeutralAction act = (MakeNeutralAction)action;
		
		Coord coord = act.getTileCoord();
		
		if(Simulator.validateKey("BallotBox", act.getPlayerKey())){
			worldState[coord.x][coord.y] = NEUTRAL;
			logger.info("Colour of tile with coordinates: ("+coord.x+","+coord.y+") changed to NEUTRAL");
		}
		else
			logger.info(act.getPlayerID() + " prevented from changing tile with coordinates: ("+coord.x+","+coord.y+") to NEUTRAL");
		return null;
	}

	public boolean coordValid(Coord coord){
		try{
			if( (coord.x >= 0) && (coord.x < worldsizeX) &&
				(coord.y >= 0) && (coord.y < worldsizeY) &&
			    (((coord.x+1)%2) == ((coord.y+1)%2)) )
				return true;
			else
				return false;
			
		} catch (NullPointerException e) {
			return false;
		}
		
	}
	
	protected Object finePlayer(Action action){
		FineAction act = (FineAction)action;
		
		if (!Simulator.validateKey(arbiterId, act.getArbiterKey())){
			logger.warn("ARBITRATION KEY FAILURE - POSSIBLE FOUL PLAY (finePlayer)");
			return null;
		}
		
		float fineAmount = 0;
		switch(act.getFineType()){
		case MOVE_FINE:
			fineAmount = config.getScoring().getMoveFine();
			break;
		case ARBITER_FINE:
			fineAmount = config.getScoring().getArbiterFine();
			break;
		}
		
		Player player = playerMap.get(act.getPlayerID());
		player.addFines(fineAmount);
		
		return null;
	}
	
	protected Object removeChips(Action action){
		RemoveChipsAction act = (RemoveChipsAction)action;
		
		if (!Simulator.validateKey(arbiterId, act.getArbiterKey())){
			logger.warn("ARBITRATION KEY FAILURE - POSSIBLE FOUL PLAY (removeChips)");
			return null;
		}
		
		Player player = playerMap.get(act.getPlayerID());
		int numberChips = player.getNumberOfChips(act.getChipColour());
		numberChips -= Math.abs(act.getNumChips());
		player.setNumberOfChips(act.getChipColour(), numberChips);
		
		return null;
	}
	
	public boolean deregister(Object[] args) {
		return false;
	}
	
	private void positionPlayers(){
		Iterator<String> iter = playerMap.keySet().iterator();
		Player currentPlayer;
		Coord pos;
		
		while (iter.hasNext()) {
			currentPlayer = playerMap.get(iter.next());
			pos = new Coord();
			pos.x = Simulator.RandomGenerator.nextInt(worldsizeX - 1);
			pos.y = Simulator.RandomGenerator.nextInt(worldsizeY - 1);
			if (pos.x%2 != pos.y%2){	// ensure validity of start point ajk05 fix
				if (pos.y==0){
					pos.y++;
				}else{
					pos.y--;
				}
			}
			currentPlayer.pos = new Coord(pos);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> register(String myId, ArrayList<String> roles) {
		TreeMap<String, Object> result = new TreeMap<String, Object>();
		
		logger.info(myId + " Role: " + roles.get(0));
		if (roles.contains("player")) {
			Coord pos = new Coord();
			Player newPlayer = new Player(myId, pos, currentColour++);
			playerMap.put(myId, newPlayer);
			result.put("colour", newPlayer.colour);
		}
		
		if(roles.contains("banker")) {
			bankId = myId;
		}
		
		if(roles.contains("arbiter")) {
			arbiterId = myId;
		}
		
		if (roles.contains("ballotbox")) {
			ballotId = myId;
		}
		
		if (roles.contains("nameserver")) {
			nameserverInfo = Simulator.participants.get(myId).getContactInfo();
			logger.info("Found nameserver on: " + nameserverInfo.host());
		}
		
		if (roles.contains("auctionhouse")) {
			logger.info("Found auction house with name " + myId);
			auctionId = myId;
			int nextColour = currentColour + config.getAuctionColours();
			auctionhouse = new Player(myId, new Coord(-1, 0), currentColour);
					/* Auctionhouse's Player start position is at -1, 0 -> a non-existent location */
			auctionHouseColours = new ArrayList<Integer>();
			for (int colCount=currentColour; colCount<nextColour; colCount++) {
				auctionHouseColours.add(new Integer(colCount));
			}
			result.put("colours", auctionHouseColours);
			result.put("interval", config.getAuctionInterval());
			currentColour = nextColour;  /* skip the range of colours reserved for auction house */
		}
		
		return result;
	}
	
	public boolean isEndGame() {
		Iterator<Player> iter = playerMap.values().iterator();
		
		while (iter.hasNext()){	
			Player p = iter.next();
			if (p.pos.equals(p.goal))
				return true;
		}
		return turn == configTimeout;		
	}
	
	public boolean live(){
		return !currentPhase.equals(COMPLETED_PHASE);
	}
	
	private double getScore(Player player){
		int avgDist = 0;
		float score = 0;
		// calculate win Bonus
		if (player.pos.equals(player.goal)){
			score = config.getScoring().getWinBonus();
		}
		//logger.trace(player.guid + " initial: " + score);
		
		Collection<Player> otherPlayers = new ArrayList<Player>(playerMap.values());
		otherPlayers.remove(player);
		
		Iterator<Player> iter;
		
		iter = otherPlayers.iterator();
		while(iter.hasNext()) {
			Player aPlayer = iter.next();
			avgDist = avgDist + Coord.distance(aPlayer.pos, aPlayer.goal);
		}
		
		avgDist = avgDist/getNumberOfPlayers()-1;
		score = score + config.getScoring().getDistanceWeight()*avgDist;
		//logger.trace(player.guid + " after distance : " + score);
		
		// calculate score from other players having "your" chips
		int numberOfOwnChips = 0;
		iter = otherPlayers.iterator();
		while(iter.hasNext()) {
			Player aPlayer = iter.next();
			numberOfOwnChips = numberOfOwnChips + 
				aPlayer.getNumberOfChips(player.colour);
		}
		score = score - config.getScoring().getOwnChipsWeight()*numberOfOwnChips;
		//logger.trace(player.guid + " after ownchipsownedbyotherplayers : " + score);
		
		// calculate score from number of other players' chips you own
		int numberOfOtherChips = 0;
		iter = otherPlayers.iterator();
		while(iter.hasNext()) {
			Player aPlayer = iter.next();
			numberOfOtherChips = numberOfOtherChips + 
				player.getNumberOfChips(aPlayer.colour);
		}
		score = score + config.getScoring().getOtherChipsWeight()*numberOfOtherChips;
		//logger.trace(player.guid + " after otherplayerchipsowned : " + score);
		
		int numberOfOwnChipsInAuction = 0;
		int numberOfAuctionChips = 0;
		if (!(auctionhouse == null)){
			// calculate the effect of you owning auction chips
			Iterator<Integer> auctionIter = getAuctionColours().iterator();
			while (auctionIter.hasNext()) {
				int auctionColour = auctionIter.next();
				numberOfAuctionChips = numberOfAuctionChips + player.getNumberOfChips(auctionColour);
			}
			score = score + config.getScoring().getOtherChipsWeight()*numberOfAuctionChips;
			//logger.trace(player.guid + " after owningauctionchips : " + score);
			
			// calculate the effect of the auction having your chips
			numberOfOwnChipsInAuction = auctionhouse.getNumberOfChips(player.colour);
			score = score - config.getScoring().getOwnChipsWeight()*numberOfOwnChipsInAuction;
			//logger.trace(player.guid + " after auctionowninghischips : " + score);
		}
		
		// Subtract the fines the player has accrued
		score = score - player.getFines();
		//logger.trace(player.guid + " final : " + score);
		return score;
	}
	
	
	private void outputScores(){
		Iterator<Player> iter = playerMap.values().iterator();
		while(iter.hasNext()) {
			Player p = iter.next();
			logger.info("Score for " + p.guid + " : " + getScore(p));
		}
	}
	
	private void outputWinner(){
		Double winScore = Double.NEGATIVE_INFINITY;
		String winnerString = "";
		int numWinners = 0;
		int numGoalPlayers = 0;
		String goalPlayersString = "";
		
		
		Collection<Player> players = playerMap.values();
		Iterator<Player> iter;
		
		iter = players.iterator();
		while (iter.hasNext()) {
			Player thisplayer = iter.next();
			if(getScore(thisplayer) > winScore){
				winScore = getScore(thisplayer);
			}
			if ((thisplayer.pos).equals(getPlayerGoal(thisplayer.guid))){
				numGoalPlayers = numGoalPlayers + 1;
				goalPlayersString = goalPlayersString + thisplayer.guid + ", ";
			}			
		}

		iter = players.iterator();
		while (iter.hasNext()) {
			Player thisplayer = iter.next();
			if(getScore(thisplayer) == winScore){
				winnerString = winnerString +  thisplayer.guid + ", ";
				numWinners = numWinners+1;
			}
		}

		if (winScore == Double.NEGATIVE_INFINITY){
			winnerString = "No one got any points!";
		}
		else {
			if (numWinners > 1){
				winnerString = "The winners are : " + winnerString + "who won with " + winScore + " points!";
			}
			else {
				winnerString = "The winner is : " + winnerString + "who won with " + winScore + " points!";
			}
			if (numGoalPlayers == 0){
				goalPlayersString = "No one got to their goal!";
			}
			else {
				goalPlayersString = goalPlayersString.substring(0, (goalPlayersString.length()-2));
				if (numGoalPlayers > 1){
					goalPlayersString = "The following people got to their goal : " + goalPlayersString + ".";
				}
				else {
					goalPlayersString = "The only person who got to their goal was " + goalPlayersString + "!";
				}
			}
		}
		// Trick the compiler to output this string regardless
		Level tempLevel = logger.getLevel();
		logger.setLevel(Level.ALL);
		logger.info(goalPlayersString);
		logger.info(winnerString);
		logger.setLevel(tempLevel);
	}
	
	private String detailedPlayerScore(Player player){
		int avgDist = 0;
		float totalScore = 0;
		float goalScore = 0;
		float distanceScore = 0;
		float chipsWithOtherPlayersScore = 0;
		float chipsWithPlayerScore = 0;
		float chipsWithAuctionScore = 0;
		float auctionChipsWithPlayerScore = 0;
		float fines = 0;
		String output = "";
		String configNumber = ( (Integer)((CTWorld)Simulator.pworld).getConfigNum() ).toString();
		String iterationNumber = ( (Integer)((CTWorld)Simulator.pworld).getIterationNum() ).toString();
		
		// calculate win Bonus
		if (player.pos.equals(player.goal)){
			totalScore = config.getScoring().getWinBonus();
			goalScore = config.getScoring().getWinBonus();
		}
		//logger.trace(player.guid + " initial: " + score);
		
		Collection<Player> otherPlayers = new ArrayList<Player>(playerMap.values());
		otherPlayers.remove(player);
		
		Iterator<Player> iter;
		
		iter = otherPlayers.iterator();
		while(iter.hasNext()) {
			Player aPlayer = iter.next();
			avgDist = avgDist + Coord.distance(aPlayer.pos, aPlayer.goal);
		}
		
		avgDist = avgDist/getNumberOfPlayers()-1;
		distanceScore = config.getScoring().getDistanceWeight()*avgDist;
		totalScore = totalScore + distanceScore;
		//logger.trace(player.guid + " after distance : " + score);
		
		// calculate score from other players having "your" chips
		int numberOfOwnChips = 0;
		iter = otherPlayers.iterator();
		while(iter.hasNext()) {
			Player aPlayer = iter.next();
			numberOfOwnChips = numberOfOwnChips + 
				aPlayer.getNumberOfChips(player.colour);
		}
		chipsWithOtherPlayersScore = config.getScoring().getOwnChipsWeight()*numberOfOwnChips;
		totalScore = totalScore - chipsWithOtherPlayersScore;
		//logger.trace(player.guid + " after ownchipsownedbyotherplayers : " + score);
		
		// calculate score from number of other players' chips you own
		int numberOfOtherChips = 0;
		iter = otherPlayers.iterator();
		while(iter.hasNext()) {
			Player aPlayer = iter.next();
			numberOfOtherChips = numberOfOtherChips + 
				player.getNumberOfChips(aPlayer.colour);
		}
		chipsWithPlayerScore = config.getScoring().getOtherChipsWeight()*numberOfOtherChips;
		totalScore = totalScore + chipsWithPlayerScore;
		//logger.trace(player.guid + " after otherplayerchipsowned : " + score);
		
		int numberOfOwnChipsInAuction = 0;
		int numberOfAuctionChips = 0;
		if (!(auctionhouse == null)){
			// calculate the effect of you owning auction chips
			Iterator<Integer> auctionIter = getAuctionColours().iterator();
			while (auctionIter.hasNext()) {
				int auctionColour = auctionIter.next();
				numberOfAuctionChips = numberOfAuctionChips + player.getNumberOfChips(auctionColour);
			}
			auctionChipsWithPlayerScore = config.getScoring().getOtherChipsWeight()*numberOfAuctionChips;
			totalScore = totalScore + auctionChipsWithPlayerScore;
			//logger.trace(player.guid + " after owningauctionchips : " + score);
			
			// calculate the effect of the auction having your chips
			numberOfOwnChipsInAuction = auctionhouse.getNumberOfChips(player.colour);
			chipsWithAuctionScore = config.getScoring().getOwnChipsWeight()*numberOfOwnChipsInAuction;
			totalScore = totalScore - chipsWithAuctionScore;
			//logger.trace(player.guid + " after auctionowninghischips : " + score);
		}
		
		// Subtract the fines the player has accrued
		fines = player.getFines();
		totalScore = totalScore - fines;
		output = configNumber + "," + iterationNumber + "," + player.guid + 
					"," + goalScore + "," + distanceScore + "," + chipsWithOtherPlayersScore + 
					"," + chipsWithAuctionScore + "," + chipsWithPlayerScore +
					"," + auctionChipsWithPlayerScore + "," + fines + "," + totalScore;
		return output;
	}
	
	private void recordDetailedScores(){
		try {
			RandomAccessFile metricFile = new RandomAccessFile(
					Simulator.outputFolderName + "simDetailedScores.csv", "rw");
			int fileLength = (int)metricFile.length();
			if (fileLength == 0){
				metricFile.writeBytes("configNumber,iterationNumber,player,goalScore,distanceScore,chipsWithOtherPlayersScore,chipsWithAuctionScore,chipsWithPlayerScore,auctionChipsWithPlayerScore,fines,totalScore");
				metricFile.writeBytes("\n");
			}
			metricFile.skipBytes(fileLength);
			Iterator<Player> iter = playerMap.values().iterator();
			while (iter.hasNext()) {
				Player thisplayer = iter.next();
				metricFile.writeBytes(detailedPlayerScore(thisplayer) + ",");
				metricFile.writeBytes("\n");
			}
			metricFile.writeBytes("\n");
			metricFile.close();
		} catch (IOException e) {
			logger.fatal("Error - Unable to record detailed scores", e);
		}
	}
	
	private void recordScores(){
		
		try {
			RandomAccessFile metricFile = new RandomAccessFile(
					Simulator.outputFolderName + "simScores.csv", "rw");
			metricFile.skipBytes((int)metricFile.length());
			Iterator<Player> iter = playerMap.values().iterator();
			while (iter.hasNext()) {
				Player thisplayer = iter.next();
				metricFile.writeBytes(thisplayer.guid + "," + getScore(thisplayer) + ",");
			}
			metricFile.writeBytes("\n");
			metricFile.close();
		} catch (IOException e) {
			logger.fatal("Error - Unable to record scores", e);
		}
		
	}

	public int getAuctionInterval() {
		return config.getAuctionInterval();
	}
	
	public ArrayList<Integer> getAuctionColours() {
		if (auctionHouseColours == null)
			return new ArrayList<Integer>();
		else
			return this.auctionHouseColours;
	}	
	
	public int getGoalDistance() {
		return config.getGoalDistance();
	}
	
	public int getStartingChips() {
		return config.getStartingChips();
	}

	public int getWinningConst() {
		return config.getScoring().getWinBonus();
	}

	public double getDistWeight() {
		return config.getScoring().getDistanceWeight();
	}

	public double getOwnChipsWeight() {
		return config.getScoring().getOwnChipsWeight();
	}
	
	public double getOtherChipsWeight() {
		return config.getScoring().getOtherChipsWeight();
	}
	
	public double getArbiterFine() {
		return config.getScoring().getArbiterFine();
	}
	
	public double getMoveFine() {
		return config.getScoring().getMoveFine();
	}
	
	public String getCurrentPhase() {
		return currentPhase;
	}
	
	public int getIterationNum() {
		return (config.getIterations() - numIterationsLeft) + 1;
	}
	
	public int getNumIterations() {
		return config.getIterations();
	}
	
	public int getTurn() {
		return turn;
	}
	
	public int getTimeout() {
		return config.getTimeout();
	}
	
	public int getConfigNum() {
		return currentConfig + 1;
	}
	
	public int getNumConfigs() {
		return configs.size();
	}
	
	public String getComment() {
		return config.getComment();
	}
	
	public String getBankId() {
		return bankId;
	}
	
	public String getAuctionId() {
		return auctionId;
	}

	public String getArbiterId() {
		return arbiterId;
	}
	
	public String getBallotId() {
		return ballotId;
	}
	
	private class Player {

		// public String icon; // file path to graphic icon
		private String guid; // id
		private Coord pos;
		private CoordColour goal;
		private TreeMap<Integer,Integer> chips;
		private int colour;
		private float fines;
		
		private boolean hasMoved;
		
		public Player(String guid, Coord pos, int colour){
			this.guid = guid;
			this.pos = new Coord(pos);
			this.chips = new TreeMap<Integer,Integer>();
			this.colour = colour;
			this.fines = 0;
		}

		public void addFines(float fineAmount){
			fines += fineAmount;
		}
		
		public float getFines() {
			return fines;
		}

		public int getNumberOfChips(Integer colour) {
			return chips.get(colour).intValue();
		}
		
		public void setNumberOfChips(Integer colour, int number) {
			this.chips.put(colour, number);
		}
		
		public boolean hasMoved() {
			return hasMoved;
		}
		
		public void setMoved(boolean flag) {
			hasMoved = flag;
		}
	}

	public int getNumIterationsLeft() {
		return numIterationsLeft;
	}

	public int getCurrentPhaseRemaining() {
		return currentPhaseRemaining;
	}


}
