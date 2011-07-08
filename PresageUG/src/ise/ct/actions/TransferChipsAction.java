package ise.ct.actions;

import presage.Action;

public class TransferChipsAction extends Action {
	
	public TransferChipsAction(String playerID, String playerKey, String destID, Integer chipColour, Integer numChips)
	{
		super("transferChips", new Object[] { playerID, playerKey, destID, chipColour, numChips });
	}
	
	public String getPlayerID() {
		return (String)this.getVariables()[0];
	}
	
	public String getPlayerKey() {
		return (String)this.getVariables()[1];
	}
	
	public String getDestID() {
		return (String)this.getVariables()[2];
	}
	
	public Integer getChipColour() {
		return (Integer)this.getVariables()[3];
	}
	
	public Integer getNumChips() {
		return (Integer)this.getVariables()[4];		
	}
}
