import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../data/itinerary_models.dart';
import '../../data/itinerary_repository.dart';
import '../providers/itinerary_provider.dart';

class ItineraryScreen extends ConsumerWidget {
  final String id;
  const ItineraryScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncValue = ref.watch(itineraryDetailProvider(id));

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: asyncValue.when(
        loading: () => const CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: AppTheme.primaryBlue),
              ),
            ),
          ],
        ),
        error: (_, _) => CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'Não foi possível carregar o roteiro.',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.manrope(
                            fontSize: 15, color: AppTheme.softBlack),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () =>
                            ref.invalidate(itineraryDetailProvider(id)),
                        child: const Text('Tentar novamente'),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
        data: (itinerary) => _ItineraryBody(itinerary: itinerary),
      ),
    );
  }
}

// ── Body ──────────────────────────────────────────────────────────────────────

class _ItineraryBody extends StatelessWidget {
  final ItineraryModel itinerary;
  const _ItineraryBody({required this.itinerary});

  static const _months = [
    '', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez',
  ];
  static const _weekdays = [
    '', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom',
  ];

  String _dayLabel(ItineraryDayModel day) {
    final d = day.date;
    final weekday = _weekdays[d.weekday];
    final month = _months[d.month];
    return 'Dia ${day.dayNumber} — $weekday, ${d.day} de $month';
  }

  String _dateRange() {
    final s = itinerary.startDate;
    final e = itinerary.endDate;
    final dayCount = e.difference(s).inDays + 1;
    final label = dayCount == 1 ? '1 dia' : '$dayCount dias';
    if (s.month == e.month) {
      return '${s.day}–${e.day} ${_months[s.month]} · $label';
    }
    return '${s.day} ${_months[s.month]} – ${e.day} ${_months[e.month]} · $label';
  }

  @override
  Widget build(BuildContext context) {
    final days = (List.of(itinerary.days)
          ..sort((a, b) => a.dayNumber.compareTo(b.dayNumber)))
        .where((d) => d.activities.isNotEmpty)
        .toList();

    return CustomScrollView(
      slivers: [
        _HeroAppBar(itinerary: itinerary),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  itinerary.title,
                  style: GoogleFonts.cormorantGaramond(
                    fontSize: 32,
                    fontWeight: FontWeight.w500,
                    color: AppTheme.softBlack,
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _dateRange(),
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppTheme.softBlack.withValues(alpha: 0.5),
                      ),
                ),
              ],
            ),
          ),
        ),
        for (final day in days) ...[
          SliverToBoxAdapter(child: _DayHeader(label: _dayLabel(day))),
          for (final slot in const ['MORNING', 'AFTERNOON', 'EVENING'])
            if (day.activitiesForSlot(slot).isNotEmpty)
              _SlotSection(
                slot: slot,
                activities: day.activitiesForSlot(slot),
              ),
        ],
        const SliverToBoxAdapter(child: SizedBox(height: 48)),
      ],
    );
  }
}

// ── Hero app bar ──────────────────────────────────────────────────────────────

class _HeroAppBar extends ConsumerWidget {
  final ItineraryModel itinerary;
  const _HeroAppBar({required this.itinerary});

