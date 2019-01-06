/*
 * Copyright 2009 Google Inc. All Rights Reserved.
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andrognito.flashbar.Flashbar;
import com.github.clans.fab.FloatingActionMenu;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.wilco375.onetwoauthenticate.BuildConfig;
import com.wilco375.onetwoauthenticate.R;
import com.wilco375.onetwoauthenticate.Snackbar;
import com.wilco375.onetwoauthenticate.database.AccountDb;
import com.wilco375.onetwoauthenticate.database.AccountDb.OtpType;
import com.wilco375.onetwoauthenticate.licensing.License;
import com.wilco375.onetwoauthenticate.otp.OtpSource;
import com.wilco375.onetwoauthenticate.otp.OtpSourceException;
import com.wilco375.onetwoauthenticate.otp.totp.TotpClock;
import com.wilco375.onetwoauthenticate.otp.totp.TotpCountdownTask;
import com.wilco375.onetwoauthenticate.otp.totp.TotpCounter;
import com.wilco375.onetwoauthenticate.testability.DependencyInjector;
import com.wilco375.onetwoauthenticate.testability.TestableActivity;
import com.wilco375.onetwoauthenticate.util.EncryptionUtilities;
import com.wilco375.onetwoauthenticate.util.FileUtilities;
import com.wilco375.onetwoauthenticate.util.Utilities;
import com.wilco375.onetwoauthenticate.view.CountdownIndicator;
import com.yydcdut.sdlv.SlideAndDragListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The main activity that displays usernames and codes
 *
 * @author sweis@google.com (Steve Weis)
 * @author adhintz@google.com (Drew Hintz)
 * @author cemp@google.com (Cem Paya)
 * @author klyubin@google.com (Alex Klyubin)
 */
public class AuthenticatorActivity extends TestableActivity {

    /**
     * The tag for log messages
     */
    private static final String LOCAL_TAG = "AuthenticatorActivity";
    private static final long VIBRATE_DURATION = 200L;

    /**
     * Frequency (milliseconds) with which TOTP countdown indicators are updated.
     */
    private static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 100;

    /**
     * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
     * generated for an account until the moment the next code can be generated for the account.
     * This is to prevent the user from generating too many HOTP codes in a short period of time.
     */
    private static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;

    /**
     * The maximum amount of time (milliseconds) for which a HOTP code is displayed after it's been
     * generated.
     */
    private static final long HOTP_DISPLAY_TIMEOUT = 2 * 60 * 1000;

    // @VisibleForTesting
    static final int DIALOG_ID_UNINSTALL_OLD_APP = 12;

    // @VisibleForTesting
    static final int DIALOG_ID_SAVE_KEY = 13;

    static final int PERMISSION_WRITE_STORAGE_IMPORT = 1;
    static final int PERMISSION_WRITE_STORAGE_EXPORT = 2;

    /**
     * Intent action to that tells this Activity to initiate the scanning of barcode to add an
     * account.
     */
    // @VisibleForTesting
    static final String ACTION_SCAN_BARCODE =
            AuthenticatorActivity.class.getName() + ".ScanBarcode";

    private View mContentNoAccounts;
    private View mContentAccountsPresent;
    private SlideAndDragListView mUserList;
    private PinListAdapter mUserAdapter;
    private ArrayList<PinInfo> mUsers = new ArrayList<>();
    private View mCustomizeView;

    /**
     * Counter used for generating TOTP verification codes.
     */
    private TotpCounter mTotpCounter;

    /**
     * Clock used for generating TOTP verification codes.
     */
    private TotpClock mTotpClock;

    /**
     * Task that periodically notifies this activity about the amount of time remaining until
     * the TOTP codes refresh. The task also notifies this activity when TOTP codes refresh.
     */
    private TotpCountdownTask mTotpCountdownTask;

    /**
     * Phase of TOTP countdown indicators. The phase is in {@code [0, 1]} with {@code 1} meaning
     * full time step remaining until the code refreshes, and {@code 0} meaning the code is refreshing
     * right now.
     */
    private double mTotpCountdownPhase;
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;

    /**
     * Key under which the {@link #mOldAppUninstallIntent} is stored in the instance state
     * {@link Bundle}.
     */
    private static final String KEY_OLD_APP_UNINSTALL_INTENT = "oldAppUninstallIntent";

    /**
     * {@link Intent} for uninstalling the "old" app or {@code null} if not known/available.
     * <p>
     * <p>
     * Note: this field is persisted in the instance state {@link Bundle}. We need to resolve to this
     * error-prone mechanism because createDialog on Eclair doesn't take parameters. Once Froyo is
     * the minimum targetted SDK, this contrived code can be removed.
     */
    private Intent mOldAppUninstallIntent;

    /**
     * Key under which the {@link #mSaveKeyDialogParams} is stored in the instance state
     * {@link Bundle}.
     */
    private static final String KEY_SAVE_KEY_DIALOG_PARAMS = "saveKeyDialogParams";

    /**
     * Parameters to the save key dialog (DIALOG_ID_SAVE_KEY).
     * <p>
     * <p>
     * Note: this field is persisted in the instance state {@link Bundle}. We need to resolve to this
     * error-prone mechanism because createDialog on Eclair doesn't take parameters. Once Froyo is
     * the minimum targetted SDK, this contrived code can be removed.
     */
    private SaveKeyDialogParams mSaveKeyDialogParams;

    /**
     * Whether this activity is currently displaying a confirmation prompt in response to the
     * "save key" Intent.
     */
    private boolean mSaveKeyIntentConfirmationInProgress;

    private static final String OTP_SCHEME = "otpauth";
    private static final String TOTP = "totp"; // time-based
    private static final String HOTP = "hotp"; // counter-based
    private static final String SECRET_PARAM = "secret";
    private static final String COUNTER_PARAM = "counter";
    // @VisibleForTesting
    public static final int CHECK_KEY_VALUE_ID = 0;
    // @VisibleForTesting
    public static final int RENAME_ID = 1;
    // @VisibleForTesting
    public static final int REMOVE_ID = 2;
    // @VisibleForTesting
    static final int COPY_TO_CLIPBOARD_ID = 3;
    // @VisibleForTesting
    static final int CUSTOMIZE_ID = 4;
    // @VisibleForTesting
    static final int SCAN_REQUEST = 31337;
    // @VisibleForTesting
    static final int CHOOSE_ICON = 31338;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new License().checkLicense(this);

