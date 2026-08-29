// Testes da feature "Item B — segundo ponto de entrada para gerar roteiro"
// (frontend). Fase TDD "testes primeiro": só as mudanças mecânicas de
// assinatura foram implementadas de verdade nesta fase —
// ItineraryRepository.generate() ganhou tripInterests/tripRhythm (padrão
// idêntico aos parâmetros opcionais já existentes) e
// GenerateItinerarySheet(destinationId/destinationName) virou opcional — o
// resto (etapa de Cidade, etapas de Interesses/Ritmo usando os widgets do
// onboarding, regra de esconder "Sozinho" na Lua de mel, rótulo "Clima da
// viagem") ainda NÃO está implementado. Os testes abaixo documentam o
// contrato esperado; a maioria fica RED de propósito (falha de asserção
// clara, não erro de compilação) até o próximo agente implementar a lógica
// de negócio. O teste (c) é proteção de regressão do atalho existente
// (tela de destino) e deve continuar GREEN.

import 'package:app/features/destinations/data/destination_summary_model.dart';
import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/widgets/generate_itinerary_sheet.dart';
import 'package:app/features/maps/data/maps_models.dart';
import 'package:app/features/maps/presentation/providers/maps_provider.dart';
import 'package:app/features/onboarding/presentation/widgets/preference_steps.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

