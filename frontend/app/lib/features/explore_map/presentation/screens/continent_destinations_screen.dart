import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../../core/themes/app_theme.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';
import '../../domain/continent.dart';

/// Lista de destinos reais do catálogo pertencentes a um continente — tela
/// de destino do toque num continente na tela "Mapa" (#26). Filtra
/// client-side a partir do mesmo `destinationsProvider` já usado pela Home,
/// via `continentOf(destination.country)`: não precisa de nenhum endpoint
/// novo nem de coluna nova no banco, já que continente é 100% derivável do
/// país que já existe.
class ContinentDestinationsScreen extends ConsumerWidget {
  final Continent continent;
  const ContinentDestinationsScreen({super.key, required this.continent});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final destinationsAsync = ref.watch(destinationsProvider);

    return Scaffold(
      backgroundColor: context.colors.background,
      appBar: AppBar(
        backgroundColor: context.colors.background,
        elevation: 0,
        iconTheme: IconThemeData(color: context.colors.foreground),
        title: Text(
          continent.label,
          style: GoogleFonts.cormorantGaramond(
            fontSize: 22,
            fontWeight: FontWeight.w500,
            color: context.colors.foreground,
          ),
        ),
      ),
      body: destinationsAsync.when(
        data: (all) {
          final destinations = all
              .where((d) => continentOf(d.country) == continent)
              .toList();
          if (destinations.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32),
                child: Text(
                  'Ainda não temos destinos curados aqui',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: context.colors.mutedForeground,
                  ),
                ),
              ),
            );
          }
          return GridView.builder(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              mainAxisSpacing: 16,
              crossAxisSpacing: 12,
              childAspectRatio: 4 / 5,
            ),
            itemCount: destinations.length,
            itemBuilder: (context, index) =>
                _DestinationTile(destination: destinations[index]),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.wifi_off_rounded,
                  size: 36,
                  color: context.colors.foreground.withValues(alpha: 0.3),
                ),
                const SizedBox(height: 12),
                Text(
                  'Não foi possível carregar os destinos',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: context.colors.foreground.withValues(alpha: 0.5),
                  ),
                ),
                const SizedBox(height: 16),
                TextButton(
                  onPressed: () => ref.refresh(destinationsProvider),
                  child: const Text('Tentar novamente'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DestinationTile extends StatelessWidget {
  final DestinationSummaryModel destination;
  const _DestinationTile({required this.destination});

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
        borderRadius: BorderRadius.circular(8),
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
                    Colors.black.withValues(alpha: 0.8),
                  ],
                  stops: const [0.4, 0.65, 1.0],
                ),
              ),
            ),
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(10, 0, 10, 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      destination.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.cormorantGaramond(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      destination.country,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.jost(
                        fontSize: 11,
                        fontWeight: FontWeight.w300,
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
