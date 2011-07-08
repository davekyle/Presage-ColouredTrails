package ise.ct.actions;

import presage.Action;

public class CreateChipsAction extends Action {
	
	public CreateChipsAction(String playerID, String playerKey, Integer numChips)
	{
		super("createChips", new Object[] { playerID, playerKey, numChips });
	}
	
	public String getPlayerID() {
		return (String)this.getVariables()[0];
	}
	
	public String getPlayerKey() {
		return (String)this.getVariables()[1];
	}
	
	public Integer getNumChips() {
		return (Integer)this.getVariables()[2];		
	}
}
