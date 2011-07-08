package ise.ct.participants;

import ise.ct.CTParticipant;
import ise.ct.CTWorld;
import ise.ct.Coord;
import ise.ct.CoordColour;
import ise.ct.actions.FineAction;
import ise.ct.actions.RemoveChipsAction;
import ise.ct.actions.TransferChipsAction;
import ise.ct.messages.ARBPunishment;
import ise.ct.messages.ARBPunishmentGiven;
import ise.ct.messages.ARBRelinquishRequest;
import ise.ct.messages.ARBRelinquishResponse;
import ise.ct.messages.ARBReport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import presage.ContactInfo;
import presage.ConvKey;
import presage.Conversation;
import presage.Message;
import presage.Simulator;

public class Arbiter extends CTParticipant {
	
	private static final int DEFAULT_TIMEOUT = 3;
	
	private CTWorld myWorld;
	
	private Map<Coord, ArrayList<String>> alreadyReported = new TreeMap<Coord, ArrayList<String>>(); 
	private Map<String, Coord> playerPositions = new TreeMap<String, Coord>();
	
	private int nextArbId = 0;
	
	private Map<Integer, Arbitration> arbitrations = new TreeMap<Integer, Arbitration>();
	private Map<ConvKey, Arbitration> convKeyToArb = new TreeMap<ConvKey, Arbitration>();
	
	Logger logger = Logger.getLogger(this.getClass().getName());

	private boolean pastInit = false;
	
	public Arbiter(String[] args) {
		super(args);
	}
	
	public void physicallyAct() {}

	public void proActiveBehaviour() {
		if (myWorld.getCurrentPhase().equals(CTWorld.INIT_PHASE)){
			setupReported();
			pastInit = true;
		}

		if (pastInit) tidyUpReported();
	}
	
	public void onActivation() {
		super.onActivation();
		myWorld = (CTWorld)Simulator.pworld;
	}

	private int getNextArbId(){
		return nextArbId++;
	}
	
	public void arbitrationreport(Message msg, Conversation conv){
		String state = conv.state;
		if (state.equalsIgnoreCase("initial")){
			goodInitialReport((ARBReport)msg, conv);
		}else if (state.equalsIgnoreCase("awaitpunishrequest")){
			goodPunishmentRequest((ARBPunishment)msg, conv);
		}
	}
	
	public void arbitrationinvestigation(Message msg, Conversation conv){
		String state = conv.state;
		if (state.equalsIgnoreCase("wait")){
			badLicenseResponse((ARBRelinquishResponse)msg, conv);
		}else if (state.equalsIgnoreCase("awaitpunishconf")) {
			badPunishReponse(msg, conv);
		}
	}
	
	public void arbitrationreport_timeout(Conversation conv){
		// The reporting party has failed to respond.
		// Presume offending party has been let off
		Arbitration arb = getArbitration(conv.myKey);
		completeArbitration(arb);
	}
	
	public void arbitrationinvestigation_timeout(Conversation conv){
		// Offender has not responded in time. BIG arbiter fine time
		Arbitration arb = getArbitration(conv.myKey);
		issueArbiterFine(arb.getBadGuy());
		completeArbitration(arb);
	}
	
	public void goodInitialReport(ARBReport msg, Conversation conv){
		ARBReport reportMessage = (ARBReport)msg;
		CoordColour reportedTile = new CoordColour(reportMessage.getTile());
		reportedTile.colour = myWorld.getWorldState(reportedTile.x, reportedTile.y);
		Conversation conv2 = new Conversation(reportMessage.getTheirId(), "arbitrationinvestigation", convKeyGen.getKey());
		conversations.put(conv2.myKey, conv2);
		
		Arbitration arb = new Arbitration(getNextArbId(), reportedTile, conv, conv2);
		addArbitration(arb);
		
		if (!alreadyReported(arb.getTile(), arb.getBadGuy())){
			if (arb.getTileColour()==myWorld.getPlayerColour(arb.getGoodGuy())){
					ARBRelinquishRequest message = new ARBRelinquishRequest(myId, arb.getInvestigateConv(), arb.tile.colour, new Coord(arb.tile.x, arb.tile.y));
					arb.getInvestigateConv().setTimeout(DEFAULT_TIMEOUT);
					arb.getReportConv().setNoTimeout();
					outbox.enqueue(message);
					arb.getReportConv().changeStateTo("wait");
					arb.getInvestigateConv().changeStateTo("wait");
					addReported(arb.getTile(), arb.getBadGuy());
			}
		}
	}

