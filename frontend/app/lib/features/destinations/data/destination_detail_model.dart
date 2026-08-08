class DestinationDetailModel {
  final String id;
  final String slug;
  final String name;
  final String country;
  final String city;
  final String tagline;
  final String? description;
  final String heroImageUrl;
  final String? signature;
  final List<String> tags;
  final List<DestinationSectionModel> sections;
  final List<PoiModel> pois;

  const DestinationDetailModel({
    required this.id,
    required this.slug,
    required this.name,
    required this.country,
    required this.city,
    required this.tagline,
    this.description,
    required this.heroImageUrl,
    this.signature,
    required this.tags,
    required this.sections,
    required this.pois,
  });

  factory DestinationDetailModel.fromJson(Map<String, dynamic> json) {
    return DestinationDetailModel(
      id: json['id'] as String,
      slug: json['slug'] as String,
      name: json['name'] as String,
      country: json['country'] as String,
      city: json['city'] as String,
      tagline: json['tagline'] as String,
      description: json['description'] as String?,
      heroImageUrl: json['heroImageUrl'] as String? ?? '',
      signature: json['signature'] as String?,
      tags: (json['tags'] as List<dynamic>?)?.cast<String>() ?? [],
      sections: (json['sections'] as List<dynamic>?)
              ?.map((s) => DestinationSectionModel.fromJson(s as Map<String, dynamic>))
              .toList() ??
          [],
      pois: (json['pois'] as List<dynamic>?)
              ?.map((p) => PoiModel.fromJson(p as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}

class DestinationSectionModel {
  final String sectionType;
  final int orderIndex;
  final List<SectionItemModel> content;

  const DestinationSectionModel({
    required this.sectionType,
    required this.orderIndex,
    required this.content,
  });

  factory DestinationSectionModel.fromJson(Map<String, dynamic> json) {
    return DestinationSectionModel(
      sectionType: json['sectionType'] as String,
      orderIndex: json['orderIndex'] as int,
      content: (json['content'] as List<dynamic>?)
              ?.map((i) => SectionItemModel.fromJson(i as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}

class SectionItemModel {
  final String name;
  final String? desc;

  const SectionItemModel({required this.name, this.desc});

  factory SectionItemModel.fromJson(Map<String, dynamic> json) {
    return SectionItemModel(
      name: json['name'] as String,
      desc: json['desc'] as String?,
    );
  }
}

class PoiModel {
  final String id;
  final String title;
  final String category;
  final String budgetTier;
  final double? latitude;
  final double? longitude;

  const PoiModel({
    required this.id,
    required this.title,
    required this.category,
    required this.budgetTier,
    this.latitude,
    this.longitude,
  });

  factory PoiModel.fromJson(Map<String, dynamic> json) {
    return PoiModel(
      id: json['id'] as String,
      title: json['title'] as String,
      category: json['category'] as String,
      budgetTier: json['budgetTier'] as String,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
    );
  }
}
