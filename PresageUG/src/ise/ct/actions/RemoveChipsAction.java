package ise.ct.actions;

import presage.Action;

public class RemoveChipsAction extends Action {
	
	public RemoveChipsAction(String playerID, String arbiterKey, Integer chipColour, Integer numChips)
	{
		super("removeChips", new Object[] { playerID, arbiterKey, chipColour, numChips });
	}
	
	public String getPlayerID() {
		return (String)this.getVariables()[0];
	}
	
	public String getArbiterKey() {
		return (String)this.getVariables()[1];
	}
	
	public Integer getChipColour() {
		return (Integer)this.getVariables()[2];
	}
	
	public Integer getNumChips() {
		return (Integer)this.getVariables()[3];
	}
}
