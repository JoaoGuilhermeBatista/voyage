import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../../core/themes/app_theme.dart';
import '../../domain/continent.dart';

/// Posição/tamanho de cada "zona" continental dentro do canvas do mapa,
/// como fração de 0 a 1 (0,0 = canto superior-esquerdo). Aproximação livre
/// de uma projeção equirretangular — não é geografia real, é só o bastante
/// pra cada zona cair perto de onde o continente de fato fica, numa
/// ilustração estática (Opção B aprovada pro #26: sem SVG/GeoJSON, sem
/// dependência nova de mapa).
class _ContinentZone {
  final Continent continent;
  final double left;
  final double top;
  final double width;
  final double height;
  const _ContinentZone(
    this.continent,
    this.left,
    this.top,
    this.width,
    this.height,
  );
}

const List<_ContinentZone> _zones = [
  _ContinentZone(Continent.americaDoNorte, 0.04, 0.06, 0.28, 0.34),
  _ContinentZone(Continent.americaDoSul, 0.18, 0.46, 0.20, 0.42),
  _ContinentZone(Continent.europa, 0.42, 0.06, 0.18, 0.24),
  _ContinentZone(Continent.africa, 0.42, 0.34, 0.20, 0.44),
  _ContinentZone(Continent.asia, 0.56, 0.06, 0.40, 0.46),
  _ContinentZone(Continent.oceania, 0.74, 0.56, 0.22, 0.26),
];

/// Tela "Mapa" (#26): mundo estilizado -> toque num continente -> lista de
/// destinos reais do catálogo naquele continente. Ainda sem entrada na
/// bottom nav bar (fica pra depois, junto da reorganização do ícone de
/// perfil) — por enquanto é alcançável só pelo ícone de mapa na Home.
class ExploreMapScreen extends StatelessWidget {
  const ExploreMapScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: context.colors.background,
      appBar: AppBar(
        backgroundColor: context.colors.background,
        elevation: 0,
        iconTheme: IconThemeData(color: context.colors.foreground),
        title: Text(
          'Mapa',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 22,
            fontWeight: FontWeight.w500,
            color: context.colors.foreground,
          ),
        ),
      ),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 12, 24, 0),
              child: Text(
                'Toque num continente para ver os destinos LUVOY por lá',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: context.colors.mutedForeground,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: InteractiveViewer(
                  minScale: 1,
                  maxScale: 3,
                  child: AspectRatio(
                    aspectRatio: 16 / 10,
                    child: _WorldCanvas(),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

class _WorldCanvas extends StatelessWidget {
  const _WorldCanvas();

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: context.colors.secondary,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: context.colors.border),
      ),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final w = constraints.maxWidth;
          final h = constraints.maxHeight;
          return Stack(
            children: [
              for (final zone in _zones)
                Positioned(
                  left: zone.left * w,
                  top: zone.top * h,
                  width: zone.width * w,
                  height: zone.height * h,
                  child: _ContinentBlob(continent: zone.continent),
                ),
            ],
          );
        },
      ),
    );
  }
}

class _ContinentBlob extends StatelessWidget {
  final Continent continent;
  const _ContinentBlob({required this.continent});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push('/mapa/${continent.name}'),
      child: Container(
        decoration: BoxDecoration(
          color: context.colors.primary.withValues(alpha: 0.18),
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: context.colors.primary, width: 1.2),
        ),
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 6),
        child: Text(
          continent.label,
          textAlign: TextAlign.center,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: GoogleFonts.jost(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: context.colors.foreground,
          ),
        ),
      ),
    );
  }
}
