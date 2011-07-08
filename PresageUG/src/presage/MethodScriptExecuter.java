package presage;
import java.awt.Container;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.Insets;
// import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.*;

import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
// import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.border.Border;

import org.apache.log4j.Logger;

import presage.util.StringParseTools;

class MethodScriptExecuter extends JFrame {
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	// for static parts later
	static Logger altLogger = Logger.getLogger(MethodScriptExecuter.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<ScriptElement> script = Collections.synchronizedList(new ArrayList<ScriptElement>());

	JComboBox classFieldBox;
	JComboBox methodListBox;
	// JComboBox scriptBox;
	// JTable scriptTable;
	// Create a list that allows adds and removes
	DefaultListModel model = new DefaultListModel();
	JList scriptList;
	
	// JLabel varLabel;

	GridBagConstraints constraints = new GridBagConstraints();
	Insets insetsTop = new Insets(4, 4, 2, 4);
	Insets insetsMiddle = new Insets(2, 4, 2, 4);
	Insets insetsBottom = new Insets(2, 4, 4, 4);

	public MethodScriptExecuter(String inputFolderName) {
		super("MethodExecuter");
		
		//logger set to final for later on
		//final Logger logger = Logger.getLogger(this.getClass().getName());

		logger.info(" Initialising...");
		try {
			RandomAccessFile actionFile = new RandomAccessFile("inputFiles/"
					+ inputFolderName + "/methods.csv", "r");
			//First readLine discards the coloumn headers
			String currentLine = actionFile.readLine();
			//Get the first Line of Agent Data
			currentLine = actionFile.readLine();

			ScriptElement currentScriptElement;

			while (!(currentLine == null)) {
				String[] theArgs = StringParseTools.readTokens(currentLine, ",");
				currentScriptElement = new ScriptElement(theArgs);

				addtoScript(currentScriptElement);
				// get next line
				currentLine = actionFile.readLine();
			}
			actionFile.close();

		} catch (Exception e) {
			logger.fatal("Error: Accessing Methods I/O file: ", e);
		}

		// Now for the Graphical components.

		setLocation(0, 0);

		JLabel classFieldLabel = new JLabel("Class:");
		classFieldBox = new JComboBox();
		classFieldBox.setEditable(true);
		updateClassListBox();
		// 	classField.setSelectedIndex(0);

		classFieldBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JComboBox cb = (JComboBox) ae.getSource();
				int index = cb.getSelectedIndex();
				updateMethodListBox((String) classFieldBox.getSelectedItem());
				cb.setSelectedIndex(index);
			}
		});

		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateClassListBox();
				// getFileChooser();
			}
		});

		JLabel methodListBoxLabel = new JLabel("Methods:");
		String[] tmpMethods = {"Select Class...."};
		methodListBox = new JComboBox(tmpMethods);
		methodListBox.setSelectedIndex(0);
		
//		methodListBox.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				JComboBox cb = (JComboBox) ae.getSource();
//				int index = cb.getSelectedIndex();
//				updateVariableLabel((String) classFieldBox.getSelectedItem(), (String) methodListBox.getSelectedItem());
//				cb.setSelectedIndex(index);
//			}
//		});
		
