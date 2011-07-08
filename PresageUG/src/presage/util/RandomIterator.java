package presage.util;
import java.util.*;

public class RandomIterator<E> {
	ArrayList<E> set;
	Random rand;
	
	public RandomIterator(SortedSet<E> s, Random rand) {
		set = new ArrayList<E>(s);
		this.rand = rand;
	}
	
	public RandomIterator(ArrayList<E> s, Random rand) {
		set = s;
		this.rand = rand;
	}
	
	public void removeElement(Object x){
		set.remove(x);
	}
	
	public boolean hasNext() {
		if (set.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}
	public Object next() {
		Object[] setArray = set.toArray();
		Object o = setArray[rand.nextInt(set.size())];
		set.remove(o);
		return o;
	}
}
