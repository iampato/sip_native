import 'package:flutter/material.dart';
import 'dart:async';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:sip_native/sip_native.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String _registrationState = "UNKNOWN";
  String _callsState = "UNKNOWN";

  @override
  void initState() {
    super.initState();
    initRegistrationStream();
  }

  @override
  void dispose() {
    SipNative.disconnectSip();
    super.dispose();
  }

  initRegistrationStream() {
    try {
      SipNative.registrationStateStream().listen((event) {
        setState(() {
          _registrationState = event.toString();
        });
      });
    } catch (e) {
      print(e.toString());
      Fluttertoast.showToast(
        msg: "Registration stream error\n${e.toString()}",
      );
    }
  }

  initCallsStream() {
    try {
      SipNative.callsStateStream().listen((event) {
        setState(() {
          _callsState = event.toString();
        });
      });
    } catch (e) {
      print(e.toString());
      Fluttertoast.showToast(
        msg: "Calls stream error\n${e.toString()}",
      );
    }
  }

  Future<bool> connectToSip(
    String username,
    String password,
    String domain,
  ) async {
    bool response = await SipNative.initSipConnection(
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
                    await SipNative.requestPermissions();
                  },
                ),
                ElevatedButton(
                  child: Text("2. SIP connect"),
                  onPressed: () async {
                    String username = "254717008247";
                    String password = "475bbd248835981240e0fab16cdeb5af";
                    String domain = "138.68.167.56";
                    bool response = await connectToSip(
                      username,
                      password,
                      domain,
                    );
                    print("Sip connect response: $response");
                  },
                ),
                ElevatedButton(
                  child: Text("3. Init registration stream"),
                  onPressed: () {
                    initRegistrationStream();
                  },
                ),
                ElevatedButton(
                  child: Text("3. SIP disconnect"),
                  onPressed: () async {
                    await SipNative.disconnectSip();
                  },
                ),
                ElevatedButton(
                  child: Text("Init call"),
                  onPressed: () async {
                    bool res = await SipNative.initCall("254727751850");
                    if (res) {
                      initCallsStream();
                    }
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
