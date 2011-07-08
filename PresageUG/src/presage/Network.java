package presage;

abstract public class Network {

	public Network() {

		
	}
	
	abstract public boolean sendMessage(String host, int port, Message msg); 
	
	abstract public void execute();
	
}