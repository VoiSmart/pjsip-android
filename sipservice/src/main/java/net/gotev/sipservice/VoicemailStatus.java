package net.gotev.sipservice;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voice-message state carried by an MWI NOTIFY (RFC 3842). Parcelable so it can ride a
 * single broadcast extra, mirroring {@link RtpStreamStats}.
 * <p>
 * Parsing follows RFC 3842's {@code application/simple-message-summary} body:
 * <ol>
 *   <li>the body must be that content type (other event bodies are ignored);</li>
 *   <li>{@code Messages-Waiting: yes|no} is the authoritative "are there new messages"
 *       flag — it can be {@code yes} with no per-class line at all;</li>
 *   <li>the optional {@code Voice-Message: new/old (urgentNew/urgentOld)} line refines it
 *       with counts when present.</li>
 * </ol>
 */
public class VoicemailStatus implements Parcelable {

    private final boolean messagesWaiting;
    private final int newMessages;
    private final int oldMessages;
    private final int urgentNew;
    private final int urgentOld;

    VoicemailStatus(boolean messagesWaiting, int newMessages, int oldMessages, int urgentNew,
                    int urgentOld) {
        this.messagesWaiting = messagesWaiting;
        this.newMessages = newMessages;
        this.oldMessages = oldMessages;
        this.urgentNew = urgentNew;
        this.urgentOld = urgentOld;
    }

    // The only body content type the message-summary event package defines.
    private static final Pattern CONTENT_TYPE = Pattern.compile("Content-Type:\\s*application" +
            "/simple-message-summary", Pattern.CASE_INSENSITIVE);
    // Authoritative waiting flag.
    private static final Pattern MESSAGES_WAITING = Pattern.compile("Messages-Waiting:\\s*" +
            "(yes|no)", Pattern.CASE_INSENSITIVE);
    // Optional per-class counts: <new>/<old>[ (<urgentNew>/<urgentOld>)].
    private static final Pattern VOICE_MESSAGE = Pattern.compile("Voice-Message:\\s*(\\d+)" +
            "\\s*/\\s*(\\d+)(?:\\s*\\(\\s*(\\d+)\\s*/\\s*(\\d+)\\s*\\))?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parse the whole NOTIFY message. Returns an all-clear status when the body isn't a
     * simple-message-summary or is empty/unparseable. Never throws.
     */
    public static VoicemailStatus parse(String wholeMessage) {
        if (wholeMessage == null || wholeMessage.isEmpty()) {
            return none();
        }
        // 1) Only interpret simple-message-summary bodies.
        if (!CONTENT_TYPE.matcher(wholeMessage).find()) {
            return none();
        }
        // 2) Messages-Waiting is the source of truth for "something is waiting".
        final Matcher waitingMatcher = MESSAGES_WAITING.matcher(wholeMessage);
        final boolean waiting =
                waitingMatcher.find() && "yes".equalsIgnoreCase(waitingMatcher.group(1));
        // 3) Refine with per-class counts when the optional line is present.
        final Matcher voiceMatcher = VOICE_MESSAGE.matcher(wholeMessage);
        if (voiceMatcher.find()) {
            return new VoicemailStatus(waiting, safeParse(voiceMatcher.group(1)),
                    safeParse(voiceMatcher.group(2)), safeParse(voiceMatcher.group(3)),
                    safeParse(voiceMatcher.group(4)));
        }
        return new VoicemailStatus(waiting, 0, 0, 0, 0);
    }

    private static VoicemailStatus none() {
        return new VoicemailStatus(false, 0, 0, 0, 0);
    }

    private static int safeParse(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static final Parcelable.Creator<VoicemailStatus> CREATOR =
            new Parcelable.Creator<VoicemailStatus>() {
        @Override
        public VoicemailStatus createFromParcel(final Parcel in) {
            return new VoicemailStatus(in);
        }

        @Override
        public VoicemailStatus[] newArray(final int size) {
            return new VoicemailStatus[size];
        }
    };

    private VoicemailStatus(Parcel in) {
        this.messagesWaiting = in.readByte() != 0;
        this.newMessages = in.readInt();
        this.oldMessages = in.readInt();
        this.urgentNew = in.readInt();
        this.urgentOld = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByte((byte) (messagesWaiting ? 1 : 0));
        parcel.writeInt(newMessages);
        parcel.writeInt(oldMessages);
        parcel.writeInt(urgentNew);
        parcel.writeInt(urgentOld);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean getMessagesWaiting() {
        return messagesWaiting;
    }

    public int getNewMessages() {
        return newMessages;
    }

    public int getOldMessages() {
        return oldMessages;
    }

    public int getUrgentNew() {
        return urgentNew;
    }

    public int getUrgentOld() {
        return urgentOld;
    }

    /**
     * True when the mailbox has messages waiting. Per RFC 3842 the {@code Messages-Waiting}
     * flag is authoritative and is always {@code yes} when new messages exist, so it alone
     * decides this.
     */
    public boolean hasNewMessages() {
        return messagesWaiting;
    }

    @NonNull
    @Override
    public String toString() {
        return "VoicemailStatus{waiting=" + messagesWaiting + ", new=" + newMessages + ", old=" + oldMessages + ", urgentNew=" + urgentNew + ", urgentOld=" + urgentOld + "}";
    }
}
