import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../../maps/presentation/providers/maps_provider.dart';
import '../../data/itinerary_models.dart';
import '../providers/itinerary_provider.dart';

/// Detalhe de uma viagem multi-destino: lista os trechos em ordem
/// cronológica, cada um navega pro roteiro completo dele (spec item 7 —
/// "ao abrir mostra os trechos").
class TripDetailScreen extends ConsumerWidget {
  final String tripGroupId;
  const TripDetailScreen({super.key, required this.tripGroupId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tripAsync = ref.watch(tripDetailProvider(tripGroupId));

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(backgroundColor: AppTheme.background, elevation: 0),
      body: SafeArea(
        child: tripAsync.when(
          loading: () => const Padding(
            padding: EdgeInsets.symmetric(horizontal: 24),
            child: Column(
              children: [
                Padding(
                  padding: EdgeInsets.only(bottom: 16),
                  child: CardShimmer(),
                ),
                Padding(
                  padding: EdgeInsets.only(bottom: 16),
                  child: CardShimmer(),
                ),
              ],
            ),
          ),
          error: (_, _) => Center(
            child: Text(
              'Não foi possível carregar a viagem',
              style: GoogleFonts.jost(
                fontSize: 13,
                color: AppTheme.mutedForeground,
              ),
            ),
          ),
          data: (trip) => ListView(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
            children: [
              Text(
                trip.title,
                style: GoogleFonts.cormorantGaramond(
                  fontSize: 30,
                  fontWeight: FontWeight.w500,
                  color: AppTheme.foreground,
                  height: 1.15,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                '${trip.legs.length} trechos',
                style: GoogleFonts.jost(
                  fontSize: 13,
                  color: AppTheme.mutedForeground,
                ),
              ),
              const SizedBox(height: 24),
              for (int i = 0; i < trip.legs.length; i++) ...[
                Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: _LegTile(
                    index: i,
                    leg: trip.legs[i],
                    legIds: trip.legs.map((l) => l.id).join(','),
                    legDayCounts:
                        trip.legs.map((l) => l.dayCount).join(','),
                  ),
                ),
                if (i < trip.legs.length - 1)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 14, left: 4),
                    child: _RouteBetweenLegs(
                      from: trip.legs[i],
                      to: trip.legs[i + 1],
                    ),
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Distância/duração de deslocamento entre as cidades de dois trechos
/// consecutivos, via GET /api/v1/maps/directions-between-destinations.
/// Some graciosamente (sem renderizar nada) em loading/erro, quando o
/// Google Maps não está configurado, ou quando alguma das duas cidades
/// não tem rota terrestre conhecida (ex.: destinos muito distantes) —
/// mesmo espírito de fallback do resto do app (_CurrencyRatesBar,
/// _RouteBetweenActivities).
class _RouteBetweenLegs extends ConsumerWidget {
  final ItinerarySummaryModel from;
  final ItinerarySummaryModel to;
  const _RouteBetweenLegs({required this.from, required this.to});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final directionsAsync = ref.watch(
      directionsBetweenDestinationsProvider((from.destinationId, to.destinationId)),
    );

    return directionsAsync.when(
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const SizedBox.shrink(),
      data: (directions) {
        final distance = directions.distanceText;
        final duration = directions.durationText;
        if (!directions.configured || distance == null || duration == null) {
          return const SizedBox.shrink();
        }

        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.directions_car_outlined,
              size: 13,
              color: AppTheme.mutedForeground.withValues(alpha: 0.7),
            ),
            const SizedBox(width: 6),
            Text(
              '$distance · $duration até o próximo destino',
              style: GoogleFonts.jost(
                fontSize: 12,
                color: AppTheme.mutedForeground,
              ),
            ),
          ],
        );
      },
    );
  }
}

class _LegTile extends StatelessWidget {
  final int index;
  final ItinerarySummaryModel leg;
  final String legIds;
  final String legDayCounts;
  const _LegTile({
    required this.index,
    required this.leg,
    required this.legIds,
    required this.legDayCounts,
  });

  static const _months = [
    '',
    'Jan',
    'Fev',
    'Mar',
    'Abr',
    'Mai',
    'Jun',
    'Jul',
    'Ago',
    'Set',
    'Out',
    'Nov',
    'Dez',
  ];

  String _dateRange() {
    final s = leg.startDate;
    final e = leg.endDate;
    if (s.month == e.month) return '${s.day}–${e.day} ${_months[s.month]}';
    return '${s.day} ${_months[s.month]} – ${e.day} ${_months[e.month]}';
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppTheme.card,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => context.push(
          '/roteiro/${leg.id}?legIds=$legIds&legDayCounts=$legDayCounts&legIndex=$index',
        ),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppTheme.border),
          ),
          child: Row(
            children: [
              Container(
                width: 28,
                height: 28,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: AppTheme.secondary,
                  shape: BoxShape.circle,
                ),
                child: Text(
                  '${index + 1}',
                  style: GoogleFonts.jost(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.primary,
                  ),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      leg.destinationName,
                      style: GoogleFonts.jost(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                        color: AppTheme.foreground,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${_dateRange()} · ${leg.dayCount} ${leg.dayCount == 1 ? 'dia' : 'dias'}',
                      style: GoogleFonts.jost(
                        fontSize: 12,
                        fontWeight: FontWeight.w300,
                        color: AppTheme.mutedForeground,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right,
                size: 20,
                color: AppTheme.mutedForeground.withValues(alpha: 0.6),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
