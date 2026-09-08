import '../../destinations/data/destination_summary_model.dart';

/// Espelha `CollectionDTO` do backend (`GET /collections`, `GET /collections/:slug`)
/// — coleções editoriais reais (ex.: "Destinos românticos", "Alta gastronomia"),
/// curadas manualmente no banco, com 5-6 destinos cada. Substitui as seções
/// temáticas fake que a Home montava filtrando `tags` no client (ver
/// [[luvoy-pendencias]] — item "Coleções editoriais").
class CollectionModel {
  final String id;
  final String slug;
  final String title;
  final String? italic;
  final String? eyebrow;
  final String? description;
  final int orderIndex;
  final List<DestinationSummaryModel> destinations;

  const CollectionModel({
    required this.id,
    required this.slug,
    required this.title,
    this.italic,
    this.eyebrow,
    this.description,
    required this.orderIndex,
    required this.destinations,
  });

  factory CollectionModel.fromJson(Map<String, dynamic> json) {
    return CollectionModel(
      id: json['id'] as String,
      slug: json['slug'] as String,
      title: json['title'] as String,
      italic: json['italic'] as String?,
      eyebrow: json['eyebrow'] as String?,
      description: json['description'] as String?,
      orderIndex: json['orderIndex'] as int? ?? 0,
      destinations: (json['destinations'] as List<dynamic>? ?? [])
          .map(
            (d) => DestinationSummaryModel.fromJson(d as Map<String, dynamic>),
          )
          .toList(),
    );
  }
}
