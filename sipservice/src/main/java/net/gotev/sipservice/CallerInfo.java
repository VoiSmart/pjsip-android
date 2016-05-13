package net.gotev.sipservice;

import org.pjsip.pjsua2.CallInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtains display name and remote uri from a CallInfo object.
 * @author gotev (Aleksandar Gotev)
 */
public class CallerInfo {
    private final Pattern displayNameAndRemoteUriPattern = Pattern.compile("^\"([^\"]+).*?sip:(.*?)>$");
    private final Pattern remoteUriPattern = Pattern.compile("^.*?sip:(.*?)>$");

    private static final String UNKNOWN = "Unknown";

    private String displayName;
    private String remoteUri;

    public CallerInfo(final CallInfo callInfo) {
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
                displayName = remoteUri = remoteUriInfo.group(1);
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
