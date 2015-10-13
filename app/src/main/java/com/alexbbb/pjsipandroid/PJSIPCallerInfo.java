package com.alexbbb.pjsipandroid;

import org.pjsip.pjsua2.CallInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtains display name and remote uri from a CallInfo object.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPCallerInfo {
    private final Pattern displayNameAndRemoteUriPattern = Pattern.compile("^\"([^\"]+).*?sip:(.*?)>$");
    private final Pattern remoteUriPattern = Pattern.compile("^.*?sip:(.*?)>$");

    private static final String UNKNOWN = "Unknown";

    private String displayName;
    private String remoteUri;

    public PJSIPCallerInfo(final CallInfo callInfo) {
        String temp = callInfo.getRemoteUri();

        if (temp == null || temp.isEmpty()) {
            displayName = remoteUri = UNKNOWN;
            return;
        }

        Matcher completeInfo = displayNameAndRemoteUriPattern.matcher(temp);
        if (completeInfo.matches()) {
            displayName = completeInfo.group(1);
            remoteUri = completeInfo.group(2);

        } else {
            Matcher remoteUriInfo = remoteUriPattern.matcher(temp);
            if (remoteUriInfo.matches()) {
                displayName = remoteUri = completeInfo.group(1);
            } else {
                displayName = remoteUri = UNKNOWN;
            }
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRemoteUri() {
        return remoteUri;
    }
}
