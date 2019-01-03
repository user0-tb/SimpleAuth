package com.wilco375.onetwoauthenticate.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.wilco375.onetwoauthenticate.R;
import com.wilco375.onetwoauthenticate.otp.OtpSource;
import com.wilco375.onetwoauthenticate.otp.OtpSourceException;
import com.wilco375.onetwoauthenticate.testability.DependencyInjector;

public class CopyKeyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        moveTaskToBack(true);

        super.onCreate(savedInstanceState);

        String name = getIntent().getStringExtra("name");
        OtpSource otpProvider = DependencyInjector.getOtpProvider();
        try {
            String code = otpProvider.getNextCode(name);

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) throw new OtpSourceException("Failed to get clipboard");
            clipboard.setPrimaryClip(ClipData.newPlainText("OTP code", code));

            Toast.makeText(this, R.string.pin_copied, Toast.LENGTH_LONG).show();
        } catch (OtpSourceException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_title, Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
