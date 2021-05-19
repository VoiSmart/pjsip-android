Change Log
==========

### Version 2.5.0

_2021-05-19_
 * **MinSdk increased** - Minimum SDK is now increased to api 21
 * **TargetSdk increased** - Minimum SDK is now increased to api 28
 * Bump BuildTools and CompileSdk to 30 and 30.0.3
 * Update libs:
    - PJSIP: 2.6 -> 2.11
    - OpenSSL: 1.0.2g -> 1.1.1k
    - OpenH264: 1.7.0 -> 2.1.0
    - Opus: 1.2.1 -> 1.3.1
    - bcg729: none -> 1.1.1 (previously it used a custom implementation)
 * Add call reconnection method: useful after a network disconnection, it will try to reconnect the call with new ip/port
 * Change default BroadcastEventEmitter NAMESPACE
 * Bump gradle versions

### Version 2.4.1

_2020-09-14_
 * **TargetSdk increased** - Target SDK is now increased to api 28
 * Fix travis file
 * Bump gradle plugin and wrapper

### Version 2.4.0

_2020-09-14_
 * **Tools increased** - Compile SDK and BuildTools are now increased to 29
 * Fix some small warnings
 * **Issues fixed**:
   + #115: Crashlytics has been removed and default logging level is now always debug. The logging level has been delegated to the loggerdelegate if set. See [Readme](README.md) for more info.

### Version 2.3.1

_2020-03-11_
 * Fix Crash if starting SipService without enabling encryption
 * **Issues fixed**:
   + #91

### Version 2.3.0

_2019-12-18_

 * **Data encryption** - It is now possible to enable encryption for stored credentials in SipAccountData
 * **\[NEW\] OpenH264** version 1.7.0
 * **Issues fixed**:
   + #87

### Version 2.2.3

_2019-07-11_

 * **Issues fixed**:
   + #78
   + #76

### Version 2.2.2

_2019-05-29_

 * **MinSdk increased** - Minimum SDK is now increased to 19
 * Improved SipAccountData class overrides

### Version 2.2.1

_2019-05-24_

 * Fixed some error with missing default values

### Version 2.2.0

_2019-05-17_

 * **Use AccountRegConfig#setCallID** - Registration Call-ID can now be set from java code
 
 * **Issues fixed**:
   + #72

### Version 2.1.0

_2019-04-19_

 * **Retrieve call Stats** - Calls stats are sent via broadcast once call is DISCONNECTED

### Version 2.0.2

_2019-03-11_
 * **\[NEW\] PJSIP** version 2.6
 * **\[NEW\] OpenSSL** version 1.0.2g
 * **\[NEW\] Opus** version 1.2.1
 *  **Builds now working via jitpack** - Added the required fixes to correctly build.
 *  **Ringing removed** - Start/Stop ringtone method are removed, ringing must now implemented outside the library. Issue #58.
