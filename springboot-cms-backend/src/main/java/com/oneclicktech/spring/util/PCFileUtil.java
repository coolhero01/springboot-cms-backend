/**
 * 
 */
package com.oneclicktech.spring.util;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author kg255035
 *
 */
public class PCFileUtil extends FileUtils {

	/**
	 * @param args
	 */

	public static boolean urlExists(String fileUrl) {
		HttpURLConnection connection = null;
		boolean fileExist = false;
		try {
			URL url = new URL(fileUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();
			fileExist = (responseCode == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (connection!=null)
 				connection.disconnect();
			} catch (Exception e) {
			}

		}
		return fileExist;
	}	
	
	public static String getItemNoFromImage(String imgName) {
		if (StringUtils.isNotBlank(imgName)) {
			return imgName.substring(0, imgName.indexOf('.'));
 		}
		return null;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String localPathDir = "C:\\Temp\\SPAVI\\prod_images\\new upload-20231004T135017Z-001\\new upload\\";
		String imgExtensions  = "png";
		File dirPath = new File(localPathDir);
		Collection<File> imgFiles = PCFileUtil.listFiles(dirPath, new String[] {imgExtensions}, false);
 
		for (File imgFile: imgFiles) {
			String itemNo = PCFileUtil.getItemNoFromImage(imgFile.getName());
			System.out.println(itemNo);
		}
	}

}
