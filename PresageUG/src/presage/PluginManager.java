package presage;
import java.awt.Container;
import java.awt.Event;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.*;
import javax.swing.BorderFactory;

import org.apache.log4j.*;

import presage.util.StringParseTools;

// import javax.swing.JList;
// import javax.swing.JScrollPane;
// import java.awt.FlowLayout;

public class PluginManager extends JFrame {
	
	Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public List<Plugin> plugins = Collections.synchronizedList(new ArrayList<Plugin>());

	//	JComboBox pluginListBox;
	JComboBox choosePluginListBox;

	//	 Create a list that allows adds and removes
	DefaultListModel model = new DefaultListModel();

	JList pluginList;

	GridBagConstraints constraints = new GridBagConstraints();

	Insets insetsTop = new Insets(5, 5, 0, 5);

	Insets insetsMiddle = new Insets(5, 5, 0, 5);

	Insets insetsBottom = new Insets(5, 5, 5, 5);

	public PluginManager(String inputFolderName) {

		super("PluginManager");

		logger.info(" Initialising...");
		try {
			RandomAccessFile actionFile = new RandomAccessFile("inputFiles/"
					+ inputFolderName + "/PLUGINS.CSV", "r");
			//First readLine discards the coloumn headers
			String currentLine = actionFile.readLine();
			//Get the first Line of Agent Data
			currentLine = actionFile.readLine();

			Plugin currentPlugin;

			while (!(currentLine == null)) {
				String[] theArgs = StringParseTools.readTokens(currentLine, ",");

				String[] variables = StringParseTools.readTokens(theArgs[1], ";");

				Class<?> cls = Class.forName(theArgs[0]);
				Constructor<?> c = cls
						.getConstructor(new Class[] { String[].class });
				currentPlugin = (Plugin) c
						.newInstance(new Object[] { variables });

				synchronized (plugins) {
					plugins.add(currentPlugin);
				}

				// get next line
				currentLine = actionFile.readLine();
			}
			actionFile.close();

		} catch (Exception e) {
			logger.fatal("Error: Accessing Plugins I/O file: ", e);
		}

		// Now for the Graphical components.

		setLocation(0, 0);

		JLabel pluginFieldLabel = new JLabel("Plugin Type:");
		// final JTextField pluginField = new JTextField(20);

		String[] avaPlugins = getAvaliblePluginNames();
		choosePluginListBox = new JComboBox(avaPlugins);
		// choosePluginListBox.setSelectedIndex(0);
		choosePluginListBox.setEditable(true);

		JLabel variableFieldLabel = new JLabel("Plugin Variables:");
		final JTextField variableField = new JTextField(20);

		JButton launchButton = new JButton("Launch");
		launchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				logger.info("PluginLauncher: Launching <"
						+ (String) choosePluginListBox.getSelectedItem() + ", "
						+ variableField.getText() + ">");
				String[] variables = StringParseTools.readTokens(
						variableField.getText(), ";");
				launchPlugin((String) choosePluginListBox.getSelectedItem(),
						variables);
			}
		});

		//		JLabel pluginListBoxLabel = new JLabel("Current Plugins:");
		//		pluginListBox = new JComboBox(listPlugins());
		//		pluginListBox.setSelectedIndex(0);

		//		pluginListBox.addActionListener(new ActionListener() {
		//			public void actionPerformed(ActionEvent ae) {
		//				JComboBox cb = (JComboBox) ae.getSource();
		//				int index = cb.getSelectedIndex();
		//				cb.removeAllItems();
		//				String[] tmp = listPlugins();
		//				for (int i = 0; i < tmp.length; i++) {
		//					cb.addItem(tmp[i]);
		//				}
		//				cb.setSelectedIndex(index);
		//			}
		//		});

		//		pluginList = new JList(listPlugins());
		//		JScrollPane scrollPane = new JScrollPane(pluginList);

		pluginList = new JList(model);
		// Initialize the list with items
		updatePluginList();

		JScrollPane pluginPane = new JScrollPane(pluginList);

		Border etch = BorderFactory.createEtchedBorder();
		pluginPane.setBorder((BorderFactory.createTitledBorder(etch,
				"Current Plugins:")));

		//		String[] items = returnStringList(script);
		//		for (int i = 0; i < items.length; i++) {
		//			model.add(i, items[i]);
		//		}

		JButton deleteButton = new JButton("Delete");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int[] selected = pluginList.getSelectedIndices();
				int index;
				for (int i = selected.length - 1; i >= 0; i--) {
					index = selected[i];
					logger.info("Delete");
					logger.info(index);
					deletePlugin(index);
					model.removeElementAt(index);
				}
			}
		});

		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
		// constraints.weightx = 1.0;
		// constraints.weighty = 1.0;
		// constraints.ipadx = -10;
		// constraints.ipady = -10;
		constraints.fill = GridBagConstraints.BOTH;

		constraints.insets = insetsTop;
		addGB(content, pluginFieldLabel, 0, 0);
		//addGB(content, pluginField, 1, 0);
		addGB(content, choosePluginListBox, 1, 0);

		constraints.insets = insetsMiddle;
		addGB(content, variableFieldLabel, 0, 1);
		addGB(content, variableField, 1, 1);
		addGB(content, launchButton, 2, 1);

		// addGB(content, pluginListBoxLabel, 0, 2);

		constraints.insets = insetsBottom;
		constraints.gridwidth = 3;
		// addGB(content, pluginListBox, 1, 1);
		addGB(content, pluginPane, 0, 3);
		constraints.gridwidth = 1;
		addGB(content, deleteButton, 2, 4);

		// content.add(pluginList);
		// content.add(scrollPane);
		pack();
		setResizable(false);
		setVisible(true);
	}

	public void killAll(){
		
		for (int i =0; i < plugins.size(); i++)
		deletePlugin(i);
		
	}
	
	public void updatePluginList() {
		synchronized (plugins) {
			String[] pluginNames = listPlugins();
			// model.removeAllElements();
			model.clear();
			for (int i = 0; i < pluginNames.length; i++) {
				model.add(i, pluginNames[i]);
			}
		}
	}

	public void handleDirectory(File directory, String path, ArrayList<String> temp) {
		File[] files = directory.listFiles();
		path = path + directory.getName() + ".";
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				handleDirectory(files[i], path, temp);
			} else if (files[i].isFile()) {
				handleFile(files[i], path, temp);
			}
		}
	}

	public void handleFile(File file, String path, ArrayList<String> temp) {
		try {
			// split on the .
			String[] className = StringParseTools.readTokens(file.getName(), ".");
			String name = path + className[0];

			Class<?> c = Class.forName(name);
			// if check if its an instance of plugin
			Class<?>[] interfaces = c.getInterfaces();
			if (interfaces.length > 0) {
				logger.debug(name + " implements " + interfaces.length
						+ " interfaces: ");
				for (int j = 0; j < interfaces.length; j++) {
					logger.debug(interfaces[j].getSimpleName());
				}
				if (interfaces[0].getSimpleName().contains("Plugin")) {
					temp.add(name);
				}
			} else {
				logger.debug(name + " implements no interfaces");
			}
		} catch (ClassNotFoundException e) {
			// Do Nothing
			//   Non java file
		} catch (Exception e) {
			logger.fatal("PluginManager.handleFile: ", e);
		}
	}

	public String[] getAvaliblePluginNames() {
		try {
			// Get a array of all the Files in the input directory
			File toF = new File(Simulator.classFilePath);
			File[] files = toF.listFiles();
			// Then Copy them all.
			//			WE cant go hard wiring sim_PhD. infront of stuff!!!
			//			for starters there may be plugins in other packages.

			String path = "";
			
			ArrayList<String> temp = new ArrayList<String>();

			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					handleDirectory(files[i], path, temp);
				} else if (files[i].isFile()) {
					handleFile(files[i], path, temp);
				}
			}
			String[] list = new String[temp.size()];
			for (int k = 0; k < temp.size(); k++) {
				list[k] = (String) temp.get(k);
			}
			return list;
		} catch (Exception e) {
			logger.fatal("PluginManager: Class file identification unsuccessful: ", e);
			return new String[] {};
		}
	}

	public void addGB(Container c, Component com, int x, int y) {
		constraints.gridx = x;
		constraints.gridy = y;
		c.add(com, constraints);
	}

	//	public void updatePluginListBox() {
	//		int index = pluginListBox.getSelectedIndex();
	//		pluginListBox.removeAllItems();
	//		String[] tmp = listPlugins();
	//		for (int i = 0; i < tmp.length; i++) {
	//			pluginListBox.addItem(tmp[i]);
	//		}
	//		pluginListBox.setSelectedIndex(index);
	//	}

	//	public void updatePluginList() {
	//		pluginList.removeAll();
	//		String[] tmp = listPlugins();
	//		for (int i = 0; i < tmp.length; i++) {
	//			pluginList.add(tmp[i]);
	//		}
	//		pluginListBox.setSelectedIndex(index);
	//	}

	public String[] listPlugins() {

		synchronized (plugins) {
			if (plugins.isEmpty()) {
				return new String[] {};
			} else {
				String[] list = new String[plugins.size()];
				Plugin currentPlugin;
				for (int i = 0; i < plugins.size(); i++) {
					currentPlugin = (Plugin) plugins.get(i);
					list[i] = currentPlugin.returnLabel();
				}
				return list;
			}
		}
	}

	public void deletePlugin(int index) {
		synchronized (plugins) {
			Plugin currentPlugin = (Plugin) plugins.get(index);
			plugins.remove(index);
			currentPlugin.onDelete();
			// updatePluginListBox();
		}
	}

	public void launchPlugin(String type, String[] variables) {
		// String[] variables = Simulator.readTokens(variables, ";");

		try {
			Class<?> cls = Class.forName(type);
			Constructor<?> c = cls.getConstructor(new Class[] { String[].class });
			Plugin currentPlugin = (Plugin) c
					.newInstance(new Object[] { variables });
			synchronized (plugins) {
				plugins.add(currentPlugin);
			}
		} catch (Exception e) {
			logger.fatal("Error: Launching Plugin : ", e);
		}
		updatePluginList();
	}
	
	

	public void executePlugins() {
				
		synchronized (plugins) {
			if (plugins.isEmpty()) {
				logger.debug("PluginManager: Notification -no plugins");
			} else {
				Plugin currentPlugin;
				Iterator<Plugin> iterator = plugins.iterator();
				while (iterator.hasNext()) {
					currentPlugin = (Plugin) iterator.next();
					currentPlugin.execute();
				}

			}
		}
	}
	
	public boolean handleEvent(Event evt) {
		if (evt.target == this && evt.id == Event.WINDOW_DESTROY) {
			// this.hide();
			Simulator.controlPanel.hidePluginManager();
			return true;
		}
		return super.handleEvent(evt);
	}
}