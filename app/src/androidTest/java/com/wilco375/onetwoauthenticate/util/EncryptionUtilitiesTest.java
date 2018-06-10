package com.wilco375.onetwoauthenticate.util;

import junit.framework.TestCase;

public class EncryptionUtilitiesTest extends TestCase {
    public void testEncryptDecrypt() {
        String source = "Hello world!";
        String key = "1-2-Authenticate";

        String encrypted = EncryptionUtilities.encrypt(source, key);
        assertFalse(source.equals(encrypted));

        String decrypted = EncryptionUtilities.decrypt(encrypted, key);
        assertEquals(source, decrypted);
    }
}