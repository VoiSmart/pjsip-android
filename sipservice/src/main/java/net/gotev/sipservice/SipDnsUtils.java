package net.gotev.sipservice;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DNS servers for PJSIP's own resolver, used in DNS SRV mode.
 * <p>
 * PJSIP's resolver sends plain UDP/53 queries to the servers it is given, bypassing the system
 * resolver. An empty list means "PJSIP can't resolve here": the caller then keeps the stack on
 * {@code host:port}, exactly as with SRV disabled.
 */
final class SipDnsUtils {

    private static final String TAG = "SipDnsUtils";

    private SipDnsUtils() {
    }

    /**
     * The active network's DNS servers, or an empty list when there is no network, no servers,
     * or strict Private DNS is on (it may block plain UDP/53, with no fallback).
     */
    static List<String> resolveNameservers(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Logger.warning(TAG, "No ConnectivityManager, DNS SRV not usable");
            return Collections.emptyList();
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            Logger.warning(TAG, "No active network, DNS SRV not usable");
            return Collections.emptyList();
        }
        LinkProperties linkProperties = cm.getLinkProperties(network);
        if (linkProperties == null) {
            Logger.warning(TAG, "No link properties for the active network, DNS SRV not usable");
            return Collections.emptyList();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && isStrictPrivateDns(linkProperties.isPrivateDnsActive(),
                linkProperties.getPrivateDnsServerName())) {
            Logger.warning(TAG, "Strict Private DNS is on, DNS SRV not usable");
            return Collections.emptyList();
        }
        List<String> servers = sanitize(linkProperties.getDnsServers());
        if (servers.isEmpty()) {
            Logger.warning(TAG, "The active network has no DNS servers, DNS SRV not usable");
        } else {
            // Only the count: the addresses can identify the user's network.
            Logger.info(TAG, "DNS servers for SRV resolution: " + servers.size());
        }
        return servers;
    }

    /**
     * Strict mode is the only Private DNS mode with a server name; opportunistic mode (active,
     * no name) still lets plain UDP/53 through.
     */
    static boolean isStrictPrivateDns(boolean privateDnsActive, String privateDnsServerName) {
        return privateDnsActive && privateDnsServerName != null && !privateDnsServerName.isEmpty();
    }

    /**
     * Plain address strings as PJSIP expects them: IPv6 scope ids ({@code %wlan0}) stripped,
     * duplicates removed, IPv4 first, original order otherwise kept.
     */
    static List<String> sanitize(List<InetAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> ipv4 = new LinkedHashSet<>();
        Set<String> ipv6 = new LinkedHashSet<>();
        for (InetAddress address : addresses) {
            if (address == null) continue;
            String host = address.getHostAddress();
            if (host == null || host.isEmpty()) continue;
            int scope = host.indexOf('%');
            if (scope >= 0) host = host.substring(0, scope);
            if (address instanceof Inet4Address) {
                ipv4.add(host);
            } else {
                ipv6.add(host);
            }
        }
        List<String> result = new ArrayList<>(ipv4);
        result.addAll(ipv6);
        return result;
    }
}