//		varLabel = new JLabel("                                                            ");

		JLabel variableFieldLabel = new JLabel("Method Variables:");
		final JTextField variableField = new JTextField(20);

		JLabel timeFieldLabel = new JLabel("@ time:");
		final JTextField timeField = new JTextField(20);

		JButton executeButton = new JButton("Execute");
		executeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] args = new String[4];
				if (timeField.getText().equalsIgnoreCase("")) {
					args = new String[]{
							new Integer(Simulator.cycle + 1).toString(),
							(String) classFieldBox.getSelectedItem(),
							(String) methodListBox.getSelectedItem(),
							variableField.getText()};
				} else {
					args = new String[]{timeField.getText(),
							(String) classFieldBox.getSelectedItem(),
							(String) methodListBox.getSelectedItem(),
							variableField.getText()};

				}
				logger.debug("MethodExecuter: Adding Method to Script on Request...("
								+ args[0]
								+ ", "
								+ args[1]
								+ ", "
								+ args[2]
								+ ", " + args[3] + ");");

				synchronized (script) {
					addtoScript((ScriptElement) new ScriptElement(args));
				}

				// updateScriptListBox();
			}
		});

		//		synchronized (script) {
		//			scriptBox = new JComboBox(returnStringList(script));
		//		}
		//		scriptBox.addActionListener(new ActionListener() {
		//			public void actionPerformed(ActionEvent ae) {
		//				JComboBox cb = (JComboBox) ae.getSource();
		//				//int index = cb.getSelectedIndex();
		//				updateScriptListBox();
		//				//cb.setSelectedIndex(index);
		//			}
		//		});
		//
		//		String[] headings = new String[]{"Cycle", "Class", "Method",
		//				"Variables"};
		//		scriptTable = new JTable(returnScriptDataMatrix(script), headings);

		scriptList = new JList(model);
		// Initialize the list with items
		updateScriptList();

		JScrollPane scriptPane = new JScrollPane(scriptList);

		Border etch = BorderFactory.createEtchedBorder();
		scriptPane.setBorder((BorderFactory.createTitledBorder(etch,
				"Script Methods:")));

		//		String[] items = returnStringList(script);
		//		for (int i = 0; i < items.length; i++) {
		//			model.add(i, items[i]);
		//		}

		JButton deleteButton = new JButton("Delete");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {

				int[] selected = scriptList.getSelectedIndices();

				int index;
				for (int i = 0; i < selected.length; i++) {
					index = selected[i];
					synchronized (script) {
						script.remove(index);
					}
					model.removeElementAt(index);
				}
			}
		});

				
		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
//		constraints.weightx = 1.0;
//		constraints.weighty = 1.0;
		// constraints.ipadx = -10;
		// constraints.ipady = -10;
		constraints.fill = GridBagConstraints.BOTH;

		constraints.insets = insetsTop;
		addGB(content, classFieldLabel, 0, 0);
		addGB(content, classFieldBox, 1, 0);
		addGB(content, refreshButton, 2, 0);

		constraints.insets = insetsMiddle;
		addGB(content, methodListBoxLabel, 0, 1);
		constraints.gridwidth = 1;
		addGB(content, methodListBox, 1, 1);
		constraints.gridwidth = 1;
		
		
		addGB(content, variableFieldLabel, 0, 2);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addGB(content, variableField, 1, 2);
		constraints.fill = GridBagConstraints.BOTH;
		
		addGB(content, timeFieldLabel, 0, 3);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addGB(content, timeField, 1, 3);
		constraints.fill = GridBagConstraints.BOTH;
		
		addGB(content, executeButton, 2, 3);

		
		constraints.gridwidth = 3;
		addGB(content, scriptPane, 0, 4);
		// addGB(content, scriptList, 0, 3);
		// addGB(content, new JScrollPane(scriptTable), 0, 3);
		constraints.gridwidth = 1;
		

		constraints.insets = insetsBottom;
		addGB(content, deleteButton, 2, 5);
		
//		constraints.insets = insetsBottom;
//		constraints.gridwidth = 2;
//		addGB(content, varLabel, 0, 6);
		

		pack();
		setResizable(false);
		setVisible(false);
	}
	
