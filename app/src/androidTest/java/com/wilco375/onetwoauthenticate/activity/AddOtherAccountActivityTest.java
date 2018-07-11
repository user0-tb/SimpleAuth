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

package com.wilco375.onetwoauthenticate.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.wilco375.onetwoauthenticate.R;
import com.wilco375.onetwoauthenticate.TestUtilities;
import com.wilco375.onetwoauthenticate.activity.AddOtherAccountActivity;
import com.wilco375.onetwoauthenticate.activity.AuthenticatorActivity;
import com.wilco375.onetwoauthenticate.activity.EnterKeyActivity;
import com.wilco375.onetwoauthenticate.testability.DependencyInjector;

/**
 * Unit tests for {@link AddOtherAccountActivity}.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class AddOtherAccountActivityTest
        extends ActivityInstrumentationTestCase2<AddOtherAccountActivity> {

    public AddOtherAccountActivityTest() {
        super(AddOtherAccountActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        DependencyInjector.resetForIntegrationTesting(getInstrumentation().getTargetContext());
        TestUtilities.withLaunchPreventingStartActivityListenerInDependencyResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        DependencyInjector.close();

        super.tearDown();
    }

    public void testScanBarcode() {
        TestUtilities.clickView(getInstrumentation(), getActivity().findViewById(R.id.scan_barcode));

        Intent actualIntent = TestUtilities.verifyWithTimeoutThatStartActivityAttemptedExactlyOnce();
        Intent expectedIntent = AuthenticatorActivity.getLaunchIntentActionScanBarcode(getActivity());
        assertEquals(expectedIntent.getAction(), actualIntent.getAction());
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    public void testManuallyAddAccount() {
        TestUtilities.clickView(
                getInstrumentation(), getActivity().findViewById(R.id.manually_add_account));

        Intent actualIntent = TestUtilities.verifyWithTimeoutThatStartActivityAttemptedExactlyOnce();
        assertEquals(
                new ComponentName(getActivity(), EnterKeyActivity.class),
                actualIntent.getComponent());
    }
}
