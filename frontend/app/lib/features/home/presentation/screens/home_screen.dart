import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';

// Seções temáticas: cada grupo define qual substring de tag deve casar.
// Se nenhum destino tiver tags suficientes, a seção é omitida automaticamente.
const _thematicGroups = [
  _ThemeGroup(key: 'cultura', label: 'Cultura & Arte'),
  _ThemeGroup(key: 'gastronomia', label: 'Gastronomia'),
  _ThemeGroup(key: 'moda', label: 'Moda & Design'),
  _ThemeGroup(key: 'natureza', label: 'Natureza'),
  _ThemeGroup(key: 'bem-estar', label: 'Bem-estar'),
  _ThemeGroup(key: 'vida noturna', label: 'Vida Noturna'),
];

class _ThemeGroup {
  final String key;
  final String label;
  const _ThemeGroup({required this.key, required this.label});
}

// ---------------------------------------------------------------------------

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final _searchController = TextEditingController();
  String _query = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = _query.isEmpty
        ? ref.watch(destinationsProvider)
        : ref.watch(destinationSearchProvider(_query));

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(child: _buildHeader()),
            SliverToBoxAdapter(child: _buildSearch()),
            const SliverToBoxAdapter(child: SizedBox(height: 8)),
            provider.when(
              data: (destinations) => _query.isNotEmpty
                  ? _buildSearchResults(destinations)
                  : _buildDiscoveryLayout(destinations),
              loading: () => _buildShimmer(),
              error: (e, _) => _buildError(e),
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 32)),
          ],
        ),
      ),
    );
  }

  // ── Header ─────────────────────────────────────────────────────────────────

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Explorar', style: Theme.of(context).textTheme.displayMedium),
          const SizedBox(height: 4),
          Text(
            'Destinos curados para cada tipo de viagem',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppTheme.softBlack.withValues(alpha: 0.45),
                ),
          ),
        ],
      ),
    );
  }

  // ── Search ──────────────────────────────────────────────────────────────────

  Widget _buildSearch() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
      child: TextField(
        controller: _searchController,
        onChanged: (value) => setState(() => _query = value.trim()),
        decoration: InputDecoration(
          hintText: 'Buscar destino...',
          hintStyle:
              TextStyle(color: AppTheme.softBlack.withValues(alpha: 0.35)),
          prefixIcon: Icon(
            Icons.search_rounded,
            color: AppTheme.softBlack.withValues(alpha: 0.4),
            size: 20,
          ),
          suffixIcon: _query.isNotEmpty
              ? IconButton(
                  icon: Icon(Icons.close_rounded,
                      color: AppTheme.softBlack.withValues(alpha: 0.4),
                      size: 18),
                  onPressed: () {
                    _searchController.clear();
                    setState(() => _query = '');
                  },
                )
              : null,
        ),
      ),
    );
  }

  // ── Discovery layout (no search active) ────────────────────────────────────

  Widget _buildDiscoveryLayout(List<DestinationSummaryModel> destinations) {
    if (destinations.isEmpty) {
      return SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.only(top: 80),
          child: Center(
            child: Text(
              'Nenhum destino disponível',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.45),
                  ),
            ),
          ),
        ),
      );
    }

    final featured = destinations.take(10).toList();

    // Build thematic sections that have at least 2 matching destinations
    final thematic = <_ThemeGroup, List<DestinationSummaryModel>>{};
    for (final group in _thematicGroups) {
      final filtered = destinations.where((d) => d.tags.any(
            (t) => t.toLowerCase().contains(group.key),
          )).toList();
      if (filtered.length >= 2) thematic[group] = filtered;
    }

    return SliverToBoxAdapter(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 16),
          _SectionHeader(title: 'Em Destaque'),
          const SizedBox(height: 12),
          _HorizontalCarousel(destinations: featured),

          for (final entry in thematic.entries) ...[
            const SizedBox(height: 28),
            _SectionHeader(title: entry.key.label),
            const SizedBox(height: 12),
            _HorizontalCarousel(destinations: entry.value),
          ],
        ],
      ),
    );
  }

  // ── Search results (vertical) ───────────────────────────────────────────────

  Widget _buildSearchResults(List<DestinationSummaryModel> destinations) {
    if (destinations.isEmpty) {
      return SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.only(top: 80),
          child: Center(
            child: Text(
              'Nenhum destino encontrado',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.45),
                  ),
            ),
          ),
        ),
      );
    }

    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate(
          (context, index) => Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: _VerticalDestinationCard(destination: destinations[index]),
          ),
          childCount: destinations.length,
        ),
      ),
    );
  }

  // ── Loading ─────────────────────────────────────────────────────────────────

  Widget _buildShimmer() {
    return SliverToBoxAdapter(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: Container(
              width: 120,
              height: 16,
              decoration: BoxDecoration(
                color: AppTheme.shimmerBase,
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 260,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 24),
              itemCount: 5,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (_, __) => const SizedBox(
                width: 200,
                child: CardShimmer(),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ── Error ───────────────────────────────────────────────────────────────────

  Widget _buildError(Object error) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
        child: Column(
          children: [
            Icon(Icons.wifi_off_rounded,
                size: 40, color: AppTheme.softBlack.withValues(alpha: 0.3)),
            const SizedBox(height: 16),
            Text(
              'Não foi possível carregar os destinos',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.5),
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            TextButton(
              onPressed: () => ref.refresh(destinationsProvider),
              child: const Text('Tentar novamente'),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Section header ────────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Text(
        title,
        style: GoogleFonts.cormorantGaramond(
          fontSize: 26,
          fontWeight: FontWeight.w500,
          color: AppTheme.softBlack,
          height: 1.2,
        ),
      ),
    );
  }
}

// ── Horizontal carousel ───────────────────────────────────────────────────────

class _HorizontalCarousel extends StatelessWidget {
  final List<DestinationSummaryModel> destinations;
  const _HorizontalCarousel({required this.destinations});

  static const double _cardWidth = 200;
  static const double _cardHeight = 268;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: _cardHeight,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 24),
        clipBehavior: Clip.none,
        itemCount: destinations.length,
        separatorBuilder: (_, __) => const SizedBox(width: 12),
        itemBuilder: (context, index) => SizedBox(
          width: _cardWidth,
          child: _CarouselCard(destination: destinations[index]),
        ),
      ),
    );
  }
}

