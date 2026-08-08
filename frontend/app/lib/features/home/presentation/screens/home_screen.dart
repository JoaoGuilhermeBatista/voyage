import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';

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
              data: (destinations) => _buildList(destinations),
              loading: () => _buildShimmerList(),
              error: (e, _) => _buildError(e),
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 24)),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Explorar',
            style: Theme.of(context).textTheme.displayMedium,
          ),
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

  Widget _buildSearch() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
      child: TextField(
        controller: _searchController,
        onChanged: (value) => setState(() => _query = value.trim()),
        decoration: InputDecoration(
          hintText: 'Buscar destino...',
          hintStyle: TextStyle(color: AppTheme.softBlack.withValues(alpha: 0.35)),
          prefixIcon: Icon(
            Icons.search_rounded,
            color: AppTheme.softBlack.withValues(alpha: 0.4),
            size: 20,
          ),
          suffixIcon: _query.isNotEmpty
              ? IconButton(
                  icon: Icon(Icons.close_rounded,
                      color: AppTheme.softBlack.withValues(alpha: 0.4), size: 18),
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

  Widget _buildList(List<DestinationSummaryModel> destinations) {
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
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate(
          (context, index) => Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: _DestinationCard(destination: destinations[index]),
          ),
          childCount: destinations.length,
        ),
      ),
    );
  }

  Widget _buildShimmerList() {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate(
          (context, index) => const Padding(
            padding: EdgeInsets.only(bottom: 16),
            child: CardShimmer(),
          ),
          childCount: 6,
        ),
      ),
    );
  }

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

// ── Destination Card ─────────────────────────────────────────────────────────

class _DestinationCard extends StatelessWidget {
  final DestinationSummaryModel destination;

  const _DestinationCard({required this.destination});

  static const List<Color> _placeholderColors = [
    Color(0xFF2C3E50),
    Color(0xFF34495E),
    Color(0xFF1A252F),
    Color(0xFF2E4053),
    Color(0xFF212F3C),
  ];

  @override
  Widget build(BuildContext context) {
    final index = destination.slug.codeUnits.fold(0, (a, b) => a + b);
    final placeholder = _placeholderColors[index % _placeholderColors.length];
    final hasImage = destination.heroImageUrl.isNotEmpty;

    return GestureDetector(
      onTap: () => context.push('/destinos/${destination.slug}'),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: SizedBox(
          height: 240,
          child: Stack(
            fit: StackFit.expand,
            children: [
              // Imagem ou placeholder
              if (hasImage)
                CachedNetworkImage(
                  imageUrl: destination.heroImageUrl,
                  fit: BoxFit.cover,
                  placeholder: (context, url) => Container(color: placeholder),
                  errorWidget: (context, url, err) => Container(color: placeholder),
                )
              else
                Container(color: placeholder),

              // Gradiente overlay
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

              // Badge país
              Positioned(
                top: 16,
                left: 16,
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
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

              // Conteúdo inferior
              Positioned(
                bottom: 0,
                left: 0,
                right: 0,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
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
                              fontSize: 28,
                              color: Colors.white,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        destination.tagline,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style:
                            Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: Colors.white.withValues(alpha: 0.8),
                                  height: 1.4,
                                ),
                      ),
                      if (destination.tags.isNotEmpty) ...[
                        const SizedBox(height: 12),
                        _buildTags(context),
                      ],
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

  Widget _buildTags(BuildContext context) {
    final visible = destination.tags.take(3).toList();
    return Wrap(
      spacing: 6,
      runSpacing: 4,
      children: visible
          .map(
            (tag) => Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: AppTheme.goldenOrange.withValues(alpha: 0.25),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                    color: AppTheme.goldenOrange.withValues(alpha: 0.5), width: 0.5),
              ),
              child: Text(
                tag,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: Colors.white,
                      letterSpacing: 0.5,
                    ),
              ),
            ),
          )
          .toList(),
    );
  }
}
