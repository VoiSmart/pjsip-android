package org.pjsip;

import android.os.Build;

import com.crashlytics.android.Crashlytics;

import net.gotev.sipservice.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PjCallID {

    private static final String TAG = PjCallID.class.getSimpleName();
    public static String USER = "";

    public static String getCallID(){
        return getMD5Hash(getSerial()+USER);
    }

    private static String getSerial() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Build.SERIAL;
        } else {
            try {
                return Build.getSerial();
            } catch (SecurityException se) {
                Crashlytics.logException(se);
                return Build.SERIAL;
            }
        }
    }

    private static String getMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return String.format("%032x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException nax) {
            Crashlytics.logException(nax);
            return input;
        }
    }
}
