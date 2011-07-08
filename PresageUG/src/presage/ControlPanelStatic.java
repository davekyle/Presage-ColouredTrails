package presage;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import java.text.NumberFormat;

/*
 * Created on 12-Jan-2005
 *
 */

/**
 * @author Brendan
 * 
 */
public class ControlPanelStatic extends JFrame {

	private static final long serialVersionUID = 1L;
	
	Container content;

//	 kyle edits
	public boolean paused = false;

	public boolean step = false;

	JProgressBar pBar;

	JLabel Label1 = new JLabel("Default");

	JButton playButton;

	JButton stepButton;

	JButton verboseButton;

	JButton endButton;
	
	JButton showPluginManager;
	
	JButton showMethodsManager;
	
	long timelastten; // = System.currentTimeMillis();
	double totaltimeleft = 0; 

	public ControlPanelStatic(boolean p) {
		super("ControlPanel");
		
		if (p)
			paused = true;
	
		
		
		println(" -Initialising....");
		content = getContentPane();
		content.setLayout(new FlowLayout());
		
		if (p)
			playButton = new JButton("Play");
		else
			playButton = new JButton("Pause");
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JButton s = (JButton) ae.getSource();
				if (s.getText().equalsIgnoreCase("Pause")) {
					s.setText("Play");
					paused = true;
					step = false;
				} else {
					s.setText("Pause");
					paused = false;
					step = false;
				}
			}
		});

		stepButton = new JButton("Step");
		stepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				playButton.setText("Play");
				paused = true;
				step = true;
			}
		});

		verboseButton = new JButton("Quiet");
		verboseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JButton s = (JButton) ae.getSource();
				if (s.getText().equalsIgnoreCase("Quiet")) {
					s.setText("Verbose");
					Simulator.verbose = new Boolean(false);
				} else {
					s.setText("Quiet");
					Simulator.verbose = new Boolean(true);
				}
			}
		});

		endButton = new JButton("End Nicely");
		endButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Simulator.experimentLength = Simulator.cycle;
				paused = false;
			}
		});
		
		showPluginManager = new JButton("Plugin Manager");
		showPluginManager.addActionListener(new showPluginButtonAction());
		
		showMethodsManager = new JButton("Methods Manager");
		showMethodsManager.addActionListener(new showMethodsButtonAction());

		pBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		pBar.setString("Simulation Progress");
		pBar.setToolTipText("Simulation Progress");
		pBar.setName("Simulation Progress");

		content.add(playButton);
		content.add(stepButton);
		content.add(verboseButton);
		content.add(endButton);
		content.add(showPluginManager);
		content.add(showMethodsManager);
		content.add(pBar);
		content.add(Label1);
		setSize(360, 125);
		setResizable(false);
		setLocation(0, 0);
		setVisible(true);
		
		timelastten = System.currentTimeMillis();
	}

	public void execute() {
		// int temp = (int)((new Double(Simulator.cycle).doubleValue()/new
		// Double(Simulator.experimentLength).doubleValue())*100);
		int temp = Math
				.round(((float) Simulator.cycle / (float) Simulator.experimentLength) * 100);
		
		if ((Simulator.cycle%10)==0){			
			
			
			
			long timepast = System.currentTimeMillis() - timelastten;
			
			timelastten = System.currentTimeMillis();
			
			int periodsleft = (Simulator.experimentLength - Simulator.cycle)/10;
		
			totaltimeleft = (timepast * periodsleft) * (2.77777778*Math.pow(10, -7));

		}
		
		NumberFormat myFormat = NumberFormat.getInstance();
		myFormat.setMaximumFractionDigits(2);
		String formattedDouble = myFormat.format(totaltimeleft);
		
		Label1.setText(Simulator.cycle + "/" + Simulator.experimentLength + ": t-minus " + formattedDouble + " hrs");
		pBar.setValue(temp);
		// pBar.setValue(Simulator.RandomGenerator.nextInt(100));
		
		
		
		while (paused) {
			// animate something

			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}

			if (step) {
				step = false;
				break;
			}
		}
	}

	public void println(String s) {

	}

	public String returnLabel() {
		String label = this.getClass().getName() + " <null>";
		return label;
	}

	public void onDelete() {
		this.setVisible(false);
		this.dispose();
	}

	public void vprintln(String s) {

	}
	
	
	public void showPluginManager(){  
		Simulator.pluginManager.setVisible(true);
		
		showPluginManager.setText("Plugin Manager");
	}
	
	public void showMethodsManager(){
		Simulator.methodExecuter.setVisible(true);
		
		showMethodsManager.setText("Methods Manager");	
	}
	
	public void hidePluginManager(){
		Simulator.pluginManager.setVisible(false);
		
		showPluginManager.setText("Plugin Manager");
	}
	
	public void hideMethodsManager(){
		Simulator.methodExecuter.setVisible(false);
		
		showMethodsManager.setText("Methods Manager");
	}
	
	private class showPluginButtonAction implements ActionListener {
		public void actionPerformed(ActionEvent actionEvent) {
			if (!Simulator.pluginManager.isVisible()) {
				showPluginManager();
			} else {
				hidePluginManager();
			}
		}
	}
	private class showMethodsButtonAction implements ActionListener {
		public void actionPerformed(ActionEvent actionEvent) {
			if (!Simulator.methodExecuter.isVisible()) {
				showMethodsManager();
			} else {
				hideMethodsManager();
			}
		}
	}

}