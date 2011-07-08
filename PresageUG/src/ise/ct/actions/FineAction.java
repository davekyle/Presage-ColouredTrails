package ise.ct.actions;

import presage.Action;

public class FineAction extends Action {
	
	public FineAction(String playerID, String arbiterKey, int fineType)
	{
		super("finePlayer", new Object[] { playerID, arbiterKey, fineType });
	}
	
	public String getPlayerID() {
		return (String)this.getVariables()[0];
	}
	
	public String getArbiterKey() {
		return (String)this.getVariables()[1];
	}
	
	public Integer getFineType() {
		return (Integer)this.getVariables()[2];
	}
}
