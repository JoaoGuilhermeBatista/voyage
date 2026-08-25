import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/itinerary_template_models.dart';
import '../../data/itinerary_template_repository.dart';

/// Lista de templates curados de um destino, via
/// GET /destinations/{slug}/itinerary-templates. Family key = slug.
final templatesForDestinationProvider = FutureProvider.family<
    List<ItineraryTemplateSummaryModel>, String>((ref, slug) {
  return ref
      .watch(itineraryTemplateRepositoryProvider)
      .getTemplatesForDestination(slug);
});

/// Preview completo (dia a dia) de um template, via
/// GET /destinations/{slug}/itinerary-templates/{templateId}. Family key é
/// o record posicional (slug, templateId) — mesmo padrão de
/// directionsProvider em maps_provider.dart.
final templateDetailProvider = FutureProvider.family<
    ItineraryTemplateDetailModel, (String slug, String templateId)>(
  (ref, key) {
    return ref
        .watch(itineraryTemplateRepositoryProvider)
        .getTemplateDetail(key.$1, key.$2);
  },
);
