# PJSIP Android
NOTICE! This project is under heavy development, so don't use it in your app right now!

Native PJSIP library for Android is compiled using [PJSIP Android Builder](https://github.com/VoiSmart/pjsip-android-builder)

What you need to work with this library:
- An android device with Android API 18+ (4.3.3 or higher)
- A PBX (E.g. VoiSmart Orchestra NG or FreeSWITCH, which is open). I'm not going to enter in the detail of how to properly configure your PBX, because that's a different topic and there are excellent tutorials out there.

# State of the art
## What is tested and is working:
- Single account
- Make a single call
- In-Call operations: mute, unmute, hold, un-hold, transfer, send DTMF (RFC 2833)
- Accept an incoming call
- Decline an incoming call
- Get/Set codec priorities
- Hang up all active calls
- Hold all active calls

## What is missing (contributions are welcome):
- Multiple calls support
  - be able to handle other calls coming in while you have an active call
  - be able to hold the current call and make another one (this is the base for attended transfers and conference calls)
- Conference calls
- Attended call transfer
- Video support
- Complete multiple accounts support
- Respond to a call and play a sound file
- Support for In-Call RTCP signaling to get call statistics
- Other things which I'm not aware at the moment...