void main() {
  late MockItineraryRepository mockRepo;

  setUpAll(() {
    registerFallbackValue(DateTime(2026, 1, 1));
  });

  setUp(() {
    mockRepo = MockItineraryRepository();
    // Stub genérico que casa com a assinatura de chamada ATUAL do wizard
    // (7 parâmetros — sem tripInterests/tripRhythm, que o wizard ainda não
    // envia). Usado pelos testes que completam o fluxo até "Gerar Roteiro".
    when(
      () => mockRepo.generate(
        destinationId: any(named: 'destinationId'),
        startDate: any(named: 'startDate'),
        endDate: any(named: 'endDate'),
        stayArea: any(named: 'stayArea'),
        tripVibe: any(named: 'tripVibe'),
        companions: any(named: 'companions'),
        tripBudget: any(named: 'tripBudget'),
      ),
    ).thenAnswer(
      (_) async => ItineraryModel(
        id: 'itin-1',
        title: 'Viagem',
        destinationName: 'Paris',
        destinationHeroImageUrl: '',
        startDate: DateTime(2026, 9, 1),
        endDate: DateTime(2026, 9, 4),
        days: const [],
      ),
    );
  });

  DestinationSummaryModel dest(String id, String name) =>
      DestinationSummaryModel(
        id: id,
        slug: name.toLowerCase(),
        name: name,
        country: 'País',
        city: name,
        tagline: 'Tagline de $name',
        heroImageUrl: '',
        tags: const [],
      );

  final mockDestinations = [dest('id-mockado', 'Paris'), dest('id-2', 'Roma')];

  /// Sugestões de bairro (Item 6): por padrão `configured: false` — sem
  /// bairros — para preservar o comportamento de hoje (só o TextField, sem
  /// chips) nos testes que não exercitam essa feature especificamente.
  /// Também é usado pra tocar o backend real de `ApiClient`, que exige
  /// Supabase inicializado (não disponível em teste de widget) — sem esse
  /// override, qualquer navegação até a etapa de Área dispararia uma
  /// chamada real e quebraria o teste.
  List<Override> providerOverrides({
    List<DestinationSummaryModel> destinations = const [],
    NeighborhoodSuggestionsModel neighborhoodSuggestions =
        const NeighborhoodSuggestionsModel(configured: false),
    void Function(String destinationId, String budgetTier)?
        onNeighborhoodSuggestionsRequested,
  }) =>
      [
        itineraryRepositoryProvider.overrideWithValue(mockRepo),
        destinationsProvider.overrideWith((ref) async => destinations),
        destinationSearchProvider.overrideWith(
          (ref, query) async => destinations
              .where((d) => d.name.toLowerCase().contains(query.toLowerCase()))
              .toList(),
        ),
        neighborhoodSuggestionsProvider.overrideWith((ref, params) async {
          onNeighborhoodSuggestionsRequested?.call(params.$1, params.$2);
          return neighborhoodSuggestions;
        }),
      ];

  /// Pump simples (sem GoRouter) — suficiente para testes que não completam
  /// o fluxo até a navegação pós-geração.
  Future<void> pumpSheet(
    WidgetTester tester, {
    String? destinationId,
    String? destinationName,
    List<DestinationSummaryModel> destinations = const [],
    NeighborhoodSuggestionsModel neighborhoodSuggestions =
        const NeighborhoodSuggestionsModel(configured: false),
    void Function(String destinationId, String budgetTier)?
        onNeighborhoodSuggestionsRequested,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: providerOverrides(
          destinations: destinations,
          neighborhoodSuggestions: neighborhoodSuggestions,
          onNeighborhoodSuggestionsRequested:
              onNeighborhoodSuggestionsRequested,
        ),
        child: MaterialApp(
          home: Scaffold(
            body: GenerateItinerarySheet(
              destinationId: destinationId,
              destinationName: destinationName,
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  /// Pump com GoRouter mínimo, abrindo o sheet via showModalBottomSheet
  /// (mesmo padrão real de uso) — necessário para os testes que completam
  /// o fluxo até `_generate()` (que faz `context.push('/roteiro/:id')`
  /// após `Navigator.pop`).
  Future<GoRouter> pumpSheetWithRouter(
    WidgetTester tester, {
    List<DestinationSummaryModel> destinations = const [],
  }) async {
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => Scaffold(
            body: Builder(
              builder: (ctx) => ElevatedButton(
                onPressed: () => showModalBottomSheet(
                  context: ctx,
                  isScrollControlled: true,
                  builder: (_) => const GenerateItinerarySheet(),
                ),
                child: const Text('Abrir wizard'),
              ),
            ),
          ),
        ),
        GoRoute(
          path: '/roteiro/:id',
          builder: (context, state) => Scaffold(
            body: Text('Roteiro ${state.pathParameters['id']}'),
          ),
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: providerOverrides(destinations: destinations),
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Abrir wizard'));
    await tester.pumpAndSettle();

    return router;
  }

  // ── Navegação passo a passo (estrutura ATUAL: 5 etapas fixas) ──────────

  Future<void> confirmDatePicker(WidgetTester tester) async {
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
  }

  Future<void> fillDatesStep(WidgetTester tester) async {
    // Chegada
    await tester.tap(find.text('Selecionar').first);
    await tester.pumpAndSettle();
    await confirmDatePicker(tester);

    // Partida
    await tester.tap(find.text('Selecionar').first);
    await tester.pumpAndSettle();
    await confirmDatePicker(tester);
  }

  Future<void> tapContinuar(WidgetTester tester) async {
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
  }

  Future<void> tapGerarRoteiro(WidgetTester tester) async {
    await tester.tap(find.text('Gerar Roteiro'));
    await tester.pumpAndSettle();
  }

  /// Percorre Interesses -> Ritmo -> chega em "Com quem vai?", selecionando
  /// (opcionalmente) os rótulos indicados em cada etapa. Assume que a etapa
  /// atual já é Interesses (ou seja, o "Vibe -> Interesses" já foi feito por
  /// quem chama). Ambas as etapas permitem avançar sem selecionar nada.
  Future<void> advanceInterestsAndRhythm(
    WidgetTester tester, {
    Set<String> interestLabels = const {},
    String? rhythmLabel,
  }) async {
    for (final label in interestLabels) {
      final finder = find.text(label);
      await tester.ensureVisible(finder);
      await tester.pumpAndSettle();
      await tester.tap(finder);
      await tester.pumpAndSettle();
    }
    await tapContinuar(tester); // Interesses -> Ritmo

    if (rhythmLabel != null) {
      final finder = find.text(rhythmLabel);
      await tester.ensureVisible(finder);
      await tester.pumpAndSettle();
      await tester.tap(finder);
      await tester.pumpAndSettle();
    }
    await tapContinuar(tester); // Ritmo -> Com quem
  }

  /// Percorre as etapas do wizard (Datas -> Budget -> Área -> Vibe ->
  /// Interesses -> Ritmo -> Com quem) selecionando uma opção em cada uma
  /// (Interesses/Ritmo só se [interestLabels]/[rhythmLabel] forem
  /// passados — ambas são opcionais no fluxo real), parando em "Gerar
  /// Roteiro" sem tocá-lo. [vibeLabel] permite escolher a vibe (por padrão
  /// uma não-romântica, para não disparar a regra do "Sozinho").
  Future<void> completeWizardUntilGenerate(
    WidgetTester tester, {
    String vibeLabel = 'Aventura',
    Set<String> interestLabels = const {},
    String? rhythmLabel,
    String companionLabel = 'Casal',
  }) async {
    await fillDatesStep(tester);
    await tapContinuar(tester); // Datas -> Budget

    await tester.tap(find.text('Moderado'));
    await tester.pumpAndSettle();
    await tapContinuar(tester); // Budget -> Área

    await tapContinuar(tester); // Área (opcional) -> Vibe

    await tester.tap(find.text(vibeLabel));
    await tester.pumpAndSettle();
    await tapContinuar(tester); // Vibe -> Interesses

    await advanceInterestsAndRhythm(
      tester,
      interestLabels: interestLabels,
      rhythmLabel: rhythmLabel,
    ); // Interesses -> Ritmo -> Com quem

    await tester.tap(find.text(companionLabel));
    await tester.pumpAndSettle();
  }

  // ── (a) Ordem e contagem de steps — modo sem destino ───────────────────

  testWidgets(
    'modo sem destino: primeira etapa deve pedir a cidade, listando os '
    'destinos mockados de destinationsProvider (RED — hoje abre direto em '
    '"Quando vai viajar?")',
    (tester) async {
      await pumpSheet(tester, destinations: mockDestinations);

      // Comportamento desejado: campo de busca de cidade + destinos
      // mockados visíveis já na primeira etapa.
      expect(find.byType(TextField), findsOneWidget);
      expect(find.text('Paris'), findsOneWidget);
      expect(find.text('Roma'), findsOneWidget);
      // E a etapa de datas (hoje mostrada primeiro) não deveria aparecer
      // ainda.
      expect(find.text('Quando vai viajar?'), findsNothing);
    },
  );

  // ── (b) Selecionar cidade avança e é usado no generate() ───────────────

  testWidgets(
    'modo sem destino: selecionar uma cidade mockada e completar o wizard '
    'deve enviar o destinationId escolhido para generate() (RED — não há '
    'etapa de cidade, destinationId nunca é definido)',
    (tester) async {
      final router = await pumpSheetWithRouter(
        tester,
        destinations: mockDestinations,
      );

      // Tentativa de selecionar a cidade mockada "Paris" (etapa ainda não
      // existe hoje — se não for encontrada, seguimos o restante do
      // wizard mesmo assim, para exercitar o generate() real).
      final parisFinder = find.text('Paris');
      if (parisFinder.evaluate().isNotEmpty) {
        await tester.tap(parisFinder.first);
        await tester.pumpAndSettle();
      }

      await completeWizardUntilGenerate(tester);
      await tapGerarRoteiro(tester);

      verify(
        () => mockRepo.generate(
          destinationId: 'id-mockado',
          startDate: any(named: 'startDate'),
          endDate: any(named: 'endDate'),
          stayArea: any(named: 'stayArea'),
          tripVibe: any(named: 'tripVibe'),
          companions: any(named: 'companions'),
          tripBudget: any(named: 'tripBudget'),
        ),
      ).called(1);

      // Referência ao router só para evitar warning de variável não usada
      // em caso de o teste falhar antes da navegação.
      expect(router, isNotNull);
    },
  );

  // ── (c) Modo com destino pré-definido — proteção de regressão (GREEN) ──

  testWidgets(
    'modo com destino pré-definido (atalho da tela de destino): primeira '
    'etapa já é "Quando vai viajar?", sem campo de busca de cidade '
    '(comportamento atual preservado)',
    (tester) async {
      await pumpSheet(
        tester,
        destinationId: 'x',
        destinationName: 'Paris',
      );

      expect(find.text('Quando vai viajar?'), findsOneWidget);
      expect(find.byType(TextField), findsNothing);
    },
  );

  // ── (d) Etapa de Interesses ─────────────────────────────────────────────

  testWidgets(
    'etapa de Interesses (InterestsStep) deve existir logo após a etapa de '
    'Vibe (RED — etapa ainda não existe, hoje vai direto para "Com quem '
    'vai?")',
    (tester) async {
      await pumpSheet(tester, destinationId: 'x', destinationName: 'Paris');

      await fillDatesStep(tester);
      await tapContinuar(tester); // Datas -> Budget

      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Budget -> Área

      await tapContinuar(tester); // Área -> Vibe

      await tester.tap(find.text('Aventura'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Vibe -> (desejado: Interesses)

      expect(find.byType(InterestsStep), findsOneWidget);
    },
  );

  // ── (e) Etapa de Ritmo ───────────────────────────────────────────────────

  testWidgets(
    'etapa de Ritmo (RhythmStep) deve existir logo após a etapa de '
    'Interesses (RED — etapa ainda não existe)',
    (tester) async {
      await pumpSheet(tester, destinationId: 'x', destinationName: 'Paris');

      await fillDatesStep(tester);
      await tapContinuar(tester); // Datas -> Budget

      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Budget -> Área

      await tapContinuar(tester); // Área -> Vibe

      await tester.tap(find.text('Aventura'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Vibe -> (desejado: Interesses)

      // Hoje isso cai direto em "Com quem vai?" — precisa selecionar uma
      // companhia para poder continuar e tentar alcançar a etapa seguinte.
      final companheiroFinder = find.text('Casal');
      if (companheiroFinder.evaluate().isNotEmpty) {
        await tester.tap(companheiroFinder);
        await tester.pumpAndSettle();
      }
      await tapContinuar(tester); // -> (desejado: Ritmo)

      expect(find.byType(RhythmStep), findsOneWidget);
    },
  );

  // ── (f) Interesses e ritmo chegam no generate() ────────────────────────

  testWidgets(
    'interesses selecionados e ritmo escolhido devem ser enviados para '
    'generate() como tripInterests/tripRhythm (RED — wizard ainda não '
    'coleta nem envia esses campos)',
    (tester) async {
      await pumpSheetWithRouter(tester, destinations: mockDestinations);

      // Modo sem destino pré-definido: precisa selecionar a cidade antes
      // de prosseguir.
      final parisFinder = find.text('Paris');
      if (parisFinder.evaluate().isNotEmpty) {
        await tester.tap(parisFinder.first);
        await tester.pumpAndSettle();
      }

      // Stub específico para a chamada com tripInterests/tripRhythm
      // preenchidos — a implementação só inclui esses named args quando
      // não-nulos/não-vazios, então essa invocação tem um "shape" diferente
      // do stub genérico registrado em setUp() (que não os menciona).
      when(
        () => mockRepo.generate(
          destinationId: any(named: 'destinationId'),
          startDate: any(named: 'startDate'),
          endDate: any(named: 'endDate'),
          stayArea: any(named: 'stayArea'),
          tripVibe: any(named: 'tripVibe'),
          companions: any(named: 'companions'),
          tripBudget: any(named: 'tripBudget'),
          tripInterests: any(named: 'tripInterests'),
          tripRhythm: any(named: 'tripRhythm'),
        ),
      ).thenAnswer(
        (_) async => ItineraryModel(
          id: 'itin-1',
          title: 'Viagem',
          destinationName: 'Paris',
          destinationHeroImageUrl: '',
          startDate: DateTime(2026, 9, 1),
          endDate: DateTime(2026, 9, 4),
          days: const [],
        ),
      );

      await completeWizardUntilGenerate(
        tester,
        interestLabels: {'Cultura', 'Gastronomia'},
        rhythmLabel: 'Intenso',
      );
      await tapGerarRoteiro(tester);

      verify(
        () => mockRepo.generate(
          destinationId: any(named: 'destinationId'),
          startDate: any(named: 'startDate'),
          endDate: any(named: 'endDate'),
          stayArea: any(named: 'stayArea'),
          tripVibe: any(named: 'tripVibe'),
          companions: any(named: 'companions'),
          tripBudget: any(named: 'tripBudget'),
          tripInterests: ['cultura', 'gastronomia'],
          tripRhythm: 'INTENSE',
        ),
      ).called(1);
    },
  );

  // ── (g) "Sozinho" some quando a vibe é Lua de mel ──────────────────────

  testWidgets(
    '"Sozinho" não deve aparecer na etapa "Com quem vai?" quando a vibe '
    'selecionada é "Lua de mel" (RED — hoje a lista de opções não muda '
    'conforme a vibe)',
    (tester) async {
      await pumpSheet(tester, destinationId: 'x', destinationName: 'Paris');

      await fillDatesStep(tester);
      await tapContinuar(tester); // Datas -> Budget

      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Budget -> Área

      await tapContinuar(tester); // Área -> Vibe

      await tester.tap(find.text('Lua de mel'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Vibe -> Interesses

      await advanceInterestsAndRhythm(tester); // Interesses -> Ritmo -> Com quem

      expect(find.text('Sozinho'), findsNothing);
      expect(find.text('Casal'), findsOneWidget);
      expect(find.text('Família'), findsOneWidget);
      expect(find.text('Grupo'), findsOneWidget);
    },
  );

  // ── (h) Reset de companhia ao mudar para Lua de mel ────────────────────

  testWidgets(
    'se "Sozinho" já estava selecionado e o usuário volta e muda a vibe '
    'para "Lua de mel", a seleção de companhia deve ser desfeita (RED — '
    'hoje a seleção anterior permanece e "Sozinho" continua na lista)',
    (tester) async {
      await pumpSheet(tester, destinationId: 'x', destinationName: 'Paris');

      await fillDatesStep(tester);
      await tapContinuar(tester); // Datas -> Budget

      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Budget -> Área

      await tapContinuar(tester); // Área -> Vibe

      await tester.tap(find.text('Aventura'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Vibe -> Interesses

      await advanceInterestsAndRhythm(tester); // Interesses -> Ritmo -> Com quem

      await tester.tap(find.text('Sozinho'));
      await tester.pumpAndSettle();

      // Volta até a etapa de Vibe (Com quem -> Ritmo -> Interesses -> Vibe).
      for (var i = 0; i < 3; i++) {
        await tester.tap(find.byIcon(Icons.arrow_back_rounded));
        await tester.pumpAndSettle();
      }

      await tester.tap(find.text('Lua de mel'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Vibe -> Interesses (de novo)
      await advanceInterestsAndRhythm(tester); // -> Ritmo -> Com quem (de novo)

      expect(find.text('Sozinho'), findsNothing);

      // "Com quem vai?" agora é a última etapa do wizard (Budget passou a
      // vir antes de Área) — o botão de avançar mostra "Gerar Roteiro" em
      // vez de "Continuar" nesse ponto.
      final gerarRoteiroButton =
          tester.widget<ElevatedButton>(find.widgetWithText(
        ElevatedButton,
        'Gerar Roteiro',
      ));
      expect(gerarRoteiroButton.onPressed, isNull);
    },
  );

  // ── (k) Rótulo "Clima da viagem" ────────────────────────────────────────

  testWidgets(
    'título da etapa de vibe deve ser "Qual é o clima da viagem?" (RED — '
    'hoje é "Qual é a vibe da viagem?")',
    (tester) async {
      await pumpSheet(tester, destinationId: 'x', destinationName: 'Paris');

      await fillDatesStep(tester);
      await tapContinuar(tester); // Datas -> Budget

      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Budget -> Área

      await tapContinuar(tester); // Área -> Vibe

      expect(find.text('Qual é o clima da viagem?'), findsOneWidget);
    },
  );

  // ── (l) Nova vibe: Silêncio & Contemplação ──────────────────────────────

  testWidgets(
    'etapa de Vibe mostra a opção "Silêncio & Contemplação"',
    (tester) async {
      await pumpSheet(tester, destinationId: 'x', destinationName: 'Paris');

      await fillDatesStep(tester);
      await tapContinuar(tester); // Datas -> Budget

      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester); // Budget -> Área

      await tapContinuar(tester); // Área -> Vibe

      expect(find.text('Silêncio & Contemplação'), findsOneWidget);
    },
  );

  testWidgets(
    'selecionar "Silêncio & Contemplação" e completar o wizard envia '
    'tripVibe: SILENCE para generate()',
    (tester) async {
      await pumpSheetWithRouter(tester, destinations: mockDestinations);

      final parisFinder = find.text('Paris');
      if (parisFinder.evaluate().isNotEmpty) {
        await tester.tap(parisFinder.first);
        await tester.pumpAndSettle();
      }

      await completeWizardUntilGenerate(
        tester,
        vibeLabel: 'Silêncio & Contemplação',
      );
      await tapGerarRoteiro(tester);

      verify(
        () => mockRepo.generate(
          destinationId: any(named: 'destinationId'),
          startDate: any(named: 'startDate'),
          endDate: any(named: 'endDate'),
          stayArea: any(named: 'stayArea'),
          tripVibe: 'SILENCE',
          companions: any(named: 'companions'),
          tripBudget: any(named: 'tripBudget'),
        ),
      ).called(1);
    },
  );

  // ── (m) Item 6 — sugestão de bairro por budget na etapa de Área ────────
  // Cobre: GET /api/v1/maps/neighborhood-suggestions via
  // neighborhoodSuggestionsProvider, exposto como chips na etapa de Área.

  /// Avança do início (modo com destino pré-definido) até a etapa de Área,
  /// selecionando [budgetLabel] na etapa de Budget (agora logo após Datas).
  Future<void> advanceToAreaStep(
    WidgetTester tester, {
    String budgetLabel = 'Moderado',
  }) async {
    await fillDatesStep(tester);
    await tapContinuar(tester); // Datas -> Budget

    await tester.tap(find.text(budgetLabel));
    await tester.pumpAndSettle();
    await tapContinuar(tester); // Budget -> Área
  }

  testWidgets(
    'etapa de Área mostra chips de sugestão quando '
    'neighborhoodSuggestionsProvider retorna configured=true com bairros',
    (tester) async {
      await pumpSheet(
        tester,
        destinationId: 'dest-1',
        destinationName: 'Paris',
        neighborhoodSuggestions: const NeighborhoodSuggestionsModel(
          configured: true,
          neighborhoods: ['Le Marais', 'Montmartre'],
        ),
      );

      await advanceToAreaStep(tester);

      expect(find.text('Sugestões:'), findsOneWidget);
      expect(find.text('Le Marais'), findsOneWidget);
      expect(find.text('Montmartre'), findsOneWidget);
    },
  );

  testWidgets(
    'tocar num chip de sugestão preenche o campo de texto com o nome do bairro',
    (tester) async {
      await pumpSheet(
        tester,
        destinationId: 'dest-1',
        destinationName: 'Paris',
        neighborhoodSuggestions: const NeighborhoodSuggestionsModel(
          configured: true,
          neighborhoods: ['Le Marais', 'Montmartre'],
        ),
      );

      await advanceToAreaStep(tester);

      await tester.tap(find.text('Le Marais'));
      await tester.pumpAndSettle();

      final textField = tester.widget<TextField>(find.byType(TextField));
      expect(textField.controller?.text, 'Le Marais');
    },
  );

  testWidgets(
    'etapa de Área não mostra chips quando configured=false (sem quebrar a '
    'tela) — só o TextField de sempre',
    (tester) async {
      await pumpSheet(
        tester,
        destinationId: 'dest-1',
        destinationName: 'Paris',
        neighborhoodSuggestions:
            const NeighborhoodSuggestionsModel(configured: false),
      );

      await advanceToAreaStep(tester);

      expect(find.text('Sugestões:'), findsNothing);
      expect(find.byType(TextField), findsOneWidget);
    },
  );

  testWidgets(
    'etapa de Área não mostra chips quando configured=true mas a lista de '
    'bairros vem vazia',
    (tester) async {
      await pumpSheet(
        tester,
        destinationId: 'dest-1',
        destinationName: 'Paris',
        neighborhoodSuggestions: const NeighborhoodSuggestionsModel(
          configured: true,
          neighborhoods: [],
        ),
      );

      await advanceToAreaStep(tester);

      expect(find.text('Sugestões:'), findsNothing);
      expect(find.byType(TextField), findsOneWidget);
    },
  );

  testWidgets(
    'a etapa de Budget aparece antes da etapa de Área na nova ordem do '
    'wizard (city -> dates -> budget -> area)',
    (tester) async {
      await pumpSheet(tester, destinations: mockDestinations);

      // Cidade
      await tester.tap(find.text('Paris'));
      await tester.pumpAndSettle();

      // Datas -> Budget
      await fillDatesStep(tester);
      await tapContinuar(tester);

      expect(find.text('Qual o budget desta viagem?'), findsOneWidget);
      expect(find.text('Onde vai se hospedar?'), findsNothing);

      // Budget -> Área
      await tester.tap(find.text('Moderado'));
      await tester.pumpAndSettle();
      await tapContinuar(tester);

      expect(find.text('Onde vai se hospedar?'), findsOneWidget);
    },
  );

  testWidgets(
    'ao chegar na etapa de Área, neighborhoodSuggestionsProvider é '
    'observado com a chave (destinationId, budgetTier) refletindo o '
    'budget selecionado na etapa anterior',
    (tester) async {
      String? capturedDestinationId;
      String? capturedBudgetTier;

      await pumpSheet(
        tester,
        destinationId: 'dest-xyz',
        destinationName: 'Paris',
        onNeighborhoodSuggestionsRequested: (destinationId, budgetTier) {
          capturedDestinationId = destinationId;
          capturedBudgetTier = budgetTier;
        },
      );

      await advanceToAreaStep(tester, budgetLabel: 'Luxo');

      expect(capturedDestinationId, 'dest-xyz');
      expect(capturedBudgetTier, 'LUXURY');
    },
  );
}
