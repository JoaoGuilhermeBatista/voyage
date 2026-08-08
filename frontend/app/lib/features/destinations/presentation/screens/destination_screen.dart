import 'package:flutter/material.dart';

class DestinationScreen extends StatelessWidget {
  final String slug;
  const DestinationScreen({super.key, required this.slug});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(child: Text('Destination $slug')),
    );
  }
}