//	public void getFileChooser(){
//		JFileChooser chooser = new JFileChooser();
//		chooser.setCurrentDirectory(new File(Simulator.classFilePath));
//	    // Note: source for ExampleFileFilter can be found in FileChooserDemo,
//	    // under the demo/jfc directory in the JDK.
//	    ExampleFileFilter filter = new ExampleFileFilter();
//	    filter.addExtension("class");
//	    filter.setDescription("Class Files");
//	    chooser.setFileFilter(filter);
//	    int returnVal = chooser.showOpenDialog(this);
//	    if(returnVal == JFileChooser.APPROVE_OPTION) {
//	       System.out.println("You chose to open this file: " +
//	            chooser.getSelectedFile().getName());
//	    }
//	}

	public String[] returnStringList(List<ScriptElement> list) {
		String[] stringList = new String[list.size()];
		Iterator<ScriptElement> iterator = list.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			stringList[i] = ((ScriptElement) iterator.next()).toString();
			i++;
		}
		return stringList;
	}

	public Object[][] returnScriptDataMatrix(List<ScriptElement> list) {
		Object[][] o = new Object[list.size()][4];
		Iterator<ScriptElement> iterator = list.iterator();
		int i = 0;
		ScriptElement element;
		while (iterator.hasNext()) {
			element = (ScriptElement) iterator.next();
			o[i][0] = new Integer(element.exeCycle);
			o[i][1] = element.className;
			o[i][2] = element.methodName;
			o[i][3] = element.variableStr;
			i++;
		}
		return o;
	}

	public void addtoScript(ScriptElement se) {

		if (script.size() == 0) {
			logger.debug(0 + ", " + se.toString());
			script.add(se);
			model.add(0, se.toString());
			return;
		}

		int i = 0;
		Iterator<ScriptElement> iterator = script.iterator();
		while (iterator.hasNext()) {
			if (((ScriptElement) iterator.next()).exeCycle > se.exeCycle) {
				break;
			} else { // ie its greater than or equal!
				i++;
			}
		}
		script.add(i, se);
		logger.debug(i + ", " + se.toString());

		int size = model.getSize();
		if (size < i) {
			model.add(size, "Error..");
		} else {
			model.add(i, se.toString());
		}
	}

	//	public void removefromScript(int index) {
	//		script.remove(index);
	//	}

	public void addGB(Container c, Component com, int x, int y) {
		constraints.gridx = x;
		constraints.gridy = y;
		c.add(com, constraints);
	}

	private static ArrayList<String> handleFolder(String path) {
		// Get a array of all the Files in the directory "path"
		
		altLogger.debug("Searching Folder: " + path);
		
		ArrayList<String> classList = new ArrayList<String>();
		
		File toF = new File(path);
		File[] files = toF.listFiles();

		try {
			for (int i = 0; i < files.length; i++) {
				// if its a file check if its a class file!
				if (files[i].isFile()) {
					String ext = StringParseTools.extensionForFilename(files[i].getName());
					if (ext.equalsIgnoreCase("class")){
						//add to list of class files!
						// System.out.println("Class Found: " + files[i].getName());
						String temp = path + "/" + StringParseTools.readTokens(files[i].getName(), ".")[0];
						temp = temp.replace("build/","");
						temp = temp.replace('/','.');
						classList.add(temp);
					}
				} else if (files[i].isDirectory()) { 
					// if its a folder you recursivly call this method with the path.
					classList.addAll(handleFolder(path + "/" + files[i].getName()));
				}
			}
		} catch (NullPointerException e1) {
			altLogger.fatal("MSE:  path not found", e1);
		}
		
		return classList;
	}
	
	public String[] getClassNames() {
//		try {
			// Get a array of all the Files in the classpath!
			// looking for .class files!
			
			String t = Simulator.classFilePath;		
			ArrayList<String> classList = handleFolder(t.substring(0,t.length()-1));
			
			Iterator<String> iterator = classList.iterator();
			while(iterator.hasNext()){
				logger.trace(iterator.next());
			}
			
			String[] temp = new String[classList.size()]; 
			temp = classList.toArray(temp);
			
			return temp;
			
//			File toF = new File(Simulator.classFilePath);
//			File[] files = toF.listFiles();
//
//			String[] temp = new String[files.length];
//
//			for (int i = 0; i < files.length; i++) {
//				// files[i];
//				// System.out.println(files[i].getName());
//				temp[i] = StringParseTools.readTokens(files[i].getName(), ".")[0];
//			}
//			return temp;
//		} catch (Exception e) {
//			System.out
//					.println("MethodExecuter: Class file identification unsuccessfull: "
//							+ e);
//			return new String[]{};
//		}
	}

	public void updateScriptList() {
		
		logger.debug("MethodScriptExecuter: Called updateScriptList()");
//		synchronized (script) {
//			String[] methodNames = returnStringList(script);
//			// model.removeAllElements();
			
		try{
			SwingUtilities.invokeLater(new UpdateScriptGUI());
		} catch (Exception e){
			
			
		}
//			model.clear();
//			for (int i = 0; i < methodNames.length; i++) {
//				model.add(i, methodNames[i]);
//			}
//		}
	}

	//	public void updateScriptListBox() {
	//		synchronized (script) {
	//			String[] methodNames = returnStringList(script);
	//			scriptBox.removeAllItems();
	//			for (int i = 0; i < methodNames.length; i++) {
	//				scriptBox.addItem(methodNames[i]);
	//			}
	//		}
	//	}

	public void updateClassListBox() {
		String[] classNames = getClassNames();
		classFieldBox.removeAllItems();
		for (int i = 0; i < classNames.length; i++) {
			classFieldBox.addItem(classNames[i]);
		}
	}

	public void updateMethodListBox(String className) {
		try {
			//get the Class object using the given class name
			Class<?> c = Class.forName(className);
			//get methods from the Class object
			Method m[] = c.getDeclaredMethods();
			// Method m[] = c.getMethods();
			methodListBox.removeAllItems();
			//filling the methods combobox
			methodListBox.addItem("Select....");
			
			for (int i = 0; i < m.length; i++) {
				
				logger.info(m[i].toString());
				
//				System.out.print(m[i].getName() + "(");
//				Class param[] = m[i].getParameterTypes();
//				
//				for (int h = 0; h < param.length; h++) {
//					System.out.print(param[h].toString() + ",");
//				}
//				System.out.println(")");
			}

			for (int i = 0; i < m.length; i++) {

				boolean parametersCorrect = false;
				Class<?> pc[] = m[i].getParameterTypes();
			
				for (int f = 0; f < pc.length; f++) {
					if (pc[f] == String[].class)
						parametersCorrect = true;
				}

				if ((Modifier.isStatic(m[i].getModifiers()))
						&& Modifier.isPublic(m[i].getModifiers())
						&& (parametersCorrect)) {
					methodListBox.addItem(m[i].getName());
					// m[i].getReturnType()
				}
			}
			methodListBox.setSelectedIndex(0);
			pack();

		} catch (Exception e) {
			methodListBox.removeAllItems();
			methodListBox.addItem("Error Loading Class Methods");
			methodListBox.setSelectedIndex(0);
			logger.fatal("MethodExecuter.updateMethodListBox:", e);
		}
	}
	
	public void runScript() {
		synchronized (script) {
			if (script.isEmpty()) {
				logger.trace("MethodScriptExecuter: Notification -Script Empty");
			} else {
				ScriptElement currentElement;
				Iterator<ScriptElement> iterator = script.iterator();
				boolean optimizer = false;
				while ((iterator.hasNext()) && (!optimizer)) {
					currentElement = (ScriptElement) iterator.next();
					if (currentElement.exeCycle < Simulator.cycle) {
						logger.error("MethodScriptExecuter: Error -this ScriptElement should have executed already!");
						logger.error("\t -Removing without Execution;");
						iterator.remove();
					} else if (currentElement.exeCycle == Simulator.cycle) {
						// this is where we will execute the ScriptElement
						executeElement(currentElement);
						iterator.remove();
						updateScriptList();
					} else if (currentElement.exeCycle > Simulator.cycle) {
						optimizer = true;
					}
				}
			}
		}
	}

	public void executeMethod(String className, String methodName,
			String variableString) {
		logger.debug("MethodExecuter: Executing Method on Request...("
				+ className + ", " + methodName + ", " + variableString + ");");
		try {
			Class<?> cls = Class.forName(className);

			String[] variables = StringParseTools.readTokens(variableString, ";");

			Method m = cls.getMethod(methodName, new Class[]{String[].class});
			m.invoke(null, new Object[]{variables});

		} catch (Exception e) {
			logger.fatal("Exception ", e);
		}

	}

	static void executeElement(ScriptElement element) {
		altLogger.debug("MethodExecuter: Executing ScriptElement...("
						+ element.toString() + ");");
		try {
			Class<?> cls = Class.forName(element.className);
			
			Method m = cls.getMethod(element.methodName,
					new Class[]{String[].class});
			m.invoke(null, new Object[]{element.variables});

		} catch (Exception e) {
			altLogger.fatal("Exception ", e);
		}
	}
	
	public boolean handleEvent(Event evt) {
		if (evt.target == this && evt.id == Event.WINDOW_DESTROY) {
			Simulator.controlPanel.hideMethodsManager();
			return true;
		}
		return super.handleEvent(evt);
	}
	
	class UpdateScriptGUI implements Runnable {
		
		UpdateScriptGUI(){}
		
		public void run(){
			
			synchronized (script) {
				String[] methodNames = returnStringList(script);
			
				model.clear();
				for (int i = 0; i < methodNames.length; i++) {
					model.add(i, methodNames[i]);
				}
			}
		}
	}	
}