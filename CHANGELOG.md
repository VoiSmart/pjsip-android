Change Log
==========

### Version 2.2.3

_2019-07-11_

 * **Issues fixed**:
   + #78
   + #76

### Version 2.2.2

_2019-05-29_

 * **MinSdk increased** - Minimum SDK is not increased to 19
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

 *  **Builds now working via jitpack** - Added the required fixes to correctly build.
 *  **Ringing removed** - Start/Stop ringtone method are removed, ringing must now implemented outside the library. Issue #58.
