package ise.ct.plugins;

import ise.ct.CTWorld;
import ise.ct.Coord;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.apache.log4j.Logger;

import presage.Plugin;
import presage.Simulator;
import presage.util.StringParseTools;


public class HexagonViewer extends JFrame implements Plugin, ComponentListener  {
	
	private static final long serialVersionUID = 1L;
	
	Image offscreenImage;
	public Graphics offscr;
	Container content;
	
	private static int initialWindowWidth = 500;
	private static int initialWindowHeight = 500;
	private static int minimumPadding = 10;
	
	private int paddingX;
	private int paddingY;
	private int tilesX;
	private int tilesY;
	private int framesizeX;
	private int framesizeY;
	
	private int tilesize;
	
	private boolean init = true;
	private boolean resize = false;
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	// Path Storage
	private HashMap<String, ArrayList<Coord>> agentPaths = new HashMap<String, ArrayList<Coord>>();
	private HashMap<String, Image> agentIcons = new HashMap<String, Image>();
	
	private void addAgentPath(String agent, Coord path)
	{
		
		ArrayList<Coord> agentPath;
		// If we've never seen this agent before, add it to the HashMap
		if (!agentPaths.containsKey(agent)) {
			agentPath = new ArrayList<Coord>();
			agentPaths.put(agent, agentPath);
		} else {
			agentPath = agentPaths.get(agent);
		}
		
		// Is this path different to the last?
		if (!(agentPath.size() > 0 && agentPath.get(agentPath.size()-1).equals(path))) {
			agentPath.add(new Coord(path)); // thank you dgb04 - path is an object. path is an object. path is an object.
			agentPaths.put(agent, agentPath);
		}
		return;
	}
	
	private class PolylinePath
	{
		// Used for passing list of pixel co-ordinates to drawPolyline(int[], int[], int)
		int[] x;
		int[] y;
		
