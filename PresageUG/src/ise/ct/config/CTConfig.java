package ise.ct.config;

import org.simpleframework.xml.*;

@Root
public class CTConfig {
	
	@Element
	private String comment;
	@Element
	private int iterations;
	@Element
	private int timeout;
	@Element
	private boardConfig board;
	@Element
	private scoringConfig scoring;
	@Element
	private int goaldistance;
	@Element
	private int startingchips;
	@Element
	private int auctioncolours;
	@Element
	private int auctioninterval;

	public CTConfig(){
		
	}
	
	public boardConfig getBoard() {
		return board;
	}

	public void setBoard(boardConfig board) {
		this.board = board;
	}

	public scoringConfig getScoring() {
		return scoring;
	}

	public void setScore(scoringConfig scoring) {
		this.scoring = scoring;
	}

	public int getGoalDistance() {
		return goaldistance;
	}

	public void setGoalDistance(int goalDistance) {
		this.goaldistance = goalDistance;
	}

	public int getStartingChips() {
		return startingchips;
	}

	public void setStartingChips(int startingChips) {
		this.startingchips = startingChips;
	}
	
	public int getAuctionColours() {
		return auctioncolours;
	}
	
	public void setAuctionColours(int auctionColours) {
		this.auctioncolours = auctionColours;
	}
	
	public int getAuctionInterval() {
		return auctioninterval;
	}
	
	public void setAuctionInterval(int auctionColours) {
		this.auctioninterval = auctionColours;
	}
	
	public static class boardConfig {
		@Element
		private int width;
		@Element
		private int height;
		
		public int getWidth() {
			return width;
		}
		public void setWidth(int width) {
			this.width = width;
		}
		public int getHeight() {
			return height;
		}
		public void setHeight(int height) {
			this.height = height;
		}

	}
	
	public static class scoringConfig {
		@Element
		private int winbonus;
		@Element
		private float distanceweight;
		@Element
		private float ownchipsweight;
		@Element
		private float otherchipsweight;
		@Element
		private float arbiterfine;
		@Element
		private float movefine;
		
		public int getWinBonus() {
			return winbonus;
		}
		public void setWinBonus(int winBonus) {
			this.winbonus = winBonus;
		}
		public float getDistanceWeight() {
			return distanceweight;
		}
		public void setDistanceWeight(float distanceWeight) {
			this.distanceweight = distanceWeight;
		}
		public float getOwnChipsWeight() {
			return ownchipsweight;
		}
		public void setOwnChipsWeight(float chipsWeight) {
			this.ownchipsweight = chipsWeight;
		}
		public float getOtherChipsWeight() {
			return otherchipsweight;
		}
		public void setOtherChipsWeight(float chipsWeight) {
			this.otherchipsweight = chipsWeight;
		}
		public float getArbiterFine() {
			return arbiterfine;
		}
		public float getMoveFine() {
			return movefine;
		}
		public void setArbiterFineWeight(float arbiterFine){
			this.arbiterfine = arbiterFine;
		}
		public void setMoveFineWeight(float moveFine) {
			this.movefine = moveFine;
		}
	}

	public String getComment() {
		return comment;
	}

	public int getIterations() {
		return iterations;
	}

	public int getTimeout() {
		return timeout;
	}
	
}