
package presage;
/**
 * Created on 26-Sep-2003
 * Brendan Neville
 * Information Systems and Networks Research Group
 * Electrical and Electronic Engineering
 * Imperial College London
 */
public class ConvKeyGen {
	int currentMajorKey;

	public ConvKeyGen() {
		currentMajorKey = 0;
	} // ends constructor ConvKeyGen

	/** returns a ConvKey with MajorUnit = majorUnit and a Minor unit = minorUnit*/
	public ConvKey getKey(String majorUnit, String minorUnit) {
		ConvKey convKey = new ConvKey(majorUnit, minorUnit);
		return convKey;
	} // ends method getKey(String majorUnit, String minorUnit)

	/** returns a ConvKey with the next avalible MajorUnit and a MinorUnit = 0*/
	public ConvKey getKey() {
		ConvKey convKey;
		convKey = new ConvKey(Integer.toString(currentMajorKey), "0");
		currentMajorKey++;
		return convKey;
	} // ends method getKey()

} //ends ConvKeyGen
