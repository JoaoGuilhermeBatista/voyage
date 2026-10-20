// Teste do toggle "Ver mapa"/"Ver rota no mapa" de itinerary_screen.dart —
// mesmo espírito de itinerary_pdf_export_test.dart: monta a tela real via
// GoRouter, mas aqui o foco é a UI de _MapPreviewToggle/PoiMapPreview/
// RouteMapPreview:
//  - some completamente quando o POI não tem latitude/longitude;
//  - aparece colapsado ("Ver mapa") quando tem, e expande ao tocar,
//    mostrando o preview (MapEmbedView — resolve pro stub em VM/flutter
//    test, ver map_embed_view_stub.dart);
//  - entre duas atividades consecutivas do mesmo slot, ambas com
//    coordenadas, também aparece "Ver rota no mapa" (RouteMapPreview).

import 'package:app/features/currency/presentation/providers/currency_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itinerary_screen.dart';
import 'package:app/features/maps/data/maps_models.dart';
import 'package:app/features/maps/presentation/providers/maps_provider.dart';
import 'package:app/features/maps/presentation/widgets/map_embed_view.dart';
import 'package:app/features/pdf/data/pdf_repository.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

class MockPdfRepository extends Mock implements PdfRepository {}

void main() {
  const poiComCoordenadas = PoiDetailModel(
    id: 'poi-1',
    title: 'Museu de Arte Moderna',
    category: 'MUSEUM',
    budgetTier: 'MODERATE',
    latitude: -23.5505,
    longitude: -46.6333,
  );

  const poiSemCoordenadas = PoiDetailModel(
    id: 'poi-2',
    title: 'Feira de Rua',
    category: 'ACTIVITY',
    budgetTier: 'BUDGET',
  );

  const poiVizinhoComCoordenadas = PoiDetailModel(
    id: 'poi-3',
    title: 'Museu Boijmans',
    category: 'MUSEUM',
    budgetTier: 'MODERATE',
    latitude: -22.9068,
    longitude: -43.1729,
  );

  ItineraryModel itineraryWithTwoActivities(
    PoiDetailModel first,
    PoiDetailModel second,
  ) {
    return ItineraryModel(
      id: 'itin-1',
      title: 'Viagem a São Paulo',
      destinationName: 'São Paulo',
      destinationHeroImageUrl: '',
      destinationCountry: '',
      startDate: DateTime(2026, 9, 1),
      endDate: DateTime(2026, 9, 2),
      days: [
        ItineraryDayModel(
          dayNumber: 1,
          date: DateTime(2026, 9, 1),
          activities: [
            ItineraryActivityModel(
              id: 'act-1',
              slot: 'MORNING',
              orderInSlot: 1,
              poi: first,
            ),
            ItineraryActivityModel(
              id: 'act-2',
              slot: 'MORNING',
              orderInSlot: 2,
              poi: second,
            ),
          ],
        ),
      ],
    );
  }

  ItineraryModel itineraryWith(PoiDetailModel poi) {
    return ItineraryModel(
      id: 'itin-1',
      title: 'Viagem a São Paulo',
      destinationName: 'São Paulo',
      destinationHeroImageUrl: '',
      destinationCountry: '',
      startDate: DateTime(2026, 9, 1),
      endDate: DateTime(2026, 9, 2),
      days: [
        ItineraryDayModel(
          dayNumber: 1,
          date: DateTime(2026, 9, 1),
          activities: [
            ItineraryActivityModel(
              id: 'act-1',
              slot: 'MORNING',
              orderInSlot: 1,
              poi: poi,
            ),
          ],
        ),
      ],
    );
  }

  Future<void> pumpScreen(
    WidgetTester tester,
    ItineraryModel itinerary, {
    List<Override> extraOverrides = const [],
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          itineraryRepositoryProvider.overrideWithValue(
            MockItineraryRepository(),
          ),
          pdfRepositoryProvider.overrideWithValue(MockPdfRepository()),
          itineraryDetailProvider(
            itinerary.id,
          ).overrideWith((ref) async => itinerary),
          currencyRatesProvider.overrideWith(
            (ref) async => throw Exception('não usado neste teste'),
          ),
          ...extraOverrides,
        ],
        child: MaterialApp.router(
          routerConfig: GoRouter(
            initialLocation: '/roteiro/${itinerary.id}',
            routes: [
              GoRoute(
                path: '/roteiro/:id',
                builder: (context, state) =>
                    ItineraryScreen(id: state.pathParameters['id']!),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets(
    'não mostra o toggle "Ver mapa" quando o POI não tem latitude/longitude',
    (tester) async {
      await pumpScreen(tester, itineraryWith(poiSemCoordenadas));

      expect(find.text('Ver mapa'), findsNothing);
    },
  );

  testWidgets(
    'mostra "Ver mapa" colapsado e, ao tocar, expande o preview do mapa',
    (tester) async {
      await pumpScreen(
        tester,
        itineraryWith(poiComCoordenadas),
        extraOverrides: [
          embedUrlProvider((-23.5505, -46.6333)).overrideWith(
            (ref) async => const EmbedUrlModel(
              configured: true,
              embedUrl: 'https://www.google.com/maps/embed/v1/place?key=k',
            ),
          ),
        ],
      );

      expect(find.text('Ver mapa'), findsOneWidget);
      expect(find.byType(MapEmbedView), findsNothing);

      await tester.tap(find.text('Ver mapa'));
      await tester.pumpAndSettle();

      expect(find.text('Ocultar mapa'), findsOneWidget);
      expect(find.byType(MapEmbedView), findsOneWidget);
    },
  );

  testWidgets(
    'mostra "Ver rota no mapa" entre duas atividades consecutivas com '
    'coordenadas, e expande a rota ao tocar',
    (tester) async {
      // Superfície de teste maior — com 2 atividades + RouteInfoBar +
      // toggle de rota, o conteúdo passa da altura padrão de teste (mesmo
      // gotcha recorrente de ListView/CustomScrollView do projeto: o que
      // fica fora do viewport padrão não é tocável de verdade).
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await pumpScreen(
        tester,
        itineraryWithTwoActivities(
          poiComCoordenadas,
          poiVizinhoComCoordenadas,
        ),
        extraOverrides: [
          // RouteInfoBar (o texto de distância/duração) é renderizado
          // incondicionalmente, sem esperar o toggle — precisa de override
          // pra não cair no ApiClient/Supabase.instance real neste teste.
          directionsProvider((-23.5505, -46.6333, -22.9068, -43.1729))
              .overrideWith(
            (ref) async => const DirectionsModel(configured: false),
          ),
          embedUrlProvider((-23.5505, -46.6333)).overrideWith(
            (ref) async => const EmbedUrlModel(configured: false),
          ),
          embedUrlProvider((-22.9068, -43.1729)).overrideWith(
            (ref) async => const EmbedUrlModel(configured: false),
          ),
          directionsEmbedUrlProvider(
            (-23.5505, -46.6333, -22.9068, -43.1729),
          ).overrideWith(
            (ref) async => const EmbedUrlModel(
              configured: true,
              embedUrl:
                  'https://www.google.com/maps/embed/v1/directions?key=k',
            ),
          ),
        ],
      );

      expect(find.text('Ver rota no mapa'), findsOneWidget);
      expect(find.byType(MapEmbedView), findsNothing);

      await tester.tap(find.text('Ver rota no mapa'));
      await tester.pumpAndSettle();

      expect(find.text('Ocultar rota'), findsOneWidget);
      expect(find.byType(MapEmbedView), findsOneWidget);
    },
  );
}
