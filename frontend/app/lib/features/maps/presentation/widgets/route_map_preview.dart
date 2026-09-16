import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/maps_provider.dart';
import 'map_embed_view.dart';

/// Preview visual da ROTA entre dois pontos (origem/destino), via GET
/// /api/v1/maps/embed-url/directions — mesmo espírito gracioso de
/// PoiMapPreview (loading/erro/configured=false → não renderiza nada),
/// mas mostrando o trajeto traçado em vez de um pino único.
class RouteMapPreview extends ConsumerWidget {
  final double originLat;
  final double originLng;
  final double destLat;
  final double destLng;

  const RouteMapPreview({
    super.key,
    required this.originLat,
    required this.originLng,
    required this.destLat,
    required this.destLng,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final embedAsync = ref.watch(
      directionsEmbedUrlProvider((originLat, originLng, destLat, destLng)),
    );

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
