package com.wilco375.onetwoauthenticate.util;

import java.security.GeneralSecurityException;
import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtilities {
    private EncryptionUtilities() {
    }

    public static String encrypt(String toEncrypt, String encryptWith) {
        try {
            SecretKeySpec secret = new SecretKeySpec(encryptWith.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            byte[] output = cipher.doFinal(toEncrypt.getBytes());

            return Base64.encodeToString(output, Base64.DEFAULT);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return toEncrypt;
        }
    }

    public static String decrypt(String toDecrypt, String decryptWith) {
        try {
            SecretKeySpec secret = new SecretKeySpec(decryptWith.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret);

            byte[] output = cipher.doFinal(Base64.decode(toDecrypt, Base64.DEFAULT));

            return new String(output);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return toDecrypt;
        }
    }
}
