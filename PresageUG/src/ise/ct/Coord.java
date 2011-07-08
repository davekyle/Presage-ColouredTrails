package ise.ct;

import org.apache.log4j.Logger;

public class Coord implements Comparable {
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	public int x;
	public int y;
	
	public Coord(){
		this.x = 0;
		this.y = 0;
	}
	
	public Coord(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public Coord(Coord coord){
		this.x = coord.x;
		this.y = coord.y;
	}
	
	public void set(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public boolean equals(Object o){
		if ( o.getClass().equals(this.getClass()) )
			return equals((Coord)o);
		else
			return false;
	}
	
	public boolean equals(Coord b){
		try{
			return ((this.x == b.x) && (this.y == b.y));
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	public int compareTo(Object o) {
		Coord other = (Coord)o;
		if (other.x < this.x) {
			if (other.y < this.y) {
				return -1;
			} else if (other.y == this.y) {
				return 0;
			} else {
				return 1;
			}
		} else {
			return -1;
		}
	}
	
	public static int distance(Coord a, Coord b){
        int yDist = Math.abs(a.y - b.y);
        int xDist = Math.abs(a.x - b.x) - yDist;
        return ((xDist <= 0) ? 0 : (xDist/2)) + yDist;
	}
	
	public int hashCode() {
		int hash = this.x;
		hash += this.y * 10000;
		return hash;
	}
	
}
