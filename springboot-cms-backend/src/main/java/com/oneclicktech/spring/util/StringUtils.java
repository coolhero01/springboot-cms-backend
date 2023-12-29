/**
 * 
 */
package com.oneclicktech.spring.util;

/**
 * @author Kerwin Gundran
 *
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

	/**
	 * @param args
	 */
	
	public static String cleanseData(String strData) {
		
		if (StringUtils.isNotEmpty(strData)) {
			String newData = strData.replaceAll("\t", "")
				    .replaceAll("\"", "")
				    .replaceAll("\r", "")
				    .replaceAll("\n", "");
			return newData;
		}
		
		return null;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String txtStr = "FOOD PAN (1/2\" x 4\")\r\n" + 
				"(PC00000037)";
		System.out.println(StringUtils.cleanseData(txtStr));
	}

}
