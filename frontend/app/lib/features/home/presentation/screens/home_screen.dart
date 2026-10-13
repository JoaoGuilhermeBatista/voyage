import 'dart:math';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../../collections/presentation/providers/collection_provider.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';

// Gradiente-placeholder determinístico (a partir do slug) usado onde ainda
// não há foto real — ex.: o hero de "destino da semana". Tons dentro da
// família Noir (navy/ink) pra já nascer "de marca" em vez de cinza neutro.
const List<List<Color>> _placeholderGradients = [
  [Color(0xFF1F3B57), Color(0xFF0E1217)],
  [Color(0xFF3B5166), Color(0xFF192A3C)],
  [Color(0xFF14212E), Color(0xFF0E1217)],
  [Color(0xFF2C4257), Color(0xFF192A3C)],
  [Color(0xFF1A2A38), Color(0xFF0E1217)],
];

List<Color> placeholderGradientFor(String slug) {
  final index =
      slug.codeUnits.fold(0, (a, b) => a + b) % _placeholderGradients.length;
  return _placeholderGradients[index];
}

/// Embaralha `items` de forma determinística a partir de `seed` — mesma
/// seed sempre produz a mesma ordem (estável entre rebuilds), mas seeds
/// diferentes produzem ordens diferentes. Sem isso, cada seção temática
/// mostra seu subconjunto preservando a MESMA ordem "natural" da lista
/// completa vinda da API, o que faz todo carrossel parecer repetir a
/// mesma sequência.
List<DestinationSummaryModel> _stableShuffled(
  List<DestinationSummaryModel> items,
  String seed,
) {
  final seedValue = seed.codeUnits.fold(0, (a, b) => a + b);
  final shuffled = [...items]..shuffle(Random(seedValue));
  return shuffled;
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
      body: CustomScrollView(
        slivers: [
          // Hero bleeds até o topo (sem SafeArea) — só o wordmark/texto por
          // dentro respeita a área segura, via padding manual.
          provider.when(
            data: (destinations) =>
                SliverToBoxAdapter(child: _buildHero(destinations)),
            loading: () => const SliverToBoxAdapter(child: SizedBox.shrink()),
            error: (_, __) =>
                const SliverToBoxAdapter(child: SizedBox.shrink()),
          ),
          SliverSafeArea(
            top: false,
            sliver: SliverToBoxAdapter(
              child: Column(
                children: [
                  _buildHeader(),
                  _buildSearch(),
                  const SizedBox(height: 8),
                ],
              ),
            ),
          ),
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
    );
  }

  // ── Hero — "destino da semana", ver spec §4.1 ───────────────────────────────
  // Mesmo critério hasImage/CachedNetworkImage dos outros cards (ex.:
  // _DestinationCaptionCard): foto real quando heroImageUrl existe, senão
  // cai no gradiente determinístico a partir do slug.

  Widget _buildHero(List<DestinationSummaryModel> destinations) {
    if (destinations.isEmpty) return const SizedBox.shrink();
    final featured = destinations.first;
    final topInset = MediaQuery.of(context).padding.top;
    final hasImage = featured.heroImageUrl.isNotEmpty;
    final gradient = placeholderGradientFor(featured.slug);

    return SizedBox(
      height: 480,
      width: double.infinity,
      child: Stack(
        fit: StackFit.expand,
        children: [
          hasImage
              ? CachedNetworkImage(
                  imageUrl: featured.heroImageUrl,
                  fit: BoxFit.cover,
                  placeholder: (_, __) => DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(colors: gradient),
                    ),
                  ),
                  errorWidget: (_, __, ___) => DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(colors: gradient),
                    ),
                  ),
                )
              : DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(colors: gradient),
                  ),
                ),
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  AppTheme.overlay.withValues(alpha: 0.2),
                  Colors.transparent,
                  AppTheme.overlay.withValues(alpha: 0.8),
                ],
                stops: const [0, 0.4, 1],
              ),
            ),
          ),
          Positioned(
            top: topInset + 20,
            left: 0,
            right: 0,
            child: Text(
              'LUVOY',
              textAlign: TextAlign.center,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 20,
                letterSpacing: 9,
                color: AppTheme.primaryForeground,
              ),
            ),
          ),
          Positioned(
            left: 24,
            right: 24,
            bottom: 28,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('DESTINO DA SEMANA', style: AppTheme.eyebrow()),
                const SizedBox(height: 10),
                Text(
                  featured.name,
                  style: GoogleFonts.cormorantGaramond(
                    fontSize: 46,
                    fontWeight: FontWeight.w500,
                    height: 1,
                    color: AppTheme.primaryForeground,
                  ),
                ),
                const SizedBox(height: 10),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 260),
                  child: Text(
                    featured.tagline,
                    style: GoogleFonts.jost(
                      fontSize: 14,
                      fontWeight: FontWeight.w300,
                      color: AppTheme.primaryForeground.withValues(alpha: 0.85),
                      height: 1.5,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ── Header ─────────────────────────────────────────────────────────────────

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 28, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('LUVOY · COLEÇÃO', style: AppTheme.eyebrow()),
          const SizedBox(height: 10),
          Text('Explorar', style: Theme.of(context).textTheme.displayMedium),
          const SizedBox(height: 6),
          Text(
            'Destinos curados para cada tipo de viagem',
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: AppTheme.mutedForeground),
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
        style: GoogleFonts.jost(fontSize: 14, color: AppTheme.foreground),
        decoration: InputDecoration(
          hintText: 'Buscar destino...',
          hintStyle: GoogleFonts.jost(color: AppTheme.mutedForeground),
          prefixIcon: Icon(
            Icons.search_rounded,
            color: AppTheme.mutedForeground,
            size: 20,
          ),
          suffixIcon: _query.isNotEmpty
              ? IconButton(
                  icon: Icon(
                    Icons.close_rounded,
                    color: AppTheme.mutedForeground,
                    size: 18,
                  ),
                  onPressed: () {
                    _searchController.clear();
                    setState(() => _query = '');
                  },
                )
              : null,
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: AppTheme.border),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: AppTheme.gold, width: 1.2),
          ),
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

    final featured = _stableShuffled(
      destinations,
      'em-destaque',
    ).take(10).toList();

    // Coleções editoriais reais (GET /collections) — curadoria manual no
    // banco, não mais filtro client-side por tag. Falha/loading silenciosos:
    // se a chamada não voltar a tempo, a Home só fica sem essas seções, sem
    // travar o resto da tela (mesmo critério de graceful-degradation já
    // usado no restante do app).
    final collectionsAsync = ref.watch(collectionsProvider);
    final collections = collectionsAsync.asData?.value ?? const [];

    return SliverToBoxAdapter(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 16),
          _SectionHeader(eyebrow: 'Curadoria', title: 'Destinos'),
          const SizedBox(height: 12),
          _HorizontalCarousel(destinations: featured, overlayStyle: false),

          for (final collection in collections)
            if (collection.destinations.isNotEmpty) ...[
              const SizedBox(height: 28),
              _SectionHeader(
                eyebrow: collection.eyebrow ?? 'Coleção',
                title: collection.title,
                italic: true,
              ),
              const SizedBox(height: 12),
              _HorizontalCarousel(destinations: collection.destinations),
            ],

          const SizedBox(height: 40),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 40),
            child: Text(
              'Viajar é a única coisa que se compra e torna você mais rico.',
              textAlign: TextAlign.center,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 15,
                fontStyle: FontStyle.italic,
                fontWeight: FontWeight.w400,
                color: AppTheme.mutedForeground,
              ),
            ),
          ),
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
              itemBuilder: (_, __) =>
                  const SizedBox(width: 200, child: CardShimmer()),
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
            Icon(
              Icons.wifi_off_rounded,
              size: 40,
              color: AppTheme.softBlack.withValues(alpha: 0.3),
            ),
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
  final String? eyebrow;
  final bool italic;
  const _SectionHeader({
    required this.title,
    this.eyebrow,
    this.italic = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (eyebrow != null) ...[
            Text(eyebrow!.toUpperCase(), style: AppTheme.eyebrow()),
            const SizedBox(height: 6),
          ],
          Text(
            title,
            style: GoogleFonts.cormorantGaramond(
              fontSize: 26,
              fontWeight: FontWeight.w500,
              fontStyle: italic ? FontStyle.italic : FontStyle.normal,
              color: AppTheme.foreground,
              height: 1.15,
            ),
          ),
        ],
      ),
    );
  }
}