		PolylinePath(int[] x, int[] y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private boolean agentPathExists(String agent)
	{
		if (agentPaths.containsKey(agent)) {
			return !agentPaths.get(agent).isEmpty();
		} else {
			return false;
		}
		
	}
	
	private PolylinePath getAgentPolyline(String agent)
	{
		Coord tempCoord;
		PixelCoord tempPixelCoord;
		int i = 0;
		ArrayList<Coord> agentPath = agentPaths.get(agent);
		Iterator<Coord> iterator = agentPath.iterator();
		int[] x = new int[agentPath.size()];
		int[] y = new int[agentPath.size()];
		
		while (iterator.hasNext()) {
			tempCoord = iterator.next();
			tempPixelCoord = coord2pixelcoord(tempCoord); // Get top left of hexagon bounding box
			tempPixelCoord = sum(tempPixelCoord, tilesize/2); // Get middle of tile
			
			x[i] = tempPixelCoord.x;
			y[i] = tempPixelCoord.y;
			i++;
		}
		
		return new PolylinePath(x,y);
	}
	
	private void calculateViewingParameters(int frameX, int frameY, int tilesX, int tilesY, int minPadding)
	{
		int tilesizeX, tilesizeY;
		
		// calculate the maximum tilesizes in both co-ordinates
		tilesizeX = (int)((double)(frameX - 2*minPadding) / (double)((tilesX + 1.0) / 2.0));
		tilesizeY = (int)((double)(frameY - 2*minPadding) / (double)(tilesY - ((double)(tilesY - 1) / 3.0)));
		
		// use the smallest tilesize
		if (tilesizeX > tilesizeY) {
			tilesize = tilesizeY;
			paddingY = minPadding;
			paddingX = ( frameX - (tilesize*((tilesX+1)/2)) )/2;
		} else {
			tilesize = tilesizeX;
			paddingX = minPadding;
			paddingY = ( frameY - (tilesize*(tilesY - ((tilesY-1)/3))) )/2;
		}
		return;
	}
	
	private class PixelCoord {
		int x = 0;
		int y = 0;
		
		PixelCoord (int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private PixelCoord coord2pixelcoord(Coord tile)
	{
		int offsetX = paddingX;
		int offsetY = content.getHeight() - paddingY;
		
		double coordX = offsetX + (  ( (double)tile.x / 2.0 )*tilesize  );
		double coordY = offsetY - (  ( (tile.y + 1) - ((double)tile.y/3.0) )*tilesize );
		return new PixelCoord((int)coordX, (int)coordY);
	}
	
	private PixelCoord intint2pixelcoord(int tileX, int tileY)
	{
		return coord2pixelcoord(new Coord(tileX, tileY));
	}
	
	private PixelCoord sum(PixelCoord coord, int num)
	{
		// No operator overloading? Java you fail me.
		return new PixelCoord(coord.x + num, coord.y + num);
	}
	
	private void setupWindow(int width, int height) {
		framesizeX = width;
		framesizeY = height;
		
		tilesX = ((CTWorld)Simulator.pworld).getWorldSizeX();
		tilesY = ((CTWorld)Simulator.pworld).getWorldSizeY();
		
		calculateViewingParameters(framesizeX, framesizeY, tilesX, tilesY, minimumPadding);
		
		return;
	}
	
	public HexagonViewer(String[] args) {
		super("Hexagon Viewer Plugin");
		logger.info("First initialisation...");
		
		super.rootPane.addComponentListener(this); //componentEvent hook
		
		content = getContentPane();
		
		// Setup Window
		setLocation(0, 0);
		setSize(initialWindowWidth, initialWindowHeight + 1); //TODO: The +1 fixes things. Somehow.
		setResizable(true);
		setVisible(true);
		
		setupWindow(initialWindowWidth, initialWindowHeight);
	}
	
	private Polygon drawHexagon(PixelCoord topleft, int tilesize)
	{
		int[] xCoords = new int[6];
		int[] yCoords = new int[6];
		
		final int tilesize2 = (int)((double)tilesize / 2.0);
		final int tilesize3 = (int)((double)tilesize / 3.0);
		final int tilesize23 = (int)(2.0*((double)tilesize / 3.0));
		
		Polygon hexagon;
		
		xCoords[0] = topleft.x + tilesize2;
		yCoords[0] = topleft.y;
		
		xCoords[1] = topleft.x + tilesize;
		yCoords[1] = topleft.y + tilesize3;
		
		xCoords[2] = topleft.x + tilesize;
		yCoords[2] = topleft.y + tilesize23;
		
		xCoords[3] = topleft.x + tilesize2;
		yCoords[3] = topleft.y + tilesize;
		
		xCoords[4] = topleft.x;
		yCoords[4] = topleft.y + tilesize23;
		
		xCoords[5] = topleft.x;
		yCoords[5] = topleft.y + tilesize3;
		
		hexagon = new Polygon(xCoords, yCoords, 6);
		
		return hexagon;
	}
	
	public void init(int width, int height) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		setSize(width, height);
		if (offscreenImage == null) {
			offscreenImage = createImage(toolkit.getScreenSize().width, toolkit.getScreenSize().height);
		}
		offscr = offscreenImage.getGraphics();
		init = false;
	}

	public void paint(Graphics g) {
		execute();
	}
	
	private void drawHexagons() {
		PixelCoord tempPixelCoord;
		
		// Draw the hexagons
		for (int y = tilesY-1; y >= 0; y--){
			if (y % 2 == 0) {
				// even row
				for (int x = 0; x <= tilesX-2; x = x + 2) {
					tempPixelCoord = intint2pixelcoord(x, y);
					offscr.setColor(getColor(((CTWorld)Simulator.pworld).getWorldState(x,y)));
					offscr.fillPolygon(drawHexagon(tempPixelCoord,tilesize));
					offscr.setColor(getColor(((CTWorld)Simulator.pworld).getWorldState(x,y)).darker());
					offscr.drawPolygon(drawHexagon(tempPixelCoord,tilesize));
					//logger.trace("Drew a " + getColorName(((CTWorld)Simulator.pworld).getWorldState(x,y)) + " tile");
				}
			} else {
				// odd row
				for (int x = 1; x <= tilesX-1; x = x + 2) {
					tempPixelCoord = intint2pixelcoord(x, y);
					offscr.setColor(getColor(((CTWorld)Simulator.pworld).getWorldState(x,y)));
					offscr.fillPolygon(drawHexagon(tempPixelCoord,tilesize));
					offscr.setColor(getColor(((CTWorld)Simulator.pworld).getWorldState(x,y)).darker());
					offscr.drawPolygon(drawHexagon(tempPixelCoord,tilesize));
					//logger.trace("Drew a " + getColorName(((CTWorld)Simulator.pworld).getWorldState(x,y)) + " tile");
				}
			}
		}
		return;
	}
	
	private void drawGoals() {
		Coord tempCoord;
		PixelCoord tempPixelCoord;
		Color tempColor;
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();
		
		// Indicate Goal Tiles
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			
			tempCoord = ((CTWorld)Simulator.pworld).getPlayerGoal(name);
			
			if (tempCoord != null) {
				tempPixelCoord = coord2pixelcoord(tempCoord);
				tempColor = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));
				
				offscr.setColor(tempColor);
				offscr.fillOval(tempPixelCoord.x + 3*tilesize/8, tempPixelCoord.y + 3*tilesize/8, tilesize/4, tilesize/4);
				offscr.setColor(Color.BLACK);
				offscr.drawOval(tempPixelCoord.x + 3*tilesize/8, tempPixelCoord.y + 3*tilesize/8, tilesize/4, tilesize/4);
			}
		}
		return;
	}
	
