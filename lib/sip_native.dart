import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class SipNativeSupport {
  final bool isVoipSupported;
  final bool isSipManagerSupported;

  SipNativeSupport(this.isVoipSupported, this.isSipManagerSupported);
}

class SipNative {
  static ValueNotifier _connectedNotifier = ValueNotifier(false);

  static StreamSubscription _streamSubscription;
  static StreamController<String> _streamController =
      StreamController.broadcast();

  static const MethodChannel _methodChannel =
      const MethodChannel('sip_native/method');
  static const EventChannel _eventChannel =
      const EventChannel('sip_native/event');

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

  static Future<SipNativeSupport> initPlugin() async {
    Map response = await _methodChannel.invokeMethod('prepSip');
    bool isVoipSupported = response["voip"];
    bool isSipManagerSupported = response["voip"];
    return SipNativeSupport(isVoipSupported, isSipManagerSupported);
  }

  static Future<bool> requestPermissions() async {
    bool response = await _methodChannel.invokeMethod('request_permissions');
    return response ?? false;
  }

  static Future<bool> initSipConnection({
    @required String username,
    @required String password,
    @required String domain,
  }) async {
    bool response = await _methodChannel.invokeMethod(
      'initSip',
      <String, dynamic>{
        'username': username,
        'password': password,
        'domain': domain,
      },
    );
    if (response) {
      _connectedNotifier.value = true;
    }
    return response ?? false;
  }

  static Future<void> initCall() async {
    await _methodChannel.invokeMethod('initCall');
  }

  static Future<void> disconnectSip() async {
    try {
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
