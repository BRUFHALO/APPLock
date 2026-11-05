import 'package:flutter/material.dart';

import 'screens/calculator_screen.dart';
import 'services/widget_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  registerWidgetCallback();
  runApp(const DontLookAtMeApp());
}

class DontLookAtMeApp extends StatelessWidget {
  const DontLookAtMeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'biDolar',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFD4AF37),
          brightness: Brightness.dark,
        ),
        scaffoldBackgroundColor: Colors.black,
      ),
      home: const CalculatorScreen(),
    );
  }
}
