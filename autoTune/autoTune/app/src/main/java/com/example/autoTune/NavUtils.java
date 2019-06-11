package com.example.autoTune;

import android.content.Context;
import android.content.Intent;


public class NavUtils {
    public static void showSettingsActivity(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
}
