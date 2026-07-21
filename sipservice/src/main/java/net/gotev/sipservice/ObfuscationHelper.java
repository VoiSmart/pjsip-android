package net.gotev.sipservice;

import android.content.Context;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 13/07/21.
 * Copyright © 2021 VoiSmart S.r.l. All rights reserved.
 */
public class ObfuscationHelper {
    public static String getValue(Context context, String string) {
        return SharedPreferencesHelper.getInstance(context).isObfuscationEnabled()
                ? obfuscate(string)
                : string;
    }

    // Package-private (not private) so it can be unit-tested directly without a
    // Context / SharedPreferences singleton.
    static String obfuscate(String string) {
        // Guard short/empty/null values: the previous impl threw
        // NegativeArraySizeException on "" (repeat(-1)) and revealed the whole
        // character for length-1 strings.
        if (string == null || string.isEmpty()) return "";
        int length = string.length();
        if (length > 5) return repeat(length - 3) + string.substring(length - 3);
        if (length > 1) return repeat(length - 1) + string.substring(length - 1);
        return "*";
    }

    private static String repeat(int n) {
        return new String(new char[n]).replace("\0", "*");
    }
}
