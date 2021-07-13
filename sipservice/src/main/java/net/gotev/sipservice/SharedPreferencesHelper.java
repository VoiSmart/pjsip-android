package net.gotev.sipservice;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voismart.crypto.Crypto;
import com.voismart.crypto.EncryptionHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * connect
 *
 * Created by Vincenzo Esposito on 25/11/19.
 * Copyright © 2019 VoiSmart S.r.l. All rights reserved.
 */
@SuppressWarnings("unused")
public class SharedPreferencesHelper {

    private final String PREFS_KEY_ACCOUNTS = "accounts";
    private final String PREFS_KEY_CODEC_PRIORITIES = "codec_priorities";
    private final String PREFS_KEY_DND = "dnd_pref";
    private final String PREFS_KEY_ENCRYPTION_ENABLED = "encryption_enabled";
    private final String PREFS_KEY_KEYSTORE_ALIAS = "keystore_alias";
    private final String PREFS_KEY_OBFUSCATION_ENABLED = "obfuscation_enabled";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private EncryptionHelper encryptionHelper = null;

    private static SharedPreferencesHelper INSTANCE = null;
    private static final String TAG = SharedPreferencesHelper.class.getSimpleName();

    private SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("sipservice_prefs", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    SharedPreferencesHelper init(Context context) {
        if (isEncryptionEnabled()) {
            initCrypto(context, getAlias());
        }
        return INSTANCE;
    }

    List<SipAccountData> retrieveConfiguredAccounts() {
        if (isEncryptionEnabled()) {
            return getDecryptedConfiguredAccounts();
        } else {
            return getConfiguredAccounts();
        }
    }

    void persistConfiguredAccounts(List<SipAccountData> configuredAccounts) {
        if (isEncryptionEnabled()) {
            setEncryptedConfiguredAccounts(configuredAccounts);
        } else {
            setConfiguredAccounts(configuredAccounts);
        }
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

    boolean isDND() {
        return sharedPreferences.getBoolean(PREFS_KEY_DND, false);
    }

    void setDND(boolean dnd) {
        sharedPreferences.edit().putBoolean(PREFS_KEY_DND, dnd).apply();
    }

    void setEncryption(Context context, boolean enableEncryption, String alias) {
        if (enableEncryption) {
            setAlias(alias);
            initCrypto(context, alias);
        }
        boolean wasEncryptionEnabled = isEncryptionEnabled();
        if (enableEncryption != wasEncryptionEnabled) {
            handleMigration(wasEncryptionEnabled);
        }
        sharedPreferences.edit().putBoolean(PREFS_KEY_ENCRYPTION_ENABLED, enableEncryption).apply();
    }

    void setObfuscation(boolean obfuscate) {
        sharedPreferences.edit().putBoolean(PREFS_KEY_OBFUSCATION_ENABLED, obfuscate).apply();
    }

    /**
     * Whether the string obfuscation is enabled in logs
     * @return what is set in {@link #setObfuscation(boolean)} or true in release builds and false in debug ones
     */
    boolean isObfuscationEnabled() {
        return sharedPreferences.getBoolean(PREFS_KEY_OBFUSCATION_ENABLED, !BuildConfig.DEBUG);
    }

    /**
     * Helpers to decrypt retrieved encrypted data
     * @return decrypted accounts
     */
    private synchronized List<SipAccountData> getDecryptedConfiguredAccounts() {
        List<SipAccountData> accounts = getConfiguredAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            accounts.get(i).setUsername(decrypt(accounts.get(i).getUsername()));
            accounts.get(i).setPassword(decrypt(accounts.get(i).getPassword()));
        }
        return accounts;
    }

    /**
     * Helpers to retrieve data
     * @return accounts
     */
    private synchronized List<SipAccountData> getConfiguredAccounts() {
        String accounts = sharedPreferences.getString(PREFS_KEY_ACCOUNTS, "");
        if (accounts.isEmpty() || accounts.equals("[]")) {
            return new ArrayList<>();
        } else {
            Type listType = new TypeToken<ArrayList<SipAccountData>>(){}.getType();
            return gson.fromJson(accounts, listType);
        }
    }

    /**
     * Helpers to encrypt and persist data
     */
    private synchronized void setEncryptedConfiguredAccounts(List<SipAccountData> accounts) {
        List<SipAccountData> temp = new ArrayList<>();
        for (int i = 0; i < accounts.size(); i++) {
            SipAccountData d = accounts.get(i).getDeepCopy();
            d.setUsername(encrypt(d.getUsername()));
            d.setPassword(encrypt(d.getPassword()));
            temp.add(d);
        }
        setConfiguredAccounts(temp);
    }

    /**
     * Helpers to persist data
     */
    private synchronized void setConfiguredAccounts(List<SipAccountData> accounts) {
        sharedPreferences.edit().putString(PREFS_KEY_ACCOUNTS, gson.toJson(accounts)).apply();
    }

    private synchronized boolean isEncryptionEnabled() {
        return sharedPreferences.getBoolean(PREFS_KEY_ENCRYPTION_ENABLED, false);
    }

    private void setAlias(String alias) {
        sharedPreferences.edit().putString(PREFS_KEY_KEYSTORE_ALIAS, alias).apply();
    }

    private String getAlias() {
        return sharedPreferences.getString(PREFS_KEY_KEYSTORE_ALIAS, "");
    }

    private void initCrypto(Context context, String alias) {
        Crypto.init(context, alias, false);
        encryptionHelper = EncryptionHelper.Companion.getInstance();
    }

    private String encrypt(String data) {
        try {
            return encryptionHelper.encrypt(data);
        } catch (Exception e) {
            Logger.error(TAG, "Error while encrypting the string", e);
            return null;
        }
    }

    private String decrypt(String data) {
        try {
            return encryptionHelper.decrypt(data);
        } catch (Exception e) {
            Logger.error(TAG, "Error while deciphering the string", e);
            return null;
        }
    }

    private void handleMigration(boolean wasEncrypted) {
        if (wasEncrypted) {
            migrateToPlainText();
        } else {
            migrateToEncryption();
        }
    }

    private void migrateToEncryption() {
        setEncryptedConfiguredAccounts(getConfiguredAccounts());
    }

    private void migrateToPlainText() {
        setConfiguredAccounts(getDecryptedConfiguredAccounts());
    }

    public static SharedPreferencesHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SharedPreferencesHelper(context);
        }
        return INSTANCE;
    }
}
