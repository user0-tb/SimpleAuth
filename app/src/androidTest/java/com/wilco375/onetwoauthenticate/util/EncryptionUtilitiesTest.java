package com.wilco375.onetwoauthenticate.util;

import junit.framework.TestCase;

public class EncryptionUtilitiesTest extends TestCase {
    private static final String SOURCE = "Hello world!";
    private static final String KEY = "CorrectKey";
    private static final String WRONG_KEY = "WrongKey";

    public void testThatEncryptingWithPasswordEncryptsMessage() {
        byte[] encrypted = getEncrypted();
        assertFalse(new String(encrypted).equals(SOURCE));
    }

    public void testThatDecryptingWithPasswordDecryptsMessage() {
        byte[] encrypted = getEncrypted();
        assertEquals(SOURCE, EncryptionUtilities.decrypt(encrypted, KEY));
    }

    public void testThatDecryptingWithWrongPasswordDoesNotDecryptMessage() {
        byte[] encrypted = getEncrypted();
        assertFalse(SOURCE.equals(EncryptionUtilities.decrypt(encrypted, WRONG_KEY)));
    }

    public void testThatEncryptingWithEmptyPasswordDoesNotEncrypt() {
        byte[] encrypted = EncryptionUtilities.encrypt(SOURCE, "");
        assertEquals(SOURCE, new String(encrypted));
    }

    public void testThatDecryptingWithEmptyPasswordDoesNotDecrypt() {
        byte[] encrypted = EncryptionUtilities.encrypt(SOURCE, "");
        assertEquals(SOURCE, EncryptionUtilities.decrypt(encrypted, ""));
    }

    private byte[] getEncrypted() {
        return EncryptionUtilities.encrypt(SOURCE, KEY);
    }
}