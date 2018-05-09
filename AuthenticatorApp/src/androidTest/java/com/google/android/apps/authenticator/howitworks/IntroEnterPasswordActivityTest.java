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

package com.google.android.apps.authenticator.howitworks;

import android.content.Intent;

import com.google.android.apps.authenticator.wizard.WizardPageActivityTestBase;

import java.io.Serializable;

/**
 * Unit tests for {@link IntroEnterPasswordActivity}.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class IntroEnterPasswordActivityTest
        extends WizardPageActivityTestBase<IntroEnterPasswordActivity, Serializable> {

    public IntroEnterPasswordActivityTest() {
        super(IntroEnterPasswordActivity.class);
    }

    public void testBackKeyFinishesActivity() throws Exception {
        assertBackKeyFinishesActivity();
    }

    public void testRightButtonStartsNextPage() {
        Intent intent = pressRightButtonAndCaptureActivityStartIntent();
        assertIntentForClassInTargetPackage(IntroEnterCodeActivity.class, intent);
        assertFalse(getActivity().isFinishing());
    }
}
