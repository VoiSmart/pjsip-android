package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a codec priority info.
 * @author gotev (Aleksandar Gotev)
 */
public class CodecPriority implements Parcelable, Comparable<CodecPriority> {

    public static final int PRIORITY_MAX = 254;
    public static final int PRIORITY_MAX_VIDEO = 128;
    public static final int PRIORITY_MIN = 1;
    public static final int PRIORITY_DISABLED = 0;

    private static final String G729_LABEL = "G.729";
    private static final String PCMU_LABEL = "PCMU";
    private static final String PCMA_LABEL = "PCMA";
    private static final String SPEEX_LABEL = "Speex";
    private static final String G722_LABEL = "G.722";
    private static final String G7221_LABEL = "G.722.1";
    private static final String OPUS_LABEL = "Opus";

    private final String mCodecId;
    private int mPriority;

    CodecPriority(String codecId, short priority) {
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

    private CodecPriority(Parcel in) {
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

    public void setPriority(int mPriority) {
        if (mPriority > PRIORITY_MAX) {
            this.mPriority = PRIORITY_MAX;
        } else this.mPriority = Math.max(mPriority, PRIORITY_DISABLED);
    }

    public String getCodecName() {
        String name = mCodecId.split("/")[0];

        switch (name) {
            case "G729": return G729_LABEL;
            case "PCMU": return PCMU_LABEL;
            case "PCMA": return PCMA_LABEL;
            case "speex": return SPEEX_LABEL;
            case "G722": return G722_LABEL;
            case "G7221": return G7221_LABEL;
            case "opus": return OPUS_LABEL;
            default: return name;
        }
    }

    public int getCodecSampleRateInKhz() {
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