        mAccountDb = DependencyInjector.getAccountDb();
        mOtpProvider = DependencyInjector.getOtpProvider();

        // Use a different (longer) title from the one that's declared in the manifest (and the one that
        // the Android launcher displays).
        setTitle(R.string.app_name);

        mTotpCounter = mOtpProvider.getTotpCounter();
        mTotpClock = mOtpProvider.getTotpClock();

        setContentView(R.layout.main);

        // restore state on screen rotation
        Object savedState = getLastCustomNonConfigurationInstance();
        if (savedState != null) {
            mUsers = (ArrayList<PinInfo>) savedState;
            // Re-enable the Get Code buttons on all HOTP accounts, otherwise they'll stay disabled.
            for (PinInfo account : mUsers) {
                if (account.isHotp) {
                    account.hotpCodeGenerationAllowed = true;
                }
            }
        }

        if (savedInstanceState != null) {
            mOldAppUninstallIntent = savedInstanceState.getParcelable(KEY_OLD_APP_UNINSTALL_INTENT);
            mSaveKeyDialogParams =
                    (SaveKeyDialogParams) savedInstanceState.getSerializable(KEY_SAVE_KEY_DIALOG_PARAMS);
        }

        mUserList = findViewById(R.id.user_list);
        mContentNoAccounts = findViewById(R.id.content_no_accounts);
        mContentAccountsPresent = findViewById(R.id.content_accounts_present);
        mContentNoAccounts.setVisibility((mUsers.size() > 0) ? View.GONE : View.VISIBLE);
        mContentAccountsPresent.setVisibility((mUsers.size() > 0) ? View.VISIBLE : View.GONE);

        mUserAdapter = new PinListAdapter(this, R.layout.user_row, mUsers);

        mUserList.setVisibility(View.GONE);
        mUserList.setMenu(new com.yydcdut.sdlv.Menu(false, 0));
        mUserList.setAdapter(mUserAdapter);
        mUserList.setDragOnLongPress(false);
        mUserList.setOnDragDropListener(new SlideAndDragListView.OnDragDropListener() {
            @Override
            public void onDragViewStart(int beginPosition) {

            }

            @Override
            public void onDragDropViewMoved(int fromPosition, int toPosition) {
                PinInfo pinInfo = mUsers.remove(fromPosition);
                mUsers.add(toPosition, pinInfo);
            }

            @Override
            public void onDragViewDown(int finalPosition) {
                String[] usernames = new String[mUsers.size()];
                for (int i = 0; i < mUsers.size(); i++) {
                    usernames[i] = mUsers.get(i).user;
                }
                // Save order to DB
                mAccountDb.reorder(usernames);

                updateShortcuts(AuthenticatorActivity.this);
            }
        });

        mUserList.setOnItemClickListener((unusedParent, row, unusedPosition, unusedId) -> {
            TextView pinTextView = row.findViewById(R.id.pin_value);
            String pin = pinTextView.getText().toString();
            if (!pin.equals(getString(R.string.empty_pin))) {
                copyPinToClipboard(pin);
            }
        });

        FloatingActionMenu fam = findViewById(R.id.add_account_fab);
        if (!BuildConfig.PRO) {
            fam.setOnMenuButtonClickListener(view -> {
                if (fam.isOpened()) {
                    fam.close(true);
                } else {
                    if (mUsers.size() < 8) {
                        fam.open(true);
                    } else {
                        Flashbar.Builder snackbar = Snackbar.make(
                                this,
                                Snackbar.Type.ERROR,
                                getString(R.string.buy_pro_text, 8)
                        );
                        snackbar.positiveActionText(R.string.buy_pro_text);
                        snackbar.positiveActionTapListener(bar -> {
                            try {
                                startActivity(
                                        new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=com.wilco375.onetwoauthenticatepro"))
                                );
                            } catch (ActivityNotFoundException e) {
                                startActivity(
                                        new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/apps/details?id=com.wilco375.onetwoauthenticatepro"))
                                );
                            }
                        });
                        snackbar.show();
                    }
                }
            });
        }

        findViewById(R.id.add_account_manual).setOnClickListener(view -> {
            fam.close(true);
            manuallyEnterAccountDetails();
        });
        findViewById(R.id.add_account_scan).setOnClickListener(view -> {
            fam.close(true);
            scanBarcode();
        });

