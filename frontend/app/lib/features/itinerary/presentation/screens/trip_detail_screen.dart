import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
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
              for (int i = 0; i < trip.legs.length; i++)
                Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: _LegTile(index: i, leg: trip.legs[i]),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LegTile extends StatelessWidget {
  final int index;
  final ItinerarySummaryModel leg;
  const _LegTile({required this.index, required this.leg});

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
        onTap: () => context.push('/roteiro/${leg.id}'),
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
