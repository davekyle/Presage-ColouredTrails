package ise.ct.plugins;

import ise.ct.CTWorld;
import ise.ct.Coord;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Logger;

import presage.Plugin;
import presage.Simulator;
import presage.util.StringParseTools;

import com.jeta.forms.components.panel.FormPanel;

public class HexagonViewerAdvance extends JFrame implements Plugin, ComponentListener, ItemListener, ActionListener
{
	
	// form primitives
	private Image backBufferImage;
	private Graphics backBuffer;
	private Component worldMap;
	
	// form element references
	private Component statusTitle;
	private JLabel fileTitle;
	private JProgressBar fileProgress;
	private JLabel iterationTitle;
	private JProgressBar iterationProgress;
	private JLabel cycleTitle;
	private JProgressBar cycleProgress;
	
	private Component currentTitle;
	private JLabel commentTitle;
	private JLabel commentLabel;
	private JLabel phaseTitle;
	private JLabel phaseLabel;
	private JLabel turnTitle;
	private JProgressBar turnProgress;
	
	private JLabel inspectorTitle;
	private JCheckBox showPaths;
	private AbstractButton playButton;
	private AbstractButton stepButton;
	private AbstractButton endButton;
	private Component seperator1;
	private Component seperator2;
	private Component seperator3;
	private Component seperator4;
	
	// inspector tree
	private JTree inspectorTree;
	private Component inspectorScrollPane;
	private DefaultTreeModel inspectorTreeModel;
	
	// item interactions
	private boolean doShowPaths;

	// plotting parameters
	private int paddingX;
	private int paddingY;
	private int tilesX;
	private int tilesY;
	private int tilesize;
	
	// image paths
	private static String IMAGE_PATH = "images/";
	private static String HEXAGON = "hexagon.png";
	private static String GOAL = "goal.png";
	private static String SPIN = "spin.gif";
	
	private static String AUCTION_NAME = "Auction House";

	private static int minimumPadding = 10;
	private static int iconSize = 16;

	private boolean initialise = true;
	private boolean resize = false;
	private boolean treeinit = false;

	// path storage
	private HashMap<String, ArrayList<Coord>> agentPaths = new HashMap<String, ArrayList<Coord>>();
	private HashMap<String, BufferedImage> agentIcons = new HashMap<String, BufferedImage>();
	
	// image buffers
	private HashMap<String, BufferedImage> scaledImagesBuffer = new HashMap<String, BufferedImage>();
	private int scaledSize = 0;
	
	// debug fps display
	DecimalFormat dp = new DecimalFormat("#.00");

	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getLogger(this.getClass().getName());

	protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
	
