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

import android.os.Build;
import android.test.AndroidTestCase;

import com.wilco375.onetwoauthenticate.util.FileUtilities;
import com.wilco375.onetwoauthenticate.util.Utilities;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for {@link Utilities}.
 *
 * @author sarvar@google.com (Sarvar Patel)
 */
public class FileUtilitiesTest extends AndroidTestCase {

    public void testMD5() {
        assertEquals("0fd3dbec9730101bff92acc820befc34", FileUtilities.getMD5("Test string"));
    }
}
