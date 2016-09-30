package ca.cuni.callrejector;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.ActionMode;


import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            int perm = this.checkSelfPermission("android.permission.CALL_PHONE");
            if( perm == PackageManager.PERMISSION_DENIED ) {
                requestPermissions(new String[]{"android.permission.CALL_PHONE"},
                    101);
            }
        }

        /*
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder nb = new Notification.Builder(this.getApplicationContext())
                .setContentTitle("New GCM")
                .setContentText("MSG: " + "NONO")
                .setSmallIcon (0) ;

        notificationManager.notify(1001, nb.build());
        */

    }
}
