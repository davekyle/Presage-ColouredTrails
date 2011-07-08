
//import java.awt.*;
package presage.util;
import java.util.*;
import org.apache.log4j.*;
//import java.io.*;
//import java.math.*;
//import javax.swing.*;
//import java.awt.event.*;
//import java.lang.*;
//import java.text.*;


public class Queue {
	
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	protected LinkedList<Object> queueList;
	protected String queueName;
	
	public Queue(String name){
		queueList = new LinkedList<Object>();
		queueName = name;
	}
	
	public synchronized void enqueue(Object object){
		queueList.addLast(object);
	}
	
	public synchronized Object dequeue() {
			return queueList.removeFirst();
		}
	
	public synchronized boolean isEmpty(){
		return queueList.isEmpty();
	}
	
} // ends class Queue
