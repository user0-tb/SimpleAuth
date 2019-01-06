package com.wilco375.onetwoauthenticate;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.StringRes;

import com.andrognito.flashbar.Flashbar;

public class Snackbar {
    public enum Type {
        INFO,
        ERROR,
        SUCCESS
    }

    public static void show(Activity activity, Type type, String message, int duration) {
        make(activity, type, message, duration).show();
    }

    public static void show(Activity activity, Type type, @StringRes int message, int duration) {
        show(activity, type, activity.getResources().getString(message), 3500);
    }

    public static void show(Activity activity, Type type, String message) {
        show(activity, type, message, 3500);
    }

    public static void show(Activity activity, Type type, @StringRes int message) {
        show(activity, type, message, 3500);
    }

    public static Flashbar.Builder make(Activity activity, Type type, String message, int duration) {
        Flashbar.Builder builder = new Flashbar.Builder(activity);
        if (type != Type.INFO) {
            builder.showIcon();
        }
        if (type == Type.ERROR) {
            builder.icon(R.drawable.ic_error)
                    .backgroundColor(Color.parseColor("#F44336"));
        } else if (type == Type.SUCCESS) {
            builder.icon(R.drawable.ic_success)
                    .backgroundColor(Color.parseColor("#4CAF50"));
        }
        return builder.message(message)
            .duration(duration);
    }

    public static Flashbar.Builder make(Activity activity, Type type, @StringRes int message, int duration) {
        return make(activity, type, activity.getResources().getString(message), 3500);
    }

    public static Flashbar.Builder make(Activity activity, Type type, String message) {
        return make(activity, type, message, 3500);
    }

    public static Flashbar.Builder make(Activity activity, Type type, @StringRes int message) {
        return make(activity, type, message, 3500);
    }


}
