package ca.cuni.callrejector;

import android.app.IntentService;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CallRejectService extends IntentService {

    final static String logtag = "CallRejectService" ;

    private boolean endCall() {
        boolean res = false ;
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE) ;

            // Get the getITelephony() method
            Class<?> classTelephony = Class.forName(tm.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(tm);

            // Get the endCall method from ITelephony
            Class<?> telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            res = (boolean) methodEndCall.invoke(telephonyInterface);

            Log.d(logtag, "phone call rejected!") ;

        } catch (Exception e) {
            res = false ;
        }
        return res ;
    }

    private boolean checkNetwork() {

        try {
            String line ;
            String [] aa ;
            BufferedReader buf = new BufferedReader(new FileReader("/proc/net/tcp"));
            // skip first line
            buf.readLine() ;
            while( (line = buf.readLine()) != null ) {
                aa = line.split("\\s+", 6);
                if( aa[4].equals ("01")  ) {
                    return true ;
                }
            }

            buf = new BufferedReader(new FileReader("/proc/net/tcp6"));
            // skip first line
            buf.readLine() ;
            while( (line = buf.readLine()) != null ) {
                aa = line.split("\\s+", 6);
                if( aa[4].equals ("01")  ) {
                    return true ;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false ;
    }

    private void runRej() {

        boolean data_enable ;
        boolean wifi_enable ;
        boolean endcall ;
        boolean startFongo ;
        boolean startHangouts ;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        data_enable = pref.getBoolean("data_enable", true) ;
        wifi_enable = pref.getBoolean("wifi_enable", false) ;

        endcall = pref.getBoolean("endcall", true) ;

        startFongo = pref.getBoolean("startFongo", true) ;
        startHangouts = pref.getBoolean("startHangouts", false) ;

        // try end the call without 'su'
        if( endcall ) {
            if( endCall() ) {
                endcall = false ;       // successed
            }
        }

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected()) {
            // no need to enable data
            data_enable = false ;
            wifi_enable = false ;
        }

        if( wifi_enable && ((WifiManager) getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true) ) {
            wifi_enable = false ;
        }

        try {
            if( endcall || data_enable || wifi_enable ) {
                Process proc = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(proc.getOutputStream());

                if (endcall) {
                    // use KEYCODE_ENDCALL to hangup imcoming calls
                    Thread.sleep(300);
                    os.writeBytes("input keyevent KEYCODE_ENDCALL\n");
                }

                if (data_enable) {
                    os.writeBytes("svc data enable\n");
                }

                if (wifi_enable) {
                    os.writeBytes("svc wifi enable\n");
                }

                os.writeBytes("exit\n");
                os.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }


        boolean dataUp = false ;

        if( startFongo || startHangouts ) {
            int retry  ;

            // would wait about 10 s for internet available
            for( retry = 0; retry<20; retry++ ){
                netInfo = cm.getActiveNetworkInfo();
                if( netInfo!=null && netInfo.isConnected() )
                    break;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break ;
                }
            }

            for( retry = 0; retry<20; retry++ ){
                if( checkNetwork() ) {
                    dataUp = true ;
                    break ;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break ;
                }
            }

            if( dataUp ) {
                PackageManager pm = getPackageManager();

                if (startFongo) {
                    Intent fongoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("fongo://"));
                    fongoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (fongoIntent.resolveActivity(pm) != null) {
                        startActivity(fongoIntent);
                    }
                }

                if (startHangouts) {
                    Intent iTalk = pm.getLaunchIntentForPackage("com.google.android.talk");
                    if (iTalk != null) {
                        iTalk.addCategory(Intent.CATEGORY_LAUNCHER);
                        startActivity(iTalk);
                    }
                }
            }

        }

    }

    public CallRejectService() {
        super("CallRejectService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            runRej();
        }
    }
}
