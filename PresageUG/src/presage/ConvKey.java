/**
 * Brendan Neville
 * Information Systems and Networks Research Group
 * Electrical and Electronic Engineering
 * Imperial College London
 */

package presage;

public class ConvKey implements Comparable<Object> {
	
	public static final ConvKey NullConvKey = new ConvKey("", "");

	public String major;
	public String minor;

	public ConvKey(String convIdMajor, String convIdMinor) {
		major = convIdMajor;
		minor = convIdMinor;
	}
	
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		// object must be ConvKey at this point
		ConvKey otherkey = (ConvKey)obj;
		return (major == otherkey.major || (major != null && major.equals(otherkey.major))) &&
		(minor == otherkey.minor || (minor != null && minor.equals(otherkey.minor)));
	}

	public int hashCode()
	{
		int hash = 7;
		hash = 31 * hash + (null == minor ? 0 : minor.hashCode());
		hash = 31 * hash + (null == major ? 0 : major.hashCode());
		return hash;
	}

	public int compareTo(Object object) {
		
		if (this.equals(object)) { // if they are equal then return 0
			return 0;
		} else if (object.getClass() != this.getClass()){	
			// if they aren't the same class then can't compare default to -1
			return -1;
		} 
		
		ConvKey otherKey = (ConvKey)object;
		
		int intMajor;
		int intMinor;
		
		try {
			intMajor = new Integer(this.major).intValue();
		} catch (NumberFormatException e) {
			intMajor = -1;
		}
		
		try {
			intMinor = new Integer(this.minor).intValue();
		} catch (NumberFormatException e) {
			intMinor = -1;
		}

		int otherMajor;
		int otherMinor;
		
		try {
			otherMajor = new Integer(otherKey.major).intValue();
		} catch (NumberFormatException e) {
			otherMajor = -1;
		}
		
		try {
			otherMinor = new Integer(otherKey.minor).intValue();
		} catch (NumberFormatException e) {
			otherMinor = -1;
		}
		
		if (intMajor > otherMajor) {
			return 1;
		} else if (intMajor < otherMajor) {
			return -1;
		} else {
			if (intMinor > otherMinor) {
				return 1;
			} else if (intMinor < otherMinor){
				return -1;
			} else {
				return 0;
			}
		}
	}

	public boolean isInstantiated() {
		if ((major.equalsIgnoreCase("")) && (minor.equalsIgnoreCase(""))) {
			return false;
		} else {
			return true;
		}
	}

	public String toString() {
		return major + "." + minor;
	}

	public ConvKey parentKey() {
		ConvKey parentKey = new ConvKey(major, "0");
		return parentKey;
	}
}