// ── Horizontal carousel ───────────────────────────────────────────────────────

class _HorizontalCarousel extends StatelessWidget {
  final List<DestinationSummaryModel> destinations;

  /// true (padrão) = cards "coleção" (nome sobre a imagem, ver spec §4.3).
  /// false = cards "destinos" (legenda ABAIXO da imagem, ver spec §4.2).
  final bool overlayStyle;
  const _HorizontalCarousel({
    required this.destinations,
    this.overlayStyle = true,
  });

  @override
  Widget build(BuildContext context) {
    // Coleção (overlay): card largo, aspect-[16/11] (spec §4.3).
    // Destinos (legenda embaixo): card retrato, aspect-[4/5] (spec §4.2).
    final cardWidth = overlayStyle ? 224.0 : 148.0;
    final cardHeight = overlayStyle ? 224 * 11 / 16 + 8 : 240.0;

    return SizedBox(
      height: cardHeight,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 24),
        clipBehavior: Clip.none,
        itemCount: destinations.length,
        separatorBuilder: (_, __) => const SizedBox(width: 12),
        itemBuilder: (context, index) => SizedBox(
          width: cardWidth,
          child: overlayStyle
              ? _CarouselCard(destination: destinations[index])
              : _DestinationCaptionCard(destination: destinations[index]),
        ),
      ),
    );
  }
}

