import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/maps_provider.dart';
import 'map_embed_view.dart';

/// Preview de mapa visual para um ponto lat/lng, via GET
/// /api/v1/maps/embed-url (mesmo endpoint já usado por OpenInMapsButton/
/// RouteInfoBar, validado ponta-a-ponta). loading/erro/configured=false
/// → não renderiza nada, mesmo espírito gracioso do resto da tela
/// (_CurrencyRatesBar, RouteInfoBar).
class PoiMapPreview extends ConsumerWidget {
  final double lat;
  final double lng;

  const PoiMapPreview({super.key, required this.lat, required this.lng});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final embedAsync = ref.watch(embedUrlProvider((lat, lng)));

    return embedAsync.when(
      loading: () => const SizedBox(
        height: 180,
        child: Center(
          child: SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
      ),
      error: (_, _) => const SizedBox.shrink(),
      data: (embed) {
        if (!embed.configured || embed.embedUrl == null) {
          return const SizedBox.shrink();
        }
        return MapEmbedView(embedUrl: embed.embedUrl!, height: 180);
      },
    );
  }
}
