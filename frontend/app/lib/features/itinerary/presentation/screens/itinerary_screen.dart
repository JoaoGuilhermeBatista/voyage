import 'package:flutter/material.dart';

class ItineraryScreen extends StatelessWidget {
  final String id;
  const ItineraryScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(child: Text('Itinerary $id')),
    );
  }
}
