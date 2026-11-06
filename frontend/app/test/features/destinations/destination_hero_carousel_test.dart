// Testes do carrossel de fotos do hero em DestinationScreen (_HeroCarousel)
// — heroImageUrl (principal) + images (extras via Wikimedia Commons, ver
// backend Destination.images / luvoy-pendencias.md, 2026-11-02).

import 'package:app/features/destinations/data/destination_detail_model.dart';
import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/destinations/presentation/screens/destination_screen.dart';
import 'package:app/features/favorites/data/favorite_model.dart';
import 'package:app/features/favorites/presentation/providers/favorite_provider.dart';
import 'package:app/features/itinerarytemplates/presentation/providers/itinerary_template_provider.dart';
import 'package:app/features/unusualplaces/presentation/providers/unusual_place_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// `_CarouselDot` é privado de `destination_screen.dart` — miramos pelo
/// nome do tipo em runtime em vez de `find.byType`, que exigiria importar
/// o símbolo privado. `AnimatedContainer` sozinho não serve: `IconButton`/
/// `Material` do Flutter também usam `AnimatedContainer` internamente (os
/// botões de voltar/favoritar do app bar), então pegaria widgets demais.
Finder findCarouselDots() => find.byWidgetPredicate(
  (widget) => widget.runtimeType.toString() == '_CarouselDot',
);

class _FakeFavoriteNotifier extends FavoriteNotifier {
  @override
  Future<List<FavoriteModel>> build() async => [];
}

void main() {
  DestinationDetailModel destination({
    String heroImageUrl = '',
    List<String> images = const [],
  }) {
    return DestinationDetailModel(
      id: 'dest-1',
      slug: 'paris',
      name: 'Paris',
      country: 'França',
      city: 'Paris',
      tagline: 'A cidade luz',
      heroImageUrl: heroImageUrl,
      images: images,
      tags: const [],
      sections: const [],
      pois: const [],
    );
  }

  Future<void> pumpDestinationScreen(
    WidgetTester tester, {
    required DestinationDetailModel dest,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          destinationDetailProvider(
            dest.slug,
          ).overrideWith((ref) async => dest),
          favoritesProvider.overrideWith(() => _FakeFavoriteNotifier()),
          unusualPlacesForDestinationProvider(
            dest.slug,
          ).overrideWith((ref) async => const []),
          templatesForDestinationProvider(
            dest.slug,
          ).overrideWith((ref) async => const []),
        ],
        child: MaterialApp(home: DestinationScreen(slug: dest.slug)),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets(
    'com heroImageUrl + 2 images, mostra 3 bolinhas de indicador no carrossel',
    (tester) async {
      await pumpDestinationScreen(
        tester,
        dest: destination(
          heroImageUrl: 'https://example.com/hero.jpg',
          images: const [
            'https://example.com/extra1.jpg',
            'https://example.com/extra2.jpg',
          ],
        ),
      );

      expect(find.byType(PageView), findsOneWidget);
      expect(findCarouselDots(), findsNWidgets(3));
    },
  );

  testWidgets(
    'só com heroImageUrl (sem images extras) não mostra bolinhas de indicador',
    (tester) async {
      await pumpDestinationScreen(
        tester,
        dest: destination(heroImageUrl: 'https://example.com/hero.jpg'),
      );

      expect(find.byType(PageView), findsOneWidget);
      expect(findCarouselDots(), findsNothing);
    },
  );

  testWidgets(
    'sem nenhuma foto (nem hero nem extras) não quebra a tela — placeholder '
    'escuro, sem PageView',
    (tester) async {
      await pumpDestinationScreen(tester, dest: destination());

      expect(find.byType(PageView), findsNothing);
      expect(find.byType(DestinationScreen), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
