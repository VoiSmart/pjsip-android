package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a codec priority info.
 * @author gotev (Aleksandar Gotev)
 */
public class CodecPriority implements Parcelable, Comparable<CodecPriority> {

    public static int PRIORITY_MAX = 254;
    public static int PRIORITY_MIN = 1;
    public static int PRIORITY_DISABLED = 0;

    private String mCodecId;
    private int mPriority;

    public CodecPriority() {
    }

    public CodecPriority(String codecId, short priority) {
        mCodecId = codecId;
        mPriority = priority;
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<CodecPriority> CREATOR =
            new Parcelable.Creator<CodecPriority>() {
                @Override
                public CodecPriority createFromParcel(final Parcel in) {
                    return new CodecPriority(in);
                }

                @Override
                public CodecPriority[] newArray(final int size) {
                    return new CodecPriority[size];
                }
            };

    public CodecPriority(Parcel in) {
        mCodecId = in.readString();
        mPriority = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(mCodecId);
        parcel.writeInt(mPriority);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getCodecId() {
        return mCodecId;
    }

    public int getPriority() {
        return mPriority;
    }

    public CodecPriority setPriority(int mPriority) {
        if (mPriority > PRIORITY_MAX) {
            this.mPriority = PRIORITY_MAX;
        } else if (mPriority < PRIORITY_DISABLED) {
            this.mPriority = PRIORITY_DISABLED;
        } else {
            this.mPriority = mPriority;
        }
        return this;
    }

    public String getCodecName() {
        return mCodecId.split("/")[0];
    }

    public int getCodecBitrateInKbitPerSecond() {
        return Integer.parseInt(mCodecId.split("/")[1]) / 1000;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodecPriority that = (CodecPriority) o;

        return mCodecId.equals(that.mCodecId);

    }

    @Override
    public int hashCode() {
        return mCodecId.hashCode();
    }

    @Override
    public String toString() {
        return "CodecID: " + mCodecId + ", Priority: " + mPriority;
    }

    @Override
    public int compareTo(CodecPriority another) {
        if (another == null) return -1;

        if (mPriority == another.mPriority) return 0;

        return (mPriority > another.mPriority) ? -1 : 1;
    }
}
