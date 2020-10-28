package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * connect
 *
 * Created by aenonGit on 23/04/19.
 * Copyright © 2019 VoiSmart S.r.l. All rights reserved.
 */
@SuppressWarnings("unused")
public class Jitter implements Parcelable {
    private final int max;
    private final int mean;
    private final int min;

    Jitter(int max, int mean, int min) {
        this.max = max;
        this.mean = mean;
        this.min = min;
    }

    public static final Parcelable.Creator<Jitter> CREATOR =
            new Parcelable.Creator<Jitter>() {
                @Override
                public Jitter createFromParcel(final Parcel in) {
                    return new Jitter(in);
                }

                @Override
                public Jitter[] newArray(final int size) {
                    return new Jitter[size];
                }
            };

    private Jitter(Parcel in) {
        this.max = in.readInt();
        this.mean = in.readInt();
        this.min = in.readInt();
    }

    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(max);
        parcel.writeInt(mean);
        parcel.writeInt(min);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "Max: "+max+" Mean: "+mean+" Min: "+min;
    }

    public int getMax() {
        return max;
    }

    public int getMean() {
        return mean;
    }

    public int getMin() {
        return min;
    }
}
