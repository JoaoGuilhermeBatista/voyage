import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shimmer/shimmer.dart';
import '../../../../core/themes/app_theme.dart';
import '../../data/favorite_model.dart';
import '../providers/favorite_provider.dart';

/// Tela "Favoritos" — spec LUVOY §7. Grid 2 col de destinos salvos
/// (aspect-square), corpo mantém dados reais (nunca hardcoded). A seção
/// "Pontos de interesse" do spec original foi deliberadamente omitida: o
/// produto hoje só tem favoritar DESTINO, não POI — não existe esse dado
/// pra mostrar (ver luvoy-pendencias.md).
class FavoritesScreen extends ConsumerWidget {
  const FavoritesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final favoritesAsync = ref.watch(favoritesProvider);

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(child: _buildHeader(context, favoritesAsync)),
            const SliverToBoxAdapter(child: SizedBox(height: 20)),
            favoritesAsync.when(
              loading: _buildShimmer,
              error: (_, _) => _buildError(context, ref),
              data: (favorites) {
                final withDest = favorites
                    .where((f) => f.destination != null)
                    .toList();
                if (withDest.isEmpty) return _buildEmptyState(context);
                return _buildGrid(context, ref, withDest);
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
    AsyncValue<List<FavoriteModel>> favoritesAsync,
  ) {
    final count = favoritesAsync.valueOrNull
        ?.where((f) => f.destination != null)
        .length;

    String subtitle;
    if (favoritesAsync.isLoading) {
      subtitle = 'Carregando...';
    } else if (count == null || count == 0) {
      subtitle = 'Seus lugares favoritos';
    } else {
      subtitle = count == 1 ? '1 destino salvo' : '$count destinos salvos';
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('SUA COLEÇÃO', style: AppTheme.eyebrow()),
          const SizedBox(height: 8),
          Text(
            'Favoritos',
            style: GoogleFonts.cormorantGaramond(
              fontSize: 36,
              fontWeight: FontWeight.w500,
              color: AppTheme.foreground,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: GoogleFonts.jost(
              fontSize: 14,
              fontWeight: FontWeight.w300,
              color: AppTheme.mutedForeground,
            ),
          ),
        ],
      ),
    );
  }

  // ── Grid ───────────────────────────────────────────────────────────────────