	public void badLicenseResponse(ARBRelinquishResponse msg, Conversation conv){
		Arbitration arb = getArbitration(conv.myKey);
		if (msg.getResponse()){
			if (myWorld.getPlayerChips(arb.getBadGuy(), myKey, arb.getTileColour()) < 1){
				issueArbiterFine(arb.getBadGuy());
			} else {
				removeChips(arb.getBadGuy(), arb.getTileColour(), 1);
			}
			ARBRelinquishResponse message = new ARBRelinquishResponse(myId, arb.getReportConv(), true);
			outbox.enqueue(message);
			completeArbitration(arb);
		} else {
			ARBRelinquishResponse message = new ARBRelinquishResponse(myId, arb.getReportConv(), false);
			arb.getReportConv().setTimeout(DEFAULT_TIMEOUT);
			arb.getInvestigateConv().setNoTimeout();
			outbox.enqueue(message);
			arb.getReportConv().changeStateTo("awaitpunishrequest");
			arb.getInvestigateConv().changeStateTo("wait");
		}
		
	}
	
	public void goodPunishmentRequest(ARBPunishment msg, Conversation conv){
		Arbitration arb = getArbitration(conv.myKey);
		if (!msg.getPunishment().equals(ARBPunishment.DEMAND_CHIPS)){
			if (msg.getPunishment().equals(ARBPunishment.FINE)) issueStandardFine(arb.getBadGuy());
			ARBPunishmentGiven message1 = new ARBPunishmentGiven(myId, arb.getReportConv(), msg.getPunishment());
			ARBPunishment message2 = new ARBPunishment(myId, arb.getInvestigateConv(), msg.getPunishment());
			outbox.enqueue(message1);
			outbox.enqueue(message2);
			completeArbitration(arb);
		} else {
			arb.setPunishColour(msg.getChipColour());
			arb.setPunishNumber(msg.getNumberOfChips());
			ARBPunishment message = new ARBPunishment(myId, arb.getInvestigateConv(), msg.getPunishment(), msg.getChipColour(), msg.getNumberOfChips());
			arb.getInvestigateConv().setTimeout(DEFAULT_TIMEOUT);
			arb.getReportConv().setNoTimeout();
			outbox.enqueue(message);
			arb.getReportConv().changeStateTo("wait");
			arb.getInvestigateConv().changeStateTo("awaitpunishconf");
		}
	}
	
	public void badPunishReponse(Message msg, Conversation conv){
		Arbitration arb = getArbitration(conv.myKey);
		if (msg.getPerformative().contains("ARBPay")){
			int badGuyColour = myWorld.getPlayerColour(arb.getBadGuy());
			int punishColour = arb.getPunishColour();
			int numberPunishChips = myWorld.getPlayerChips(arb.getBadGuy(), myKey, punishColour);
			if ( (badGuyColour != punishColour) && (numberPunishChips < arb.getPunishNumber()) ){
				issueArbiterFine(arb.getBadGuy());
				ARBPunishmentGiven message = new ARBPunishmentGiven(myId, arb.getReportConv(), ARBPunishment.FINE);
				outbox.enqueue(message);
			} else {
				transferChips(arb.getBadGuy(), arb.getGoodGuy(), arb.getPunishColour(), arb.getPunishNumber());
				ARBPunishmentGiven message = new ARBPunishmentGiven(myId, arb.getReportConv(), ARBPunishment.DEMAND_CHIPS, arb.getPunishColour(), arb.getPunishNumber());
				outbox.enqueue(message);	
			}
			completeArbitration(arb);
		} else if (msg.getPerformative().contains("ARBNonPayment")){
			issueStandardFine(arb.getBadGuy());
			ARBPunishmentGiven message = new ARBPunishmentGiven(myId, arb.getReportConv(), ARBPunishment.FINE);
			outbox.enqueue(message);
			completeArbitration(arb);
		}
	}
	

/**************************************************************************************
	WORLD ACTIONS
**************************************************************************************/
	private void removeChips(String playerId, int colour, int number){
		myWorld.act(new RemoveChipsAction(playerId, myKey, colour, number));
	}
	
