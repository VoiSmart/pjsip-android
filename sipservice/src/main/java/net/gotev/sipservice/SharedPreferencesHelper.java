package net.gotev.sipservice;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * connect
 *
 * Created by Vincenzo Esposito on 25/11/19.
 * Copyright © 2019 VoiSmart S.r.l. All rights reserved.
 */
public class SharedPreferencesHelper {

    private final String PREFS_KEY_ACCOUNTS = "accounts";
    private final String PREFS_KEY_CODEC_PRIORITIES = "codec_priorities";
    private final String PREFS_KEY_DND = "dnd_pref";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("sipservice_prefs", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    List<SipAccountData> retrieveConfiguredAccounts() {
        String accounts = sharedPreferences.getString(PREFS_KEY_ACCOUNTS, "");

        if (accounts.isEmpty()) {
            return new ArrayList<>();
        } else {
            Type listType = new TypeToken<ArrayList<SipAccountData>>(){}.getType();
            return gson.fromJson(accounts, listType);
        }
    }

    void persistConfiguredAccounts(List<SipAccountData> configuredAccounts) {
        sharedPreferences.edit().putString(PREFS_KEY_ACCOUNTS, gson.toJson(configuredAccounts)).apply();
    }

    ArrayList<CodecPriority> retrieveConfiguredCodecPriorities() {
        String codecPriorities = sharedPreferences.getString(PREFS_KEY_CODEC_PRIORITIES, "");
        if (codecPriorities.isEmpty()) return null;

        Type listType = new TypeToken<ArrayList<CodecPriority>>(){}.getType();
        return gson.fromJson(codecPriorities, listType);
    }

    void persistConfiguredCodecPriorities(ArrayList<CodecPriority> codecPriorities) {
        sharedPreferences.edit().putString(PREFS_KEY_CODEC_PRIORITIES, gson.toJson(codecPriorities)).apply();
    }

    void setDND(boolean dnd) {
        sharedPreferences.edit().putBoolean(PREFS_KEY_DND, dnd).apply();
    }

    boolean isDND() {
        return sharedPreferences.getBoolean(PREFS_KEY_DND, false);
    }
}
