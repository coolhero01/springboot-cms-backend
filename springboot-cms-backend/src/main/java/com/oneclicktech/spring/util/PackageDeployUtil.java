/**
 * 
 */
package com.oneclicktech.spring.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * @author kg255035
 *
 */
public class PackageDeployUtil {

	/**
	 * @param args
	 */
	private static final Logger logger = Logger.getLogger("PackageDeployUtil");
	public static List<String> filesListInDir = new ArrayList<String>();

	public static void convertBackupPackageEnv(String changeToEnv, Map<String, String> backupPaths,
			String backupOutputPath, boolean runBackup, List<Map<String, String>> searchMapList) {
		logger.info("*** PackageDeployUtil >> convertBackupPackageEnv >>  [START]");

		System.out.println("*****************************************************************************");
		System.out.println("*** PackageDeployUtil >> processing for ..... **** [ " + changeToEnv + " ] ****");
		System.out.println("*****************************************************************************");
		try {

			/*
			 * Backup All Workspaces/Folders
			 */
			logger.info("*** PackageDeployUtil >> convertBackupPackageEnv >>runBackup: " + runBackup);
			if (runBackup) {
				backupDirsZip(backupPaths, backupOutputPath);
			}

			switch (changeToEnv) {
			case "LOCAL":

				// Set Correct Application Properties
				FileUtils.copyFile(new File(
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\LOCAL\\application.properties"),
						new File(
								"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\application.properties"));

				break;
			case "LOCAL_TOMCAT":

				// Set Correct Application Properties
				FileUtils.copyFile(new File(
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\LOCAL_TOMCAT\\application.properties"),
						new File(
								"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\application.properties"));

				break;
			case "DEV":
				FileUtils.copyFile(new File(
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\DEV\\application.properties"),
						new File(
								"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\application.properties"));

				break;
			case "PROD":
				FileUtils.copyFile(new File(
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\PROD\\application.properties"),
						new File(
								"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\resources\\application.properties"));
				break;

			default:
				break;
			}

			// Search & Replace Text
			searchAndUpdateContent(changeToEnv);

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		logger.info("*** PackageDeployUtil >> convertBackupPackageEnv >>  [END]");
	}

	private static void searchAndUpdateContent(String changeToEnv) {
		logger.info("*** PackageDeployUtil >> searchAndUpdateContent >>  [START] ");

		try {
			Map<String, String> searchConstant = new HashMap<>();
			Map<String, String> searchUPAY_TEST = new HashMap<>();

			Map<String, String> searchControllers = new HashMap<>();
			List<Map<String, String>> searchMapList = new ArrayList<>();
			switch (changeToEnv) {
			case "LOCAL":
				searchConstant.put("fileToChange",
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring\\domain\\Constants.java");
				searchConstant.put("oldStr", "TEST_ONLY = false;");
				searchConstant.put("newStr", "TEST_ONLY = true;");
				searchMapList.add(searchConstant);

				searchUPAY_TEST.put("fileToChange",
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring\\domain\\Constants.java");
				searchUPAY_TEST.put("oldStr", "UPAY_TEST = false;");
				searchUPAY_TEST.put("newStr", "UPAY_TEST = true;");
				searchMapList.add(searchUPAY_TEST);

				break;
			case "DEV":

				searchConstant.put("fileToChange",
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring\\domain\\Constants.java");
				searchConstant.put("oldStr", "TEST_ONLY = true;");
				searchConstant.put("newStr", "TEST_ONLY = false;");
				searchMapList.add(searchConstant);

				searchUPAY_TEST.put("fileToChange",
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring\\domain\\Constants.java");
				searchUPAY_TEST.put("oldStr", "UPAY_TEST = true;");
				searchUPAY_TEST.put("newStr", "UPAY_TEST = false;");
				searchMapList.add(searchUPAY_TEST);

				break;
			case "PROD":
				searchConstant.put("fileToChange",
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring\\domain\\Constants.java");
				searchConstant.put("oldStr", "TEST_ONLY = true;");
				searchConstant.put("newStr", "TEST_ONLY = false;");
				searchMapList.add(searchConstant);

				searchUPAY_TEST.put("fileToChange",
						"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring\\domain\\Constants.java");
				searchUPAY_TEST.put("oldStr", "UPAY_TEST = true;");
				searchUPAY_TEST.put("newStr", "UPAY_TEST = false;");
				searchMapList.add(searchUPAY_TEST);
				break;

			default:
				break;
			}

			for (Map<String, String> searchMap : searchMapList) {
				String filePath = searchMap.get("fileToChange");
				String oldStr = searchMap.get("oldStr");
				String newStr = searchMap.get("newStr");

				modifyFile(filePath, oldStr, newStr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.info("*** PackageDeployUtil >> searchAndUpdateContent >>  [END] ");

	}

	private static void searchAndUpdateControllers(String changeToEnv) {
		logger.info("*** PackageDeployUtil >> searchAndUpdateControllers >>  [START] ");

		try {
			Map<String, String> searchConstant = new HashMap<>();
			Map<String, String> searchControllers = new HashMap<>();
			List<Map<String, String>> searchMapList = new ArrayList<>();
			switch (changeToEnv) {
			case "LOCAL":

				break;
			case "DEV":

				break;
			case "PROD":

				break;

			default:
				break;
			}

			List<File> fileControllers = getAllFilesInDir(
					"C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend\\src\\main\\java\\com\\oneclicktech\\spring",
					"Controllers");
			for (File fileCon : fileControllers) {
				System.out.println(fileCon.getAbsolutePath());

			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("*** PackageDeployUtil >> searchAndUpdateControllers >>  [END] ");

	}

	public static List<File> getAllFilesInDir(String startDir, String keyword) {
		List<File> allFiles = new ArrayList<>();
		File dir = new File(startDir);
		File[] files = dir.listFiles();

		if (files != null && files.length > 0) {
			for (File file : files) {
				// Check if the file is a directory
				if (file.isDirectory()) {
					// We will not print the directory name, just use it as a new
					// starting point to list files from
					allFiles.addAll(getAllFilesInDir(file.getAbsolutePath(), keyword));
				} else {
					if (file.getName().contains(keyword)) {
						// System.out.println(file.getName());
						allFiles.add(file);
					}

				}
			}
		}

		return allFiles;
	}

	public static void modifyFile(String filePath, String oldString, String newString) {
		File fileToBeModified = new File(filePath);

		String oldContent = "";

		BufferedReader buffReader = null;
		FileWriter fileWriter = null;
		try {

			buffReader = new BufferedReader(new FileReader(fileToBeModified));
			String line = buffReader.readLine();
			while (line != null) {
				oldContent = oldContent + line + System.lineSeparator();
				line = buffReader.readLine();
			}

			String newContent = oldContent.replaceAll(oldString, newString);
			fileWriter = new FileWriter(fileToBeModified);
			fileWriter.write(newContent);

			buffReader.close();
			fileWriter.flush();
			fileWriter.close();

		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(buffReader);
			IOUtils.closeQuietly(fileWriter);
		}
	}

	private static void renameFile(String fromFile, String toFile) {
		try {
			File file = new File(fromFile);
			// Create an object of the File class
			// Replace the file path with path of the directory
			File rename = new File(toFile);
			boolean renameSuccess = file.renameTo(rename);
			logger.info("*** PackageDeployUtil >> renameSuccess: " + renameSuccess);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

	}

	public static void backupFilesZip(List<String> backupPaths, String backupOutputPath) {

		try {

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

	}

	public static void backupDirsZip(Map<String, String> backupPaths, String backupOutputPath) {
		logger.info("*** PackageDeployUtil >> backupDirsZip >>  [START]");
		String timeName = DateUtil.getDateNowInPattern("MMddyyyy_HHmmss");

		for (Map.Entry<String, String> entry : backupPaths.entrySet()) {

			try {
				String zipFileName = new StringBuilder(backupOutputPath).append(entry.getKey()).append("_")
						.append(timeName).append(".zip").toString();
				logger.info("*** PackageDeployUtil >> backupDirsZip >>zipFileName: " + zipFileName);
				zipDirectory(new File(entry.getValue()), zipFileName);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}
		logger.info("*** PackageDeployUtil >> backupDirsZip >>  [END]");
	}

	private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		fis.close();
	}

	private static void zipDirectory(File dir, String zipDirName) {
		try {
			populateFilesList(dir);
			// now zip files one by one
			// create ZipOutputStream to write to the zip file
			FileOutputStream fos = new FileOutputStream(zipDirName);
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (String filePath : filesListInDir) {
				System.out.println("Zipping " + filePath);
				// for ZipEntry we need to keep only relative file path, so we used substring on
				// absolute path
				ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1, filePath.length()));
				zos.putNextEntry(ze);
				// read the file and write to ZipOutputStream
				FileInputStream fis = new FileInputStream(filePath);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
				fis.close();
			}
			zos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * This method populates all the files in a directory to a List
	 * 
	 * @param dir
	 * @throws IOException
	 */
	private static void populateFilesList(File dir) throws IOException {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isFile())
				filesListInDir.add(file.getAbsolutePath());
			else
				populateFilesList(file);
		}
	}

	/**
	 * This method compresses the single file to zip format
	 * 
	 * @param file
	 * @param zipFileName
	 */
	private static void zipSingleFile(File file, String zipFileName) {
		try {
			// create ZipOutputStream to write to the zip file
			FileOutputStream fos = new FileOutputStream(zipFileName);
			ZipOutputStream zos = new ZipOutputStream(fos);
			// add a new Zip Entry to the ZipOutputStream
			ZipEntry ze = new ZipEntry(file.getName());
			zos.putNextEntry(ze);
			// read the file and write to ZipOutputStream
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}

			// Close the zip entry to write to zip file
			zos.closeEntry();
			// Close resources
			zos.close();
			fis.close();
			fos.close();

			System.out.println(file.getCanonicalPath() + " is zipped to " + zipFileName);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Map<String, String> backupPaths = new LinkedHashMap<String, String>();
		backupPaths.put("springboot-cms-backend", "C:\\Temp\\SPAVI\\workspace\\springboot-cms-backend");
		backupPaths.put("pc-cms", "C:\\Temp\\SPAVI\\workspace\\angular-14-client\\src");

//		 PackageDeployUtil.backupDirsZip(backupPaths, 
//				 "C:\\Temp\\SPAVI\\local_backup\\");    
//		 boolean RUN_BACKUP_FILES = true;  
//		       
		boolean RUN_BACKUP_FILES = false;

		// ******************************
		// LOCAL / DEV (UAT) / PROD  
		// ******************************
		PackageDeployUtil.convertBackupPackageEnv("LOCAL", backupPaths, "C:\\Temp\\SPAVI\\local_backup\\",
				RUN_BACKUP_FILES, null);

//		 PackageDeployUtil.convertBackupPackageEnv("LOCAL", backupPaths, 
//				 "C:\\Temp\\SPAVI\\local_backup\\", RUN_BACKUP_FILES, null);

		// PackageDeployUtil.searchAndUpdateContent("LOCAL");

		// PackageDeployUtil.searchAndUpdateControllers("LOCAL");
	}

}
