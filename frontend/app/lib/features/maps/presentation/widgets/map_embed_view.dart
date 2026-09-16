/// Widget que embute visualmente a `embedUrl` da Google Maps Embed API
/// (via <iframe> em HtmlElementView) — só existe implementação real no
/// Flutter Web (map_embed_view_web.dart); nas demais plataformas cai no
/// stub gracioso (map_embed_view_stub.dart), sem precisar de nenhuma API
/// nativa do Maps SDK nem de mudança de conta Google Cloud (reaproveita a
/// mesma chave/endpoint /api/v1/maps/embed-url já validado).
///
/// Import condicional clássico do Dart: `dart:ui_web`/`package:web` só
/// existem quando compilando para web — importar sem essa guarda quebra
/// a build de Android/iOS/desktop.
library;

export 'map_embed_view_stub.dart'
    if (dart.library.js_interop) 'map_embed_view_web.dart';
