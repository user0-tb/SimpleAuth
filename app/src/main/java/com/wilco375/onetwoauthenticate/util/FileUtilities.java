/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Modified Copyright 2018 Wilco van Beijnum.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wilco375.onetwoauthenticate.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class for handling file system related methods, such as setting permissions.
 *
 * @author sarvar@google.com (Sarvar Patel)
 */
public class FileUtilities {

    private static String ICONS_DIR = "icons";

    /**
     * Hidden constructor to prevent instantiation.
     */
    private FileUtilities() {
    }

    public static void saveBitmap(Context context, String name, Bitmap bitmap) {
        ContextWrapper contextWrapper = new ContextWrapper(context);
        File dir = contextWrapper.getDir(ICONS_DIR, Context.MODE_PRIVATE);
        File path = new File(dir, getMD5(name) + ".png");
        try (FileOutputStream stream = new FileOutputStream(path)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getBitmap(Context context, String name) {
        ContextWrapper contextWrapper = new ContextWrapper(context);
        File dir = contextWrapper.getDir(ICONS_DIR, Context.MODE_PRIVATE);
        File path = new File(dir, getMD5(name) + ".png");
        if (path.exists()) {
            try (FileInputStream stream = new FileInputStream(path)) {
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected static String getMD5(String text) {
        byte[] bytes = text.getBytes();
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bytes);
            String md5 = new BigInteger(1, digest.digest()).toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            return md5;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e("FileUtils", "MD5 is an invalid MessageDigest algorithm");
            return null;
        }
    }
}
