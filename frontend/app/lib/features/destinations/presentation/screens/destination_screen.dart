import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../favorites/presentation/providers/favorite_provider.dart';
import '../../../itinerary/presentation/widgets/generate_itinerary_sheet.dart';
import '../../../itinerarytemplates/data/itinerary_template_models.dart';
import '../../../itinerarytemplates/presentation/providers/itinerary_template_provider.dart';
import '../../../itinerarytemplates/presentation/screens/template_preview_screen.dart';
import '../../../unusualplaces/data/unusual_place_models.dart';
import '../../../unusualplaces/presentation/providers/unusual_place_provider.dart';
import '../../data/destination_detail_model.dart';
import '../providers/destination_provider.dart';

const _sectionLabels = <String, String>{
  'ROTEIRO': 'Roteiro sugerido',
  'RESTAURANTES': 'Restaurantes',
  'CAFES': 'Cafés',
  'ATRACOES': 'Atrações',
  'EXPERIENCIAS': 'Experiências',
  'RUAS': 'Ruas & bairros',
  'INSTAGRAM': 'Para o Instagram',
  'LOJAS': 'Lojas',
  'ESCONDIDOS': 'Joias escondidas',
  'ROOFTOPS': 'Rooftops & bares',
  'SKINCARE': 'Skincare & bem-estar',
  'COMPRAS': 'Compras',
  'PRATOS': 'Pratos típicos',
  'MAPA': 'Mapa',
};

class DestinationScreen extends ConsumerWidget {
  final String slug;
  const DestinationScreen({super.key, required this.slug});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncValue = ref.watch(destinationDetailProvider(slug));

    return Scaffold(
      backgroundColor: context.colors.background,
      bottomNavigationBar: asyncValue.hasValue
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 12),
                child: SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () {
                      final dest = asyncValue.valueOrNull;
                      if (dest == null) return;
                      showModalBottomSheet(
                        context: context,
                        isScrollControlled: true,
                        backgroundColor: Colors.transparent,
                        builder: (_) => GenerateItinerarySheet(
                          destinationId: dest.id,
                          destinationName: dest.name,
                        ),
                      );
                    },
                    child: const Text('Gerar roteiro'),
                  ),
                ),
              ),
            )
          : null,
      body: asyncValue.when(
        loading: () => CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: context.colors.primary),
              ),
            ),
          ],
        ),
        error: (error, _) => CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'Não foi possível carregar o destino.',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.jost(
                          fontSize: 15,
                          color: context.colors.foreground,
                        ),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () =>
                            ref.invalidate(destinationDetailProvider(slug)),
                        child: const Text('Tentar novamente'),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
        data: (destination) => _DestinationBody(destination: destination),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Main body (data state)
// ---------------------------------------------------------------------------

class _DestinationBody extends StatelessWidget {
  final DestinationDetailModel destination;
  const _DestinationBody({required this.destination});

