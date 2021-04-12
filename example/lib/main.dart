 
import 'package:flutter/material.dart';
// import 'package:fluttertoast/fluttertoast.dart';
import 'package:sip_native/sip_native.dart';
import 'package:sip_native_example/home.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SipNative.initPlugin().then((SipNativeSupport value) {
    // Fluttertoast.showToast(
    //   msg: "Voip supported: ${value.isVoipSupported}\n" +
    //       "SipManager supported: ${value.isSipManagerSupported}",
    //   backgroundColor: value.isVoipSupported ? Colors.green : Colors.red,
    //   toastLength: Toast.LENGTH_LONG,
    // );
  });
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomePage(),
    );
  }
}
