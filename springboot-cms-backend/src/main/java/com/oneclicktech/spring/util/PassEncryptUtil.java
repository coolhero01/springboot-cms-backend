/**
 * 
 */
package com.oneclicktech.spring.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author kg255035
 *
 */
public class PassEncryptUtil {

	/**
	 * @param args
	 */

	public static String plainText = "SAMPLE TEST This is a plain text which need to be encrypted by Java AES 256 GCM Encryption Algorithm";
	public static final int AES_KEY_SIZE = 256;
	public static final int GCM_IV_LENGTH = 16;
	public static final int GCM_TAG_LENGTH = 16; 

	public static byte[] encrypt(byte[] plaintext, SecretKey key, byte[] IV) throws Exception {
		// Get Cipher Instance
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
  
		// Create GCMParameterSpec
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV);

		// Initialize Cipher for ENCRYPT_MODE
		cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);

		// Perform Encryption
		byte[] cipherText = cipher.doFinal(plaintext);
        byte[] cipherTextWithIv = ByteBuffer.allocate(IV.length + cipherText.length)
                .put(IV)
                .put(cipherText)
                .array();

		return cipherTextWithIv;
	}
	 
	public static byte[] convertHexToByteArr(String hexStr) {

		byte[] ans = new byte[hexStr.length() / 2];

		System.out.println("Hex String : " + hexStr);

		for (int i = 0; i < ans.length; i++) {
			int index = i * 2;
 			int val = Integer.parseInt(hexStr.substring(index, index + 2), 16);
			ans[i] = (byte) val;
		}

		// Printing the required Byte Array
		System.out.print("Byte Array : ");
		for (int i = 0; i < ans.length; i++) {
			System.out.print(ans[i] + " ");
		}
		
		return ans;
	}
	
	 public static byte[] hexStringToByteArray(String hex) {
	        int len = hex.length();
	        byte[] data = new byte[len / 2];
	        for (int i = 0; i < len; i += 2) {
	            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
	                                 + Character.digit(hex.charAt(i+1), 16));
	        }
	        return data;
	    }

	public static String decrypt(byte[] cipherText, SecretKey key, byte[] IV) throws Exception {
		// Get Cipher Instance
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

		// Create SecretKeySpec
		//SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

		// Create GCMParameterSpec
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV);

		// Initialize Cipher for DECRYPT_MODE
		cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

		// Perform Decryption
		byte[] decryptedText = cipher.doFinal(cipherText);

		return new String(decryptedText);
	}

	 

	
	public static SecretKey convertStringToSecretKeyto(String encodedKey) {
		byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
		SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		return originalKey;
	}

	public static byte[] getRandomNonceOrIV(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }
	 
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(AES_KEY_SIZE);
		
		String jsonTxt = FileUtils.readFileToString(new File("C:\\Temp\\SPAVI\\upay_json_req.json"));
		System.out.println(jsonTxt);
//		String jsonTxt = "{\r\n" + "  \"Amt\": \"194.25\",\r\n" + "  \"Email\": \"matthew.poako@gmail.com\",\r\n"
//				+ "  \"Mobile\": \"9171234567\",\r\n"
//				+ "  \"Redir\": \"https://pc-cms.uat.shakeys.solutions/springboot-cms-backend/pc/upay/processPayment\",\r\n"
//				+ "  \"References\": [\r\n" + "    {\r\n" + "      \"Id\": \"1\",\r\n"
//				+ "      \"Name\": \"orderName\",\r\n" + "      \"Val\": \"POR1021\"\r\n" + "    },\r\n" + "    {\r\n"
//				+ "      \"Id\": \"2\",\r\n" + "      \"Name\": \"orderId\",\r\n"
//				+ "      \"Val\": \"6719230411027\"\r\n" + "    },\r\n" + "    {\r\n" + "      \"Id\": \"3\",\r\n"
//				+ "      \"Name\": \"customerNo\",\r\n" + "      \"Val\": \"FR844PCF01505\"\r\n" + "    },\r\n"
//				+ "    {\r\n" + "      \"Id\": \"4\",\r\n" + "      \"Name\": \"customerName\",\r\n"
//				+ "      \"Val\": \"Matthew Poako\"\r\n" + "    },\r\n" + "    {\r\n" + "      \"Id\": \"5\",\r\n"
//				+ "      \"Name\": \"email\",\r\n" + "      \"Val\": \"matthew.poako@gmail.com\"\r\n" + "    },\r\n"
//				+ "    {\r\n" + "      \"Id\": \"6\",\r\n" + "      \"Name\": \"phone\",\r\n"
//				+ "      \"Val\": \"+639171234567\"\r\n" + "    },\r\n" + "    {\r\n" + "      \"Id\": \"7\",\r\n"
//				+ "      \"Name\": \"totalOrderPrice\",\r\n" + "      \"Val\": \"194.25\"\r\n" + "    }\r\n" + "  ]\r\n"
//				+ "}";

		// Generate Key