  @override
  Widget build(BuildContext context) {
    final sections =
        destination.sections.where((s) => s.content.isNotEmpty).toList()
          ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));

    return CustomScrollView(
      slivers: [
        _HeroAppBar(destination: destination),
        _DestinationHeader(destination: destination),
        if (destination.description != null &&
            destination.description!.isNotEmpty)
          _DescriptionSliver(description: destination.description!),
        _CuratedTemplatesSliver(slug: destination.slug),
        _UnusualPlacesSliver(slug: destination.slug),
        for (final section in sections) _SectionSliver(section: section),
        const SliverToBoxAdapter(child: SizedBox(height: 24)),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// SliverAppBar — hero image
// ---------------------------------------------------------------------------

class _HeroAppBar extends ConsumerWidget {
  final DestinationDetailModel destination;
  const _HeroAppBar({required this.destination});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final favoriteId = ref.watch(destinationFavoriteIdProvider(destination.id));
    final isFavorited = favoriteId != null;

    return SliverAppBar(
      expandedHeight: 320,
      pinned: true,
      backgroundColor: Colors.transparent,
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
            child: IconButton(
              icon: Icon(
                isFavorited
                    ? Icons.favorite_rounded
                    : Icons.favorite_border_rounded,
                color: isFavorited ? AppTheme.naval : Colors.white,
                size: 20,
              ),
              padding: EdgeInsets.zero,
              onPressed: () async {
                try {
                  if (isFavorited) {
                    await ref
                        .read(favoritesProvider.notifier)
                        .remove(favoriteId);
                  } else {
                    await ref
                        .read(favoritesProvider.notifier)
                        .add(destination.id);
                  }
                } catch (_) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Erro ao atualizar favorito'),
                      ),
                    );
                  }
                }
              },
            ),
          ),
        ),
      ],
      flexibleSpace: FlexibleSpaceBar(
        collapseMode: CollapseMode.parallax,
        background: Stack(
          fit: StackFit.expand,
          children: [
            // Hero image or dark placeholder
            destination.heroImageUrl.isNotEmpty
                ? CachedNetworkImage(
                    imageUrl: destination.heroImageUrl,
                    fit: BoxFit.cover,
                    placeholder: (_, _) =>
                        const ColoredBox(color: Color(0xFF1A252F)),
                    errorWidget: (_, _, _) =>
                        const ColoredBox(color: Color(0xFF1A252F)),
                  )
                : const ColoredBox(color: Color(0xFF1A252F)),
            // Bottom gradient so text is legible
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              height: 150,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      Colors.black.withValues(alpha: 0.7),
                      Colors.transparent,
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

// ---------------------------------------------------------------------------
// Header — badge, name, tagline, signature, weather, tags
// ---------------------------------------------------------------------------

class _DestinationHeader extends StatelessWidget {
  final DestinationDetailModel destination;
  const _DestinationHeader({required this.destination});

  bool get _hasWeather =>
      destination.weatherTemp != null ||
      destination.weatherCond != null ||
      destination.sunrise != null ||
      destination.sunset != null;

  @override
  Widget build(BuildContext context) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Country badge
            _CountryBadge(country: destination.country),
            const SizedBox(height: 14),

            // Destination name
            Text(
              destination.name,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 44,
                fontWeight: FontWeight.w600,
                color: context.colors.foreground,
                height: 1.05,
              ),
            ),
            const SizedBox(height: 8),

            // Tagline
            Text(
              destination.tagline,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 17,
                fontStyle: FontStyle.italic,
                fontWeight: FontWeight.w400,
                color: context.colors.mutedForeground,
                height: 1.5,
              ),
            ),

            // Signature
            if (destination.signature != null &&
                destination.signature!.isNotEmpty) ...[
              const SizedBox(height: 16),
              const Divider(),
              const SizedBox(height: 12),
              Text(
                destination.signature!,
                style: GoogleFonts.cormorantGaramond(
                  fontSize: 14,
                  fontStyle: FontStyle.italic,
                  color: context.colors.mutedForeground,
                  height: 1.5,
                ),
              ),
            ],

            // Weather row
            if (_hasWeather) ...[
              const SizedBox(height: 16),
              _WeatherRow(destination: destination),
            ],

            // Tags chips
            if (destination.tags.isNotEmpty) ...[
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: destination.tags
                    .map((tag) => _TagChip(label: tag))
                    .toList(),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Description
// ---------------------------------------------------------------------------

class _DescriptionSliver extends StatelessWidget {
  final String description;
  const _DescriptionSliver({required this.description});

  @override
  Widget build(BuildContext context) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Divider(),
            const SizedBox(height: 16),
            Text(
              description,
              style: GoogleFonts.jost(
                fontSize: 15,
                fontWeight: FontWeight.w300,
                height: 1.6,
                color: context.colors.foreground.withValues(alpha: 0.75),
              ),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Curated itinerary templates — item 1 do backlog
// ---------------------------------------------------------------------------
//
// Wrapper (_CuratedTemplatesSliver) implementado de verdade: o fallback
// gracioso em loading/error/lista-vazia é o MESMO padrão trivial já usado em
// _CurrencyRatesBar (itinerary_screen.dart) / RouteInfoBar (maps), então não
// há motivo para stub aqui — e, mais importante, DestinationScreen é uma
// tela já em uso; um stub que sempre lança em build() quebraria a navegação
// para QUALQUER destino, mesmo sem templates configurados. Deixar esse
// wrapper real limita o "raio de explosão" do estágio somente-testes: hoje
// nenhum destino tem templates publicados, então o branch `data` com lista
// não-vazia (onde mora o stub _CuratedTemplatesSection, que lança
// UnimplementedError de propósito) nunca é exercitado em uso real.

class _CuratedTemplatesSliver extends ConsumerWidget {
  final String slug;
  const _CuratedTemplatesSliver({required this.slug});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncTemplates = ref.watch(templatesForDestinationProvider(slug));

    return asyncTemplates.when(
      loading: () => const SliverToBoxAdapter(child: SizedBox.shrink()),
      error: (_, _) => const SliverToBoxAdapter(child: SizedBox.shrink()),
      data: (templates) {
        if (templates.isEmpty) {
          return const SliverToBoxAdapter(child: SizedBox.shrink());
        }
        return SliverToBoxAdapter(
          child: _CuratedTemplatesSection(slug: slug, templates: templates),
        );
      },
    );
  }
}

/// Renderiza os cards de templates curados (título, "N dias", chip de
/// budget tier) em uma lista horizontal; tocar em um card navega para
/// /destinos/:slug/roteiros/:templateId (TemplatePreviewScreen).
///
/// Navegação: usa `context.push` (go_router) quando há GoRouter disponível
/// no contexto (uso real, já que DestinationScreen sempre é montada sob o
/// GoRouter do app), com fallback para `Navigator.push` direto quando não
/// há — necessário porque os testes de widget desta seção montam
/// `MaterialApp(home: DestinationScreen(...))` sem GoRouter (ver
/// test/features/itinerarytemplates/itinerary_template_test.dart).
class _CuratedTemplatesSection extends StatelessWidget {
  final String slug;
  final List<ItineraryTemplateSummaryModel> templates;
  const _CuratedTemplatesSection({required this.slug, required this.templates});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Divider(),
          const SizedBox(height: 20),
          Text(
            'ROTEIROS PRONTOS',
            style: GoogleFonts.jost(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              letterSpacing: 1.5,
              color: context.colors.primary,
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 176,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: EdgeInsets.zero,
              itemCount: templates.length,
              separatorBuilder: (_, _) => const SizedBox(width: 12),
              itemBuilder: (context, index) =>
                  _CuratedTemplateCard(slug: slug, template: templates[index]),
            ),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

class _CuratedTemplateCard extends StatelessWidget {
  final String slug;
  final ItineraryTemplateSummaryModel template;
  const _CuratedTemplateCard({required this.slug, required this.template});

  static const _budgetLabels = {
    'ECONOMY': '€',
    'MODERATE': '€€',
    'LUXURY': '€€€',
  };

  void _openPreview(BuildContext context) {
    final route = '/destinos/$slug/roteiros/${template.id}';
    final router = GoRouter.maybeOf(context);
    if (router != null) {
      context.push(route);
    } else {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) =>
              TemplatePreviewScreen(slug: slug, templateId: template.id),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final budgetLabel =
        _budgetLabels[template.budgetTier] ?? template.budgetTier;

    return SizedBox(
      width: 220,
      child: Material(
        color: context.colors.card,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => _openPreview(context),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: context.colors.foreground.withValues(alpha: 0.08),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  template.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.cormorantGaramond(
                    fontSize: 20,
                    fontWeight: FontWeight.w600,
                    color: context.colors.foreground,
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(
                      Icons.calendar_today_outlined,
                      size: 12,
                      color: context.colors.foreground.withValues(alpha: 0.4),
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '${template.durationDays} dias',
                      style: GoogleFonts.jost(
                        fontSize: 12,
                        color: context.colors.foreground.withValues(alpha: 0.6),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: context.colors.primary.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        budgetLabel,
                        style: GoogleFonts.jost(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: context.colors.primary,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Lugares Incomuns — feature premium (escopo definido na implementação, ver
// resumo entregue ao usuário; não é spec oficial da contratante)
// ---------------------------------------------------------------------------
//
// Mesmo padrão gracioso de _CuratedTemplatesSliver: loading/error/lista
// vazia encolhem pra SizedBox.shrink() em vez de quebrar a tela do destino.

class _UnusualPlacesSliver extends ConsumerWidget {
  final String slug;
  const _UnusualPlacesSliver({required this.slug});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncPlaces = ref.watch(unusualPlacesForDestinationProvider(slug));

    return asyncPlaces.when(
      loading: () => const SliverToBoxAdapter(child: SizedBox.shrink()),
      error: (_, _) => const SliverToBoxAdapter(child: SizedBox.shrink()),
      data: (places) {
        if (places.isEmpty) {
          return const SliverToBoxAdapter(child: SizedBox.shrink());
        }
        return SliverToBoxAdapter(child: _UnusualPlacesSection(places: places));
      },
    );
  }
}

class _UnusualPlacesSection extends StatelessWidget {
  final List<UnusualPlaceModel> places;
  const _UnusualPlacesSection({required this.places});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Divider(),
          const SizedBox(height: 20),
          Text(
            'LUGARES INCOMUNS',
            style: GoogleFonts.jost(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              letterSpacing: 1.5,
              color: context.colors.primary,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Fora do roteiro óbvio — recomendação de quem conhece a cidade.',
            style: GoogleFonts.jost(
              fontSize: 13,
              color: context.colors.mutedForeground,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 16),
          for (int i = 0; i < places.length; i++) ...[
            if (i > 0) const Divider(thickness: 0.5),
            _UnusualPlaceTile(place: places[i]),
          ],
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

class _UnusualPlaceTile extends StatelessWidget {
  final UnusualPlaceModel place;
  const _UnusualPlaceTile({required this.place});

  @override
  Widget build(BuildContext context) {
    if (place.locked) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            Icon(
              Icons.lock_outline,
              size: 16,
              color: context.colors.mutedForeground.withValues(alpha: 0.6),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                place.title,
                style: GoogleFonts.jost(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: context.colors.mutedForeground,
                ),
              ),
            ),
            TextButton(
              onPressed: () => context.push('/premium'),
              child: Text(
                'Desbloquear',
                style: GoogleFonts.jost(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: context.colors.primary,
                ),
              ),
            ),
          ],
        ),
      );
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  place.title,
                  style: GoogleFonts.jost(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: context.colors.foreground,
                    height: 1.3,
                  ),
                ),
              ),
              if (place.category != null && place.category!.isNotEmpty)
                Container(
                  margin: const EdgeInsets.only(left: 8),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: context.colors.primary.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    place.category!,
                    style: GoogleFonts.jost(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: context.colors.primary,
                    ),
                  ),
                ),
            ],
          ),
          if (place.description != null && place.description!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              place.description!,
              style: GoogleFonts.jost(
                fontSize: 13,
                color: context.colors.mutedForeground,
                height: 1.5,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Section
// ---------------------------------------------------------------------------

class _SectionSliver extends StatelessWidget {
  final DestinationSectionModel section;
  const _SectionSliver({required this.section});

  @override
  Widget build(BuildContext context) {
    final label = _sectionLabels[section.sectionType] ?? section.sectionType;

    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Divider(),
            const SizedBox(height: 20),
            Text(
              label.toUpperCase(),
              style: GoogleFonts.jost(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                letterSpacing: 1.5,
                color: context.colors.primary,
              ),
            ),
            const SizedBox(height: 12),
            ...section.content.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (index > 0) const Divider(thickness: 0.5),
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.name,
                          style: GoogleFonts.jost(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: context.colors.foreground,
                            height: 1.3,
                          ),
                        ),
                        if (item.desc != null && item.desc!.isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text(
                            item.desc!,
                            style: GoogleFonts.jost(
                              fontSize: 13,
                              color: context.colors.mutedForeground,
                              height: 1.5,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              );
            }),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Small reusable widgets
// ---------------------------------------------------------------------------

class _CountryBadge extends StatelessWidget {
  final String country;
  const _CountryBadge({required this.country});

  @override
  Widget build(BuildContext context) {
    return Text(
      country.toUpperCase(),
      style: AppTheme.eyebrow(color: context.colors.primary),
    );
  }
}

class _WeatherRow extends StatelessWidget {
  final DestinationDetailModel destination;
  const _WeatherRow({required this.destination});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 16,
      runSpacing: 8,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        if (destination.weatherTemp != null)
          _WeatherItem(
            icon: Icons.thermostat_outlined,
            text: destination.weatherTemp!,
          ),
        if (destination.weatherCond != null)
          _WeatherItem(
            icon: Icons.wb_cloudy_outlined,
            text: destination.weatherCond!,
          ),
        if (destination.sunrise != null)
          _WeatherItem(
            icon: Icons.wb_sunny_outlined,
            text: destination.sunrise!,
          ),
        if (destination.sunset != null)
          _WeatherItem(
            icon: Icons.nights_stay_outlined,
            text: destination.sunset!,
          ),
      ],
    );
  }
}

class _WeatherItem extends StatelessWidget {
  final IconData icon;
  final String text;
  const _WeatherItem({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: context.colors.mutedForeground),
        const SizedBox(width: 4),
        Text(
          text,
          style: GoogleFonts.jost(
            fontSize: 13,
            color: context.colors.mutedForeground,
          ),
        ),
      ],
    );
  }
}

class _TagChip extends StatelessWidget {
  final String label;
  const _TagChip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(4),
        border: Border.all(
          color: context.colors.primary.withValues(alpha: 0.5),
          width: 1,
        ),
      ),
      child: Text(
        label,
        style: GoogleFonts.jost(
          fontSize: 12,
          fontWeight: FontWeight.w400,
          color: context.colors.mutedForeground,
        ),
      ),
    );
  }
}
