# PJSIP Android
**NOTICE!** This project is experimental, so don't use it in production! No support is provided and only bug fixes and pull requests can be accepted.

What you need to work with this library:
- An android device with Android API 18+ (4.3.3 or higher)
- A PBX (E.g. VoiSmart Orchestra NG or FreeSWITCH, which is open). I'm not going to enter in the detail of how to properly configure your PBX, because that's a different topic and there are excellent tutorials out there.

# Architecture
![Architecture](https://github.com/VoiSmart/pjsip-android/blob/master/pjsip-android.png "Architecture")

This project wraps the standard PJSUA2 bindings in a background service and completely hides SIP from the rest of the application, to be able to have VoIP capabilities at a high level of abstraction. You can talk to the service using static methods and you will receive broadcast intents as a response. To talk to the service, refer to [SipServiceCommand](https://github.com/VoiSmart/pjsip-android/blob/master/sipservice/src/main/java/net/gotev/sipservice/SipServiceCommand.java) static methods. To receive events from the service, extend [BroadcastEventReceiver](https://github.com/VoiSmart/pjsip-android/blob/master/sipservice/src/main/java/net/gotev/sipservice/BroadcastEventReceiver.java). To see which events are emitted by the service, refer to [BroadcastEventEmitter](https://github.com/VoiSmart/pjsip-android/blob/master/sipservice/src/main/java/net/gotev/sipservice/BroadcastEventEmitter.java). All the commands that you will send to the service will get executed in the background and without blocking your main thread. Once the service has done the requested job or operation, it will notify you with a broadcast intent. So, you don't risk blocking your UI thread in any way.

Native PJSIP library for Android is compiled using [PJSIP Android Builder](https://github.com/VoiSmart/pjsip-android-builder)

# State of the art
## What is tested and working:
- Single account
- Make a single call
- In-Call operations
  - mute
  - unmute
  - hold
  - un-hold
  - blind transfer
  - send DTMF (RFC 2833)
- Accept an incoming call
- Answer with video an incoming call
- Decline an incoming call
- Get/Set codec priorities
- Hang up all active calls
- Hold all active calls
- Hold/Decline sip call when incoming/outgoing gsm call
- Video support
  - switch camera
  - mute/unmute video
  - video preview
- Use of a fixed SIP `Call-ID Header`. Refer to [this](https://github.com/VoiSmart/pjsip-android-builder/tree/master/patches/fixed_callid) for more details

## What is missing (contributions are welcome):
- Multiple calls support
  - be able to handle other calls coming in while you have an active call
  - be able to hold the current call and make another one (this is the base for attended transfers and conference calls)
- Conference calls
- Attended call transfer
- Complete multiple accounts support
- Respond to a call and play a sound file
- Support for In-Call RTCP signaling to get call statistics
- Other things which I'm not aware at the moment...

# Recompile native libraries
Refer to [PJSIP Android Builder](https://github.com/VoiSmart/pjsip-android-builder)
