package com.wilco375.onetwoauthenticate.util;

import junit.framework.TestCase;

public class EncryptionUtilitiesTest extends TestCase {
    public void testEncryptDecrypt() {
        String source = "Hello world!";
        String key = "1-2-Authenticate";

        byte[] encrypted = EncryptionUtilities.encrypt(source, key);
        assertFalse(new String(encrypted).equals(source));

        String decrypted = EncryptionUtilities.decrypt(encrypted, key);
        assertEquals(source, decrypted);
    }
}