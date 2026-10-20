// Testes do fluxo de exportação de PDF a partir do menu "⋮" da tela de
// roteiro (itinerary_screen.dart._exportPdf). A geração do PDF em si já é
// coberta em test/features/pdf/pdf_generator_test.dart — aqui o foco é a
// orquestração: chama PdfRepository.confirmExport, e só então compartilha
// o PDF; trata o 402 (limite grátis esgotado) com uma mensagem específica
// e ação "Ver planos".
//
// O pacote `printing` (diferente de url_launcher) NÃO expõe publicamente
// sua PrintingPlatform pra mock (fica em lib/src/, sem pacote
// -platform_interface separado) — por isso o mock aqui é no nível do
// MethodChannel nativo ('net.nfet.printing'/'sharePdf'), via
// TestDefaultBinaryMessengerBinding, em vez do padrão de
// UrlLauncherPlatform.instance usado em maps_test.dart.

import 'package:app/features/currency/presentation/providers/currency_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itinerary_screen.dart';
import 'package:app/features/pdf/data/pdf_repository.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

class MockPdfRepository extends Mock implements PdfRepository {}

void main() {
  late MockPdfRepository mockPdfRepo;
  const printingChannel = MethodChannel('net.nfet.printing');
  final List<MethodCall> printingCalls = [];

  setUp(() {
    mockPdfRepo = MockPdfRepository();
    printingCalls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(printingChannel, (call) async {
          printingCalls.add(call);
          if (call.method == 'sharePdf') return 1; // truthy = sucesso
          return null;
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(printingChannel, null);
  });

  const poi = PoiDetailModel(
    id: 'poi-1',
    title: 'Museu de Arte Moderna',
    category: 'MUSEUM',
    budgetTier: 'MODERATE',
  );

  ItineraryModel sampleItinerary() {
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
          activities: const [
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

  Future<void> pumpAndOpenMenu(
    WidgetTester tester,
    ItineraryModel itinerary,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          itineraryRepositoryProvider.overrideWithValue(
            MockItineraryRepository(),
          ),
          pdfRepositoryProvider.overrideWithValue(mockPdfRepo),
          itineraryDetailProvider(
            itinerary.id,
          ).overrideWith((ref) async => itinerary),
          // Câmbio não importa pra este teste — erro é tratado
          // graciosamente por _CurrencyRatesBar (não renderiza nada).
          currencyRatesProvider.overrideWith(
            (ref) async => throw Exception('não usado neste teste'),
          ),
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
              GoRoute(
                path: '/premium',
                builder: (context, state) =>
                    const Scaffold(body: Text('Tela de Planos')),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.more_vert));
    await tester.pumpAndSettle();
  }

  testWidgets(
    'tocar em "Exportar PDF" chama confirmExport e compartilha o PDF gerado',
    (tester) async {
      when(() => mockPdfRepo.confirmExport('itin-1')).thenAnswer((_) async {});

      await pumpAndOpenMenu(tester, sampleItinerary());

      await tester.tap(find.text('Exportar PDF'));
      await tester.pumpAndSettle();

      verify(() => mockPdfRepo.confirmExport('itin-1')).called(1);
      expect(printingCalls.where((c) => c.method == 'sharePdf'), hasLength(1));
      expect(find.byType(SnackBar), findsNothing);
    },
  );

  testWidgets(
    'quando o backend recusa com 402 (limite grátis esgotado), mostra a '
    'mensagem específica com ação "Ver planos" — sem chamar o compartilhamento',
    (tester) async {
      final requestOptions = RequestOptions(path: '/pdf/export/itin-1');
      when(() => mockPdfRepo.confirmExport('itin-1')).thenThrow(
        DioException(
          requestOptions: requestOptions,
          response: Response(requestOptions: requestOptions, statusCode: 402),
        ),
      );

      await pumpAndOpenMenu(tester, sampleItinerary());

      await tester.tap(find.text('Exportar PDF'));
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Você já usou sua exportação gratuita'),
        findsOneWidget,
      );
      expect(find.text('Ver planos'), findsOneWidget);
      expect(printingCalls.where((c) => c.method == 'sharePdf'), isEmpty);

      await tester.tap(find.text('Ver planos'));
      await tester.pumpAndSettle();

      expect(find.text('Tela de Planos'), findsOneWidget);
    },
  );

  testWidgets('erro genérico (não é 402) mostra mensagem de erro genérica', (
    tester,
  ) async {
    when(
      () => mockPdfRepo.confirmExport('itin-1'),
    ).thenThrow(Exception('falha de rede'));

    await pumpAndOpenMenu(tester, sampleItinerary());

    await tester.tap(find.text('Exportar PDF'));
    await tester.pumpAndSettle();

    expect(
      find.text('Não foi possível exportar o PDF. Tente novamente.'),
      findsOneWidget,
    );
  });
}
