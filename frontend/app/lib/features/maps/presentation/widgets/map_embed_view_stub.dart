import 'package:flutter/material.dart';

import '../../../../core/themes/app_theme.dart';

/// Stub não-web de [MapEmbedView] — usado em Android/iOS nativo e
/// desktop, onde não há suporte a <iframe> nem ao Maps SDK nativo ainda
/// (isso dependeria de google_maps_flutter, uma API do Google diferente
/// da Embed API usada aqui — ver contexto em map_embed_view_web.dart).
/// Mesmo espírito de fallback gracioso de RouteInfoBar: não quebra a
/// tela, só mostra uma nota discreta.
class MapEmbedView extends StatelessWidget {
  final String embedUrl;
  final double height;

  const MapEmbedView({super.key, required this.embedUrl, this.height = 220});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: height,
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppTheme.shimmerBase,
        borderRadius: BorderRadius.circular(12),
      ),
      alignment: Alignment.center,
      child: Text(
        'Mapa visual disponível na versão web do LUVOY',
        textAlign: TextAlign.center,
        style: TextStyle(
          fontSize: 12,
          color: AppTheme.mutedForeground,
        ),
      ),
    );
  }
}
