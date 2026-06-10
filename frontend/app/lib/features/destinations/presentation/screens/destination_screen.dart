import 'package:flutter/material.dart';

class DestinationScreen extends StatelessWidget {
  final String id;
  const DestinationScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(child: Text('Destination $id')),
    );
  }
}