  Future<void> _confirmDelete(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Excluir roteiro'),
        content: const Text('Tem certeza que deseja excluir este roteiro? Esta ação não pode ser desfeita.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Excluir'),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    try {
      await ref.read(itineraryRepositoryProvider).delete(itinerary.id);
      ref.invalidate(itinerariesProvider);
      if (context.mounted) context.pop();
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Não foi possível excluir o roteiro.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final hasImage = itinerary.destinationHeroImageUrl.isNotEmpty;

    return SliverAppBar(
      expandedHeight: 240,
      pinned: true,
      backgroundColor: AppTheme.background,
      elevation: 0,
      scrolledUnderElevation: 0,
      leading: Padding(
        padding: const EdgeInsets.all(8),
        child: CircleAvatar(
          backgroundColor: Colors.black.withValues(alpha: 0.35),
          child: IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white, size: 20),
            padding: EdgeInsets.zero,
            onPressed: () => context.pop(),
          ),
        ),
      ),
      actions: [
        Padding(
          padding: const EdgeInsets.all(8),
          child: CircleAvatar(
            backgroundColor: Colors.black.withValues(alpha: 0.35),
            child: PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, color: Colors.white, size: 20),
              padding: EdgeInsets.zero,
              color: AppTheme.background,
              onSelected: (value) {
                if (value == 'delete') _confirmDelete(context, ref);
              },
              itemBuilder: (_) => const [
                PopupMenuItem(
                  value: 'delete',
                  child: Row(
                    children: [
                      Icon(Icons.delete_outline, color: Colors.red, size: 18),
                      SizedBox(width: 8),
                      Text('Excluir roteiro', style: TextStyle(color: Colors.red)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
      flexibleSpace: FlexibleSpaceBar(
        collapseMode: CollapseMode.parallax,
        background: Stack(
          fit: StackFit.expand,
          children: [
            if (hasImage)
              CachedNetworkImage(
                imageUrl: itinerary.destinationHeroImageUrl,
                fit: BoxFit.cover,
                placeholder: (_, _) => const ColoredBox(color: Color(0xFF1A252F)),
                errorWidget: (_, _, _) =>
                    const ColoredBox(color: Color(0xFF1A252F)),
              )
            else
              const ColoredBox(color: Color(0xFF1A252F)),
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              height: 100,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      AppTheme.background,
                      AppTheme.background.withValues(alpha: 0),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Day header ────────────────────────────────────────────────────────────────

class _DayHeader extends StatelessWidget {
  final String label;
  const _DayHeader({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: GoogleFonts.cormorantGaramond(
              fontSize: 24,
              fontWeight: FontWeight.w500,
              color: AppTheme.softBlack,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 8),
          Divider(
            color: AppTheme.softBlack.withValues(alpha: 0.08),
            height: 1,
          ),
        ],
      ),
    );
  }
}

// ── Slot section ──────────────────────────────────────────────────────────────

class _SlotSection extends StatelessWidget {
  final String slot;
  final List<ItineraryActivityModel> activities;
  const _SlotSection({required this.slot, required this.activities});

  static const _slotLabels = {
    'MORNING': 'Manhã',
    'AFTERNOON': 'Tarde',
    'EVENING': 'Noite',
  };

  static const _slotIcons = {
    'MORNING': Icons.wb_sunny_outlined,
    'AFTERNOON': Icons.wb_cloudy_outlined,
    'EVENING': Icons.nights_stay_outlined,
  };

  @override
  Widget build(BuildContext context) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  _slotIcons[slot] ?? Icons.schedule_outlined,
                  size: 16,
                  color: AppTheme.softBlack.withValues(alpha: 0.4),
                ),
                const SizedBox(width: 6),
                Text(
                  _slotLabels[slot] ?? slot,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: AppTheme.softBlack.withValues(alpha: 0.4),
                        letterSpacing: 1.1,
                      ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ...activities.map((a) => _ActivityTile(activity: a)),
          ],
        ),
      ),
    );
  }
}

// ── Activity tile ─────────────────────────────────────────────────────────────

class _ActivityTile extends StatelessWidget {
  final ItineraryActivityModel activity;
  const _ActivityTile({required this.activity});

  static const _categoryLabels = {
    'MUSEUM': 'Museu',
    'RESTAURANT': 'Restaurante',
    'HOTEL': 'Hotel',
    'BEACH': 'Praia',
    'PARK': 'Parque',
    'SHOPPING': 'Shopping',
    'NIGHTLIFE': 'Vida Noturna',
  };

  static const _budgetLabels = {
    'ECONOMY': '€',
    'MODERATE': '€€',
    'LUXURY': '€€€',
  };

  @override
  Widget build(BuildContext context) {
    final poi = activity.poi;
    final categoryLabel =
        _categoryLabels[poi.category] ?? poi.category;
    final budgetLabel = _budgetLabels[poi.budgetTier] ?? '';
    final duration = poi.averageDurationMinutes != null
        ? '${poi.averageDurationMinutes} min'
        : null;

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Order indicator
          Container(
            width: 28,
            height: 28,
            margin: const EdgeInsets.only(top: 2),
            decoration: BoxDecoration(
              color: AppTheme.primaryBlue.withValues(alpha: 0.08),
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                '${activity.orderInSlot}',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: AppTheme.primaryBlue,
                      fontWeight: FontWeight.w700,
                    ),
              ),
            ),
          ),
          const SizedBox(width: 12),

          // Content
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  poi.title,
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: AppTheme.softBlack,
                      ),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Text(
                      categoryLabel,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppTheme.softBlack.withValues(alpha: 0.5),
                          ),
                    ),
                    if (budgetLabel.isNotEmpty) ...[
                      Text(
                        ' · ',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color:
                                  AppTheme.softBlack.withValues(alpha: 0.3),
                            ),
                      ),
                      Text(
                        budgetLabel,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: AppTheme.goldenOrange,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                    ],
                    if (duration != null) ...[
                      Text(
                        ' · ',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color:
                                  AppTheme.softBlack.withValues(alpha: 0.3),
                            ),
                      ),
                      Text(
                        duration,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color:
                                  AppTheme.softBlack.withValues(alpha: 0.5),
                            ),
                      ),
                    ],
                  ],
                ),
                if (poi.description != null &&
                    poi.description!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    poi.description!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppTheme.softBlack.withValues(alpha: 0.45),
                          height: 1.5,
                        ),
                  ),
                ],
                if (poi.address != null && poi.address!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(Icons.location_on_outlined,
                          size: 12,
                          color: AppTheme.softBlack.withValues(alpha: 0.35)),
                      const SizedBox(width: 3),
                      Expanded(
                        child: Text(
                          poi.address!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context)
                              .textTheme
                              .bodySmall
                              ?.copyWith(
                                color:
                                    AppTheme.softBlack.withValues(alpha: 0.4),
                              ),
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
