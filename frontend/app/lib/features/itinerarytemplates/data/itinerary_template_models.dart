/// Modelos espelhando os DTOs do backend (item 1 do backlog — "templates de
/// roteiro curados"). Endpoints:
///   GET /api/v1/destinations/{slug}/itinerary-templates            (público)
///     -> List<{id, title, description, durationDays, budgetTier}>
///   GET /api/v1/destinations/{slug}/itinerary-templates/{templateId} (público)
///     -> {id, title, description, durationDays, budgetTier, destinationId,
///         destinationName, days: [{dayNumber, activities: [{slot,
///         orderInSlot, poi: PoiDetailDTO}]}]}
///
/// O preview NÃO tem datas reais — só dayNumber relativo (dia 1, dia 2...).
/// `poi` reaproveita o mesmo shape de PoiDetailModel já usado em
/// itinerary_models.dart (por isso o fromJson daquele model é reutilizado
/// aqui, sem duplicar parsing).
library;

import '../../itinerary/data/itinerary_models.dart';

class ItineraryTemplateSummaryModel {
  final String id;
  final String title;
  final String description;
  final int durationDays;
  final String budgetTier;

  const ItineraryTemplateSummaryModel({
    required this.id,
    required this.title,
    required this.description,
    required this.durationDays,
    required this.budgetTier,
  });

  factory ItineraryTemplateSummaryModel.fromJson(Map<String, dynamic> json) {
    return ItineraryTemplateSummaryModel(
      id: json['id'] as String,
      title: json['title'] as String,
      description: json['description'] as String,
      durationDays: json['durationDays'] as int,
      budgetTier: json['budgetTier'] as String,
    );
  }
}

class ItineraryTemplateDetailModel {
  final String id;
  final String title;
  final String description;
  final int durationDays;
  final String budgetTier;
  final String destinationId;
  final String destinationName;
  final List<TemplateDayModel> days;

  const ItineraryTemplateDetailModel({
    required this.id,
    required this.title,
    required this.description,
    required this.durationDays,
    required this.budgetTier,
    required this.destinationId,
    required this.destinationName,
    required this.days,
  });

  factory ItineraryTemplateDetailModel.fromJson(Map<String, dynamic> json) {
    return ItineraryTemplateDetailModel(
      id: json['id'] as String,
      title: json['title'] as String,
      description: json['description'] as String,
      durationDays: json['durationDays'] as int,
      budgetTier: json['budgetTier'] as String,
      destinationId: json['destinationId'] as String,
      destinationName: json['destinationName'] as String,
      days: (json['days'] as List<dynamic>)
          .map((d) => TemplateDayModel.fromJson(d as Map<String, dynamic>))
          .toList(),
    );
  }
}

class TemplateDayModel {
  final int dayNumber;
  final List<TemplateActivityModel> activities;

  const TemplateDayModel({
    required this.dayNumber,
    required this.activities,
  });

  factory TemplateDayModel.fromJson(Map<String, dynamic> json) {
    return TemplateDayModel(
      dayNumber: json['dayNumber'] as int,
      activities: (json['activities'] as List<dynamic>)
          .map(
              (a) => TemplateActivityModel.fromJson(a as Map<String, dynamic>))
          .toList(),
    );
  }

  /// Mesma convenção de ItineraryDayModel.activitiesForSlot em
  /// itinerary_models.dart: filtra por slot e ordena por orderInSlot.
  List<TemplateActivityModel> activitiesForSlot(String slot) =>
      activities.where((a) => a.slot == slot).toList()
        ..sort((a, b) => a.orderInSlot.compareTo(b.orderInSlot));
}

class TemplateActivityModel {
  final String slot;
  final int orderInSlot;
  final PoiDetailModel poi;

  const TemplateActivityModel({
    required this.slot,
    required this.orderInSlot,
    required this.poi,
  });

  factory TemplateActivityModel.fromJson(Map<String, dynamic> json) {
    return TemplateActivityModel(
      slot: json['slot'] as String,
      orderInSlot: json['orderInSlot'] as int,
      poi: PoiDetailModel.fromJson(json['poi'] as Map<String, dynamic>),
    );
  }
}