  Widget _buildGrid(
    BuildContext context,
    WidgetRef ref,
    List<FavoriteModel> favorites,
  ) {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      sliver: SliverGrid(
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 16,
          crossAxisSpacing: 16,
          childAspectRatio: 1,
        ),
        delegate: SliverChildBuilderDelegate(
          (context, index) => _FavoriteCard(
            favorite: favorites[index],
            onRemove: () => _confirmRemove(context, ref, favorites[index]),
          ),
          childCount: favorites.length,
        ),
      ),
    );
  }

  Future<void> _confirmRemove(
    BuildContext context,
    WidgetRef ref,
    FavoriteModel favorite,
  ) async {
    try {
      await ref.read(favoritesProvider.notifier).remove(favorite.id);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${favorite.destination?.name ?? 'Destino'} removido dos favoritos',
            ),
            action: SnackBarAction(
              label: 'Desfazer',
              onPressed: () async {
                if (favorite.destination != null) {
                  await ref
                      .read(favoritesProvider.notifier)
                      .add(favorite.destination!.id);
                }
              },
            ),
          ),
        );
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao remover. Tente novamente.')),
        );
      }
    }
  }

  // ── Empty state ─────────────────────────────────────────────────────────────

  Widget _buildEmptyState(BuildContext context) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 56),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppTheme.border),
          ),
          child: Column(
            children: [
              Text(
                'Nada salvo ainda',
                style: GoogleFonts.cormorantGaramond(
                  fontSize: 24,
                  fontStyle: FontStyle.italic,
                  fontWeight: FontWeight.w500,
                  color: AppTheme.foreground,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 10),
              Text(
                'Guarde aqui os lugares que fazem seu olhar demorar.',
                style: GoogleFonts.jost(
                  fontSize: 14,
                  fontWeight: FontWeight.w300,
                  color: AppTheme.mutedForeground,
                  height: 1.6,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 28),
              ElevatedButton(
                onPressed: () => context.go('/home'),
                child: const Text('Explorar destinos'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ── Loading ─────────────────────────────────────────────────────────────────

  Widget _buildShimmer() {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      sliver: SliverGrid(
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 16,
          crossAxisSpacing: 16,
          childAspectRatio: 1,
        ),
        delegate: SliverChildBuilderDelegate(
          (_, _) => const _FavoriteCardShimmer(),
          childCount: 4,
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
              color: AppTheme.mutedForeground.withValues(alpha: 0.6),
            ),
            const SizedBox(height: 16),
            Text(
              'Não foi possível carregar os favoritos',
              style: GoogleFonts.jost(
                fontSize: 14,
                color: AppTheme.mutedForeground,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            TextButton(
              onPressed: () => ref.refresh(favoritesProvider),
              child: const Text('Tentar novamente'),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Shimmer (quadrado — o CardShimmer compartilhado tem altura fixa de
// lista e não cabe numa célula de grid) ─────────────────────────────────────

class _FavoriteCardShimmer extends StatelessWidget {
  const _FavoriteCardShimmer();

  @override
  Widget build(BuildContext context) {
    return Shimmer.fromColors(
      baseColor: AppTheme.shimmerBase,
      highlightColor: AppTheme.shimmerHigh,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: AppTheme.shimmerBase,
          borderRadius: BorderRadius.circular(8),
        ),
      ),
    );
  }
}

// ── Favorite Card ─────────────────────────────────────────────────────────────

class _FavoriteCard extends StatelessWidget {
  final FavoriteModel favorite;
  final VoidCallback onRemove;

  const _FavoriteCard({required this.favorite, required this.onRemove});

  static const List<Color> _placeholders = [
    Color(0xFF2C3E50),
    Color(0xFF34495E),
    Color(0xFF1A252F),
    Color(0xFF2E4053),
    Color(0xFF212F3C),
  ];

  @override
  Widget build(BuildContext context) {
    final dest = favorite.destination!;
    final colorIndex =
        dest.slug.codeUnits.fold(0, (a, b) => a + b) % _placeholders.length;
    final placeholder = _placeholders[colorIndex];
    final hasImage = dest.heroImageUrl.isNotEmpty;

    return GestureDetector(
      onTap: () => context.push('/destinos/${dest.slug}'),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (hasImage)
              CachedNetworkImage(
                imageUrl: dest.heroImageUrl,
                fit: BoxFit.cover,
                placeholder: (_, _) => Container(color: placeholder),
                errorWidget: (_, _, _) => Container(color: placeholder),
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
                    AppTheme.overlay.withValues(alpha: 0.2),
                    AppTheme.overlay.withValues(alpha: 0.78),
                  ],
                  stops: const [0.35, 0.6, 1.0],
                ),
              ),
            ),

            // Remove heart button
            Positioned(
              top: 8,
              right: 8,
              child: GestureDetector(
                onTap: onRemove,
                child: Container(
                  width: 30,
                  height: 30,
                  decoration: BoxDecoration(
                    color: AppTheme.overlay.withValues(alpha: 0.35),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.favorite_rounded,
                    color: Color(0xFFE57373),
                    size: 15,
                  ),
                ),
              ),
            ),

            // Destination info
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(14, 0, 14, 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      dest.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.cormorantGaramond(
                        fontSize: 19,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                        height: 1.1,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      dest.country.toUpperCase(),
                      style: GoogleFonts.jost(
                        fontSize: 10,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 1.0,
                        color: Colors.white.withValues(alpha: 0.8),
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
