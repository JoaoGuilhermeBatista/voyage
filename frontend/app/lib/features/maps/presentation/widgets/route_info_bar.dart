import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/themes/app_theme.dart';
import '../providers/maps_provider.dart';

/// Indicador discreto de distância/duração entre dois pontos, via
/// GET /api/v1/maps/directions. Mesmo espírito de fallback gracioso de
/// `_CurrencyRatesBar` (itinerary_screen.dart): loading/erro/
/// configured=false → não quebra a tela, só não mostra nada (ou uma nota
/// discreta "mapa em breve", quando `configured == false`).
class RouteInfoBar extends ConsumerWidget {
  final double originLat;
  final double originLng;
  final double destLat;
  final double destLng;

  const RouteInfoBar({
    super.key,
    required this.originLat,
    required this.originLng,
    required this.destLat,
    required this.destLng,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final directionsAsync = ref.watch(
      directionsProvider((originLat, originLng, destLat, destLng)),
    );

    return directionsAsync.when(
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const SizedBox.shrink(),
      data: (directions) {
        if (!directions.configured) {
          return _buildNote(context, 'Mapa em breve');
        }

        final distance = directions.distanceText;
        final duration = directions.durationText;
        if (distance == null || duration == null) {
          return const SizedBox.shrink();
        }

        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.directions_outlined,
              size: 13,
              color: AppTheme.softBlack.withValues(alpha: 0.35),
            ),
            const SizedBox(width: 6),
            Text(
              '$distance · $duration',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.5),
                  ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildNote(BuildContext context, String text) {
    return Text(
      text,
      style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: AppTheme.softBlack.withValues(alpha: 0.35),
          ),
    );
  }
}
