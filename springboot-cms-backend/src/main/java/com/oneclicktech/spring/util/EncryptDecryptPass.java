package com.oneclicktech.spring.util;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptDecryptPass {
	private static final Logger logger = Logger.getLogger("EncryptDecryptPass");

	private static final String SECRET_KEY = "P0t4t0Corn3r!321";
	private static final String SALT = "Sh4k3ysP1zZ4987!";

	public static String decrypt(String strToDecrypt) {
		try {
			byte[] iv = {4, 5, 6, 9, 7, 8, 3, 2, 1, 0, 5, 4, 3, 2, 1, 0};
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}
	
	
	public static String encrypt(String strToEncrypt) {
	    try {
	    	
	      byte[] iv = {4, 5, 6, 9, 7, 8, 3, 2, 1, 0, 5, 4, 3, 2, 1, 0};
	      IvParameterSpec ivspec = new IvParameterSpec(iv);
	 
	      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
	      KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
	      SecretKey tmp = factory.generateSecret(spec);
	      SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
	 
	      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
	      return Base64.getEncoder()
	          .encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
	    } catch (Exception e) {
	      System.out.println("Error while encrypting: " + e.toString());
	    }
	    return null;
	  }
	public static boolean validPassword(String plainPass, String encryptPass) {
		String decryptPass = decrypt(encryptPass);
		return plainPass.equals(decryptPass);
	}

	public static void main(String[] args) throws Exception {
		/*
		 * create key If we need to generate a new key use a KeyGenerator If we have
		 * existing plaintext key use a SecretKeyFactory
		 */
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(128); // block size is 128bits
		SecretKey secretKey = keyGenerator.generateKey();

		/*
		 * Cipher Info Algorithm : for the encryption of electronic data mode of
		 * operation : to avoid repeated blocks encrypt to the same values. padding:
		 * ensuring messages are the proper length necessary for certain ciphers
		 * mode/padding are not used with stream cyphers.
		 */
		// cipher = Cipher.getInstance("AES"); // SunJCE provider AES algorithm,
		// mode(optional) and padding
		// schema(optional)

		String plainText = "AnaR@122584";
		System.out.println("Plain Text Before Encryption: " + plainText);

		String encryptedText = encrypt(plainText);
		System.out.println("Encrypted Text After Encryption: " + encryptedText);

		String decryptedText = decrypt(encryptedText);
		System.out.println("Decrypted Text After Decryption: " + decryptedText);
	}

}
