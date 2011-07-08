package presage;
/**
 * Brendan Neville
 * Information Systems and Networks Research Group
 * Electrical and Electronic Engineering
 * Imperial College London
 */

public class Intention {
	public String method;

	public Object[] variables;

	public int executionTime;

	// a new form of intetion which allows you to call any object
	// that takes an array of objects as its variables.
	// In addition you can specify when the intention should be executed
	public Intention(String _method, int time, Object[] objects) {
		method = _method;
		variables = objects;
		executionTime = time;
	}

	public Intention(String _method, Object[] objects) {
		method = _method;
		variables = objects;
		executionTime = Simulator.cycle;
	}

	public String toString() {
		
		String variablesString = "empty";
		if (variables.length > 0){
			variablesString = variables[0].toString();
			for (int counter = 1; counter < variables.length; counter++) {
				variablesString += ", " + variables[counter].toString();
			}
		}
		
		return "<" + method + "(" + variablesString + ")," + executionTime + ">";
	}

	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null || this.getClass() != object.getClass())
			return false;
		
		Intention other = (Intention)object;
		
		if (!other.method.equals(method)) {
			return false;
		} else if (!java.util.Arrays.equals(variables, other.variables)) {
			return false;
		} else if (executionTime != other.executionTime) {
			return false;
		} else {
			return true;
		}
	}

	public int hashCode() {
		int hash = 42;
		hash = 31 * hash + (null == method ? 0 : method.hashCode());
		hash = 31 * hash + executionTime;
		return hash;
	}
}