	private void transferChips(String fromPlayerId, String toPlayerId, int colour, int number){
		myWorld.act(new TransferChipsAction(fromPlayerId, myKey, toPlayerId, colour, number));
	}
	
	private void issueStandardFine(String playerId){
		myWorld.act(new FineAction(playerId, myKey, CTWorld.MOVE_FINE));
	}
	
	private void issueArbiterFine(String playerId){
		myWorld.act(new FineAction(playerId, myKey, CTWorld.ARBITER_FINE));
	}

/**************************************************************************************
	CHECK FOR ALREADY REPORTED
**************************************************************************************/
 	private boolean alreadyReported(Coord tile, String playerId){
		ArrayList<String> reportedIPV;
		try{
			reportedIPV = (ArrayList<String>)this.alreadyReported.get(tile);
		} catch (Exception e) {
			return false;
		} 
		if (reportedIPV==null) return false;
		return reportedIPV.contains(playerId);
	}
	
	private void addReported(Coord tile, String playerId){
		ArrayList<String> reportedIPV;
		try{
			reportedIPV = (ArrayList<String>)this.alreadyReported.get(tile);
		} catch (Exception e) {
			reportedIPV = new ArrayList<String>();
		} 
		if (reportedIPV==null) reportedIPV = new ArrayList<String>();
		
		reportedIPV.add(playerId);
		alreadyReported.put(tile, reportedIPV);
	}
	
	private void removeReported(Coord tile, String playerId){
		if (alreadyReported(tile, playerId)){
			ArrayList<String> temp = alreadyReported.get(tile);
			temp.remove(playerId);
		}
	}
	
	private void tidyUpReported(){
		Iterator<ContactInfo> iter = searchContactsByRole("player").iterator();
		Coord currentPos, lastPos;
		String currentName;
		
		while (iter.hasNext()){
			currentName = iter.next().name();
			currentPos = myWorld.getPlayerPos(currentName);
			lastPos = playerPositions.get(currentName);
			if ((!currentPos.equals(lastPos))&&(alreadyReported(lastPos, currentName))){
				removeReported(lastPos, currentName);
				playerPositions.put(currentName, currentPos);
			}
		}
	}
	
	private void setupReported(){
		Iterator<ContactInfo> iter = searchContactsByRole("player").iterator();
		Coord currentPos;
		String currentName;
		
		while (iter.hasNext()){
			currentName = iter.next().name();
			currentPos = myWorld.getPlayerPos(currentName);
			playerPositions.put(currentName, currentPos);
		}
	}
	
/**************************************************************************************
	HOLD LIST OF ARBITRATIONS
**************************************************************************************/
	private void addArbitration(Arbitration arb) {
		arbitrations.put(arb.arbId, arb);
		convKeyToArb.put(arb.getInvestigateConv().myKey, arb);
		convKeyToArb.put(arb.getReportConv().myKey, arb);
	}
	
	private void completeArbitration(Arbitration arb) {
		arb.getInvestigateConv().changeStateTo("end");
		arb.getReportConv().changeStateTo("end");
		arbitrations.remove(arb.arbId);
	}
	
	private Arbitration getArbitration(ConvKey convKey){
		return convKeyToArb.get(convKey);
	}
	
	private class Arbitration {
		private int arbId;
		private CoordColour tile;
		private Conversation reportConv, investigateConv;
		
		private int punishColour;
		private int punishNumber;
		
		Arbitration(int arbId, CoordColour tile, Conversation reportConv, Conversation investigateConv){
			this.arbId = arbId;
			this.tile = tile;
			this.reportConv = reportConv;
			this.investigateConv = investigateConv;
		}
		
		public String getGoodGuy() { return reportConv.theirId; }
		public String getBadGuy() { return investigateConv.theirId; }
		public int getTileColour() { return tile.colour; }
		public Coord getTile() { return new Coord(tile.x, tile.y); }
		public Conversation getReportConv() { return reportConv; }
		public Conversation getInvestigateConv() { return investigateConv; }
		
		public void setPunishColour(int colour) { punishColour = colour; }
		public void setPunishNumber(int number) { punishNumber = number; }
		public int getPunishColour() { return punishColour; }
		public int getPunishNumber() { return punishNumber; }
		
	}
	
}
