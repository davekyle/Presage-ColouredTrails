package ise.ct.actions;

import ise.ct.Coord;
import presage.Action;

public class MakeNeutralAction extends Action {
	
	public MakeNeutralAction(String playerID, String playerKey, Coord tileCoord)
	{
		super("makeNeutral", new Object[] { playerID, playerKey, tileCoord });
	}
	
	public String getPlayerID() {
		return (String)this.getVariables()[0];
	}
	
	public String getPlayerKey() {
		return (String)this.getVariables()[1];
	}
	
	public Coord getTileCoord() {
		return (Coord)this.getVariables()[2];		
	}
}
