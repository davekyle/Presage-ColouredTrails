
package presage;
public interface Plugin {
	
	abstract public void execute();
	
	public String returnLabel();
	
	public void onDelete();
	
} // ends class plugin