/// Card "destinos" (spec §4.2): imagem `aspect-[4/5]` + legenda ABAIXO —
/// nome em serifa, país pequeno e discreto. Sem overlay/gradiente.
class _DestinationCaptionCard extends StatelessWidget {
  final DestinationSummaryModel destination;
  const _DestinationCaptionCard({required this.destination});

  @override
  Widget build(BuildContext context) {
    final hasImage = destination.heroImageUrl.isNotEmpty;
    final gradient = placeholderGradientFor(destination.slug);

    return GestureDetector(
      onTap: () => context.push('/destinos/${destination.slug}'),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          AspectRatio(
            aspectRatio: 4 / 5,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: hasImage
                  ? CachedNetworkImage(
                      imageUrl: destination.heroImageUrl,
                      fit: BoxFit.cover,
                      placeholder: (_, __) => DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(colors: gradient),
                        ),
                      ),
                      errorWidget: (_, __, ___) => DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(colors: gradient),
                        ),
                      ),
                    )
                  : DecoratedBox(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(colors: gradient),
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            destination.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.cormorantGaramond(
              fontSize: 18,
              fontWeight: FontWeight.w500,
              color: AppTheme.foreground,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            destination.country,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.jost(
              fontSize: 12,
              fontWeight: FontWeight.w300,
              color: AppTheme.mutedForeground,
            ),
          ),
        ],
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
        borderRadius: BorderRadius.circular(6),
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

            // Nome sobre a imagem (spec §4.3 — cards "coleção" não têm
            // badge de país nem tagline, só o nome).
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
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                        height: 1.1,
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
        borderRadius: BorderRadius.circular(6),
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
                      Colors.black.withValues(alpha: 0.35),
                      Colors.transparent,
                      Colors.black.withValues(alpha: 0.2),
                      Colors.black.withValues(alpha: 0.75),
                    ],
                    stops: const [0.0, 0.3, 0.6, 1.0],
                  ),
                ),
              ),

              Positioned(
                top: 16,
                left: 18,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.45),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    child: Text(
                      destination.country.toUpperCase(),
                      style: AppTheme.eyebrow(
                        color: AppTheme.goldSoft,
                        letterSpacing: 1.6,
                      ).copyWith(fontSize: 10),
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
                        style: Theme.of(context).textTheme.displayMedium
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
                        style: GoogleFonts.cormorantGaramond(
                          fontStyle: FontStyle.italic,
                          fontWeight: FontWeight.w400,
                          color: Colors.white.withValues(alpha: 0.8),
                          height: 1.4,
                          fontSize: 14,
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
