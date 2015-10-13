package com.alexbbb.pjsipandroid;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;

/**
 * Contains the account's configuration data.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPAccountData {

    private String username;
    private String password;
    private String realm;
    private String host;
    private long port = 5060;
    private boolean tcpTransport = false;

    public String getUsername() {
        return username;
    }

    public PJSIPAccountData setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public PJSIPAccountData setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public PJSIPAccountData setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public String getHost() {
        return host;
    }

    public PJSIPAccountData setHost(String host) {
        this.host = host;
        return this;
    }

    public long getPort() {
        return port;
    }

    public PJSIPAccountData setPort(long port) {
        this.port = port;
        return this;
    }

    public boolean isTcpTransport() {
        return tcpTransport;
    }

    public PJSIPAccountData setTcpTransport(boolean tcpTransport) {
        this.tcpTransport = tcpTransport;
        return this;
    }

    public String getIdUri() {
        return "sip:" + username + "@" + realm;
    }

    public String getRegistrarUri() {
        return "sip:" + host + ":" + port;
    }

    public String getProxyUri() {
        StringBuilder proxyUri = new StringBuilder();

        proxyUri.append("sip:").append(host).append(":").append(port);

        if (tcpTransport) {
            proxyUri.append(";transport=tcp");
        }

        return proxyUri.toString();
    }

    public boolean isValid() {
        return ((username != null) && !username.isEmpty()
                && (password != null) && !password.isEmpty()
                && (host != null) && !host.isEmpty()
                && (realm != null) && !realm.isEmpty());
    }

    protected AccountConfig getAccountConfig() {
        AccountConfig accountConfig = new AccountConfig();
        accountConfig.setIdUri(getIdUri());
        accountConfig.getRegConfig().setRegistrarUri(getRegistrarUri());

        AuthCredInfo cred = new AuthCredInfo("digest", getRealm(), getUsername(), 0, getPassword());
        accountConfig.getSipConfig().getAuthCreds().add(cred);
        accountConfig.getSipConfig().getProxies().add(getProxyUri());

        return accountConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PJSIPAccountData that = (PJSIPAccountData) o;

        if (port != that.port) return false;
        if (tcpTransport != that.tcpTransport) return false;
        if (!username.equals(that.username)) return false;
        if (!password.equals(that.password)) return false;
        if (!realm.equals(that.realm)) return false;
        return host.equals(that.host);

    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + realm.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + (int) (port ^ (port >>> 32));
        result = 31 * result + (tcpTransport ? 1 : 0);
        return result;
    }
}

