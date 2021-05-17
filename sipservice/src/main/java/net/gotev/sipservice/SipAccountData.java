package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.pj_constants_;
import org.pjsip.pjsua2.pj_qos_type;

import java.util.Objects;

/**
 * Contains the account's configuration data.
 * @author gotev (Aleksandar Gotev)
 */
@SuppressWarnings("unused")
public class SipAccountData implements Parcelable {

    public static final String AUTH_TYPE_DIGEST = "digest";
    public static final String AUTH_TYPE_PLAIN = "plain";

    private String username;
    private String password;
    private String realm;
    private String host;
    private long port = 5060;
    private boolean tcpTransport = false;
    private String authenticationType = AUTH_TYPE_DIGEST;
    private String contactUriParams = "";
    private int regExpirationTimeout = 300;     // 300s
    private String guestDisplayName = "";
    private String callId = "";

    public SipAccountData() { }

    /*****          Parcelable overrides        ******/
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
        parcel.writeString(authenticationType);
        parcel.writeString(contactUriParams);
        parcel.writeInt(regExpirationTimeout);
        parcel.writeString(guestDisplayName);
        parcel.writeString(callId);
    }

    private SipAccountData(Parcel in) {
        username = in.readString();
        password = in.readString();
        realm = in.readString();
        host = in.readString();
        port = in.readLong();
        tcpTransport = in.readByte() == 1;
        authenticationType = in.readString();
        contactUriParams = in.readString();
        regExpirationTimeout = in.readInt();
        guestDisplayName = in.readString();
        callId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }
    /*          Parcelable overrides end        */

    /*****          Getters and Setters        ******/
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

    public String getAuthenticationType() {
        return authenticationType;
    }

    public SipAccountData setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
        return this;
    }

    public SipAccountData setContactUriParams(String contactUriParams){
        this.contactUriParams = contactUriParams;
        return this;
    }

    public String getContactUriParams(){
        return contactUriParams;
    }

    public SipAccountData setRegExpirationTimeout(int regExpirationTimeout){
        this.regExpirationTimeout = regExpirationTimeout;
        return this;
    }

    public int getRegExpirationTimeout(){
        return this.regExpirationTimeout;
    }

    public String getGuestDisplayName() {
        return this.guestDisplayName;
    }

    public SipAccountData setGuestDisplayName(String guestDisplayName) {
        this.guestDisplayName = guestDisplayName;
        return this;
    }

    public String getCallId() {
        return callId;
    }

    public SipAccountData setCallId(String callId) {
        this.callId = callId;
        return this;
    }
    /*          Getters and Setters end        */

    /*****          Utilities        ******/
    AuthCredInfo getAuthCredInfo() {
        return new AuthCredInfo(authenticationType, realm,
                username, 0, password);
    }

    String getIdUri() {
        if ("*".equals(realm))
            return "sip:" + username;

        return "sip:" + username + "@" + realm;
    }

    String getProxyUri() {
        StringBuilder proxyUri = new StringBuilder();

        proxyUri.append("sip:").append(host).append(":").append(port);

        if (tcpTransport) {
            proxyUri.append(";transport=tcp");
        }

        return proxyUri.toString();
    }

    String getRegistrarUri() {
        return "sip:" + host + ":" + port;
    }

    public boolean isValid() {
        return ((username != null) && !username.isEmpty()
                && (password != null) && !password.isEmpty()
                && (host != null) && !host.isEmpty()
                && (realm != null) && !realm.isEmpty());
    }

    AccountConfig getAccountConfig() {
        AccountConfig accountConfig = new AccountConfig();

        // account configs
        accountConfig.setIdUri(getIdUri());

        // account registration stuff configs
        if (callId != null && !callId.isEmpty()) {
            accountConfig.getRegConfig().setCallID(callId);
        }
        accountConfig.getRegConfig().setRegistrarUri(getRegistrarUri());
        accountConfig.getRegConfig().setTimeoutSec(regExpirationTimeout);

        // account sip stuff configs
        accountConfig.getSipConfig().getAuthCreds().add(getAuthCredInfo());
        accountConfig.getSipConfig().getProxies().add(getProxyUri());
        accountConfig.getSipConfig().setContactUriParams(contactUriParams);

        // nat configs to allow call reconnection across networks
        accountConfig.getNatConfig().setSdpNatRewriteUse(pj_constants_.PJ_TRUE);
        accountConfig.getNatConfig().setViaRewriteUse(pj_constants_.PJ_TRUE);

        // account media  stuff configs
        accountConfig.getMediaConfig().getTransportConfig().setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
        setVideoConfig(accountConfig);

        return accountConfig;
    }

    AccountConfig getGuestAccountConfig() {
        AccountConfig accountConfig = new AccountConfig();
        accountConfig.getMediaConfig().getTransportConfig().setQosType(pj_qos_type.PJ_QOS_TYPE_VIDEO);
        String idUri = getGuestDisplayName().isEmpty()
                ? getIdUri()
                : "\""+getGuestDisplayName()+"\" <"+getIdUri()+">";
        accountConfig.setIdUri(idUri);
        accountConfig.getSipConfig().getProxies().add(getProxyUri());
        accountConfig.getRegConfig().setRegisterOnAdd(false);
        setVideoConfig(accountConfig);
        return accountConfig;
    }

    private void setVideoConfig(AccountConfig accountConfig) {
        accountConfig.getVideoConfig().setAutoTransmitOutgoing(false);
        accountConfig.getVideoConfig().setAutoShowIncoming(true);
        accountConfig.getVideoConfig().setDefaultCaptureDevice(SipServiceConstants.FRONT_CAMERA_CAPTURE_DEVICE);
        accountConfig.getVideoConfig().setDefaultRenderDevice(SipServiceConstants.DEFAULT_RENDER_DEVICE);
    }
    /*          Utilities end           */

    /*****          Object overrides        ******/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SipAccountData that = (SipAccountData) o;

        if (!Objects.equals(username, that.username)) return false;
        if (!Objects.equals(password, that.password)) return false;
        if (!Objects.equals(realm, that.realm)) return false;
        if (!Objects.equals(host, that.host)) return false;
        if (port != that.port) return false;
        if (tcpTransport != that.tcpTransport) return false;
        if (!Objects.equals(contactUriParams, that.contactUriParams)) return false;
        if (regExpirationTimeout != that.regExpirationTimeout) return false;
        if (!Objects.equals(callId, that.callId)) return false;

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
        result = 31 * result + contactUriParams.hashCode();
        result = 31 * result + regExpirationTimeout;
        result = 31 * result + callId.hashCode();
        return result;
    }

    SipAccountData getDeepCopy() {
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        SipAccountData temp = SipAccountData.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return temp;
    }
    /*          Object overrides end        */
}
