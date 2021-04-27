import 'package:flutter/material.dart';
import 'dart:async';
import 'package:sip_native/sip_native.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  bool _registrationState;
  String _callsState = "UNKNOWN";
  SipNative _sipNative;

  @override
  void initState() {
    super.initState();
    _sipNative = SipNative();
    _sipNative.onSipStateChanged.listen((map) {
      final state = map['call_state'];
      // final remoteUri = map['remote_uri'];
      print("************************************");
      print("******************State: $state******************");
      print("************************************");
      switch (state) {
        case "CALLING":
          break;
        case "INCOMING":
          break;

        case "EARLY":
          break;

        case "CONNECTING":
          break;

        case "CONFIRMED":
          break;

        case "DISCONNECTED":
          break;

        default:
          break;
      }

      setState(() {
        this._callsState = state;
      });
    });
  }

  @override
  void dispose() {
    SipNative().disconnectSip();
    super.dispose();
  }

  Future<bool> connectToSip(
    String username,
    String password,
    String domain,
  ) async {
    bool response = await _sipNative.initSipConnection(
      username: username,
      password: password,
      domain: domain,
    );
    return response;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('SIP Plugin example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Center(
              child: Text('Registration state: $_registrationState\n'),
            ),
            Text('Calls state: $_callsState\n'),
            Wrap(
              spacing: 5,
              children: [
                ElevatedButton(
                  child: Text("1. Get permissions"),
                  onPressed: () async {
                    await _sipNative.requestPermissions();
                  },
                ),
                ElevatedButton(
                  child: Text("1.1. Init Plugin"),
                  onPressed: () async {
                    await _sipNative.initPlugin();
                  },
                ),
                ElevatedButton(
                  child: Text("2. SIP connect"),
                  onPressed: () async {
                    String username = "254716843446";
                    String password = "ad21bd5330b1cdb2725df40a43622ae0";
                    String domain = "138.68.167.56";
                    bool response = await connectToSip(
                      username,
                      password,
                      domain,
                    );
                    setState(() {
                      _registrationState = response;
                    });
                    print("Sip connect response: $response");
                  },
                ),
                // ElevatedButton(
                //   child: Text("3. Init registration stream"),
                //   onPressed: () {
                //     initRegistrationStream();
                //   },
                // ),
                ElevatedButton(
                  child: Text("3. SIP disconnect"),
                  onPressed: () async {
                    await _sipNative.disconnectSip();
                  },
                ),
                ElevatedButton(
                  child: Text("Init call"),
                  onPressed: () async {
                    bool res = await _sipNative.initCall("254716843447");
                    // if (res) {
                    //   initCallsStream();
                    // }
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
