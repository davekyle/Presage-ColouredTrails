package presage;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.simpleframework.xml.*;
import org.simpleframework.xml.load.*;

import org.apache.log4j.*;

@Root
public class Protocol {
	
	static Logger logger = Logger.getLogger(Protocol.class.getName());
	
	@Element
	private String name;
	
	@ElementMap (entry="state", key="name", value="validMessages", attribute=true, inline=true)
	private Hashtable<String, ProtocolState> states;

	private static Hashtable<String, Protocol> protocols = new Hashtable<String, Protocol>();
	public static Protocol getProtocol(String name) {
		return protocols.get(name);
	}
	
	public static void init() {
		try {
			Serializer serializer = new Persister();
			
			File protocolDir = new File("protocols/");
			File[] files = protocolDir.listFiles();

			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {
					Protocol p;
					p = serializer.read(Protocol.class, files[i]);
					protocols.put(p.getName(), p);
				}
			}
		} catch (Exception e) {
			logger.fatal("FATAL ERROR: EXITING...", e);
			System.exit(1);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public ProtocolState getState(String name) {
		return states.get(name);
	}
	
	public Set<String> getStateNames() {
		return states.keySet();
	}

	public Protocol(String name, Hashtable<String, ProtocolState> states) {
		this.name = name;
		this.states = states;
	}
	
	public Protocol(){
	
	}
	
	@Validate
	public void fixStateNames() {
		Iterator<String> i = states.keySet().iterator();
		while (i.hasNext()) {
			String k = (String)i.next();
			states.get(k).setName(k);
		}
	}
}
