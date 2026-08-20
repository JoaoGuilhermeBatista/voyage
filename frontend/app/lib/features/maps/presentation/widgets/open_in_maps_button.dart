import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../../core/themes/app_theme.dart';

/// Botão "Abrir no Google Maps" — deep-link puro via URL, SEM chave de
/// API e SEM chamada ao backend (item 3 do backlog de Maps). Funciona
/// hoje independentemente do estado de configuração do Directions/Embed
/// proxy (item 5), já que usa apenas lat/lng já armazenados no POI.
///
/// URL scheme: https://www.google.com/maps/search/?api=1&query=lat,lng
/// — abre o app do Google Maps (se instalado) ou o site, no dispositivo
/// do usuário.
class OpenInMapsButton extends StatelessWidget {
  final double lat;
  final double lng;

  const OpenInMapsButton({
    super.key,
    required this.lat,
    required this.lng,
  });

  @visibleForTesting
  Uri buildUri() {
    return Uri.parse('https://www.google.com/maps/search/?api=1&query=$lat,$lng');
  }

  Future<void> _open(BuildContext context) async {
    final launched = await launchUrl(
      buildUri(),
      mode: LaunchMode.externalApplication,
    );
    if (!launched && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Não foi possível abrir o mapa.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Icon(
        Icons.map_outlined,
        size: 18,
        color: AppTheme.primaryBlue,
      ),
      tooltip: 'Abrir no Google Maps',
      padding: EdgeInsets.zero,
      constraints: const BoxConstraints(),
      onPressed: () => _open(context),
    );
  }
}
