import 'package:flutter/material.dart';
import 'dart:ui' as ui;

void run(Widget child) async {
  await ui.webOnlyInitializePlatform();
  runApp(Previewer(child));
}

class Previewer extends StatelessWidget {
  final Widget child;

  Previewer(this.child);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Widget Preview',
      theme: ThemeData(
        primarySwatch: Colors.amber,
      ),
      home: child,
    );
  }
}

