import 'dart:math';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../shared/widgets/loading_shimmer.dart';
import '../../../../shared/widgets/scroll_focus_blur.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';
import '../../../onboarding/presentation/widgets/preference_steps.dart'
    show interestOptions, interestLabels;
import '../../../profile/data/user_model.dart';
import '../../../profile/domain/traveler_archetype.dart';
import '../../../profile/presentation/providers/user_provider.dart';

// Rótulos específicos desta tela (mais descritivos que o texto curto usado
// no onboarding, ex. "Cultura & arte" em vez de só "Cultura") — sempre a
// partir das mesmas chaves de interestOptions, nunca uma lista à parte,
// senão reintroduz o mesmo tipo de divergência que já causou a tela
// mostrar seções como "arquitetura"/"praia" cruas (valores de interesse
// antigos que não existem mais em interestOptions).
const _interestLabelOverrides = {
  'cultura': 'Cultura & arte',
  'moda': 'Moda & design',
  'musica': 'Música & shows',
};

String _interestLabel(String interest) =>
    _interestLabelOverrides[interest] ?? interestLabels[interest] ?? interest;

const _diacriticsMap = {
  'á': 'a',
  'à': 'a',
  'ã': 'a',
  'â': 'a',
  'é': 'e',
  'ê': 'e',
  'í': 'i',
  'ó': 'o',
  'õ': 'o',
  'ô': 'o',
  'ú': 'u',
  'ç': 'c',
};

/// Normaliza pra comparar interesse (`interestOptions`, com hífen, ex.
/// "vida-noturna") contra tag de destino (`destination_tags`, com espaço,
/// ex. "vida noturna") — sem isso, `"vida noturna".contains("vida-noturna")`
/// é sempre falso e a seção inteira some mesmo havendo destinos reais com
/// aquela tag (bug real: 41 destinos tagueados "vida noturna" nunca
/// apareciam em "Para Você"). Também remove acentos, pelo mesmo motivo
/// ("música-trad" não batia com a chave "musica" sem acento).
String _normalizeForMatch(String value) {
  final lower = value.toLowerCase().replaceAll('-', ' ');
  final buffer = StringBuffer();
  for (final char in lower.split('')) {
    buffer.write(_diacriticsMap[char] ?? char);
  }
  return buffer.toString();
}

/// Só os interesses que ainda existem no onboarding atual — descarta valores
/// legados (ex. de uma versão antiga da lista de interesses) que não têm
/// mais rótulo nem ícone definidos, em vez de mostrá-los crus na tela.
List<String> _validInterests(List<String> interests) =>
    interests.where(interestOptions.contains).toList();

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
    final validInterests = _validInterests(user?.interests ?? const []);

    return Scaffold(
      backgroundColor: context.colors.background,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(child: _buildHeader(context, user)),
            if (validInterests.isNotEmpty)
              SliverToBoxAdapter(
                child: _buildInterestChips(context, validInterests),
              ),
            const SliverToBoxAdapter(child: SizedBox(height: 8)),
            ...destinationsAsync.when(
              loading: () => [_buildShimmerCarousels()],
              error: (e, _) => [_buildError(context, ref)],
              data: (destinations) {
                final hasInterests =
                    user != null &&
                    user.onboardingCompleted &&
                    validInterests.isNotEmpty;
                if (!hasInterests) return [_buildEmptyState(context)];
                return _buildCategorySections(
                  context,
                  destinations,
                  validInterests,
                );
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
    final hasProfile = user != null && user.onboardingCompleted;
    final archetype = hasProfile
        ? computeTravelerArchetype(
            _validInterests(user.interests),
            rhythm: user.rhythm,
          )
        : null;

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'PARA VOCÊ',
            style: AppTheme.eyebrow(color: context.colors.foreground),
          ),
          const SizedBox(height: 10),
          Text(
            'Selecionado para você',
            style: GoogleFonts.cormorantGaramond(
              fontSize: 36,
              fontWeight: FontWeight.w500,
              color: context.colors.foreground,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 6),
          if (archetype == null)
            Text(
              'Complete seu perfil para ver destinos personalizados',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: context.colors.mutedForeground,
              ),
            )
          else ...[
            Text(
              archetype.title,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 20,
                fontStyle: FontStyle.italic,
                fontWeight: FontWeight.w600,
                color: context.colors.foreground,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              archetype.description,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: context.colors.mutedForeground,
                height: 1.4,
              ),
            ),
          ],
        ],
      ),
    );
  }

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
              color: context.colors.mutedForeground,
              letterSpacing: 1.2,
            ),
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: interests
                .map((i) => _InterestChip(label: _interestLabel(i)))
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
    List<String> interests,
  ) {
    // Uma seção por interesse do usuário, cada uma buscando na lista
    // COMPLETA de destinos (não num top-10 combinado de todos os
    // interesses) — corrige bug em que interesses menos "populares" (ex.:
    // bem-estar, vida-local) sumiam inteiros quando não pontuavam alto o
    // bastante frente aos outros interesses do mesmo perfil, mesmo havendo
    // destinos reais com aquela tag.
    final sections = <({String label, List<DestinationSummaryModel> items})>[];
    for (final interest in interests) {
      final key = _normalizeForMatch(interest);
      final matching = destinations
          .where((d) => d.tags.any((t) => _normalizeForMatch(t).contains(key)))
          .toList();
      if (matching.isNotEmpty) {
        sections.add((
          label: _interestLabel(interest),
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
        .map(
          (s) => SliverToBoxAdapter(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 24),
                _SectionHeader(title: s.label),
                const SizedBox(height: 12),
                ScrollFocusBlur(
                  child: _HorizontalCarousel(destinations: s.items),
                ),
              ],
            ),
          ),
        )
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
                color: context.colors.foreground,
                height: 1.25,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(
              'Complete seu perfil de viajante para receber sugestões personalizadas baseadas nos seus gostos.',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: context.colors.mutedForeground,
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
                  itemBuilder: (_, _) =>
                      const SizedBox(width: 200, child: CardShimmer()),
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
            Icon(
              Icons.wifi_off_rounded,
              size: 40,
              color: context.colors.mutedForeground,
            ),
            const SizedBox(height: 16),
            Text(
              'Não foi possível carregar os destinos',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: context.colors.mutedForeground,
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
          color: context.colors.foreground,
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
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 3,
                  ),
                  child: Text(
                    destination.country.toUpperCase(),
                    style: AppTheme.eyebrow(
                      color: AppTheme.goldSoft,
                      letterSpacing: 1.6,
                    ).copyWith(fontSize: 9),
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
        border: Border.all(
          color: AppTheme.primary.withValues(alpha: 0.25),
          width: 1,
        ),
      ),
      child: Text(
        label,
        style: GoogleFonts.jost(
          fontSize: 11,
          color: context.colors.foreground,
          letterSpacing: 0.4,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}
