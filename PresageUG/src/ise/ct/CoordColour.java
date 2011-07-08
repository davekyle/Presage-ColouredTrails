package ise.ct;


public class CoordColour extends Coord {
	public int colour;
	
	public CoordColour(){
		super();
		this.colour = CTWorld.INVALID;
	}
	
	public CoordColour(int x, int y){
		super(x,y);
		this.colour = CTWorld.INVALID;
	}
	
	public CoordColour(int x, int y, int colour) {
		this.x = x;
		this.y = y;
		this.colour = colour;
	}
	
	public CoordColour(Coord coord, int colour) {
		this.x = coord.x;
		this.y = coord.y;
		this.colour = colour;
	}
	
	public CoordColour(CoordColour coord) {
		this.x = coord.x;
		this.y = coord.y;
		this.colour = coord.colour;
	}

	public CoordColour(Coord coord) {
		this.x = coord.x;
		this.y = coord.y;
		this.colour = CTWorld.INVALID;
	}

	public void set(int x, int y, int colour) {
		this.x = x;
		this.y = y;
		this.colour = colour;
	}
	
}
