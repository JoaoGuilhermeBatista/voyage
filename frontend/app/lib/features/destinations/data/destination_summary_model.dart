class DestinationSummaryModel {
  final String id;
  final String slug;
  final String name;
  final String country;
  final String city;
  final String tagline;
  final String heroImageUrl;
  final String? signature;
  final List<String> tags;

  const DestinationSummaryModel({
    required this.id,
    required this.slug,
    required this.name,
    required this.country,
    required this.city,
    required this.tagline,
    required this.heroImageUrl,
    this.signature,
    required this.tags,
  });

  factory DestinationSummaryModel.fromJson(Map<String, dynamic> json) {
    return DestinationSummaryModel(
      id: json['id'] as String,
      slug: json['slug'] as String,
      name: json['name'] as String,
      country: json['country'] as String,
      city: json['city'] as String,
      tagline: json['tagline'] as String,
      heroImageUrl: json['heroImageUrl'] as String? ?? '',
      signature: json['signature'] as String?,
      tags: (json['tags'] as List<dynamic>?)?.cast<String>() ?? [],
    );
  }
}
