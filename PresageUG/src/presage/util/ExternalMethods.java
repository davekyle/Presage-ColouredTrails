
package presage.util;

//import sim.adhocnet.ProducerParticipant;
import java.io.*;

import org.apache.log4j.*;

class ExternalMethods {
	
	static Logger logger = Logger.getLogger(ExternalMethods.class.getName());

	/**
	 * Takes as input a GnuPlot script file path and executes this script.
	 * inside the script the user can plot to screen, pause and then print to a post script file.
	 * 
	 * @param args args[0] should be the path to the GnuPlot script file relative to the Simulator directory e.g. ./inputFiles/100C_20P_centralised/Gnuplot.gnu
	 * 
	 * Example Script file GnuPlot.gnu:
	 * 
set xlabel "Lateral Displacement"; set ylabel "Contra-Lateral Displacement"; set zlabel "Amplitude"
set parametric
set isosamples 75,75 
set contour base
set cntrparam level incremental -1, 0.2, 10 
set clabel '%4.2f' 
set contour surface
set contour base; set nosurface
set surface;
set view 20,60
set view 60,30 
set hidden3d
splot u,v,sin(u)*cos(v) title "Standing Waves"
set size 1.0, 0.6
set terminal postscript portrait enhanced color dashed lw 1 "Helvetica" 14 
set output 'tempoutput/my-plot.ps'
replot
set terminal x11
set size 1,1
#pause 5
	 * 
	 */
	public static void gnuplot(String[] args) {

		logger.info("gnuplot called on scriptfile: " + args[0]);
		// get a Runtime object
		Runtime r = Runtime.getRuntime();

		try {
			// start the process: gnuplot
			String exe = "wgnuplot " + args[0];
			// String exe = "C:/Program Files/gnuplot/bin/wgnuplot " + args[0];
			logger.info(exe);
			Process p = r.exec(exe);

//			any error message?
			StreamGobbler errorGobbler = new 
			StreamGobbler(p.getErrorStream(), "ERR");            

			// any output?
			StreamGobbler outputGobbler = new 
			StreamGobbler(p.getInputStream(), "OUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			int exitVal = p.waitFor();
			logger.error("ExitValue: " + exitVal);

		} catch (Exception e){
			logger.fatal("Error Executing gnuplot: " + args[0] + " - ", e);
		}
	}


	public static void convertPstoEps(String[] args) {

		logger.info("Converting Postscript file to an Encapsulated Postscript file with Bounding box");

		String directory = args[0];
		String[] cmd = new String[] {"ps2epsi", args[1], args[2]};

		if (cmd.length > 0){
			String cmdString = "Executing process: \\"+ directory +">" + cmd[0].toString();
			for (int counter = 1; counter < cmd.length; counter++) {
				cmdString += " " + cmd[counter].toString();
			}
			logger.info(cmdString);
		} else {
			logger.error("convertPstoEps ERROR: empty command string! EXITING.....");
			return;
		}

		Runtime r = Runtime.getRuntime();

		try {
			// start the process
			Process p = r.exec(cmd, null, new File(directory));

			// any error message?
			StreamGobbler errorGobbler = new 
			StreamGobbler(p.getErrorStream(), "ERR");            

			// any output?
			StreamGobbler outputGobbler = new 
			StreamGobbler(p.getInputStream(), "OUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			int exitVal = p.waitFor();
			logger.error("ExitValue: " + exitVal);

		} catch (Exception e){
			logger.fatal("Error Executing convertPstoEps", e);
		}
	}

	public static void convertEpstoPDF(String[] args) {

		logger.info("Converting Encapsulated Postscript file to a cropped PDF");

		String directory = args[0];
		String[] cmd = new String[] {"eps2pdf", "/f=\"" + args[1] + "\""};

		if (cmd.length > 0){
			String cmdString = "Executing process: \\"+ directory +">" + cmd[0].toString();
			for (int counter = 1; counter < cmd.length; counter++) {
				cmdString += " " + cmd[counter].toString();
			}
			logger.info(cmdString);
		} else {
			logger.error("convertEpstoPDF ERROR: empty command string! EXITING.....");
			return;
		}

		Runtime r = Runtime.getRuntime();

		try {
			// start the process
			Process p = r.exec(cmd, null, new File(directory));

			// any error message?
			StreamGobbler errorGobbler = new 
			StreamGobbler(p.getErrorStream(), "ERR");            

			// any output?
			StreamGobbler outputGobbler = new 
			StreamGobbler(p.getInputStream(), "OUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			int exitVal = p.waitFor();
			logger.error("ExitValue: " + exitVal);

		} catch (Exception e){
			logger.fatal("Error Executing convertPstoEps", e);
		}
	}

	/**
	 * Runs a Windows command line tool e.g. dir etc
	 * 
	 * not for running processes
	 * 
	 * Argument format is "\aboslute path;commandname;arg0;arg1;arg2;.....;argN" OR ".relative path;commandname;arg0;arg1;arg2;.....;argN"
	 * 
	 * you cannot enter ";dir" to indicate current directory as it will use dir as the directory!
	 * 
	 * where arg0, arg1 etc are space separated arguements.
	 * 
	 * so to execute the command dir in the simulator directory the variables should be .;dir
	 * to execute dir in a directory tempoutput one level up from simulator .\tempoutput;dir
	 * to execute dir in the root directory \;dir
	 * 
	 * @param args
	 */

	public static void command(String[] args) {

		logger.info("Shell Command executer for Windows XP called");

		String[] cmd = new String[args.length +1];
		cmd[0] = "cmd.exe";
		cmd[1] = "/C";

		String directory = "";

		//System.out.println("args[0] = " + args[0]);
		//System.out.println("args[0].charAt(0) = " + args[0].charAt(0));
		
		try{
			switch ((args[0].charAt(0))) {
			case '.':  directory = (new File(".")).getCanonicalPath() + args[0].substring(1); break;
			case '\\': directory = args[0]; break;
			default:   directory = (new File(".")).getCanonicalPath() + args[0].substring(1);
			}
		} catch (IOException e){
			logger.fatal("command: Failed to get the current path", e);
		}
		
		logger.info("directory =" + directory);
		

		if (args.length > 1){
			for (int counter = 1; counter < args.length; counter++) {
				cmd[counter + 1] = args[counter];
			}
		}

		if (cmd.length > 0){
			String cmdString;

			cmdString = "Executing process: " + directory +">" + cmd[0].toString();


			for (int counter = 1; counter < cmd.length; counter++) {
				cmdString += " " + cmd[counter].toString();
			}
			logger.info(cmdString);
		} else {
			logger.error("command ERROR: empty command string! EXITING.....");
			return;
		}		

		Runtime r = Runtime.getRuntime();

		try {
			// start the process
			Process p = r.exec(cmd, null, new File(directory));

			// any error message?
			StreamGobbler errorGobbler = new 
			StreamGobbler(p.getErrorStream(), "ERR");            

			// any output?
			StreamGobbler outputGobbler = new 
			StreamGobbler(p.getInputStream(), "OUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			int exitVal = p.waitFor();
			logger.error("ExitValue: " + exitVal);

		} catch (Exception e){
			logger.fatal("Error Executing command", e);
		}
	}



}

class StreamGobbler extends Thread
{
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	InputStream is;
	String type;
	OutputStream os;

	StreamGobbler(InputStream is, String type)
	{
		this(is, type, null);
	}
	StreamGobbler(InputStream is, String type, OutputStream redirect)
	{
		this.is = is;
		this.type = type;
		this.os = redirect;
	}

	public void run()
	{
		try
		{
			PrintWriter pw = null;
			if (os != null)
				pw = new PrintWriter(os);

			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line=null;
			while ( (line = br.readLine()) != null)
			{
				if (pw != null)
					pw.println(line);
				logger.info(type + ">" + line);    
			}
			if (pw != null)
				pw.flush();
		} catch (IOException ioe)
		{
			logger.fatal("IOE: ", ioe);  
		}
	}
}