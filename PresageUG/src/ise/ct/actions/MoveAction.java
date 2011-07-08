package ise.ct.actions;

import presage.Action;

public class MoveAction extends Action {
	
	public MoveAction(String playerID, String playerKey, Integer direction)
	{
		super("move", new Object[] { playerID, playerKey, direction });
	}
	
	public String getPlayerID() {
		return (String)this.getVariables()[0];
	}
	
	public String getPlayerKey() {
		return (String)this.getVariables()[1];
	}
	
	public Integer getDirection() {
		return (Integer)this.getVariables()[2];
	}
}
