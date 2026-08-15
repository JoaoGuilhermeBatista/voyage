import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../favorites/presentation/providers/favorite_provider.dart';
import '../../../itinerary/presentation/widgets/generate_itinerary_sheet.dart';
import '../../data/destination_detail_model.dart';
import '../providers/destination_provider.dart';

const _sectionLabels = <String, String>{
  'ROTEIRO': 'Roteiro Sugerido',
  'RESTAURANTES': 'Restaurantes',
  'CAFES': 'Cafés',
  'ATRACOES': 'Atrações',
  'EXPERIENCIAS': 'Experiências',
  'RUAS': 'Ruas & Bairros',
  'INSTAGRAM': 'Para o Instagram',
  'LOJAS': 'Lojas',
  'ESCONDIDOS': 'Joias Escondidas',
  'ROOFTOPS': 'Rooftops & Bares',
  'SKINCARE': 'Skincare & Bem-estar',
  'COMPRAS': 'Compras',
  'PRATOS': 'Pratos Típicos',
  'MAPA': 'Mapa',
};

class DestinationScreen extends ConsumerWidget {
  final String slug;
  const DestinationScreen({super.key, required this.slug});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncValue = ref.watch(destinationDetailProvider(slug));

    return Scaffold(
      backgroundColor: AppTheme.background,
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
                    child: const Text('Gerar Roteiro'),
                  ),
                ),
              ),
            )
          : null,
      body: asyncValue.when(
        loading: () => const CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(
                  color: AppTheme.primaryBlue,
                ),
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
                        style: GoogleFonts.manrope(
                          fontSize: 15,
                          color: AppTheme.softBlack,
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
    final sections = destination.sections
        .where((s) => s.content.isNotEmpty)
        .toList()
      ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));

    return CustomScrollView(
      slivers: [
        _HeroAppBar(destination: destination),
        _DestinationHeader(destination: destination),
        if (destination.description != null &&
            destination.description!.isNotEmpty)
          _DescriptionSliver(description: destination.description!),
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
    final favoriteId =
        ref.watch(destinationFavoriteIdProvider(destination.id));
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
                color: isFavorited
                    ? const Color(0xFFE57373)
                    : Colors.white,
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
                          content: Text('Erro ao atualizar favorito')),
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
            const SizedBox(height: 12),

            // Destination name
            Text(
              destination.name,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 42,
                fontWeight: FontWeight.w600,
                color: AppTheme.softBlack,
                height: 1.1,
              ),
            ),
            const SizedBox(height: 8),

            // Tagline
            Text(
              destination.tagline,
              style: GoogleFonts.manrope(
                fontSize: 15,
                color: const Color(0xFF666666),
                height: 1.5,
              ),
            ),

            // Signature
            if (destination.signature != null &&
                destination.signature!.isNotEmpty) ...[
              const SizedBox(height: 16),
              const Divider(thickness: 1, color: Color(0xFFE8E4DC)),
              const SizedBox(height: 12),
              Text(
                destination.signature!,
                style: GoogleFonts.cormorantGaramond(
                  fontSize: 14,
                  fontStyle: FontStyle.italic,
                  color: const Color(0xFF666666),
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
            const Divider(thickness: 1, color: Color(0xFFE8E4DC)),
            const SizedBox(height: 16),
            Text(
              description,
              style: GoogleFonts.manrope(
                fontSize: 15,
                height: 1.6,
                color: const Color(0xFF444444),
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
// Section
// ---------------------------------------------------------------------------

class _SectionSliver extends StatelessWidget {
  final DestinationSectionModel section;
  const _SectionSliver({required this.section});

  @override
  Widget build(BuildContext context) {
    final label =
        _sectionLabels[section.sectionType] ?? section.sectionType;

    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Divider(thickness: 1, color: Color(0xFFE8E4DC)),
            const SizedBox(height: 20),
            Text(
              label.toUpperCase(),
              style: GoogleFonts.manrope(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                letterSpacing: 1.5,
                color: AppTheme.goldenOrange,
              ),
            ),
            const SizedBox(height: 12),
            ...section.content.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (index > 0)
                    const Divider(
                      thickness: 0.5,
                      color: Color(0xFFE8E4DC),
                    ),
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.name,
                          style: GoogleFonts.manrope(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: AppTheme.softBlack,
                            height: 1.3,
                          ),
                        ),
                        if (item.desc != null && item.desc!.isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text(
                            item.desc!,
                            style: GoogleFonts.manrope(
                              fontSize: 13,
                              color: const Color(0xFF888888),
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
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: AppTheme.goldenOrange.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(
          color: AppTheme.goldenOrange.withValues(alpha: 0.4),
          width: 1,
        ),
      ),
      child: Text(
        country.toUpperCase(),
        style: GoogleFonts.manrope(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          letterSpacing: 1.4,
          color: AppTheme.goldenOrange,
        ),
      ),
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
        Icon(icon, size: 14, color: const Color(0xFF888888)),
        const SizedBox(width: 4),
        Text(
          text,
          style: GoogleFonts.manrope(
            fontSize: 13,
            color: const Color(0xFF666666),
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
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: AppTheme.goldenOrange.withValues(alpha: 0.5),
          width: 1,
        ),
      ),
      child: Text(
        label,
        style: GoogleFonts.manrope(
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: const Color(0xFF666666),
        ),
      ),
    );
  }
}
