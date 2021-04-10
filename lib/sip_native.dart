import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

/// enum that respresent the different sip protocol supported
enum SipProtocol {UDP,TCP,TLS}

class SipNativeSupport {
  final bool isVoipSupported;
  final bool isSipManagerSupported;

  SipNativeSupport(this.isVoipSupported, this.isSipManagerSupported);
}

class SipNative {
  static bool _speaker = false;
  static ValueNotifier _connectedNotifier = ValueNotifier(false);

  static StreamSubscription _streamSubscription;
  static StreamController<String> _streamController =
      StreamController.broadcast();

  static const MethodChannel _methodChannel =
      const MethodChannel('sip_native/method');
  static const EventChannel _eventChannel =
      const EventChannel('sip_native/register_events');

  /// registrationStateStream
  /// return stream of the different sip connections state
  /// They include:
  /// 1. UNKNOWN -> we do not know anything (it's the initial)
  /// 2. ONREGISTERING -> attempting to connect
  /// 3. ONREGISTRATIONDONE -> connected all is well
  /// 4. ONREGISTRATIONFAILED -> an error occurred and you should wrap this function
  ///    in a try catch to catch the exception plus the reason for the failure
  static Stream<String> registrationStateStream() {
    _streamSubscription?.cancel();
    if (_streamController == null) {
      _streamController = StreamController.broadcast();
    }
    _streamController.sink.add("UNKNOWN");
    _connectedNotifier.addListener(() {
      if (_connectedNotifier.value == true) {
        debugPrint("Hallelujah");
        _streamSubscription =
            _eventChannel.receiveBroadcastStream().listen((event) {
          if (_streamController == null) {
            _streamController = StreamController.broadcast();
          }
          _streamController.sink.add(event.toString());
        });
      }
    });

    return _streamController.stream;
  }

  /// initPlugin
  /// return a complex data type <SipNativeSupport> which is a class that contains two fields
  /// 1. isVoipSupported (boolean)
  /// 2. isSipManagerSupported (boolean)
  /// You should always call this method to check if the plugin supports your device
  /// please run the app in a real device <sipmanager> is not supported in emulators
  static Future<SipNativeSupport> initPlugin() async {
    Map response = await _methodChannel.invokeMethod('prepSip');
    bool isVoipSupported = response["voip"];
    bool isSipManagerSupported = response["voip"];
    return SipNativeSupport(isVoipSupported, isSipManagerSupported);
  }

  /// requestPermissions
  /// returns a future boolean if all is well a true if an error occurred false,and you should wrap this function
  /// in a try catch to catch the exception plus the reason for the failure
  /// please run this first before connecting to a sip server
  static Future<bool> requestPermissions() async {
    bool response = await _methodChannel.invokeMethod('request_permissions');
    return response ?? false;
  }

  /// initSipConnection
  /// requires:
  /// 1. username -> String : sip username required
  /// 2. password -> String : sip password required
  /// 3. domain -> String : sip domain required
  /// 4. port -> int : not required default port is 5060 (UDP)
  /// 5. protocol -> String : default is UDP but also TCP and TLS are supported
  static Future<bool> initSipConnection({
    @required String username,
    @required String password,
    @required String domain,
    int port = 5060, /// default UDP port is 5060
    SipProtocol sipProtocol = SipProtocol.UDP, /// default protocol is UDP
  }) async {
    bool response = await _methodChannel.invokeMethod(
      'initSip',
      <String, dynamic>{
        'username': username,
        'password': password,
        'domain': domain,
        'port':port,
        'protocol':sipProtocol.toString(),
      },
    );
    if (response) {
      _connectedNotifier.value = true;
    }
    return response ?? false;
  }

  /// initCall
  /// requires caller username and the server domain
  static Future<void> initCall(String username, String domain) async {
    await _methodChannel.invokeMethod(
      'initCall',
      <String, dynamic>{
        'uri': "$username@$domain",
      },
    );
  }

  /// endCall
  /// returns a future boolean if true the call ended successfully
  /// an exception is thrown if there was no call in progress was found
  static Future<bool> endCall() async {
    return await _methodChannel.invokeMethod("endCall");
  }

  /// holdCall
  /// returns a future boolean if true the call held successfully
  /// an exception is thrown if there was no call in progress was found
  static Future<bool> holdCall() async {
    return await _methodChannel.invokeMethod("holdCall");
  }
  /// muteCall
  /// returns a future boolean if true the call muted successfully
  /// an exception is thrown if there was no call in progress was found
  static Future<bool> muteCall() async {
    return await _methodChannel.invokeMethod("muteCall");
  }

  /// muteCall
  /// requires nothing the call has internal bool for state management
  /// returns a future boolean if true the call muted successfully
  /// an exception is thrown if there was no call in progress was found
  static Future<bool> changeSpeakerMode() async {
    return await _methodChannel.invokeMethod(
      "speakerMode",
      <String, dynamic>{
        'mode': !_speaker,
      },
    );
  }

  /// disconnectSip
  /// close everything
  static Future<void> disconnectSip() async {
    try {
      _speaker = false;
      bool isDisconnected = await _methodChannel.invokeMethod('discSip');
      debugPrint("isDisconnected: $isDisconnected");
      if (isDisconnected) {
        _streamController.sink.add("DISCONNECTED");
      } else {
        debugPrint("isDisconnected: $isDisconnected");
      }
    } catch (e) {
      debugPrint(e.toString());
    }
    _streamSubscription?.cancel();
    _streamController.close();
  }
}
