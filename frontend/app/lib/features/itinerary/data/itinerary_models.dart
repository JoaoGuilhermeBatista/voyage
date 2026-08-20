class ItinerarySummaryModel {
  final String id;
  final String title;
  final String destinationName;
  final String destinationHeroImageUrl;
  final DateTime startDate;
  final DateTime endDate;
  final int dayCount;

  const ItinerarySummaryModel({
    required this.id,
    required this.title,
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
      destinationName: json['destinationName'] as String,
      destinationHeroImageUrl:
          json['destinationHeroImageUrl'] as String? ?? '',
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
  final DateTime startDate;
  final DateTime endDate;
  final List<ItineraryDayModel> days;

  const ItineraryModel({
    required this.id,
    required this.title,
    required this.destinationName,
    required this.destinationHeroImageUrl,
    required this.startDate,
    required this.endDate,
    required this.days,
  });

  factory ItineraryModel.fromJson(Map<String, dynamic> json) {
    return ItineraryModel(
      id: json['id'] as String,
      title: json['title'] as String,
      destinationName: json['destinationName'] as String,
      destinationHeroImageUrl:
          json['destinationHeroImageUrl'] as String? ?? '',
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
          .map((a) =>
              ItineraryActivityModel.fromJson(a as Map<String, dynamic>))
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
      images: (json['images'] as List<dynamic>?)
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
