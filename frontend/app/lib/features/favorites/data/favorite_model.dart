import '../../destinations/data/destination_summary_model.dart';

class FavoriteModel {
  final String id;
  final DestinationSummaryModel? destination;

  const FavoriteModel({required this.id, this.destination});

  factory FavoriteModel.fromJson(Map<String, dynamic> json) {
    return FavoriteModel(
      id: json['id'] as String,
      destination: json['destination'] != null
          ? DestinationSummaryModel.fromJson(
              json['destination'] as Map<String, dynamic>)
          : null,
    );
  }
}
