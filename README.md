# PJSIP Android

**NOTICE!** This project is experimental, so don't use it in production! No support is provided and only bug fixes and pull requests can be accepted.

What you need to work with this library:

- An android device with Android API 23
- A PBX (E.g. VoiSmart Orchestra NG or FreeSWITCH, which is open). I'm not going to enter in the detail of how to properly configure your PBX, because that's a different topic and there are excellent tutorials out there.

## Architecture

![Architecture](https://github.com/VoiSmart/pjsip-android/blob/master/pjsip-android.png "Architecture")

This project wraps the standard PJSUA2 bindings in a background service and completely hides SIP from the rest of the application, to be able to have VoIP capabilities at a high level of abstraction. You can talk to the service using static methods and you will receive broadcast intents as a response. To talk to the service, refer to [SipServiceCommand](https://github.com/VoiSmart/pjsip-android/blob/master/sipservice/src/main/java/net/gotev/sipservice/SipServiceCommand.java) static methods. To receive events from the service, extend [BroadcastEventReceiver](https://github.com/VoiSmart/pjsip-android/blob/master/sipservice/src/main/java/net/gotev/sipservice/BroadcastEventReceiver.java). To see which events are emitted by the service, refer to [BroadcastEventEmitter](https://github.com/VoiSmart/pjsip-android/blob/master/sipservice/src/main/java/net/gotev/sipservice/BroadcastEventEmitter.java). All the commands that you will send to the service will get executed in the background and without blocking your main thread. Once the service has done the requested job or operation, it will notify you with a broadcast intent. So, you don't risk blocking your UI thread in any way.

Native PJSIP library for Android is compiled using [PJSIP Android Builder](https://github.com/VoiSmart/pjsip-android-builder)

## State of the art

### What is tested and working

- Single account
- Make a single call
- In-Call operations
  - mute
  - unmute
  - hold
  - un-hold
  - blind transfer
  - Attended call transfer
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
- Get Call Statistics on call disconnected
- Sip Credential encryption on device. Refer to [Android Jetpack Security](https://developer.android.com/topic/security/data) library for more details
- Call Reconnection: useful after a network disconnection, it will try to reconnect the call with new ip/port
- TLS and SRTP support
  - with server certificate verification option (enabled via SipServiceCommand#setVerifySipServerCert but to make it work, it is necessary to add a `ca-bundle.crt` file in the sip`./sipservice/src/main/assets/` directory; without that the verification will not be enabled)
- Silent Calls

### What is missing (contributions are welcome)

- Multiple calls support
  - be able to handle other calls coming in while you have an active call
- Conference calls
- Complete multiple accounts support
- Respond to a call and play a sound file
- Support for In-Call RTCP signaling to get call statistics
- Other things which I'm not aware at the moment...

### Used Libraries versions

- PJSIP: 2.12.1
- OpenSSL: 1.1.1k
- OpenH264: 2.1.0
- Opus: 1.3.1
- bcg729: 1.1.1

## Logging

This library ships with a default logger which logs with the default Android `Log` class and a default `DEBUG` loglevel.
You can customize such behaviour by either:

1. Setting a specific loglevel (see Logger#setLogLevel)
2. Setting your own LoggerDelegate. E.g.:

    ```java
    class SipServiceLogger extends Logger.LoggerDelegate {
        @Override
        public void error(String tag, String message) {
            Timber.tag(tag).e(message);
        }

        @Override
        public void error(String tag, String message, Throwable exception) {
            Timber.tag(tag).e(exception, message);
        }

        @Override
        public void debug(String tag, String message) {
            Timber.tag(tag).d(message);
        }

        @Override
        public void info(String tag, String message) {
            Timber.tag(tag).i(message);
        }
    }
    ```

   [Timber](https://github.com/JakeWharton/timber) can be configured as you wish, to log everything or just in debug mode, or log to anywhere.

## SIP Logging

To allow see PjSip logs you need to set `SipServiceUtils.ENABLE_SIP_LOGGING` to `true` which will log all pjsip logs while in debug mode.

## Recompile native libraries

Refer to [PJSIP Android Builder](https://github.com/VoiSmart/pjsip-android-builder)
