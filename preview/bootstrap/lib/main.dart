import 'package:flutter/material.dart';
import 'dart:ui' as ui;

late final PreviewerController controller;

void run(Widget child) async {
  await ui.webOnlyInitializePlatform();

  controller = PreviewerController(child);
  runApp(MaterialApp(
    title: 'Widget Preview',
    theme: ThemeData(
      primarySwatch: Colors.amber,
    ),
    home: Previewer(
      controller: controller,
    ),
  ));
}

void reload(Widget child) {
  controller.setWidget(child);
}

class PreviewerController extends ChangeNotifier {
  late Widget _widget;

  PreviewerController(this._widget);

  Widget get widget => _widget;

  void setWidget(Widget value) {
    _widget = value;
    notifyListeners();
  }
}

class Previewer extends StatefulWidget {
  final PreviewerController controller;

  const Previewer({Key? key, required this.controller}) : super(key: key);

  @override
  _PreviewerState createState() => _PreviewerState();
}

class _PreviewerState extends State<Previewer> {
  @override
  void initState() {
    super.initState();

    widget.controller.addListener(_onUpdate);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onUpdate);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return widget.controller.widget;
  }

  void _onUpdate() {
    setState(() {});
  }
}
