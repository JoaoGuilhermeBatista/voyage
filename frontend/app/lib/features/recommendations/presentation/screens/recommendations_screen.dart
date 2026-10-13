import 'dart:math';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';
import '../../../profile/data/user_model.dart';
import '../../../profile/presentation/providers/user_provider.dart';

const _interestLabels = {
  'cultura': 'Cultura & Arte',
  'gastronomia': 'Gastronomia',
  'natureza': 'Natureza',
  'moda': 'Moda & Design',
  'bem-estar': 'Bem-estar',
  'vida-noturna': 'Vida Noturna',
  'vida-local': 'Vida Local',
  'musica': 'Música & Shows',
};

/// Embaralha `items` de forma determinística a partir de `seed` — mesma
/// seed sempre produz a mesma ordem (estável entre rebuilds), mas seeds
/// diferentes produzem ordens diferentes (cada seção com sua própria
/// sequência). Soma dos code units em vez de `String.hashCode` porque o
/// hashCode de String não é garantido estável entre execuções da VM.
List<DestinationSummaryModel> _stableShuffled(
  List<DestinationSummaryModel> items,
  String seed,
) {
  final seedValue = seed.codeUnits.fold(0, (a, b) => a + b);
  final shuffled = [...items]..shuffle(Random(seedValue));
  return shuffled;
}