	private void drawPaths() {
		Color tempColor;
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();
		
		// Draw paths
		PolylinePath tempPolylinePath;
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			if (agentPathExists(name)) {
				tempPolylinePath = getAgentPolyline(name);
				tempColor = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));
				
				offscr.setColor(tempColor.darker());
				offscr.drawPolyline(tempPolylinePath.x, tempPolylinePath.y, tempPolylinePath.x.length);
			}
		}
		return;
	}
	
	private void getAgentIcons() {	
		try {
			RandomAccessFile participantsFile =
				new RandomAccessFile(Simulator.inputFoldersPath + Simulator.inputFolder + "/participants.csv", "r");
			participantsFile.readLine();

			String currentLine = participantsFile.readLine();

			// Set the System Data
			while (!(currentLine == null)) {
				String[] theArgs = StringParseTools.readTokens(currentLine, ",");

				if (theArgs[2].contains("<player>")) {
					try {
						agentIcons.put( theArgs[1], ImageIO.read(new File("images/" + theArgs[3])) );
					} catch (Exception e) {
						logger.warn("Couldn't find the icon file 'images/" + theArgs[3] + "' for agent '" + theArgs[1] + "'.");
					}
				}

				// get next line
				currentLine = participantsFile.readLine();
			}
			participantsFile.close();
		} catch (Exception e) {
			logger.fatal("Error access System I/O file: ", e);
		}
	}
	
	private void drawAgents() {
		Coord tempCoord;
		PixelCoord tempPixelCoord;
		Color tempColor;
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();
		
		// Display Agents
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			
			tempCoord = ((CTWorld)Simulator.pworld).getPlayerPos(name);
			tempPixelCoord = coord2pixelcoord(tempCoord);
			tempColor = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));

			offscr.setColor(Color.BLACK);
			offscr.fillRect(tempPixelCoord.x + tilesize/4 - 1, tempPixelCoord.y + tilesize/4 - 1, tilesize/2 + 2, tilesize/2 + 2);
			if (agentIcons.containsKey(name)) {
				offscr.drawImage(agentIcons.get(name), tempPixelCoord.x + tilesize/4, tempPixelCoord.y + tilesize/4, tilesize/2, tilesize/2, tempColor, null);
			} else {
				// Blank icon
				offscr.setColor(tempColor);
				offscr.fillRect(tempPixelCoord.x + tilesize/4, tempPixelCoord.y + tilesize/4, tilesize/2, tilesize/2);
			}
		}
		return;
	}
	
	private void addAgentPaths() {
		Coord tempCoord;
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();
		
		// Draw paths
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			tempCoord = ((CTWorld)Simulator.pworld).getPlayerPos(name);
			addAgentPath(name, tempCoord);
		}
		return;
	}
	
	private void clearBuffer()
	{
		offscr.setColor(Color.white);
		offscr.fillRect(0, 0, content.getWidth(), content.getHeight());
		
		return;
	}
	
	public synchronized void execute() {
		if (init) init(initialWindowWidth, initialWindowHeight);
		
		displayAgentColours();
		displayAuctionColours();
		
		// Draw the viewer components in background-foreground order
		// updating certain elements for different phases

		if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.REG_PHASE) ) {
			// Registration phase
			clearBuffer();
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.INIT_PHASE) ) {
			// Initiation phase
			getAgentIcons();
			addAgentPaths();
			clearBuffer();
			drawHexagons();
			drawGoals();
			drawAgents();
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.COMM_PHASE) ) {
			// Communication phase
			if (resize) {
				// If a resize event has happened, we need to rerender, but only once!
				resize = false;
				clearBuffer();
				drawHexagons();
				drawPaths();
				drawGoals();
				drawAgents();
			}
			
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.MOVE_PHASE) ) {
			// Moving phase
			addAgentPaths();
			clearBuffer();
			drawHexagons();
			drawPaths();
			drawGoals();
			drawAgents();
		} /*else {
			// Unknown phase
		}*/
			
		// Show buffer to screen
		content.getGraphics().drawImage(offscreenImage, 0, 0, this);

		return;
	}
	
	private Color getColor(int posValue){
		Color result = Color.white;
		int mul = 0;
		int mod = 0;
		int getColorBaseNum = 9;
		
		// black is invalid colour
		if (posValue<0) result = Color.black;
		// white is neutral colour
		else if (posValue == 0) result = Color.white;
		else{
			if (posValue > getColorBaseNum){
				mul = posValue/getColorBaseNum;
				mod = (posValue%getColorBaseNum)+1;
			}
			else {
				mod = posValue;
			}
				
			switch (mod) {
				//case 0: result = Color.white; break; 
				case 1: result = Color.yellow; break;
				case 2: result = Color.blue; break;
				case 3: result = Color.green; break;
				case 4: result = Color.orange; break;
				case 5: result = Color.red; break;
				case 6: result = Color.magenta; break;
				case 7: result = Color.gray; break;
				case 8: result = Color.cyan; break;
				case 9: result = Color.pink; break;
				//default: result = Color.black; break;
			}
			for (int i = 0;i<=mul-1;i++){
				result = result.darker();
			}
		
		}
		return result;
	}
	
	private String getColorName(int posValue)
	{
		String result = "";
		int mul = 0;
		int mod = 0;
		int getColorBaseNum = 9;
		
		// black is invalid colour
		if (posValue<0) result = "INVALID";
		// white is neutral colour
		else if (posValue == 0) result = "white";
		else{
			if (posValue > getColorBaseNum){
				mul = posValue/getColorBaseNum;
				mod = (posValue%getColorBaseNum)+1;
			}
			else {
				mod = posValue;
			}
				
			switch (mod) {
				//case 0: result = "white"; break;
				case 1: result = "yellow"; break;
				case 2: result = "blue"; break;
				case 3: result = "green"; break;
				case 4: result = "orange"; break;
				case 5: result = "red"; break;
				case 6: result = "magenta"; break;
				case 7: result = "gray"; break;
				case 8: result = "cyan"; break;
				case 9: result = "pink"; break;
				default: result = "ERROR : posvalue: " + posValue + 
									" // mod: " + mod + 
									" // mul: " + mul; break;
			}
			for (int i = 0;i<=mul-1;i++){
				result = "darker " + result;
			}
		
		}
		return result;
	}
	
	public void displayAgentColours()
	{
		// Output agent colours to console
		if (((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.INIT_PHASE) ) {
			Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
			Iterator<String> iterator = playerNames.iterator();
			
			while (iterator.hasNext()) {
				String name = (String)iterator.next();
				logger.trace(name + " owns the " + getColorName(((CTWorld)Simulator.pworld).getPlayerColour(name)) + " tiles.");
			}
		}
		return;
	}
	
	public void displayAuctionColours(){
		// Output auctionhouse colours to console
		if (((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.INIT_PHASE) ) {
			ArrayList<Integer> auctionColours = ((CTWorld)Simulator.pworld).getAuctionColours();
			if ((auctionColours != null) && !auctionColours.isEmpty()) {
				Iterator<Integer> iterator = auctionColours.iterator();
				
				while (iterator.hasNext()) {
					Integer colour = iterator.next();
					logger.trace("The AuctionHouse owns the " + getColorName(colour) + " tiles.");
				}
			}
		}
		return;
	}
	
	public String returnLabel() {
		String label = this.getClass().getName() + " <null>";
		return label;
	}

	public void onDelete() {
		this.setVisible(false);
		this.dispose();
	}

	public void println(String s) {
		logger.debug("HexagonViewer println: " + this.getClass().getName() + ": " + s);
	}

	public void vprintln(String s) {
		logger.debug("HexagonViewer println: " + this.getClass().getName() + ": " + s);
	}

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentResized(ComponentEvent arg0) {
		setupWindow(content.getWidth(), content.getHeight());
		resize = true;
	}

	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}