        if (savedInstanceState == null) {
            // This is the first time this Activity is starting (i.e., not restoring previous state which
            // was saved, for example, due to orientation change)
            DependencyInjector.getOptionalFeatures().onAuthenticatorActivityCreated(this);
            handleIntent(getIntent());
        }
    }

    /**
     * Reacts to the {@link Intent} that started this activity or arrived to this activity without
     * restarting it (i.e., arrived via {@link #onNewIntent(Intent)}). Does nothing if the provided
     * intent is {@code null}.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (ACTION_SCAN_BARCODE.equals(action)) {
            scanBarcode();
        } else if (intent.getData() != null) {
            interpretScanResult(intent.getData(), true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_OLD_APP_UNINSTALL_INTENT, mOldAppUninstallIntent);
        outState.putSerializable(KEY_SAVE_KEY_DIALOG_PARAMS, mSaveKeyDialogParams);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mUsers;  // save state of users and currently displayed PINs
    }

    // Because this activity is marked as singleTop, new launch intents will be
    // delivered via this API instead of onResume().
    // Override here to catch otpauth:// URL being opened from QR code reader.
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(getString(R.string.app_name), LOCAL_TAG + ": onNewIntent");
        handleIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateCodesAndStartTotpCountdownTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(getString(R.string.app_name), LOCAL_TAG + ": onResume");
    }

    @Override
    protected void onStop() {
        stopTotpCountdownTask();

        super.onStop();
    }

    private void updateCodesAndStartTotpCountdownTask() {
        stopTotpCountdownTask();

        mTotpCountdownTask =
                new TotpCountdownTask(mTotpCounter, mTotpClock, TOTP_COUNTDOWN_REFRESH_PERIOD);
        mTotpCountdownTask.setListener(new TotpCountdownTask.Listener() {
            @Override
            public void onTotpCountdown(long millisRemaining) {
                if (isFinishing()) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return;
                }
                setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining);
            }

            @Override
            public void onTotpCounterValueChanged() {
                if (isFinishing()) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return;
                }
                refreshVerificationCodes();
            }
        });

        mTotpCountdownTask.startAndNotifyListener();
    }

    private void stopTotpCountdownTask() {
        if (mTotpCountdownTask != null) {
            mTotpCountdownTask.stop();
            mTotpCountdownTask = null;
        }
    }

    /**
     * Display list of user emails and updated pin codes.
     */
    protected void refreshUserList() {
        refreshUserList(false);
    }

    private void setTotpCountdownPhase(double phase) {
        mTotpCountdownPhase = phase;
        updateCountdownIndicators();
    }

    private void setTotpCountdownPhaseFromTimeTillNextValue(long millisRemaining) {
        setTotpCountdownPhase(
                ((double) millisRemaining) / Utilities.secondsToMillis(mTotpCounter.getTimeStep()));
    }

    private void refreshVerificationCodes() {
        refreshUserList();
        setTotpCountdownPhase(1.0);
    }

    private void updateCountdownIndicators() {
        for (int i = 0, len = mUserList.getChildCount(); i < len; i++) {
            View listEntry = mUserList.getChildAt(i);
            CountdownIndicator indicator =
                    listEntry.findViewById(R.id.countdown_icon);
            if (indicator != null) {
                indicator.setPhase(mTotpCountdownPhase);
            }
        }
    }

    /**
     * Display list of user emails and updated pin codes.
     *
     * @param isAccountModified if true, force full refresh
     */
    // @VisibleForTesting
    public void refreshUserList(boolean isAccountModified) {
        ArrayList<String> usernames = new ArrayList<>();
        mAccountDb.getNames(usernames);

        int userCount = usernames.size();

        if (userCount > 0) {
            boolean newListRequired = isAccountModified || mUsers.size() != userCount;
            if (newListRequired) {
                mUsers.clear();
                for (int i = 0; i < userCount; i++) {
                    mUsers.add(null);
                }
            }

            for (int i = 0; i < userCount; ++i) {
                String user = usernames.get(i);
                try {
                    computeAndDisplayPin(user, i, false);
                } catch (OtpSourceException ignored) {
                }
            }

            if (newListRequired) {
                // Make the list display the data from the newly created array of accounts
                // This forces the list to scroll to top.
                mUserAdapter = new PinListAdapter(this, R.layout.user_row, mUsers);
                mUserList.setAdapter(mUserAdapter);
            }

            mUserAdapter.notifyDataSetChanged();

            if (mUserList.getVisibility() != View.VISIBLE) {
                mUserList.setVisibility(View.VISIBLE);
                registerForContextMenu(mUserList);
            }
        } else {
            mUsers.clear(); // clear any existing user PIN state
            mUserList.setVisibility(View.GONE);
        }

        // Display the list of accounts if there are accounts, otherwise display a
        // different layout explaining the user how this app works and providing the user with an easy
        // way to add an account.
        mContentNoAccounts.setVisibility((mUsers.size() > 0) ? View.GONE : View.VISIBLE);
        mContentAccountsPresent.setVisibility((mUsers.size() > 0) ? View.VISIBLE : View.GONE);
    }

    /**
     * Computes the PIN and saves it in mUsers. This currently runs in the UI
     * thread so it should not take more than a second or so. If necessary, we can
     * move the computation to a background thread.
     *
     * @param user        the user email to display with the PIN
     * @param position    the index for the screen of this user and PIN
     * @param computeHotp true if we should increment counter and display new hotp
     */
    public void computeAndDisplayPin(String user, int position,
                                     boolean computeHotp) throws OtpSourceException {

        PinInfo currentPin;
        if (mUsers.get(position) != null) {
            currentPin = mUsers.get(position); // existing PinInfo, so we'll update it
        } else {
            currentPin = new PinInfo();
            currentPin.pin = getString(R.string.empty_pin);
            currentPin.hotpCodeGenerationAllowed = true;
            Integer color = mAccountDb.getColor(user);
            if (color == null)
                currentPin.color = getResources().getColor(R.color.theme_color);
            else
                currentPin.color = color;

            Bitmap bitmap = FileUtilities.getBitmap(getApplicationContext(), user);
            if (bitmap != null) {
                // Resize image
                int size = Utilities.dpToPx(70);
                currentPin.image = Bitmap.createScaledBitmap(bitmap, size, size, false);
                bitmap.recycle();
            }
        }

        OtpType type = mAccountDb.getType(user);
        currentPin.isHotp = (type == OtpType.HOTP);

        currentPin.user = user;

        if (!currentPin.isHotp || computeHotp) {
            // Always safe to recompute, because this code path is only
            // reached if the account is:
            // - Time-based, in which case getNextCode() does not change state.
            // - Counter-based (HOTP) and computeHotp is true.
            currentPin.pin = mOtpProvider.getNextCode(user);
            currentPin.hotpCodeGenerationAllowed = true;
        }

        mUsers.set(position, currentPin);
    }

    /**
     * Parses a secret value from a URI. The format will be:
     * <p>
     * otpauth://totp/user@example.com?secret=FFF...
     * otpauth://hotp/user@example.com?secret=FFF...&counter=123
     *
     * @param uri               The URI containing the secret key
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void parseSecret(Uri uri, boolean confirmBeforeSave) {
        final String scheme = uri.getScheme().toLowerCase();
        final String path = uri.getPath();
        final String authority = uri.getAuthority();
        final String user;
        final String secret;
        final OtpType type;
        final Integer counter;

        if (!OTP_SCHEME.equals(scheme)) {
            Log.e(getString(R.string.app_name), LOCAL_TAG + ": Invalid or missing scheme in uri");
            createDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        switch (authority) {
            case TOTP:
                type = OtpType.TOTP;
                counter = AccountDb.DEFAULT_HOTP_COUNTER; // only interesting for HOTP

                break;
            case HOTP:
                type = OtpType.HOTP;
                String counterParameter = uri.getQueryParameter(COUNTER_PARAM);
                if (counterParameter != null) {
                    try {
                        counter = Integer.parseInt(counterParameter);
                    } catch (NumberFormatException e) {
                        Log.e(getString(R.string.app_name), LOCAL_TAG + ": Invalid counter in uri");
                        createDialog(Utilities.INVALID_QR_CODE);
                        return;
                    }
                } else {
                    counter = AccountDb.DEFAULT_HOTP_COUNTER;
                }
                break;
            default:
                Log.e(getString(R.string.app_name), LOCAL_TAG + ": Invalid or missing authority in uri");
                createDialog(Utilities.INVALID_QR_CODE);
                return;
        }

        user = validateAndGetUserInPath(path);
        if (user == null) {
            Log.e(getString(R.string.app_name), LOCAL_TAG + ": Missing user id in uri");
            createDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        secret = uri.getQueryParameter(SECRET_PARAM);

        if (secret == null || secret.length() == 0) {
            Log.e(getString(R.string.app_name), LOCAL_TAG +
                    ": Secret key not found in URI");
            createDialog(Utilities.INVALID_SECRET_IN_QR_CODE);
            return;
        }

        if (AccountDb.getSigningOracle(secret) == null) {
            Log.e(getString(R.string.app_name), LOCAL_TAG + ": Invalid secret key");
            createDialog(Utilities.INVALID_SECRET_IN_QR_CODE);
            return;
        }

        if (secret.equals(mAccountDb.getSecret(user)) &&
                counter == mAccountDb.getCounter(user) &&
                type == mAccountDb.getType(user)) {
            return;  // nothing to update.
        }

        if (confirmBeforeSave) {
            mSaveKeyDialogParams = new SaveKeyDialogParams(user, secret, type, counter);
            createDialog(DIALOG_ID_SAVE_KEY);
        } else {
            saveSecretAndRefreshUserList(user, secret, null, type, counter);
        }
    }

    private static String validateAndGetUserInPath(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        // path is "/user", so remove leading "/", and trailing white spaces
        String user = path.substring(1).trim();
        if (user.length() == 0) {
            return null; // only white spaces.
        }
        return user;
    }

    /**
     * Saves the secret key to local storage on the phone and updates the displayed account list.
     *
     * @param user         the user email address. When editing, the new user email.
     * @param secret       the secret key
     * @param originalUser If editing, the original user email, otherwise null.
     * @param type         hotp vs totp
     * @param counter      only important for the hotp type
     */
    private void saveSecretAndRefreshUserList(String user, String secret,
                                              String originalUser, OtpType type, Integer counter, boolean showNotification) {
        if (saveSecret(this, user, secret, originalUser, type, counter, showNotification)) {
            refreshUserList(true);
        }
    }

    private void saveSecretAndRefreshUserList(String user, String secret,
                                              String originalUser, OtpType type, Integer counter) {
        saveSecretAndRefreshUserList(user, secret, originalUser, type, counter, true);
    }

    /**
     * Saves the secret key to local storage on the phone.
     *
     * @param user         the user email address. When editing, the new user email.
     * @param secret       the secret key
     * @param originalUser If editing, the original user email, otherwise null.
     * @param type         hotp vs totp
     * @param counter      only important for the hotp type
     * @return {@code true} if the secret was saved, {@code false} otherwise.
     */
    static boolean saveSecret(Activity context, String user, String secret,
                              String originalUser, OtpType type, Integer counter, boolean showNotification) {
        if (originalUser == null) {  // new user account
            originalUser = user;
        }
        if (secret != null) {
            AccountDb accountDb = DependencyInjector.getAccountDb();
            accountDb.update(user, secret, originalUser, type, counter);
            DependencyInjector.getOptionalFeatures().onAuthenticatorActivityAccountSaved(context, user);

            if (showNotification) {
                Snackbar.show(context, Snackbar.Type.SUCCESS, R.string.secret_saved);
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                        .vibrate(VIBRATE_DURATION);
            }

            updateShortcuts(context);

            return true;
        } else {
            Log.e(LOCAL_TAG, "Trying to save an empty secret key");

            Snackbar.show(context, Snackbar.Type.ERROR, R.string.error_empty_secret);
            return false;
        }
    }

    static boolean saveSecret(Activity context, String user, String secret,
                              String originalUser, OtpType type, Integer counter) {
        return saveSecret(context, user, secret, originalUser, type, counter, true);
    }

    /**
     * Update the dynamic shortcuts to show the top items
     */
    private static void updateShortcuts(Context context) {
        if (Build.VERSION.SDK_INT < 25 || !BuildConfig.PRO) return;

        new Thread(() -> {
            AccountDb accountDb = DependencyInjector.getAccountDb();
            List<String> names = new ArrayList<>();
            accountDb.getNames(names);

            ShortcutManager sm = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            if (sm == null) return;

            int maxShortcuts = sm.getMaxShortcutCountPerActivity();
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            for (int i = 0; i < names.size() && i < maxShortcuts; i++) {
                String name = names.get(i);
                Icon icon;
                Bitmap foreground = FileUtilities.getBitmap(context, name);
                if (foreground != null) {
                    Drawable background = ContextCompat.getDrawable(context, R.drawable.ic_shortcut_bg);
                    Bitmap bitmapIcon = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmapIcon);

                    // Draw background
                    background.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    background.draw(canvas);

                    // Draw foreground
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    canvas.drawBitmap(foreground,
                            new Rect(0, 0, foreground.getWidth(), foreground.getHeight()),
                            new Rect(12, 12, 36, 36),
                            paint
                    );

                    icon = Icon.createWithBitmap(bitmapIcon);
                } else {
                    icon = Icon.createWithResource(context, R.drawable.ic_shortcut_key);
                }
                shortcuts.add(
                        new ShortcutInfo.Builder(context, "code-"+i)
                                .setShortLabel(name.length() > 10 ? name.substring(0, 10) : name)
                                .setLongLabel(name.length() > 20 ? names.get(i).substring(0, 20) : name)
                                .setIcon(icon)
                                .setIntent(
                                        new Intent(context, CopyKeyActivity.class)
                                                .setAction(Intent.ACTION_VIEW)
                                                .putExtra("name", name)
                                )
                                .build()
                );
            }
            sm.setDynamicShortcuts(shortcuts);
        }).start();
    }

    /**
     * Converts user list ordinal id to user email
     */
    private String idToEmail(long id) {
        return mUsers.get((int) id).user;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        String user = idToEmail(info.id);
        OtpType type = mAccountDb.getType(user);
        menu.setHeaderTitle(user);
        menu.add(0, COPY_TO_CLIPBOARD_ID, 0, R.string.copy_to_clipboard);
        // Option to display the check-code is only available for HOTP accounts.
        if (type == OtpType.HOTP) {
            menu.add(0, CHECK_KEY_VALUE_ID, 0, R.string.check_code_menu_item);
        }
        menu.add(0, CUSTOMIZE_ID, 0, R.string.customize);
        menu.add(0, RENAME_ID, 0, R.string.rename);
        menu.add(0, REMOVE_ID, 0, R.string.context_menu_remove_account);
    }

    private void copyPinToClipboard(String pin) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(getString(R.string.app_name), pin);
        clipboard.setPrimaryClip(clipData);

        Snackbar.show(this, Snackbar.Type.SUCCESS, R.string.pin_copied);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Intent intent;
        final String user = idToEmail(info.id); // final so listener can see value
        switch (item.getItemId()) {
            case COPY_TO_CLIPBOARD_ID:
                copyPinToClipboard(mUsers.get((int) info.id).pin);
                return true;
            case CHECK_KEY_VALUE_ID:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(this, CheckCodeActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                return true;
            case CUSTOMIZE_ID:
                mCustomizeView = getLayoutInflater().inflate(R.layout.customize,
                        findViewById(R.id.customize_root));

                View customizeColor = mCustomizeView.findViewById(R.id.customize_color);
                Integer dbColor = mAccountDb.getColor(user);
                if (dbColor == null) {
                    dbColor = getResources().getColor(R.color.theme_color);
                }
                int color = dbColor; // Needs to be effectively final in lambda
                customizeColor.setOnClickListener(view -> {
                    ColorPicker colorPicker = new ColorPicker(AuthenticatorActivity.this, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
                    colorPicker.show();
                    colorPicker.setCallback(newColor -> {
                        customizeColor.setBackgroundColor(newColor);
                        colorPicker.dismiss();
                    });
                });
                customizeColor.setBackgroundColor(color);

                ImageView customizeIcon = mCustomizeView.findViewById(R.id.customize_icon);
                Bitmap icon = FileUtilities.getBitmap(getApplicationContext(), user);
                customizeIcon.setOnClickListener(view -> {
                    Intent i = new Intent()
                            .setType("image/*")
                            .setAction(Intent.ACTION_GET_CONTENT)
                            .putExtra("return-data", true)
                            .putExtra("scale", true)
                            .putExtra("outputX", 256)
                            .putExtra("outputY", 256);
                    Intent chooser = Intent.createChooser(i, getString(R.string.icon));
                    startActivityForResult(chooser, CHOOSE_ICON);
                });
                customizeIcon.setImageBitmap(icon);

                new AlertDialog.Builder(this)
                        .setTitle(R.string.customize)
                        .setView(mCustomizeView)
                        .setPositiveButton(R.string.submit, (dialogInterface, index) -> {
                            PinInfo pinInfoToUpdate = null;
                            for (PinInfo pinInfo : mUsers) {
                                if (pinInfo.user.equals(user)) {
                                    pinInfoToUpdate = pinInfo;
                                    break;
                                }
                            }

                            // Save color to DB
                            Drawable colorBackground = customizeColor.getBackground();
                            if (colorBackground instanceof ColorDrawable) {
                                int newColor = ((ColorDrawable) colorBackground).getColor();
                                if (newColor != color) {
                                    mAccountDb.update(user,
                                            mAccountDb.getSecret(user), user,
                                            mAccountDb.getType(user),
                                            mAccountDb.getCounter(user),
                                            null,
                                            newColor);
                                    pinInfoToUpdate.color = newColor;
                                }
                            }

                            // Save icon to storage
                            Drawable iconDrawable = customizeIcon.getDrawable();
                            if (iconDrawable instanceof BitmapDrawable) {
                                Bitmap newIcon = ((BitmapDrawable) iconDrawable).getBitmap();
                                if (newIcon != icon && newIcon != null) {
                                    FileUtilities.saveBitmap(getApplicationContext(), user, newIcon);
                                    pinInfoToUpdate.image = newIcon;
                                }
                            }

                            mUserAdapter.notifyDataSetChanged();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case RENAME_ID:
                final Activity context = this; // final so listener can see value
                final View frame = getLayoutInflater().inflate(R.layout.rename,
                        findViewById(R.id.rename_root));
                final EditText nameEdit = frame.findViewById(R.id.rename_edittext);
                nameEdit.setText(user);
                new AlertDialog.Builder(this)
                        .setTitle(String.format(getString(R.string.rename_message), user))
                        .setView(frame)
                        .setPositiveButton(R.string.submit,
                                this.getRenameClickListener(context, user, nameEdit))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case REMOVE_ID:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.remove_account_dialog_title, user))
                        .setIcon(R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.remove_account_dialog_button_remove,
                                (dialog, whichButton) -> {
                                    mAccountDb.delete(user);
                                    refreshUserList(true);
                                }
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private DialogInterface.OnClickListener getRenameClickListener(final Activity context,
                                                                   final String user, final EditText nameEdit) {
        return (dialog, whichButton) -> {
            String newName = nameEdit.getText().toString();
            if (!newName.equals(user)) {
                if (mAccountDb.nameExists(newName)) {
                    Snackbar.show(context, Snackbar.Type.ERROR, R.string.error_exists);
                } else {
                    saveSecretAndRefreshUserList(newName,
                            mAccountDb.getSecret(user), user, mAccountDb.getType(user),
                            mAccountDb.getCounter(user));
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_entries:
                importEntries();
                return true;
            case R.id.export_entries:
                exportEntries();
                return true;
            case R.id.settings:
                showSettings();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.i(getString(R.string.app_name), LOCAL_TAG + ": onActivityResult");
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SCAN_REQUEST:
                    // Grab the scan results and convert it into a URI
                    String scanResult = (intent != null) ? intent.getStringExtra("SCAN_RESULT") : null;
                    Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
                    interpretScanResult(uri, false);
                    break;
                case CHOOSE_ICON:
                    Uri data = intent.getData();
                    if (data != null) {
                        try {
                            Bitmap icon = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data);
                            if (mCustomizeView != null) {
                                ImageView customizeIcon = mCustomizeView.findViewById(R.id.customize_icon);
                                customizeIcon.setImageBitmap(icon);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }


    private void manuallyEnterAccountDetails() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, EnterKeyActivity.class);
        startActivity(intent);
    }

    private void scanBarcode() {
        Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
        intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intentScan.putExtra("SAVE_HISTORY", false);
        try {
            startActivityForResult(intentScan, SCAN_REQUEST);
        } catch (ActivityNotFoundException error) {
            createDialog(Utilities.DOWNLOAD_DIALOG);
        }
    }

    public static Intent getLaunchIntentActionScanBarcode(Context context) {
        return new Intent(AuthenticatorActivity.ACTION_SCAN_BARCODE)
                .setComponent(new ComponentName(context, AuthenticatorActivity.class));
    }

    private void showSettings() {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Shows a dialog with a list of exports and imports the selected one.
     * If it is encrypted, asks for a password.
     */
    private void importEntries() {
        if (!checkStoragePermission(PERMISSION_WRITE_STORAGE_IMPORT)) return;

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        List<String> exports = new ArrayList<>();
        for (String fileName : directory.list()) {
            if (fileName.endsWith(".json") || fileName.endsWith(".json.aes")) {
                exports.add(fileName);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.import_choose);
        builder.setItems(exports.toArray(new String[0]), (dialogInterface, index) -> {
            File file = new File(directory, exports.get(index));

            if(file.getName().endsWith(".aes")) {
                // File is encrypted, ask for password and try to decrypt it
                AlertDialog.Builder passwordDialogBuilder = new AlertDialog.Builder(this);
                passwordDialogBuilder.setTitle(R.string.enter_password);

                LinearLayout layout = new LinearLayout(this);
                float dpi = this.getResources().getDisplayMetrics().density;
                layout.setPadding((int)(20*dpi), 0, (int)(20*dpi), 0);
                EditText passwordEditText = new EditText(this);
                passwordEditText.setSingleLine();
                passwordEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                layout.addView(passwordEditText);

                passwordDialogBuilder.setView(layout);
                passwordDialogBuilder.setPositiveButton(android.R.string.ok, (passwordDialogInterface, passwordButtonIndex) -> {
                    importEntriesFile(file, passwordEditText.getText().toString());
                });
                passwordDialogBuilder.setNegativeButton(android.R.string.cancel, null);
                passwordDialogBuilder.show();
            } else {
                // File should not be encrypted
                importEntriesFile(file, "");
            }
        });
        builder.show();
    }

    /**
     * Import a specific file.
     * @param file file to import
     * @param password password to decrypt the file with, or empty string if it is not encrypted
     */
    private void importEntriesFile(File file, String password) {
        try (FileInputStream is = new FileInputStream(file)) {
            int size = (int) file.length();
            byte bytes[] = new byte[size];
            byte buffer[] = new byte[size];
            int read = is.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = is.read(buffer, 0, remain);
                    System.arraycopy(buffer, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }

            String jsonString = EncryptionUtilities.decrypt(bytes, password);
            JSONArray json = new JSONArray(jsonString);

            for (int i = 0; i < json.length(); i++) {
                JSONObject item = json.getJSONObject(i);

                saveSecretAndRefreshUserList(
                        item.getString("email"),
                        item.getString("secret"),
                        item.getString("email"),
                        OtpType.valueOf(item.getString("type")),
                        item.getInt("counter"),
                        false
                );
            }

            Snackbar.show(this, Snackbar.Type.SUCCESS, R.string.import_success);
        } catch (IOException | JSONException e) {
            Log.e(LOCAL_TAG, "Failed to import entries", e);
            Snackbar.show(this, Snackbar.Type.ERROR, R.string.import_failed);
        }
    }

    /**
     * Export all the entries. Shows a dialog with the option to enter a password.
     * Will encrypt the export if a password is entered or otherwise store it as plain JSON text.
     */
    private void exportEntries() {
        if (!checkStoragePermission(PERMISSION_WRITE_STORAGE_EXPORT)) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_password);

        LinearLayout layout = new LinearLayout(this);
        float dpi = this.getResources().getDisplayMetrics().density;
        layout.setPadding((int)(20*dpi), 0, (int)(20*dpi), 0);
        EditText passwordEditText = new EditText(this);
        passwordEditText.setSingleLine();
        passwordEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(passwordEditText);

        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            List<String> usernames = new ArrayList<>();
            mAccountDb.getNames(usernames);
            try {
                JSONArray json = new JSONArray();
                for (String username : usernames) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("email", username);
                    jsonObject.put("secret", mAccountDb.getSecret(username));
                    jsonObject.put("counter", mAccountDb.getCounter(username));
                    jsonObject.put("type", mAccountDb.getType(username).toString());
                    jsonObject.put("color", mAccountDb.getColor(username));
                    json.put(jsonObject);
                }
                String jsonString = json.toString();

                String password = passwordEditText.getText().toString();
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                String extension = password.equals("") ? ".json" : ".json.aes";
                File file = new File(directory, "1-2-authenticate-export-" + System.currentTimeMillis() + extension);
                OutputStream os = new FileOutputStream(file);
                os.write(EncryptionUtilities.encrypt(jsonString, password));
                os.close();

                Snackbar.show(this, Snackbar.Type.SUCCESS,
                        String.format(getString(R.string.exported_to), file.toString()), 5000);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    /**
     * Check if the app has permissions to read and write from/to the external storage
     *
     * @param request request id to use if the app does not have permission yet
     * @return false if the permission is not granted yet, true otherwise
     */
    private boolean checkStoragePermission(int request) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, request);
            return false;
        }
        return true;
    }

    /**
     * Called when the user has accepted or denied a requested permission
     *
     * @param requestCode  request code used with the permission request
     * @param permissions  permissions that were requested
     * @param grantResults if the permissions were granted or not
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_WRITE_STORAGE_EXPORT) {
                exportEntries();
            } else if (requestCode == PERMISSION_WRITE_STORAGE_IMPORT) {
                importEntries();
            }
        }
    }

    /**
     * Interprets the QR code that was scanned by the user.  Decides whether to
     * launch the key provisioning sequence or the OTP seed setting sequence.
     *
     * @param scanResult        a URI holding the contents of the QR scan result
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void interpretScanResult(Uri scanResult, boolean confirmBeforeSave) {
        if (DependencyInjector.getOptionalFeatures().interpretScanResult(this, scanResult)) {
            // Scan result consumed by an optional component
            return;
        }
        // The scan result is expected to be a URL that adds an account.

        // If confirmBeforeSave is true, the user has to confirm/reject the action.
        // We need to ensure that new results are accepted only if the previous ones have been
        // confirmed/rejected by the user. This is to prevent the attacker from sending multiple results
        // in sequence to confuse/DoS the user.
        if (confirmBeforeSave) {
            if (mSaveKeyIntentConfirmationInProgress) {
                Log.w(LOCAL_TAG, "Ignoring save key Intent: previous Intent not yet confirmed by user");
                return;
            }
            // No matter what happens below, we'll show a prompt which, once dismissed, will reset the
            // flag below.
            mSaveKeyIntentConfirmationInProgress = true;
        }

        // Sanity check
        if (scanResult == null) {
            createDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        // See if the URL is an account setup URL containing a shared secret
        if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
            parseSecret(scanResult, confirmBeforeSave);
        } else {
            createDialog(Utilities.INVALID_QR_CODE);
        }
    }

    /**
     * Creates and shows the specified dialog
     * @param id id of the dialog
     */
    protected void createDialog(final int id) {
        Dialog dialog;
        switch (id) {
            /*
             * Prompt to download ZXing from Market. If Market app is not installed,
             * such as on a development phone, open the HTTPS URI for the ZXing apk.
             */
            case Utilities.DOWNLOAD_DIALOG:
                AlertDialog.Builder dlBuilder = new AlertDialog.Builder(this);
                dlBuilder.setTitle(R.string.install_dialog_title);
                dlBuilder.setMessage(R.string.install_dialog_message);
                dlBuilder.setIcon(R.drawable.ic_dialog_alert);
                dlBuilder.setPositiveButton(R.string.install_button,
                        (dialog14, whichButton) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(Utilities.ZXING_MARKET));
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) { // if no Market app
                                intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(Utilities.ZXING_DIRECT));
                                startActivity(intent);
                            }
                        }
                );
                dlBuilder.setNegativeButton(R.string.cancel, null);
                dialog = dlBuilder.create();
                break;

            case DIALOG_ID_SAVE_KEY:
                final SaveKeyDialogParams saveKeyDialogParams = mSaveKeyDialogParams;
                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.save_key_message)
                        .setMessage(saveKeyDialogParams.user)
                        .setIcon(R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.ok,
                                (dialog13, whichButton) -> saveSecretAndRefreshUserList(
                                        saveKeyDialogParams.user,
                                        saveKeyDialogParams.secret,
                                        null,
                                        saveKeyDialogParams.type,
                                        saveKeyDialogParams.counter))
                        .setNegativeButton(R.string.cancel, null)
                        .create();
                dialog.setOnDismissListener(dialog12 -> {
                    onSaveKeyIntentConfirmationPromptDismissed();
                });
                break;

            case Utilities.INVALID_QR_CODE:
                dialog = createOkAlertDialog(R.string.error_title, R.string.error_qr,
                        R.drawable.ic_dialog_alert);
                markDialogAsResultOfSaveKeyIntent(dialog);
                break;

            case Utilities.INVALID_SECRET_IN_QR_CODE:
                dialog = createOkAlertDialog(
                        R.string.error_title, R.string.error_uri, R.drawable.ic_dialog_alert);
                markDialogAsResultOfSaveKeyIntent(dialog);
                break;

            case DIALOG_ID_UNINSTALL_OLD_APP:
                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.dataimport_import_succeeded_uninstall_dialog_title)
                        .setMessage(
                                DependencyInjector.getOptionalFeatures().appendDataImportLearnMoreLink(
                                        this,
                                        getString(R.string.dataimport_import_succeeded_uninstall_dialog_prompt)))
                        .setCancelable(true)
                        .setPositiveButton(
                                R.string.button_uninstall_old_app,
                                (dialog1, whichButton) -> startActivity(mOldAppUninstallIntent))
                        .setNegativeButton(R.string.cancel, null)
                        .create();
                break;

            default:
                dialog =
                        DependencyInjector.getOptionalFeatures().onAuthenticatorActivityCreateDialog(this, id);
                break;
        }
        dialog.show();
    }

    private void markDialogAsResultOfSaveKeyIntent(Dialog dialog) {
        dialog.setOnDismissListener(dialog1 -> onSaveKeyIntentConfirmationPromptDismissed());
    }

    /**
     * Invoked when a user-visible confirmation prompt for the Intent to add a new account has been
     * dimissed.
     */
    private void onSaveKeyIntentConfirmationPromptDismissed() {
        mSaveKeyIntentConfirmationInProgress = false;
    }

    /**
     * Create dialog with supplied ids; icon is not set if iconId is 0.
     */
    private Dialog createOkAlertDialog(int titleId, int messageId, int iconId) {
        return new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setMessage(messageId)
                .setIcon(iconId)
                .setPositiveButton(R.string.ok, null)
                .create();
    }

    /**
     * A tuple of user, OTP value, and type, that represents a particular user.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private static class PinInfo {
        private String pin; // calculated OTP, or a placeholder if not calculated
        private String user;
        private boolean isHotp = false; // used to see if button needs to be displayed
        private Bitmap image;
        private int color;

        /**
         * HOTP only: Whether code generation is allowed for this account.
         */
        private boolean hotpCodeGenerationAllowed;
    }


    /**
     * Scale to use for the text displaying the PIN numbers.
     */
    private static final float PIN_TEXT_SCALEX_NORMAL = 1.0f;
    /**
     * Underscores are shown slightly smaller.
     */
    private static final float PIN_TEXT_SCALEX_UNDERSCORE = 0.87f;

    /**
     * Listener for the Button that generates the next OTP value.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private class NextOtpButtonListener implements OnClickListener {
        private final Handler mHandler = new Handler();
        private final PinInfo mAccount;

        private NextOtpButtonListener(PinInfo account) {
            mAccount = account;
        }

        @Override
        public void onClick(View v) {
            int position = findAccountPositionInList();
            if (position == -1) {
                throw new RuntimeException("Account not in list: " + mAccount);
            }

            try {
                computeAndDisplayPin(mAccount.user, position, true);
            } catch (OtpSourceException e) {
                DependencyInjector.getOptionalFeatures().onAuthenticatorActivityGetNextOtpFailed(
                        AuthenticatorActivity.this, mAccount.user, e);
                return;
            }

            final String pin = mAccount.pin;

            // Temporarily disable code generation for this account
            mAccount.hotpCodeGenerationAllowed = false;
            mUserAdapter.notifyDataSetChanged();
            // The delayed operation below will be invoked once code generation is yet again allowed for
            // this account. The delay is in wall clock time (monotonically increasing) and is thus not
            // susceptible to system time jumps.
            mHandler.postDelayed(
                    () -> {
                        mAccount.hotpCodeGenerationAllowed = true;
                        mUserAdapter.notifyDataSetChanged();
                    },
                    HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES);
            // The delayed operation below will hide this OTP to prevent the user from seeing this OTP
            // long after it's been generated (and thus hopefully used).
            mHandler.postDelayed(
                    () -> {
                        if (!pin.equals(mAccount.pin)) {
                            return;
                        }
                        mAccount.pin = getString(R.string.empty_pin);
                        mUserAdapter.notifyDataSetChanged();
                    },
                    HOTP_DISPLAY_TIMEOUT);
        }

        /**
         * Gets the position in the account list of the account this listener is associated with.
         *
         * @return {@code 0}-based position or {@code -1} if the account is not in the list.
         */
        private int findAccountPositionInList() {
            return mUsers.indexOf(mAccount);
        }
    }

    /**
     * Displays the list of users and the current OTP values.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private class PinListAdapter extends ArrayAdapter<PinInfo> {

        public PinListAdapter(Context context, int userRowId, ArrayList<PinInfo> items) {
            super(context, userRowId, items);
        }

        /**
         * Displays the user and OTP for the specified position. For HOTP, displays
         * the button for generating the next OTP value; for TOTP, displays the countdown indicator.
         */
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            PinInfo currentPin = getItem(position);

            View row;
            if (convertView != null) {
                // Reuse an existing view
                row = convertView;
            } else {
                // Create a new view
                row = inflater.inflate(R.layout.user_row, null);
            }

            if (currentPin == null) return row;

            ImageView iconView = row.findViewById(R.id.icon);
            TextView pinView = row.findViewById(R.id.pin_value);
            TextView userView = row.findViewById(R.id.current_user);
            ImageButton buttonView = row.findViewById(R.id.next_otp);
            CountdownIndicator countdownIndicator =
                    row.findViewById(R.id.countdown_icon);

            View.OnTouchListener dragOnTouch = (v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mUserList.startDrag(position);
                    return true;
                }
                return false;
            };
            countdownIndicator.setOnTouchListener(dragOnTouch);
            iconView.setOnTouchListener(dragOnTouch);

            if (currentPin.image != null) {
                iconView.setImageBitmap(currentPin.image);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }

            if (currentPin.isHotp) {
                buttonView.setVisibility(View.VISIBLE);
                buttonView.setEnabled(currentPin.hotpCodeGenerationAllowed);
                if (buttonView.isEnabled()) {
                    buttonView.setColorFilter(currentPin.color);
                } else {
                    buttonView.clearColorFilter();
                }
                ((ViewGroup) row).setDescendantFocusability(
                        ViewGroup.FOCUS_BLOCK_DESCENDANTS); // makes long press work
                NextOtpButtonListener clickListener = new NextOtpButtonListener(currentPin);
                buttonView.setOnClickListener(clickListener);
                row.setTag(clickListener);

                countdownIndicator.setVisibility(View.GONE);
            } else { // TOTP, so no button needed
                buttonView.setVisibility(View.GONE);
                buttonView.setOnClickListener(null);
                row.setTag(null);

                countdownIndicator.setVisibility(View.VISIBLE);
                countdownIndicator.setPhase(mTotpCountdownPhase);
                countdownIndicator.setColor(currentPin.color);
            }

            if (getString(R.string.empty_pin).equals(currentPin.pin)) {
                pinView.setTextScaleX(PIN_TEXT_SCALEX_UNDERSCORE); // smaller gap between underscores
            } else {
                pinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);
            }
            pinView.setText(currentPin.pin);
            userView.setText(currentPin.user);
            userView.setTextColor(currentPin.color);

            return row;
        }
    }

    /**
     * Parameters to the {@link AuthenticatorActivity#DIALOG_ID_SAVE_KEY} dialog.
     */
    private static class SaveKeyDialogParams implements Serializable {
        private final String user;
        private final String secret;
        private final OtpType type;
        private final Integer counter;

        private SaveKeyDialogParams(String user, String secret, OtpType type, Integer counter) {
            this.user = user;
            this.secret = secret;
            this.type = type;
            this.counter = counter;
        }
    }


}
