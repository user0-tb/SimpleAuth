package com.wilco375.onetwoauthenticate.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtilities {
    private EncryptionUtilities() {
    }

    public static byte[] encrypt(String toEncrypt, String encryptWith) {
        try {
            SecretKeySpec secret = new SecretKeySpec(getKeyForPassword(encryptWith), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            return cipher.doFinal(toEncrypt.getBytes());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return toEncrypt.getBytes();
        }
    }

    public static String decrypt(byte[] toDecrypt, String decryptWith) {
        try {
            SecretKeySpec secret = new SecretKeySpec(getKeyForPassword(decryptWith), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret);

            byte[] output = cipher.doFinal(toDecrypt);

            return new String(output);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return new String(toDecrypt);
        }
    }

    private static byte[] getKeyForPassword(String password) throws GeneralSecurityException {
        if (password.length() == 0) throw new GeneralSecurityException("Password is empty");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes());
    }
}