//		SecretKey key = keyGenerator.generateKey();
//
//		byte[] IV = new byte[GCM_IV_LENGTH];
//		SecureRandom random = new SecureRandom();
//		random.nextBytes(IV);
		
 
		String upayIndexLink = "https://ubotpsentry-tst1.outsystemsenterprise.com/UPAY";
		String billerUUID = "A4ED0006-A366-2C18-9D1F-7E707852D4B5";
	
		String pcAESHexKey = "2f3646344439432566372f2f313133653f2525233765372537232a2f6434422a";
		byte[] hexKeyByte = PassEncryptUtil.hexStringToByteArray(pcAESHexKey);
		//System.out.println(hexKeyByte.length);
		
		
//	  //  SecretKey pcSecretKey = PassEncryptUtil.convertStringToSecretKeyto(pcAESHexKey);
//		
//	    SecretKey pcSecretKey = new SecretKeySpec(hexKeyByte, "AES");
//	      
//		System.out.println("SecretKey key: " + key.getEncoded()); 
//		//System.out.println("Text to Encrypt: " + jsonTxt);
//		//byte[] cipherText = encrypt(jsonTxt.getBytes(), hexKeyByte, IV); 
//	    byte[] cipherText = encrypt(jsonTxt.getBytes(), pcSecretKey, IV);
//		System.out.println("Encrypted Text : " + Base64.getEncoder().encodeToString(cipherText));
//		
//		
//		String jsonCipherRequest =  Base64.getEncoder().encodeToString(cipherText);
//		System.out.println("jsonCipherRequest Length : " + jsonCipherRequest.length());
//		String urlGet = PCDataUtil.buildUPayPostLinkRequest(upayIndexLink, billerUUID, 
//				jsonCipherRequest);
//		
//		System.out.println("FINAL URL : " + urlGet); 
//		
//		 
		    //Security.addProvider(new BouncyCastleProvider());

	        // Secret key provided as hexadecimal string
	        String keyHex = "2f3646344439432566372f2f313133653f2525233765372537232a2f6434422a";
	        byte[] keyBytes = hexStringToByteArray(keyHex);
	        System.out.println("keyBytes Length : " + keyBytes.length);
	        SecretKey key = new SecretKeySpec(keyBytes, "AES");

	        // Generate a random IV
//	        byte[] ivBytes = new byte[16];
//	        SecureRandom random = new SecureRandom();
//	        random.nextBytes(ivBytes);
	        
	        byte[] ivBytes = PassEncryptUtil.getRandomNonceOrIV(16);
	        
	        System.out.println("ivBytes Length : " + ivBytes.length);
	        
	        // Encrypt plaintext using AES-GCM
	        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding" );
	        // Putting the IV at the front of the cipher text
	        // ivBytes, 0, ivBytes.length
	        
	        GCMParameterSpec gcmParams = new GCMParameterSpec(128, ivBytes);
	        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParams);
	        byte[] plaintext = jsonTxt.getBytes("UTF-8");
	        byte[] ciphertext = cipher.doFinal(plaintext);
	        
	        byte[] cipherTextWithIv = ByteBuffer.allocate(ivBytes.length + ciphertext.length)
	                .put(ivBytes)
	                .put(ciphertext)
	                .array();
	        
			String jsonCipherData =  org.apache.commons.codec.binary.Base64.encodeBase64String(cipherTextWithIv);
			 
			System.out.println("jsonCipherData Length : " + jsonCipherData.length());
			String urlGet = PCDataUtil.buildUPayPostLinkRequest(upayIndexLink, billerUUID, 
					jsonCipherData);
	        
			System.out.println("FINAL URL length: " + urlGet.length()); 
	 		System.out.println("FINAL URL : " + urlGet); 
		 
	//	String decryptedText = decrypt(cipherText, pcSecretKey, IV);
		//System.out.println("DeCrypted Text : " + decryptedText);
	}

}
