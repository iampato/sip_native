# sip_native

A native SIP flutter plugin

## Motivation
Currently our team is working on solution that requires and uses sip but available sip packages only support websocket and after struggles using it we were disappointed, so we decided to build one from scratch using [link here ](https://developer.android.com/guide/topics/connectivity/sip)

## Disclaimer
1. #### Android Supported Only
  Currently only android is supported even though flutter is fully cross platform
  
2. #### Support
  Even though the specs states the SIP API is available for Android devices from 2.3, it is not. The Android SIP API is not supported on all devices. Interestingly,   it is supported on my old 2.3 LG P970 and not my 4.4 Moto G. (By the way, you have to test on real device and not the Android emulator).
  Now there are alternatives:
  With the Android SIP API out, what are the alternatives? Well, there are a couple of open source SIP stacks for Android. The popular ones people talk about are:

- JAIN
- MjSIP
- PjSIP
- Doubango

more on this link: https://obem.be/2014/06/04/sip-on-android.html


## Getting Started
1. #### Install this plugin
2. #### SipNative plugin supported
   Check if the plugin is supported by the device
```
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SipNative.initPlugin().then((SipNativeSupport value) {
    Fluttertoast.showToast(
      msg: "Voip supported: ${value.isVoipSupported}\n" +
          "SipManager supported: ${value.isSipManagerSupported}",
      backgroundColor: value.isVoipSupported ? Colors.green : Colors.red,
      toastLength: Toast.LENGTH_LONG,
    );
  });
  runApp(MyApp());
}
   
```

2. #### Sip connect
3. #### Sip connection status listener
4. #### Sip init call
5. #### Sip call state status listener
6. #### Sip hold call
7. #### Sip mute call
8. #### Sip speaker mode
9. #### Sip end call
10. #### Sip disconnect

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

