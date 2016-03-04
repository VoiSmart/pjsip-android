package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.pj_qos_type;

/**
 * Contains the account's configuration data.
 * @author gotev (Aleksandar Gotev)
 */
public class SipAccountData implements Parcelable {

    private String username;
    private String password;
    private String realm;
    private String host;
    private long port = 5060;
    private boolean tcpTransport = false;

    public SipAccountData() { }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<SipAccountData> CREATOR =
            new Parcelable.Creator<SipAccountData>() {
                @Override
                public SipAccountData createFromParcel(final Parcel in) {
                    return new SipAccountData(in);
                }

                @Override
                public SipAccountData[] newArray(final int size) {
                    return new SipAccountData[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeString(realm);
        parcel.writeString(host);
        parcel.writeLong(port);
        parcel.writeByte((byte) (tcpTransport ? 1 : 0));
    }

    private SipAccountData(Parcel in) {
        username = in.readString();
        password = in.readString();
        realm = in.readString();
        host = in.readString();
        port = in.readLong();
        tcpTransport = in.readByte() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getUsername() {
        return username;
    }

    public SipAccountData setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SipAccountData setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public SipAccountData setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public String getHost() {
        return host;
    }

    public SipAccountData setHost(String host) {
        this.host = host;
        return this;
    }

    public long getPort() {
        return port;
    }

    public SipAccountData setPort(long port) {
        this.port = port;
        return this;
    }

    public boolean isTcpTransport() {
        return tcpTransport;
    }

    public SipAccountData setTcpTransport(boolean tcpTransport) {
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
        accountConfig.getMediaConfig().getTransportConfig().setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
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

        SipAccountData that = (SipAccountData) o;

        return getIdUri().equals(that.getIdUri());

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

