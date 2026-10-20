/// Viagem multi-destino (spec item 7 — "N cidades + datas por trecho"):
/// [tripGroupId] é null quando o grupo representa um roteiro avulso
/// (nesse caso [legs] tem exatamente 1 elemento e a UI trata como sempre
/// tratou, sem indicação de "trechos"). [startDate]/[endDate] cobrem a
/// viagem inteira.
class TripSummaryModel {
  final String? tripGroupId;
  final String title;
  final DateTime startDate;
  final DateTime endDate;
  final List<ItinerarySummaryModel> legs;

  const TripSummaryModel({
    required this.tripGroupId,
    required this.title,
    required this.startDate,
    required this.endDate,
    required this.legs,
  });

  bool get isMultiDestino => legs.length > 1;

  factory TripSummaryModel.fromJson(Map<String, dynamic> json) {
    return TripSummaryModel(
      tripGroupId: json['tripGroupId'] as String?,
      title: json['title'] as String,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      legs: (json['legs'] as List<dynamic>)
          .map((l) => ItinerarySummaryModel.fromJson(l as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// Um trecho a criar dentro de uma viagem multi-destino — versão enxuta de
/// [ItineraryRequest] (só destino + datas): o motor de geração já cai de
/// volta nas preferências do onboarding quando os campos opcionais não são
/// enviados, mesmo comportamento de um roteiro avulso gerado sem preencher
/// vibe/interesses/etc.
class TripLegInput {
  final String destinationId;
  final DateTime startDate;
  final DateTime endDate;

  const TripLegInput({
    required this.destinationId,
    required this.startDate,
    required this.endDate,
  });

  Map<String, dynamic> toJson() => {
    'destinationId': destinationId,
    'startDate': _formatDate(startDate),
    'endDate': _formatDate(endDate),
  };

  static String _formatDate(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

class ItinerarySummaryModel {
  final String id;
  final String title;
  final String destinationId;
  final String destinationName;
  final String destinationHeroImageUrl;
  final DateTime startDate;
  final DateTime endDate;
  final int dayCount;

  const ItinerarySummaryModel({
    required this.id,
    required this.title,
    required this.destinationId,
    required this.destinationName,
    required this.destinationHeroImageUrl,
    required this.startDate,
    required this.endDate,
    required this.dayCount,
  });

  factory ItinerarySummaryModel.fromJson(Map<String, dynamic> json) {
    return ItinerarySummaryModel(
      id: json['id'] as String,
      title: json['title'] as String,
      destinationId: json['destinationId'] as String,
      destinationName: json['destinationName'] as String,
      destinationHeroImageUrl: json['destinationHeroImageUrl'] as String? ?? '',
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      dayCount: json['dayCount'] as int,
    );
  }
}

class ItineraryModel {
  final String id;
  final String title;
  final String destinationName;
  final String destinationHeroImageUrl;
  final String destinationCountry;
  final DateTime startDate;
  final DateTime endDate;
  final List<ItineraryDayModel> days;

  const ItineraryModel({
    required this.id,
    required this.title,
    required this.destinationName,
    required this.destinationHeroImageUrl,
    required this.destinationCountry,
    required this.startDate,
    required this.endDate,
    required this.days,
  });

  factory ItineraryModel.fromJson(Map<String, dynamic> json) {
    return ItineraryModel(
      id: json['id'] as String,
      title: json['title'] as String,
      destinationName: json['destinationName'] as String,
      destinationHeroImageUrl: json['destinationHeroImageUrl'] as String? ?? '',
      destinationCountry: json['destinationCountry'] as String? ?? '',
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      days: (json['days'] as List<dynamic>)
          .map((d) => ItineraryDayModel.fromJson(d as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ItineraryDayModel {
  final int dayNumber;
  final DateTime date;
  final List<ItineraryActivityModel> activities;

  const ItineraryDayModel({
    required this.dayNumber,
    required this.date,
    required this.activities,
  });

  factory ItineraryDayModel.fromJson(Map<String, dynamic> json) {
    return ItineraryDayModel(
      dayNumber: json['dayNumber'] as int,
      date: DateTime.parse(json['date'] as String),
      activities: (json['activities'] as List<dynamic>)
          .map(
            (a) => ItineraryActivityModel.fromJson(a as Map<String, dynamic>),
          )
          .toList(),
    );
  }

  List<ItineraryActivityModel> activitiesForSlot(String slot) =>
      activities.where((a) => a.slot == slot).toList()
        ..sort((a, b) => a.orderInSlot.compareTo(b.orderInSlot));
}

class ItineraryActivityModel {
  final String id;
  final String slot;
  final int orderInSlot;
  final PoiDetailModel poi;

  const ItineraryActivityModel({
    required this.id,
    required this.slot,
    required this.orderInSlot,
    required this.poi,
  });

  factory ItineraryActivityModel.fromJson(Map<String, dynamic> json) {
    return ItineraryActivityModel(
      id: json['id'] as String,
      slot: json['slot'] as String,
      orderInSlot: json['orderInSlot'] as int,
      poi: PoiDetailModel.fromJson(json['poi'] as Map<String, dynamic>),
    );
  }
}

class PoiDetailModel {
  final String id;
  final String title;
  final String category;
  final String? description;
  final String? address;
  final List<String> images;
  final String budgetTier;
  final int? averageDurationMinutes;
  final double? latitude;
  final double? longitude;

  const PoiDetailModel({
    required this.id,
    required this.title,
    required this.category,
    this.description,
    this.address,
    this.images = const [],
    required this.budgetTier,
    this.averageDurationMinutes,
    this.latitude,
    this.longitude,
  });

  factory PoiDetailModel.fromJson(Map<String, dynamic> json) {
    return PoiDetailModel(
      id: json['id'] as String,
      title: json['title'] as String,
      category: json['category'] as String,
      description: json['description'] as String?,
      address: json['address'] as String?,
      images:
          (json['images'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const [],
      budgetTier: json['budgetTier'] as String,
      averageDurationMinutes: json['averageDurationMinutes'] as int?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
    );
  }
}
