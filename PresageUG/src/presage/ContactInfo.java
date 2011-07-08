package presage;

import java.util.TreeMap;

public class ContactInfo {
	
	private String name;
	private int port;
	private String host;
	private String roles;
	private TreeMap<String, Object>  metadata;
	
	public ContactInfo(String _name, String _host, int _port, String _roles){
		name = _name;
		port = _port;
		host =_host;
		roles = _roles;
		metadata = new TreeMap<String, Object>();
	}
	
	public ContactInfo(String _name, String _host, int _port, String _roles, TreeMap<String, Object> _metadata){
		name = _name;
		port = _port;
		host =_host;
		roles = _roles;
		metadata = _metadata;
	}
	
	public String name(){
		return name;
	}
	public String host(){
		return host;		
	}
	public int port(){
		
		return port;
	}
	public String roles(){
		return roles;
	}
	
	public TreeMap<String, Object> getAllMetadata(){
		return metadata;
	}
	
	public Object getMetadata(String key){
		return metadata.get(key);
	}
	
	public void setMetadata(String key, Object value) {
		metadata.put(key, value);
	}
	
	public String toString(){
		return name + " a " + roles + " @<"+host+","+port+" " + metadata.toString() + ">";
	}
}
