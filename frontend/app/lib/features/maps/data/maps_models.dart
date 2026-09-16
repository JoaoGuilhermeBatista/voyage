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

// EmbedUrlModel também é reaproveitado pra GET /maps/embed-url/directions
// (mesmo shape de resposta — { configured, embedUrl } — muda só a URL de
// origem, que vem no modo "directions" da Embed API em vez de "place").
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

/// Um bairro sugerido para hospedagem, com a frase de vantagem explicando
/// por que vale a pena se hospedar ali para o budget escolhido.
class NeighborhoodRecommendation {
  final String name;
  final String advantage;

  const NeighborhoodRecommendation({
    required this.name,
    required this.advantage,
  });

  factory NeighborhoodRecommendation.fromJson(Map<String, dynamic> json) {
    return NeighborhoodRecommendation(
      name: json['name'] as String,
      advantage: json['advantage'] as String,
    );
  }
}

/// Espelha NeighborhoodSuggestionsResponseDTO — sugestões de bairro para
/// hospedagem conforme o destino e o budget da viagem. Mesmo espírito de
/// `configured` gracioso de DirectionsModel/EmbedUrlModel: `configured ==
/// false` (Maps não configurado no backend) ou `neighborhoods` vazio devem
/// ser tratados sem quebrar a UI.
class NeighborhoodSuggestionsModel {
  final bool configured;
  final List<NeighborhoodRecommendation> neighborhoods;

  const NeighborhoodSuggestionsModel({
    required this.configured,
    this.neighborhoods = const [],
  });

  factory NeighborhoodSuggestionsModel.fromJson(Map<String, dynamic> json) {
    return NeighborhoodSuggestionsModel(
      configured: json['configured'] as bool,
      neighborhoods: (json['neighborhoods'] as List<dynamic>?)
              ?.map((e) =>
                  NeighborhoodRecommendation.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const [],
    );
  }
}
