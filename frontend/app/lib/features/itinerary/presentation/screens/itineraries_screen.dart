import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../data/itinerary_models.dart';
import '../../data/itinerary_repository.dart';
import '../providers/itinerary_provider.dart';
import '../widgets/generate_itinerary_sheet.dart';

class ItinerariesScreen extends ConsumerWidget {
  const ItinerariesScreen({super.key});

  void _openCreateSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const GenerateItinerarySheet(),
    );
  }

  /// Duas formas de criar: roteiro simples (fluxo de sempre, sem mudança)
  /// ou viagem multi-destino (item 7 do backlog) — pequeno menu em vez de
  /// duplicar o botão principal, pra não competir visualmente com o fluxo
  /// mais comum. Usado tanto pelo FAB quanto pelo botão do estado vazio —
  /// é justamente na primeira viagem que a escolha mais importa.
  void _showCreateOptions(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => Material(
        color: AppTheme.background,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.map_outlined),
                title: const Text('Roteiro simples'),
                subtitle: const Text('Um único destino'),
                onTap: () {
                  Navigator.of(sheetContext).pop();
                  _openCreateSheet(context);
                },
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.alt_route_outlined),
                title: const Text('Viagem com vários destinos'),
                subtitle: const Text('Sequência de cidades e datas'),
                onTap: () {
                  Navigator.of(sheetContext).pop();
                  context.push('/roteiros/nova-viagem');
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tripsAsync = ref.watch(tripsProvider);
    // O estado vazio já tem seu próprio botão "Criar roteiro" em destaque —
    // o FAB só aparece quando já existem roteiros (ou enquanto ainda não
    // sabemos, isto é, carregando/erro), evitando duplicar o CTA na tela.
    final trips = tripsAsync.valueOrNull;
    final showFab = trips == null || trips.isNotEmpty;

    return Scaffold(
      backgroundColor: AppTheme.background,
      floatingActionButton: showFab
          ? FloatingActionButton.extended(
              onPressed: () => _showCreateOptions(context),
              icon: const Icon(Icons.add),
              label: const Text('Criar roteiro'),
            )
          : null,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(child: _buildHeader(context, tripsAsync)),
            const SliverToBoxAdapter(child: SizedBox(height: 16)),
            tripsAsync.when(
              loading: _buildShimmer,
              error: (_, _) => _buildError(context, ref),
              data: (trips) {
                if (trips.isEmpty) return _buildEmptyState(context);
                return _buildList(context, ref, trips);
              },
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 32)),
          ],
        ),
      ),
    );
  }

  // ── Header ─────────────────────────────────────────────────────────────────

  Widget _buildHeader(
    BuildContext context,
    AsyncValue<List<TripSummaryModel>> tripsAsync,
  ) {
    final count = tripsAsync.valueOrNull?.length;
    final subtitle = count == null || count == 0
        ? 'Seus roteiros personalizados'
        : count == 1
        ? '1 roteiro criado'
        : '$count roteiros criados';

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Roteiros',
            style: GoogleFonts.cormorantGaramond(
              fontSize: 36,
              fontWeight: FontWeight.w500,
              color: AppTheme.softBlack,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: AppTheme.softBlack.withValues(alpha: 0.45),
            ),
          ),
        ],
      ),
    );
  }

  // ── List ───────────────────────────────────────────────────────────────────

  Widget _buildList(
    BuildContext context,
    WidgetRef ref,
    List<TripSummaryModel> trips,
  ) {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate((context, index) {
          final trip = trips[index];

          // Grupo de 1 trecho = roteiro avulso, comportamento de sempre
          // (card + swipe pra remover). Multi-destino ainda não tem
          // exclusão da viagem inteira no backend — card não é
          // dismissible pra não sugerir uma ação que não existe.
          if (!trip.isMultiDestino) {
            final item = trip.legs.first;
            return Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Dismissible(
                key: ValueKey(item.id),
                direction: DismissDirection.endToStart,
                background: _buildDeleteBackground(),
                confirmDismiss: (_) => _confirmDelete(context),
                onDismissed: (_) => _delete(context, ref, item),
                child: _ItinerarySummaryCard(itinerary: item),
              ),
            );
          }

          return Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: _TripSummaryCard(trip: trip),
          );
        }, childCount: trips.length),
      ),
    );
  }

  Widget _buildDeleteBackground() {
    return Container(
      alignment: Alignment.centerRight,
      padding: const EdgeInsets.only(right: 20),
      decoration: BoxDecoration(
        color: const Color(0xFFE57373),
        borderRadius: BorderRadius.circular(24),
      ),
      child: const Icon(
        Icons.delete_outline_rounded,
        color: Colors.white,
        size: 24,
      ),
    );
  }

  Future<bool?> _confirmDelete(BuildContext context) {
    return showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Remover roteiro?'),
        content: const Text('Esta ação não pode ser desfeita.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: Text('Remover', style: TextStyle(color: Colors.red[400])),
          ),
        ],
      ),
    );
  }

  Future<void> _delete(
    BuildContext context,
    WidgetRef ref,
    ItinerarySummaryModel item,
  ) async {
    try {
      await ref.read(itineraryRepositoryProvider).delete(item.id);
      ref.invalidate(tripsProvider);
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao remover roteiro')),
        );
        ref.invalidate(tripsProvider);
      }
    }
  }

  // ── Empty state ─────────────────────────────────────────────────────────────

  Widget _buildEmptyState(BuildContext context) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(40, 56, 40, 0),
        child: Column(
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                color: AppTheme.primaryBlue.withValues(alpha: 0.08),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.map_outlined,
                size: 34,
                color: AppTheme.primaryBlue,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Nenhum roteiro ainda',
              style: GoogleFonts.cormorantGaramond(
                fontSize: 26,
                fontWeight: FontWeight.w500,
                color: AppTheme.softBlack,
                height: 1.25,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(
              'Explore um destino e toque em "Gerar Roteiro" para criar seu itinerário personalizado.',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: AppTheme.softBlack.withValues(alpha: 0.5),
                height: 1.6,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: () => _showCreateOptions(context),
              child: const Text('Criar roteiro'),
            ),
          ],
        ),
      ),
    );
  }

  // ── Loading ─────────────────────────────────────────────────────────────────

  Widget _buildShimmer() {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate(
          (_, _) => const Padding(
            padding: EdgeInsets.only(bottom: 16),
            child: CardShimmer(),
          ),
          childCount: 3,
        ),
      ),
    );
  }

  // ── Error ───────────────────────────────────────────────────────────────────

  Widget _buildError(BuildContext context, WidgetRef ref) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
        child: Column(
          children: [
            Icon(
              Icons.wifi_off_rounded,
              size: 40,
              color: AppTheme.softBlack.withValues(alpha: 0.3),
            ),
            const SizedBox(height: 16),
            Text(
              'Não foi possível carregar os roteiros',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: AppTheme.softBlack.withValues(alpha: 0.5),
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            TextButton(
              onPressed: () => ref.refresh(tripsProvider),
              child: const Text('Tentar novamente'),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Summary card ──────────────────────────────────────────────────────────────

class _ItinerarySummaryCard extends StatelessWidget {
  final ItinerarySummaryModel itinerary;
  const _ItinerarySummaryCard({required this.itinerary});

  static const List<Color> _placeholders = [
    Color(0xFF2C3E50),
    Color(0xFF34495E),
    Color(0xFF1A252F),
    Color(0xFF2E4053),
    Color(0xFF212F3C),
  ];

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
    final s = itinerary.startDate;
    final e = itinerary.endDate;
    if (s.month == e.month) {
      return '${s.day}–${e.day} ${_months[s.month]}';
    }
    return '${s.day} ${_months[s.month]} – ${e.day} ${_months[e.month]}';
  }

  @override
  Widget build(BuildContext context) {
    final colorIndex =
        itinerary.id.codeUnits.fold(0, (a, b) => a + b) % _placeholders.length;
    final placeholder = _placeholders[colorIndex];
    final hasImage = itinerary.destinationHeroImageUrl.isNotEmpty;

    return GestureDetector(
      onTap: () => context.push('/roteiro/${itinerary.id}'),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: SizedBox(
          height: 200,
          child: Stack(
            fit: StackFit.expand,
            children: [
              if (hasImage)
                CachedNetworkImage(
                  imageUrl: itinerary.destinationHeroImageUrl,
                  fit: BoxFit.cover,
                  placeholder: (_, _) => Container(color: placeholder),
                  errorWidget: (_, _, _) => Container(color: placeholder),
                )
              else
                Container(color: placeholder),

              // Gradient
              DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withValues(alpha: 0.2),
                      Colors.black.withValues(alpha: 0.78),
                    ],
                    stops: const [0.3, 0.6, 1.0],
                  ),
                ),
              ),

              // Day count badge
              Positioned(
                top: 14,
                left: 16,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.2),
                      width: 0.5,
                    ),
                  ),
                  child: Text(
                    '${itinerary.dayCount} ${itinerary.dayCount == 1 ? 'dia' : 'dias'}',
                    style: GoogleFonts.jost(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),

              // Content
              Positioned(
                bottom: 0,
                left: 0,
                right: 0,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 18),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        itinerary.destinationName,
                        style: GoogleFonts.cormorantGaramond(
                          fontSize: 26,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                          height: 1.1,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        _dateRange(),
                        style: GoogleFonts.jost(
                          fontSize: 13,
                          color: Colors.white.withValues(alpha: 0.75),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Trip (multi-destino) card ─────────────────────────────────────────────────

class _TripSummaryCard extends StatelessWidget {
  final TripSummaryModel trip;
  const _TripSummaryCard({required this.trip});

  static const List<Color> _placeholders = [
    Color(0xFF2C3E50),
    Color(0xFF34495E),
    Color(0xFF1A252F),
  ];

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
    final s = trip.startDate;
    final e = trip.endDate;
    if (s.month == e.month) return '${s.day}–${e.day} ${_months[s.month]}';
    return '${s.day} ${_months[s.month]} – ${e.day} ${_months[e.month]}';
  }

  @override
  Widget build(BuildContext context) {
    final colorIndex =
        (trip.tripGroupId ?? trip.title).codeUnits.fold(0, (a, b) => a + b) %
        _placeholders.length;
    final placeholder = _placeholders[colorIndex];

    return GestureDetector(
      onTap: () => context.push('/roteiros/viagem/${trip.tripGroupId}'),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Container(
          height: 200,
          color: placeholder,
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: Colors.white.withValues(alpha: 0.25),
                    width: 0.5,
                  ),
                ),
                child: Text(
                  '${trip.legs.length} destinos',
                  style: GoogleFonts.jost(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
              ),
              const Spacer(),
              Text(
                trip.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: GoogleFonts.cormorantGaramond(
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                  height: 1.1,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                _dateRange(),
                style: GoogleFonts.jost(
                  fontSize: 13,
                  color: Colors.white.withValues(alpha: 0.75),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