	public HexagonViewerAdvance(String[] args)
	{
		FormPanel panel = new FormPanel("ise/ct/plugins/HexagonViewerForm.jfrm");
		super.add(panel);

		// add references to configurable components on form
		worldMap = panel.getComponentByName("HexagonViewerImage");
		
		statusTitle = panel.getComponentByName("StatusTitle");
		fileTitle = panel.getLabel("FileTitle");
		fileProgress = panel.getProgressBar("FileProgress");
		iterationTitle = panel.getLabel("IterationTitle");
		iterationProgress = panel.getProgressBar("IterationProgress");
		cycleTitle = panel.getLabel("CycleTitle");
		cycleProgress = panel.getProgressBar("CycleProgress");
		
		currentTitle = panel.getComponentByName("CurrentTitle");
		commentTitle = panel.getLabel("CommentTitle");
		commentLabel = panel.getLabel("CommentLabel");
		phaseTitle = panel.getLabel("PhaseTitle");
		phaseLabel = panel.getLabel("PhaseLabel");
		turnTitle = panel.getLabel("TurnTitle");
		turnProgress = panel.getProgressBar("TurnProgress");
		
		showPaths = panel.getCheckBox("ShowPaths");
		showPaths.setActionCommand("showpaths");
		showPaths.setSelected(true);
		doShowPaths = true;
		showPaths.addItemListener(this);

		playButton = panel.getButton("PlayButton");
		stepButton = panel.getButton("StepButton");
		endButton = panel.getButton("EndButton");
		playButton.setActionCommand("play");
		stepButton.setActionCommand("step");
		endButton.setActionCommand("end");
		playButton.addItemListener(this);
		stepButton.addItemListener(this);
		endButton.addItemListener(this);
		playButton.setEnabled(true);
		stepButton.setEnabled(true);
		endButton.setEnabled(true);

		
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JButton s = (JButton) ae.getSource();
				if (s.getText().equalsIgnoreCase("Pause")) {
					s.setText("Play");
					Simulator.controlPanel.paused = true;
					Simulator.controlPanel.step = false;
				} else {
					s.setText("Pause");
					Simulator.controlPanel.paused = false;
					Simulator.controlPanel.step = false;
				}
			}
		});
		
		stepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				playButton.setText("Play");
				Simulator.controlPanel.paused = true;
				Simulator.controlPanel.step = true;
			}
		});
		
		endButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Simulator.experimentLength = Simulator.cycle;
				Simulator.controlPanel.paused = false;
			}
		});
		
		cycleTitle = panel.getLabel("CycleTitle");
		phaseTitle = panel.getLabel("PhaseTitle");
		inspectorTitle = panel.getLabel("InspectorTitle");
		cycleProgress = panel.getProgressBar("CycleProgress");
		
		seperator1 = panel.getComponentByName("Seperator1");
		seperator2 = panel.getComponentByName("Seperator2");
		seperator3 = panel.getComponentByName("Seperator3");
		seperator4 = panel.getComponentByName("Seperator4");
		
		// inspector JScrollPane and JTree
		inspectorScrollPane = panel.getComponentByName("InspectorScrollPane");
		inspectorTree = new JTree();
		inspectorTree.setRowHeight(iconSize);
		inspectorTree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Icon getLeafIcon()
			{
				Icon icon;
				if (this.getText().equals(AUCTION_NAME)) {
					icon = new ImageIcon(createAuctionIcon(iconSize, iconSize));
				} else {
					icon = new ImageIcon(createAgentIcon(this.getText(), iconSize, iconSize));
				}
				return icon;
			}
		});
		
		getRootPane().setDefaultButton((JButton) playButton);
		super.addComponentListener(this); // resize events etc.

		// set the parameters this frame
		setSize(600, 500);
		setLocation(200, 100);
		setTitle("Hexagon Viewer Advance");
		setVisible(true);
	}
	
	private Image createAuctionIcon(int width, int height)
	{
		Image buffer = createImage(width, height);
		Graphics canvas = buffer.getGraphics();
		
		ArrayList<Integer> auctionColours = ((CTWorld)Simulator.pworld).getAuctionColours();
		
		if ((auctionColours != null) && !auctionColours.isEmpty()) {
			int blockWidth = width / auctionColours.size();
			int offset = 0;
			Iterator<Integer> iterator = auctionColours.iterator();
			
			while (iterator.hasNext()) {
				Integer auctionColour = iterator.next();
				Color color = getColor(auctionColour);
				canvas.setColor(color);
				canvas.fillRect(offset, 0, blockWidth, height);
				offset = offset + blockWidth;
			}
		} else {
			canvas.setColor(getColor(CTWorld.NEUTRAL));
			canvas.fillRect(0, 0, width, height);
		}
		
		return buffer;
	}

	private Image createAgentIcon(String name, int width, int height)
	{
		Color color = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));
		Image buffer = createImage(width, height);
		Graphics canvas = buffer.getGraphics();
		
		//canvas.drawImage(agentIcons.get(name), 0, 0, color, null);
		canvas.setColor(color);
		canvas.fillRect(0, 0, width, height);
		
		return buffer;
	}
	
	private void expandAll(JTree tree)
	{
		int row = 0;
		while (row < tree.getRowCount()) {
			tree.expandRow(row);
			row++;
		}
	}
	
	private void updateInspectorTree()
	{
		if (treeinit) return;
		
		//inspectorTree = new JTree();
		
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Coloured Trails World");
		
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();

		// Add the agents to the tree
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			rootNode.add(new DefaultMutableTreeNode(name));
		}
		
		// Add the auction house
		//TODO: Perform existence check for auction house
		rootNode.add(new DefaultMutableTreeNode(AUCTION_NAME));
		
		inspectorTreeModel = new DefaultTreeModel(rootNode);
		inspectorTree.setModel(inspectorTreeModel);
		((JScrollPane)inspectorScrollPane).setViewportView(inspectorTree);
		inspectorTree.expandPath(inspectorTree.getPathForRow(0));
		expandAll(inspectorTree);
		treeinit = true;
		return;
	}
	
	private void updateImageBuffer()
	{
		int hexagonsize = tilesize - (int)((double)tilesize/10.0);
		int agentsize = tilesize/2;
		
		if (tilesize != scaledSize) {
		
			logger.debug("Updating Image Buffer with all images resized to " + tilesize + "px.");
			
			try {
				scaledImagesBuffer.put("hexagon", scale(convert(ImageIO.read(new File(IMAGE_PATH + HEXAGON))), hexagonsize, hexagonsize));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				scaledImagesBuffer.put("goal", scale(convert(ImageIO.read(new File(IMAGE_PATH + GOAL))), agentsize, agentsize));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Iterator<String> iterator = agentIcons.keySet().iterator();
			String currentPlayer;
			while(iterator.hasNext()) {
			    currentPlayer = iterator.next();
			    scaledImagesBuffer.put(currentPlayer, scale(agentIcons.get(currentPlayer), agentsize, agentsize));
			}
		
			scaledSize = tilesize;
		}
		
		return;
	}

	private void clearBuffer()
	{
		backBuffer.setColor(Color.WHITE);
		backBuffer.fillRect(0, 0, worldMap.getWidth(), worldMap.getHeight());
		return;
	}

	private void initialise()
	{
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		backBuffer = worldMap.getGraphics();

		// Make the image buffer as large as could possibly be used (screensize)
		if (backBufferImage == null) {
			logger.debug("Creating backBufferImage of dimensions " + toolkit.getScreenSize().width + "," + toolkit.getScreenSize().height + ".");
			backBufferImage = createImage(toolkit.getScreenSize().width, toolkit.getScreenSize().height);
			backBuffer = backBufferImage.getGraphics();
		} else {
			backBufferImage.flush();
			backBufferImage = createImage(toolkit.getScreenSize().width, toolkit.getScreenSize().height);
			backBuffer.dispose();
			backBuffer = backBufferImage.getGraphics();
		}

		setupWindow(worldMap.getWidth(), worldMap.getHeight());

		initialise = false;
		
		logger.debug("initialised backBufferImage with size " + worldMap.getWidth() + "," + worldMap.getHeight() + ".");
	}
	
	public void paint(Graphics g) {
		worldMap.getGraphics().drawImage(backBufferImage, 0, 0, this);
		// hideous hack
		statusTitle.repaint();
		fileTitle.repaint();
		fileProgress.repaint();
		iterationTitle.repaint();
		iterationProgress.repaint();
		cycleTitle.repaint();
		cycleProgress.repaint();
		
		currentTitle.repaint();
		commentTitle.repaint();
		commentLabel.repaint();
		phaseTitle.repaint();
		phaseLabel.repaint();
		turnTitle.repaint();
		turnProgress.repaint();
		
		inspectorTitle.repaint();
		showPaths.repaint();
		playButton.repaint();
		stepButton.repaint();
		endButton.repaint();
		inspectorScrollPane.repaint();
		seperator1.repaint();
		seperator2.repaint();
		seperator3.repaint();
		seperator4.repaint();
		cycleProgress.repaint();
	}

	public synchronized void execute()
	{
		long start = System.currentTimeMillis(); // For calculating render time

		fileProgress.setStringPainted(true);
		fileProgress.setMinimum(0);
		fileProgress.setMaximum(((CTWorld)Simulator.pworld).getNumConfigs());
		fileProgress.setValue(((CTWorld)Simulator.pworld).getConfigNum());
		fileProgress.setString(((CTWorld)Simulator.pworld).getConfigNum() + "/" + ((CTWorld)Simulator.pworld).getNumConfigs());
		
		iterationProgress.setStringPainted(true);
		iterationProgress.setMinimum(0);
		iterationProgress.setMaximum(((CTWorld)Simulator.pworld).getNumIterations());
		iterationProgress.setValue(((CTWorld)Simulator.pworld).getIterationNum());
		iterationProgress.setString(((CTWorld)Simulator.pworld).getIterationNum() + "/" + ((CTWorld)Simulator.pworld).getNumIterations());
		
		cycleProgress.setStringPainted(true);
		cycleProgress.setMinimum(0);
		cycleProgress.setMaximum(Simulator.experimentLength);
		cycleProgress.setValue(Simulator.cycle);
		cycleProgress.setString(Simulator.cycle + "/" + Simulator.experimentLength);
		
		commentLabel.setText(((CTWorld)Simulator.pworld).getComment());
		
		phaseLabel.setText(getPhase());
		
		turnProgress.setStringPainted(true);
		turnProgress.setMinimum(0);
		turnProgress.setMaximum(((CTWorld)Simulator.pworld).getTimeout());
		turnProgress.setValue(((CTWorld)Simulator.pworld).getTurn());
		turnProgress.setString(((CTWorld)Simulator.pworld).getTurn() + "/" + ((CTWorld)Simulator.pworld).getTimeout());
		
		if (initialise) initialise();

		// Draw the viewer components in background-foreground order
		// updating certain elements for different phases

		if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.REG_PHASE) ) {
			// Registration phase
			showSpinner();
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.INIT_PHASE) ) {

			if (Simulator.controlPanel.paused) {
				playButton.setText("Play");
			} else {
				playButton.setText("Pause");
			}
			
			updateInspectorTree();
			
			// Initiation phase
			getAgentIcons();
			updateImageBuffer();
			addAgentPaths();
			clearBuffer();
			drawHexagons();
			drawGoals();
			drawAgents();
			
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.COMM_PHASE) ) {
			// Communication phase
			if (resize) {
				// If a resize event has happened, we need to re-render
				resize = false;
				updateImageBuffer();
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

		} else if (((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.RESET_PHASE) ) {
			agentPaths = new HashMap<String, ArrayList<Coord>>();
			initialise = true;
		} else {
			logger.debug("Perhaps I could use this unknown phase for something...");
		}
		
		// backBuffer ready for display
		worldMap.getGraphics().drawImage(backBufferImage, 0, 0, this);
		
		long finish = System.currentTimeMillis(); // For calculating render time
		String renderSpeed = dp.format(1000.0/(finish-start));
		
		// TODO claim james wrote something that "renders" at 3k fps or more?
		if (finish == start){
			logger.trace("Rendering speed: <1ms.");
		} else {
			logger.trace("Rendering speed: " + renderSpeed + "fps.");
		}
		
	}


	private Polygon drawHexagon(PixelCoord topleft, int tilesizeX, int tilesizeY)
	{
		int[] xCoords = new int[6];
		int[] yCoords = new int[6];

		final int tilesizeX2 = (int)((double)tilesizeX / 2.0);
		final int tilesizeY3 = (int)((double)tilesizeY / 3.0);
		final int tilesizeY23 = (int)(2.0*((double)tilesizeY / 3.0));

		Polygon hexagon;

		xCoords[0] = topleft.x + tilesizeX2;
		yCoords[0] = topleft.y;

		xCoords[1] = topleft.x + tilesizeX;
		yCoords[1] = topleft.y + tilesizeY3;

		xCoords[2] = topleft.x + tilesizeX;
		yCoords[2] = topleft.y + tilesizeY23;

		xCoords[3] = topleft.x + tilesizeX2;
		yCoords[3] = topleft.y + tilesizeY;

		xCoords[4] = topleft.x;
		yCoords[4] = topleft.y + tilesizeY23;

		xCoords[5] = topleft.x;
		yCoords[5] = topleft.y + tilesizeY3;

		hexagon = new Polygon(xCoords, yCoords, 6);

		return hexagon;
	}

	private String getPhase() {
		if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.REG_PHASE) ) {
			// Registration phase
			return "Registration";
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.INIT_PHASE) ) {
			// Initiation phase
			return "Initiation";
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.COMM_PHASE) ) {
			// Communication phase
			return "Communication";
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.MOVE_PHASE) ) {
			// Moving phase
			return "Moving";
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.END_PHASE) ) {
			// Moving phase
			return "End";
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.RESET_PHASE) ) {
			// Moving phase
			return "Reset";
		} else if ( ((CTWorld)Simulator.pworld).getCurrentPhase().equals(CTWorld.COMPLETED_PHASE) ) {
			// Moving phase
			return "Completed";
		} else {
			// Unknown phase
			return ((CTWorld)Simulator.pworld).getCurrentPhase();
		}
	}

	private void drawGoals() {
		Coord tempCoord;
		PixelCoord tempPixelCoord;
		Color tempColor;
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> iterator = playerNames.iterator();
		
		int inset = (int)(((1.0/20.0)*((double)tilesize/2.0)) + ((double)tilesize/4.0) );
		int size = (int)((9.0/10.0)*((double)tilesize/2.0));

		// Indicate Goal Tiles
		while (iterator.hasNext()) {
			String name = (String)iterator.next();

			tempCoord = ((CTWorld)Simulator.pworld).getPlayerGoal(name);

			if (tempCoord != null) {
				tempPixelCoord = coord2pixelcoord(tempCoord);
				tempColor = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));

				backBuffer.setColor(tempColor);
				backBuffer.fillOval(tempPixelCoord.x + inset, tempPixelCoord.y + inset, size, size);
				backBuffer.drawImage(scaledImagesBuffer.get("goal"), tempPixelCoord.x + tilesize/4, tempPixelCoord.y + tilesize/4, null);
			}
		}
		return;
	}

	private void drawPaths() {
		if (!doShowPaths) return;

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
				((Graphics2D)backBuffer).setStroke(new BasicStroke(2.0f));
				((Graphics2D)backBuffer).setColor(tempColor);
				((Graphics2D)backBuffer).drawPolyline(tempPolylinePath.x, tempPolylinePath.y, tempPolylinePath.x.length);
			}
		}
		return;
	}

	private BufferedImage convert(Image im)
	{
		BufferedImage bi = new BufferedImage(im.getWidth(null),im.getHeight(null),BufferedImage.TYPE_INT_ARGB);
		Graphics bg = bi.getGraphics();
		bg.drawImage(im, 0, 0, null);
		bg.dispose();
		return bi;
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
						agentIcons.put( theArgs[1], convert(ImageIO.read(new File("images/" + theArgs[3]))) );
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
	
	private HashMap<Coord,HashSet<String>> getLocsAndPlayers(){
		HashMap<Coord,HashSet<String>> locsAndPlayers = new HashMap<Coord,HashSet<String>>();
		Set<String> playerNames = ((CTWorld)Simulator.pworld).getPlayerNames();
		Iterator<String> playerIterator = playerNames.iterator();
		
		while (playerIterator.hasNext()) {
			String name = (String)playerIterator.next();
			Coord tempCoord = ((CTWorld)Simulator.pworld).getPlayerPos(name);
			HashSet<String> tempSet = new HashSet<String>();
/*			playersAndLocs.put(name,tempCoord);
			logger.trace("Adding " + name + " at ( " + tempCoord.x + " , " + tempCoord.y + " ) to playersAndLocs()");
			logger.trace("playersAndLocs is " + playersAndLocs.size() + " big");*/
			if (!(locsAndPlayers.containsKey(tempCoord))){
				tempSet.add(name);
				locsAndPlayers.put(tempCoord,tempSet);
				logger.trace(tempCoord + " (" + tempCoord.x + " , " + tempCoord.y + ") not in locsAndPlayers() so adding " + name + " to set for locsAndPlayers(" + tempCoord.x + "," + tempCoord.y + ") which is now " + tempSet + " = " + tempSet.size() );
			} else {
				tempSet = locsAndPlayers.get(tempCoord);
				logger.trace(tempCoord + " (" + tempCoord.x + " , " + tempCoord.y + ") was found in locsAndPlayers() with set: " + tempSet + ". Adding " + name);
				tempSet.add(name);
			}
		}
		return locsAndPlayers;
	}

	private void drawAgents(){

		HashMap<Coord,HashSet<String>> locsAndPlayers = getLocsAndPlayers();
		Set<Coord> locs = locsAndPlayers.keySet();
		Iterator<Coord> locIter = locs.iterator();
		
		while (locIter.hasNext()){
			Coord tempLoc = (Coord)locIter.next();
			HashSet<String> playersOnLoc = locsAndPlayers.get(tempLoc);
			Iterator<String> playersOnLocIter = playersOnLoc.iterator();
			if (playersOnLoc.size()==1){
				String name = playersOnLocIter.next();
				Color tempColor = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));
				PixelCoord tempPixelCoord = coord2pixelcoord(tempLoc);
				
				// draw agent name at tempLoc with tempColor
				backBuffer.setColor(Color.BLACK);
				backBuffer.fillRect(tempPixelCoord.x + tilesize/4 - 1, tempPixelCoord.y + tilesize/4 - 1, tilesize/2 + 2, tilesize/2 + 2);
				if (agentIcons.containsKey(name)) {
					backBuffer.drawImage(scaledImagesBuffer.get(name), tempPixelCoord.x + tilesize/4, tempPixelCoord.y + tilesize/4, tempColor, null);
				} else {
					// Blank icon
					backBuffer.setColor(tempColor);
					backBuffer.fillRect(tempPixelCoord.x + tilesize/4, tempPixelCoord.y + tilesize/4, tilesize/2, tilesize/2);
				}
				
			} else {
				// Draw a stack
				int stackOffset = tilesize/8;
				int stackStepping = 2*(stackOffset / (playersOnLoc.size() - 1));
				
				while (playersOnLocIter.hasNext()){
					String name = playersOnLocIter.next();
					Color tempColor = getColor(((CTWorld)Simulator.pworld).getPlayerColour(name));
					PixelCoord tempPixelCoord = coord2pixelcoord(tempLoc);
	
					// clever stuffs
					//add the offset then take stepping off each time
					int x = tempPixelCoord.x + (tilesize/4) + stackOffset;
					int y = tempPixelCoord.y + (tilesize/4) + stackOffset;
					
					backBuffer.setColor(Color.BLACK);
					backBuffer.fillRect(x-1, y-1, tilesize/2 + 2, tilesize/2 + 2);
					if (agentIcons.containsKey(name)) {
						backBuffer.drawImage(scaledImagesBuffer.get(name), x, y, tempColor, null);
					} else {
						// Blank icon
						backBuffer.setColor(tempColor);
						backBuffer.fillRect(x, y, tilesize/2, tilesize/2);
					}
					
					stackOffset = stackOffset - stackStepping;
					
				} //while (stack drawing)
			}
		} //while (getLocsAndPlayers)
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

	public String returnLabel() {
		String label = this.getClass().getName() + " <null>";
		return label;
	}

	public void onDelete() {
		this.setVisible(false);
		this.dispose();
	}

	private BufferedImage scale(BufferedImage image, int newWidth, int newHeight)
	{
		float width;
		float height;
		
		if (newWidth <= 0 || newHeight <= 0) {
			logger.warn("Invalid scale attempt aborted.");
			return null;
		}

		width = (float) newWidth / (float) image.getWidth();
		height = (float) newHeight / (float) image.getHeight();

		BufferedImage out = new BufferedImage(newWidth, newHeight,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = out.createGraphics();
		RenderingHints qualityHints = new RenderingHints(
				RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHints(qualityHints);
		AffineTransform at = AffineTransform.getScaleInstance(width, height);
		g2.drawImage(image, at, null);
		g2.dispose();

		return out;
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
			paddingX = (int)((double)( frameX - (tilesize*((double)(tilesX+1.0)/2.0)) )/2.0);
		} else {
			tilesize = tilesizeX;
			paddingX = minPadding;
			paddingY = (int)((double)( frameY - (tilesize*(tilesY - ((double)(tilesY-1.0)/3.0))) )/2.0);
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
		int offsetY = worldMap.getHeight() - paddingY;

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

		tilesX = ((CTWorld)Simulator.pworld).getWorldSizeX();
		tilesY = ((CTWorld)Simulator.pworld).getWorldSizeY();

		calculateViewingParameters(width, height, tilesX, tilesY, minimumPadding);

		return;
	}

	private void drawHexagons() {
		PixelCoord tempPixelCoord;
		
		//TODO: Simplify these(!)
		int tilespace = (int)((double)tilesize/20.0);
		int insetWidth = (int)( (56.0/60.0)*(9.0/10.0)*(double)tilesize );
		int insetHeight = (int)( (36.0/40.0)*(9.0/10.0)*(double)tilesize );
		int insetOffsetX = (int)( ((1.0/20.0)*(double)tilesize) + ((2.0/60.0)*(9.0/10.0)*(double)tilesize) );
		int insetOffsetY = (int)( ((1.0/20.0)*(double)tilesize) + ((2.0/40.0)*(9.0/10.0)*(double)tilesize) );
		
		// Draw the hexagons
		for (int y = tilesY-1; y >= 0; y--){
			if (y % 2 == 0) {
				// even rowg
				for (int x = 0; x <= tilesX-2; x = x + 2) {
					tempPixelCoord = intint2pixelcoord(x, y);
					backBuffer.setColor(getColor(((CTWorld)Simulator.pworld).getWorldState(x,y)));
					backBuffer.fillPolygon(drawHexagon(new PixelCoord(tempPixelCoord.x + insetOffsetX, tempPixelCoord.y + insetOffsetY), insetWidth, insetHeight));
					backBuffer.drawImage(scaledImagesBuffer.get("hexagon"), tempPixelCoord.x + tilespace, tempPixelCoord.y + tilespace, null);
				}
			} else {
				// odd row
				for (int x = 1; x <= tilesX-1; x = x + 2) {
					tempPixelCoord = intint2pixelcoord(x, y);
					backBuffer.setColor(getColor(((CTWorld)Simulator.pworld).getWorldState(x,y)));
					backBuffer.fillPolygon(drawHexagon(new PixelCoord(tempPixelCoord.x + insetOffsetX, tempPixelCoord.y + insetOffsetY), insetWidth, insetHeight));
					backBuffer.drawImage(scaledImagesBuffer.get("hexagon"), tempPixelCoord.x + tilespace, tempPixelCoord.y + tilespace, null);
				}
			}
		}
		
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

	private void showSpinner()
	{
		clearBuffer();
		Image spinImage;
		try {
			spinImage = ImageIO.read(new File(IMAGE_PATH + SPIN));
			backBuffer.drawImage(spinImage, worldMap.getWidth()/2 - 15, worldMap.getHeight()/2 - 15, null);
		} catch (Exception e) {
			logger.warn("Couldn't find the spinner image file '" + IMAGE_PATH + SPIN + "'.");
		}
	}

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void componentResized(ComponentEvent arg0) {
		if (initialise) return;
		setupWindow(worldMap.getWidth(), worldMap.getHeight());
		updateImageBuffer();
		resize = true;
		execute();
	}

	public void componentShown(ComponentEvent arg0) {
		execute();
	}


	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();

		if (source == showPaths) doShowPaths = (e.getStateChange() == ItemEvent.SELECTED);

		execute();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("play")) {
			logger.debug("Play button action has been performed.");
		} else {
			//
		}
	}

}