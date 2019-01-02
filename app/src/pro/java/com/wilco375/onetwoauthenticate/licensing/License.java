package com.wilco375.onetwoauthenticate.licensing;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.wilco375.onetwoauthenticate.BuildConfig;
import com.wilco375.onetwoauthenticate.R;

public class License {
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw6yt9lO5QzmGWzVYB1aupKVu1DmQ/g4z4a8cYI+a1wqBgp1ZZ07jbN3/qZjgH8IT8wJ3p/3/8OCV9f8b0QqQhJ0LNXo4yccV+K2rZdEA8E0mX8cAem3r6isQhqtqFXDyp0vQ8Xy83Zl9flLmNSr0zNdyCdjwiRQ/t4UQKpIL6j2w5SgczfP1vO3aGG6RXgolOraJK9zEm2UQyqowwCQLbZmILiuoW08FIfhhtOMG5ZhPLejSSxGcWaqhrAECoe88wL+MFTjqkXNs4RG++i4YjiVFIgbrYdK2pNwpVoWA2bgi+jRSJ9w4EPz80icsYb9YSFE7wsUatnowHEo5HQB3pQIDAQAB";
    private static final byte[] SALT = new byte[] {28, 12, 123, 64, 46, 35, 52, 85, 47, 89};

    boolean licensed;
    boolean checkingLicense;
    boolean didCheck;
    Activity activity;

    @Nullable
    ProgressDialog licenseDialog;

    public void checkLicense(Activity activity) {
        if(!licensed && !checkingLicense && !BuildConfig.DEBUG) {
            didCheck = false;
            checkingLicense = true;
            this.activity = activity;

            showLicenseCheckingDialog();

            String deviceId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
            Log.i("Device Id", deviceId);

            LicenseCheckerCallback licenseCheckerCallback = new MyLicenseCheckerCallback();
            LicenseChecker checker = new LicenseChecker(activity, new ServerManagedPolicy(activity, new AESObfuscator(SALT, activity.getPackageName(), deviceId)), BASE64_PUBLIC_KEY);

            checker.checkAccess(licenseCheckerCallback);
        }
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        @Override
        public void allow(int reason) {
            Log.i("License", "Accepted!");

            licensed = true;
            checkingLicense = false;
            didCheck = true;

            if(licenseDialog != null){
                licenseDialog.cancel();
            }
        }

        @Override
        public void dontAllow(int reason) {
            Log.i("License", "Denied!");
            Log.i("License", "Reason for denial: " + reason);

            licensed = false;
            checkingLicense = false;
            didCheck = true;

            showLicenseFailDialog(reason);
        }

        @Override
        public void applicationError(int reason) {
            Log.i("License", "Application error!");

            licensed = true;
            checkingLicense = false;
            didCheck = false;

            showLicenseFailDialog(-1);
        }
    }

    private void showLicenseFailDialog(final int reason){
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(false);
            builder.setTitle(R.string.license_fail_title);
            builder.setMessage(activity.getResources().getString(R.string.license_fail_desc, reason));
            builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> System.exit(0));
            builder.setNeutralButton(R.string.retry, (dialog, which) -> {
                dialog.cancel();
                checkLicense(activity);
            });
            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);

            if(licenseDialog != null){
                licenseDialog.cancel();
            }

            dialog.show();
        });
    }

    private void showLicenseCheckingDialog(){
        activity.runOnUiThread(() -> {
            licenseDialog = new ProgressDialog(activity);
            licenseDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            licenseDialog.setMessage(activity.getResources().getString(R.string.checking_license));
            licenseDialog.setIndeterminate(true);
            licenseDialog.setCanceledOnTouchOutside(false);
            licenseDialog.setCancelable(false);
            licenseDialog.setProgressNumberFormat(null);
            licenseDialog.setProgressPercentFormat(null);
            licenseDialog.show();
        });
    }
}
