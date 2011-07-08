package presage;

public class Action {

	private String method;
	private Object[] variables;
	
	public Action(String actionMethod, Object[] actionVariables){
		method = actionMethod;
		variables = actionVariables;
	}

	public String getMethod(){	
		return method;
	}
	
	
	public Object[] getVariables(){	
		return variables;
	}
}