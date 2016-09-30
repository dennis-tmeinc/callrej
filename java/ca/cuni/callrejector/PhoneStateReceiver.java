package ca.cuni.callrejector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String extrastate = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ;
        String incoming_number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ;

        if( action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                && extrastate.equals(TelephonyManager.EXTRA_STATE_RINGING)
                && ( incoming_number == null || incoming_number.length() < 5 ) )
        {
            Intent iRej = new Intent(context, CallRejectService.class);
            iRej.setAction(action);
            iRej.putExtra(TelephonyManager.EXTRA_STATE, extrastate);
            context.startService(iRej);
        }
        else {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               Context appContext = context.getApplicationContext();
               int perm = appContext.checkSelfPermission("android.permission.CALL_PHONE");
               if( perm == PackageManager.PERMISSION_DENIED ) {
                   Intent iSet = new Intent(appContext, SettingsActivity.class);
                   appContext.startService(iSet);
               }
            }
        }
    }
}