class _CarouselCard extends StatelessWidget {
  final DestinationSummaryModel destination;
  const _CarouselCard({required this.destination});

  static const List<Color> _placeholders = [
    Color(0xFF2C3E50),
    Color(0xFF34495E),
    Color(0xFF1A252F),
    Color(0xFF2E4053),
    Color(0xFF212F3C),
  ];

  @override
  Widget build(BuildContext context) {
    final colorIndex =
        destination.slug.codeUnits.fold(0, (a, b) => a + b) %
            _placeholders.length;
    final placeholder = _placeholders[colorIndex];
    final hasImage = destination.heroImageUrl.isNotEmpty;

    return GestureDetector(
      onTap: () => context.push('/destinos/${destination.slug}'),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Stack(
          fit: StackFit.expand,
          children: [
            // Image or placeholder
            if (hasImage)
              CachedNetworkImage(
                imageUrl: destination.heroImageUrl,
                fit: BoxFit.cover,
                placeholder: (_, __) => Container(color: placeholder),
                errorWidget: (_, __, ___) => Container(color: placeholder),
              )
            else
              Container(color: placeholder),

            // Gradient overlay
            DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.transparent,
                    Colors.black.withValues(alpha: 0.15),
                    Colors.black.withValues(alpha: 0.78),
                  ],
                  stops: const [0.35, 0.6, 1.0],
                ),
              ),
            ),

            // Country badge top-left
            Positioned(
              top: 12,
              left: 12,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.35),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                      color: Colors.white.withValues(alpha: 0.2), width: 0.5),
                ),
                child: Text(
                  destination.country.toUpperCase(),
                  style: GoogleFonts.manrope(
                    fontSize: 9,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.0,
                    color: Colors.white,
                  ),
                ),
              ),
            ),

            // Name + tagline bottom
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(14, 0, 14, 14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      destination.name,
                      style: GoogleFonts.cormorantGaramond(
                        fontSize: 22,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                        height: 1.1,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      destination.tagline,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.manrope(
                        fontSize: 11,
                        color: Colors.white.withValues(alpha: 0.75),
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Vertical card (search results only) ──────────────────────────────────────

class _VerticalDestinationCard extends StatelessWidget {
  final DestinationSummaryModel destination;
  const _VerticalDestinationCard({required this.destination});

  static const List<Color> _placeholders = [
    Color(0xFF2C3E50),
    Color(0xFF34495E),
    Color(0xFF1A252F),
    Color(0xFF2E4053),
    Color(0xFF212F3C),
  ];

  @override
  Widget build(BuildContext context) {
    final colorIndex =
        destination.slug.codeUnits.fold(0, (a, b) => a + b) %
            _placeholders.length;
    final placeholder = _placeholders[colorIndex];
    final hasImage = destination.heroImageUrl.isNotEmpty;

    return GestureDetector(
      onTap: () => context.push('/destinos/${destination.slug}'),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: SizedBox(
          height: 220,
          child: Stack(
            fit: StackFit.expand,
            children: [
              if (hasImage)
                CachedNetworkImage(
                  imageUrl: destination.heroImageUrl,
                  fit: BoxFit.cover,
                  placeholder: (_, __) => Container(color: placeholder),
                  errorWidget: (_, __, ___) => Container(color: placeholder),
                )
              else
                Container(color: placeholder),

              DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withValues(alpha: 0.2),
                      Colors.black.withValues(alpha: 0.75),
                    ],
                    stops: const [0.3, 0.6, 1.0],
                  ),
                ),
              ),

              Positioned(
                top: 14,
                left: 16,
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                        color: Colors.white.withValues(alpha: 0.2), width: 0.5),
                  ),
                  child: Text(
                    destination.country,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: Colors.white,
                          letterSpacing: 1.0,
                        ),
                  ),
                ),
              ),

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
                        destination.name,
                        style: Theme.of(context)
                            .textTheme
                            .displayMedium
                            ?.copyWith(
                              fontSize: 26,
                              color: Colors.white,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        destination.tagline,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: Colors.white.withValues(alpha: 0.8),
                              height: 1.4,
                              fontSize: 13,
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
