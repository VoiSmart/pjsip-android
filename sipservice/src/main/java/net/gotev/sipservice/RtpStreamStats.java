package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * sipservice
 *
 * Created by aenonGit on 19/04/19.
 * Copyright © 2019 VoiSmart S.r.l. All rights reserved.
 */
@SuppressWarnings("unused")
public class RtpStreamStats implements Parcelable {

    private final int pkt;
    private final int discard;
    private final int loss;
    private final int reorder;
    private final int dup;
    private final Jitter jitter;

    RtpStreamStats(int pkts, int discard, int loss, int reorder, int dup, Jitter jitter) {
        this.pkt = pkts;
        this.discard = discard;
        this.loss = loss;
        this.reorder = reorder;
        this.dup = dup;
        this.jitter = jitter;
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<RtpStreamStats> CREATOR =
            new Parcelable.Creator<RtpStreamStats>() {
                @Override
                public RtpStreamStats createFromParcel(final Parcel in) {
                    return new RtpStreamStats(in);
                }

                @Override
                public RtpStreamStats[] newArray(final int size) {
                    return new RtpStreamStats[size];
                }
            };

    private RtpStreamStats(Parcel in) {
        this.pkt = in.readInt();
        this.discard = in.readInt();
        this.loss = in.readInt();
        this.reorder = in.readInt();
        this.dup = in.readInt();
        this.jitter = in.readParcelable(Jitter.class.getClassLoader());
    }

    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(pkt);
        parcel.writeInt(discard);
        parcel.writeInt(loss);
        parcel.writeInt(reorder);
        parcel.writeInt(dup);
        parcel.writeParcelable(jitter, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "Pkts: "+pkt+"\n"
                +"Discard: "+discard+"\n"
                +"Loss: "+loss+"\n"
                +"Reorder: "+reorder+"\n"
                +"Duplicate: "+dup+"\n"
                +"Jitter: "+jitter.toString()+"\n";
    }

    public int getPkts() {
        return pkt;
    }

    public int getDiscard() {
        return discard;
    }

    public int getLoss() {
        return loss;
    }

    public int getReorder() {
        return reorder;
    }

    public int getDup() {
        return dup;
    }

    public Jitter getJitter() {
        return jitter;
    }
}
