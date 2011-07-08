package presage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.*;

import presage.util.Queue;


public abstract class PhysicalWorld {
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	Queue queuedActions = new Queue("queuedActions");
	
	// you will need to handle the world state.
	// this is likely to include the registered agents
	// and their positions; any server controlled objects etc.
	
	public PhysicalWorld() {
	
	}
	
	/**
	 * implement this method to facilitate the agents actions in the physical
	 * world. Sending a MoveRequest object to the simulators socket executes 
	 * this method for external agents. The object returned could be the 
	 * agents sensory perception of the world.
	 */
	
	/**
	 * Will call the your method corresponding to the class.method expressed by Action.method
	 * 
	 * i.e. implement a method move and this will call it with variables Object[] Action.variables. 
	 * 
	 * */
	public Object act(Action effector) {
		queuedActions.enqueue(effector);

		// always return null
		return null;
	}
	
	// should take a Action object, first parameter being the action type
	// then a Object[] variables
	
	// reflection to enable flexibility.
	
	// need to register agents to the physical world
	// in order for them to be added to the world state.
	abstract public Map<String, Object> register(String myId, ArrayList<String> roles);

	abstract public boolean deregister(Object[] args);
	
	protected void execute() {
		while (!queuedActions.isEmpty()) {
			Action effector = (Action)queuedActions.dequeue();
			try {
				Class<?> c = this.getClass();
				Method m = c.getDeclaredMethod(effector.getMethod(), new Class[] { Action.class });
				// hack to get around visibility of subclass methods from superclass
				// anybody found abusing this will, themselves, be abused.
				// and then forced to figure out Java Security and implement code signing.
				m.setAccessible(true);
				m.invoke(this, effector);

			} catch (NoSuchMethodException e2) {
				logger.fatal("execute physical action: NoSuchMethodException - ", e2);
			} catch (IllegalAccessException e3) {
				logger.fatal("execute physical action: IllegalAccessException - ", e3);
			} catch (InvocationTargetException e4) {
				logger.fatal("execute physical action: InvocationTargetException - ", e4);
			}
		}
	}
	
	// If a world is no longer live, the simulation will terminate
	public boolean live() {
		return true;
	}
	
}
