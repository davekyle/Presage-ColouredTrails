package ise.ct.plugins;

import ise.ct.CTWorld;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import presage.Plugin;
import presage.Simulator;

public class CTStats implements Plugin {

	Logger logger = Logger.getLogger(this.getClass().getName());
	
	CTWorld operatingWorld;
	
	String myName = "CTStats_PLUGIN";
	String myKey;
	
	String lastPhase = "";
	String currentPhase = "";
	
	int numberPlayers;
	int numberColours;
	
	String filename;
	RandomAccessFile chipFile;
	
	int lastConfig = -1;
	int config, iteration, turn;
	
	HashMap<Integer, String> agentColours;
	
	public CTStats(String[] args) {
		operatingWorld = ((CTWorld)Simulator.pworld);
		myKey = Simulator.generateKey(myName);
	}
	
	private void writeFileHeader() {
		closeFile();
		try {
			chipFile = new RandomAccessFile(
					Simulator.outputFolderName + "chipHistory_config" + config + ".csv", "rw");
			chipFile.setLength(0);
			chipFile.writeBytes("iteration,turnNumber,player");
			for (int i=0; i<numberColours; i++){
				chipFile.writeBytes(",");
				//chipFile.writeBytes("colour");
				//chipFile.writeBytes(((Integer)(i+1)).toString());
				String agent = getAgentFromColour((Integer)(i+1));
				chipFile.writeBytes(agent);
			}
			chipFile.writeBytes("\n");
		} catch (IOException e) {
			logger.fatal("Error - Unable to record chips", e);
		}
	}
	
	
	private String getAgentFromColour(Integer colour){
		String result = "";
		operatingWorld = ((CTWorld)Simulator.pworld);
		String auctionId = operatingWorld.getAuctionId();
		ArrayList<Integer> auctionColours = operatingWorld.getAuctionColours();
		
		if (auctionColours.contains(colour)){
			result = auctionId + colour.toString();
		} else {
			result = agentColours.get(colour);
		}
		
		return result;
	}
	
	private HashMap<Integer, String> populateAgentColours()
	{
		HashMap<Integer, String> agentColours = new HashMap<Integer, String>();
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();

		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			int colour = operatingWorld.getPlayerColour(name);
			agentColours.put(new Integer(colour), new String(name));
		}
		
		return agentColours;
	}
	
	private void closeFile() {
		try {
			chipFile.close();
		} catch (Exception e) {
			// nothing
		}
	}
	
	private void init() {
		numberPlayers = operatingWorld.getNumberOfPlayers();
		numberColours = numberPlayers + operatingWorld.getAuctionColours().size();
		lastConfig = config;
		config = operatingWorld.getConfigNum();
		agentColours = populateAgentColours();
	}
	
	private void writeNewline() {
		try {
			chipFile.writeBytes("\n");
		} catch (Exception e) {
			logger.fatal("Error - Unable to record chips", e);
		}
	}
	
	private void updateChips() {
		Set<String> names = operatingWorld.getPlayerNames();
		Iterator<String> name = names.iterator();
		String currentName;
		int i;
		try {
		while (name.hasNext()) {
			currentName = name.next();
			chipFile.writeBytes(iteration + "," + turn + "," + currentName);
			for (i=0; i<numberColours; i++){
				chipFile.writeBytes(",");
				chipFile.writeBytes(((Integer)operatingWorld.getPlayerChips(currentName, myKey, i+1)).toString());
			}
			chipFile.writeBytes("\n");
		}
		} catch (Exception e) {
			logger.fatal("Error - Unable to record chips", e);
		}
	}
	
	public void execute() {
		lastPhase = currentPhase;
		currentPhase = operatingWorld.getCurrentPhase();
		
		iteration = operatingWorld.getIterationNum();
		turn = operatingWorld.getTurn();
		
		if (lastPhase.equals(CTWorld.INIT_PHASE)) {
			init();
			if (config != lastConfig) writeFileHeader();
			writeNewline();
		}
		if (lastPhase.equals(CTWorld.MOVE_PHASE)) {
			updateChips();
		}
	}

	public void onDelete() {
		closeFile();
	}

	public String returnLabel() {
		return null;
	}

}
