package presage;


import java.util.ArrayList;
import org.simpleframework.xml.*;


public class ProtocolState {
	private String name;
	
	@ElementList(inline=true, required=false)
	private ArrayList<String> validMessages;
	
	public static ProtocolState EndState = new ProtocolState("end", new ArrayList<String>());
	
	public ProtocolState(String name, ArrayList<String> validMessages) {
		this.name = name;
		this.validMessages = validMessages;
	}
	
	public ProtocolState() {
		this.name = "Unnamed";
		this.validMessages = new ArrayList<String>();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Boolean isValidMessage(String performant) {
		return (validMessages.indexOf(performant) != -1);
	}
	
	public ArrayList<String> getValidMessages() {
		return validMessages;
	}
	


}