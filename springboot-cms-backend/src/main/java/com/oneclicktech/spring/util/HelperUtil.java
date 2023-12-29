/**
 * 
 */
package com.oneclicktech.spring.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;

/**
 * @author kg255035
 *
 */
public class HelperUtil extends StringUtils {

	private static final Logger logger = Logger.getLogger("HelperUtil");

	/**
	 * @param args
	 */

	public static void viewInJSON(Map<String, Object> dataMap) {
		if (Constants.TEST_ONLY) {
			if (MapUtils.isNotEmpty(dataMap)) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String jsonRequest = gson.toJson(dataMap, Map.class);
				logger.info("*****************************************************");
				logger.info(jsonRequest);
				logger.info("*****************************************************");
			}
		}
	}

	public static List<String> convertJSONFileToDBColumns(File jsonTxtFile) {
		BufferedReader reader = null;
		List<String> result = new ArrayList<>();
		try {
			reader = new BufferedReader(new FileReader(jsonTxtFile));
			String line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
				if (line != null) {
					String columnOnly = line.substring(0, line.indexOf(":")).replaceAll("\"", "");
					result.add(camelCaseToUnderscores(StringUtils.trimToEmpty(columnOnly)));
				}
			}
			reader.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);
		}

		return result;
	}

	public static List<String> getCamelColumnFromJSONFile(File jsonTxtFile) {

		BufferedReader reader = null;
		List<String> result = new ArrayList<>();
		try {
			reader = new BufferedReader(new FileReader(jsonTxtFile));
			String line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
				if (line != null) {
					String columnOnly = StringUtils
							.trimToEmpty(line.substring(0, line.indexOf(":")).replaceAll("\"", ""));

					result.add(columnOnly);
				}
			}
			reader.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);
		}

		return result;
	}

	public static List<String> convertCamelCaseToDBColumns(String jsonStr) {
		List<String> result = new ArrayList<>();
		String[] txtLines = jsonStr.split("\n");
		for (String txtLine : txtLines) {
			result.add(lowerCase(camelCaseToUnderScoreLowerCase(txtLine)));
		}
		return result;
	}

	public static List<String> convertJSONToDBColumns(String jsonStr) {
		BufferedReader reader = null;
		List<String> result = new ArrayList<>();
		try {
			reader = new BufferedReader(new StringReader(jsonStr));
			String line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
				if (line != null) {
					line = camelCaseToUnderScoreLowerCase(line);
					result.add(line);
				}

			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		return result;
	}

	public static String camelCaseToUnderScoreLowerCase(String camelCase) {
		String result = "";
		boolean prevLowerCase = false;
		for (int i = 0; i < camelCase.length(); i++) {
			char c = camelCase.charAt(i);
			if (!Character.isLetter(c))
				return camelCase;
			if (Character.isUpperCase(c)) {
				if (prevLowerCase)
					return camelCase;
				result += "_" + c;
				prevLowerCase = true;
			} else {
				result += Character.toUpperCase(c);
				prevLowerCase = false;
			}
		}
		return lowerCase(result);
	}

	public static String camelCaseToUnderscores(String camel) {
		String underscore;/* from w w w . ja v a 2 s. c o m */
		underscore = String.valueOf(Character.toLowerCase(camel.charAt(0)));
		for (int i = 1; i < camel.length(); i++) {
			underscore += Character.isLowerCase(camel.charAt(i)) ? String.valueOf(camel.charAt(i))
					: "_" + String.valueOf(Character.toLowerCase(camel.charAt(i)));
		}

		return underscore;
	}

	public static String lowCaseFirstLetter(String txt) {
		if (isNotBlank(txt)) {
			String firstLetter = lowerCase(String.valueOf(txt.charAt(0)));
			String fullTxt = new StringBuilder(firstLetter).append(txt.substring(1, txt.length())).toString();
			return fullTxt;
		}

		return txt;
	}

	public static String camelCaseStr(String text) {
		String[] words = text.split("[\\W_]+");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (i == 0) {
				word = word.isEmpty() ? word : word.toLowerCase();
			} else {
				word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
			}
			builder.append(word);
		}
		return builder.toString();

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		File jsonTxt = new File("/temp/SPAVI/item_json.txt");
//		List<String> txtLines = HelperUtil.getCamelColumnFromJSONFile(jsonTxt);
//		StringBuilder sbTxt = new StringBuilder();
//		for (String txtLine : txtLines) {
//			logger.info(txtLine);
//
//		}
//		logger.info("*************************************");
//		for (String txtLine : txtLines) {
//			logger.info(HelperUtil.lowCaseFirstLetter(txtLine));
//			sbTxt.append(HelperUtil.lowCaseFirstLetter(txtLine)).append("\n");
//		}
//
//		logger.info("***************************************");
//
//		txtLines = HelperUtil.convertCamelCaseToDBColumns(sbTxt.toString());
//		for (String txtLine : txtLines) {
//			logger.info(txtLine);
//		}
//
//		logger.info("***************************************");
//
//		// For DB Related column
		try {
			List<String> dbTxtLines = FileUtils.readLines(new File("/temp/SPAVI/db_fields.txt"));
			for (String dbLine : dbTxtLines) {
				System.out.println(HelperUtil.camelCaseStr(dbLine));
			}
			
			File headFile = new File("/temp/SPAVI/ratio_header.txt");
			 String metaDef = FileUtils.readFileToString(headFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		try {
//			File headFile = new File("‪‪C:\\Temp\\UHG\\xmlDataHeader.txt");
//			String  xmlDataHeader = FileUtils.readFileToString(headFile);
//
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
	
	}

}
