/// Modelos espelhando os DTOs do backend em
/// backend/api/voyage/.../features/maps/dto/{DirectionsResponseDTO,EmbedUrlResponseDTO}.java
///
/// Ambos trazem um flag `configured`: quando a chave do Google Maps não
/// está configurada no backend, `configured` vem `false` e os demais
/// campos vêm nulos — a UI deve esconder a seção graciosamente nesse
/// caso (mesmo espírito de CurrencyRatesModel/`_CurrencyRatesBar`).
class DirectionsModel {
  final bool configured;
  final double? distanceMeters;
  final int? durationSeconds;
  final String? distanceText;
  final String? durationText;

  const DirectionsModel({
    required this.configured,
    this.distanceMeters,
    this.durationSeconds,
    this.distanceText,
    this.durationText,
  });

  factory DirectionsModel.fromJson(Map<String, dynamic> json) {
    return DirectionsModel(
      configured: json['configured'] as bool,
      distanceMeters: (json['distanceMeters'] as num?)?.toDouble(),
      durationSeconds: json['durationSeconds'] as int?,
      distanceText: json['distanceText'] as String?,
      durationText: json['durationText'] as String?,
    );
  }
}

class EmbedUrlModel {
  final bool configured;
  final String? embedUrl;

  const EmbedUrlModel({
    required this.configured,
    this.embedUrl,
  });

  factory EmbedUrlModel.fromJson(Map<String, dynamic> json) {
    return EmbedUrlModel(
      configured: json['configured'] as bool,
      embedUrl: json['embedUrl'] as String?,
    );
  }
}
