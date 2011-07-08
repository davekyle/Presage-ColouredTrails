/*
 * Created on 19-Jan-2005
 *
 */
package presage.util;

import java.util.StringTokenizer;

/**
 * @author Brendan
 * 
 */
public class StringParseTools {

	public static int wordCount(String text, String token){
		StringTokenizer parser = new StringTokenizer(text, token);
		return parser.countTokens();
	}
	
	public static String[] readTokens(String text, String token) {
		StringTokenizer parser = new StringTokenizer(text, token);
		int numTokens = parser.countTokens();
		String[] list = new String[numTokens];

		for (int i = 0; i < numTokens; i++) {
			list[i] = parser.nextToken();
		}
		return list;
	} //ends the readTokens method
	
	public static String extensionForFilename(String filename) {
		if (filename.lastIndexOf(".") == -1 )
			return "";
		else
			return filename.substring(filename.lastIndexOf(".")+1, filename.length());
	}

}