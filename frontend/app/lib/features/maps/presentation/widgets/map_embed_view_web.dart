import 'dart:ui_web' as ui_web;

import 'package:flutter/material.dart';
import 'package:web/web.dart' as web;

/// Implementação real (Flutter Web) de [MapEmbedView] — embute a
/// `embedUrl` da Google Maps Embed API (já validada ponta-a-ponta via
/// GET /api/v1/maps/embed-url, ver PoiMapPreview) dentro de um <iframe>,
/// usando HtmlElementView + `dart:ui_web`'s platformViewRegistry. É a
/// única forma viável hoje de mostrar um mapa visual: o Maps SDK nativo
/// (google_maps_flutter) não pode ser testado neste ambiente (sem
/// device Android nem Mac/Xcode), e webview_flutter não roda em Flutter
/// Web — mas a Embed API + iframe funciona nos dois mundos (nativo via
/// stub, web via esta implementação).
class MapEmbedView extends StatelessWidget {
  final String embedUrl;
  final double height;

  const MapEmbedView({super.key, required this.embedUrl, this.height = 220});

  // Registrar a mesma view factory duas vezes faz o platformViewRegistry
  // lançar exceção — por isso o cache global por viewType (a factory em
  // si é pura, então reaproveitar entre rebuilds do mesmo embedUrl é seguro).
  static final Set<String> _registeredViewTypes = {};

  String get _viewType => 'luvoy-map-embed-${embedUrl.hashCode}';

  @override
  Widget build(BuildContext context) {
    final viewType = _viewType;
    if (_registeredViewTypes.add(viewType)) {
      ui_web.platformViewRegistry.registerViewFactory(viewType, (int _) {
        return web.HTMLIFrameElement()
          ..src = embedUrl
          ..style.border = 'none'
          ..style.width = '100%'
          ..style.height = '100%'
          ..allowFullscreen = true
          ..loading = 'lazy';
      });
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: SizedBox(
        height: height,
        width: double.infinity,
        child: HtmlElementView(viewType: viewType),
      ),
    );
  }
}