class RecommendationsScreen extends ConsumerWidget {
  const RecommendationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final destinationsAsync = ref.watch(destinationsProvider);
    final userAsync = ref.watch(userProvider);
    final user = userAsync.valueOrNull;

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(child: _buildHeader(context, user)),
            if (user != null && user.interests.isNotEmpty)
              SliverToBoxAdapter(
                  child: _buildInterestChips(context, user.interests)),
            const SliverToBoxAdapter(child: SizedBox(height: 8)),
            ...destinationsAsync.when(
              loading: () => [_buildShimmerCarousels()],
              error: (e, _) => [_buildError(context, ref)],
              data: (destinations) {
                final hasInterests = user != null &&
                    user.onboardingCompleted &&
                    user.interests.isNotEmpty;
                if (!hasInterests) return [_buildEmptyState(context)];
                return _buildCategorySections(context, destinations, user);
              },
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 32)),
          ],
        ),
      ),
    );
  }

  // ── Header ─────────────────────────────────────────────────────────────────

  Widget _buildHeader(BuildContext context, UserModel? user) {
    final subtitle = _buildSubtitle(user);
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('PARA VOCÊ', style: AppTheme.eyebrow()),
          const SizedBox(height: 10),
          Text(
            'Selecionado para você',
            style: GoogleFonts.cormorantGaramond(
              fontSize: 36,
              fontWeight: FontWeight.w500,
              color: AppTheme.foreground,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            subtitle,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppTheme.mutedForeground,
                ),
          ),
        ],
      ),
    );
  }

  String _buildSubtitle(UserModel? user) {
    if (user == null || !user.onboardingCompleted) {
      return 'Complete seu perfil para ver destinos personalizados';
    }
    if (user.budget != null || user.rhythm != null) {
      final parts = <String>[];
      if (user.budget != null) parts.add(_budgetLabel(user.budget!));
      if (user.rhythm != null) parts.add(_rhythmLabel(user.rhythm!));
      return 'Perfil ${parts.join(', ')} · selecionado para você';
    }
    return 'Baseado no seu perfil de viajante';
  }

  String _budgetLabel(String budget) => switch (budget) {
        'ECONOMY' => 'econômico',
        'MODERATE' => 'moderado',
        'LUXURY' => 'de luxo',
        _ => budget.toLowerCase(),
      };

  String _rhythmLabel(String rhythm) => switch (rhythm) {
        'RELAXED' => 'relaxado',
        'MODERATE' => 'moderado',
        'INTENSE' => 'intenso',
        _ => rhythm.toLowerCase(),
      };

  // ── Interest chips ──────────────────────────────────────────────────────────

  Widget _buildInterestChips(BuildContext context, List<String> interests) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'SEUS INTERESSES',
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: AppTheme.softBlack.withValues(alpha: 0.4),
                  letterSpacing: 1.2,
                ),
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: interests
                .map((i) => _InterestChip(
                    label: _interestLabels[i] ?? i))
                .toList(),
          ),
        ],
      ),
    );
  }

  // ── Category sections ───────────────────────────────────────────────────────

  List<Widget> _buildCategorySections(
    BuildContext context,
    List<DestinationSummaryModel> destinations,
    UserModel user,
  ) {
    // Uma seção por interesse do usuário, cada uma buscando na lista
    // COMPLETA de destinos (não num top-10 combinado de todos os
    // interesses) — corrige bug em que interesses menos "populares" (ex.:
    // bem-estar, vida-local) sumiam inteiros quando não pontuavam alto o
    // bastante frente aos outros interesses do mesmo perfil, mesmo havendo
    // destinos reais com aquela tag.
    final sections = <({String label, List<DestinationSummaryModel> items})>[];
    for (final interest in user.interests) {
      final key = interest.toLowerCase();
      final matching = destinations
          .where((d) => d.tags.any((t) => t.toLowerCase().contains(key)))
          .toList();
      if (matching.isNotEmpty) {
        sections.add((
          label: _interestLabels[interest] ?? interest,
          // Embaralho de forma ESTÁVEL (mesma semente = mesma ordem em
          // todo rebuild) por seção, pra cada interesse não repetir a
          // mesma sequência de destinos que as outras seções mostram —
          // sem isso, todo carrossel via a lista na mesma ordem "natural"
          // da API, parecendo repetitivo.
          items: _stableShuffled(matching, key).take(10).toList(),
        ));
      }
    }

    if (sections.isEmpty) return [_buildEmptyState(context)];

    return sections
        .map((s) => SliverToBoxAdapter(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 24),
                  _SectionHeader(title: s.label),
                  const SizedBox(height: 12),
                  _HorizontalCarousel(destinations: s.items),
                ],
              ),
            ))
        .toList();
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
                Icons.travel_explore_rounded,
                size: 34,
                color: AppTheme.primaryBlue,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Descubra destinos feitos para você',
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
              'Complete seu perfil de viajante para receber sugestões personalizadas baseadas nos seus gostos.',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.5),
                    height: 1.6,
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: () => context.go('/perfil'),
              child: const Text('Completar perfil'),
            ),
          ],
        ),
      ),
    );
  }

  // ── Loading ─────────────────────────────────────────────────────────────────

  Widget _buildShimmerCarousels() {
    return SliverToBoxAdapter(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: List.generate(
          3,
          (_) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 24),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Container(
                  width: 140,
                  height: 16,
                  decoration: BoxDecoration(
                    color: AppTheme.shimmerBase,
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                height: 268,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  itemCount: 3,
                  separatorBuilder: (_, _) => const SizedBox(width: 12),
                  itemBuilder: (_, _) => const SizedBox(
                    width: 200,
                    child: CardShimmer(),
                  ),
                ),
              ),
            ],
          ),
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

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 268,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 24),
        clipBehavior: Clip.none,
        itemCount: destinations.length,
        separatorBuilder: (_, _) => const SizedBox(width: 12),
        itemBuilder: (context, index) => SizedBox(
          width: 200,
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
        borderRadius: BorderRadius.circular(6),
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (hasImage)
              CachedNetworkImage(
                imageUrl: destination.heroImageUrl,
                fit: BoxFit.cover,
                placeholder: (_, _) => Container(color: placeholder),
                errorWidget: (_, _, _) => Container(color: placeholder),
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
                    Colors.black.withValues(alpha: 0.15),
                    Colors.black.withValues(alpha: 0.78),
                  ],
                  stops: const [0.0, 0.35, 0.6, 1.0],
                ),
              ),
            ),

            Positioned(
              top: 14,
              left: 14,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.45),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  child: Text(
                    destination.country.toUpperCase(),
                    style: AppTheme.eyebrow(color: AppTheme.goldSoft, letterSpacing: 1.6)
                        .copyWith(fontSize: 9),
                  ),
                ),
              ),
            ),

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
                      style: GoogleFonts.cormorantGaramond(
                        fontSize: 12,
                        fontStyle: FontStyle.italic,
                        fontWeight: FontWeight.w400,
                        color: Colors.white.withValues(alpha: 0.78),
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

// ── Interest Chip ─────────────────────────────────────────────────────────────

class _InterestChip extends StatelessWidget {
  final String label;
  const _InterestChip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
      decoration: BoxDecoration(
        color: AppTheme.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(4),
        border:
            Border.all(color: AppTheme.primary.withValues(alpha: 0.25), width: 1),
      ),
      child: Text(
        label,
        style: GoogleFonts.jost(
          fontSize: 11,
          color: AppTheme.primary,
          letterSpacing: 0.4,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}
