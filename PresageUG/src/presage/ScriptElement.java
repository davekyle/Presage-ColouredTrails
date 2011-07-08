package presage;

import presage.util.StringParseTools;

public class ScriptElement {
	
	int exeCycle;
	String methodName;
	String className;
	String  variableStr;	
	String[] variables;

	public ScriptElement(String[] args) {
		exeCycle = Integer.parseInt(args[0]);
		className = args[1];
		methodName = args[2];
		variableStr = args[3];
		variables = StringParseTools.readTokens(args[3], ";");
	} //ends ScriptElement constructor

	public String toString() {
		String str = "<" + exeCycle + ", "
				+ className + "." + methodName + "("
				+ variableStr + ")>;"; 
		return str;
	}//ends the toString method 
	
} // ends class ScriptElement
