// Testes da tela "Planos" (features/billing/) — os 3 fluxos de pagamento
// (cartão-assinatura, Pix-premium-mensal, Pix-PDF-avulso), ver
// luvoy-pendencias.md. Mocka UrlLauncherPlatform.instance no mesmo padrão
// já estabelecido em maps_test.dart (OpenInMapsButton).

import 'package:app/features/billing/data/billing_repository.dart';
import 'package:app/features/billing/presentation/screens/premium_screen.dart';
import 'package:app/features/profile/data/user_model.dart';
import 'package:app/features/profile/presentation/providers/user_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:url_launcher_platform_interface/url_launcher_platform_interface.dart';

class MockBillingRepository extends Mock implements BillingRepository {}

class MockUrlLauncherPlatform extends Mock
    with MockPlatformInterfaceMixin
    implements UrlLauncherPlatform {}

void main() {
  late MockBillingRepository mockRepo;
  late MockUrlLauncherPlatform mockUrlLauncher;

  setUpAll(() {
    registerFallbackValue(const LaunchOptions());
  });

  setUp(() {
    mockRepo = MockBillingRepository();
    mockUrlLauncher = MockUrlLauncherPlatform();
    UrlLauncherPlatform.instance = mockUrlLauncher;
  });

  // A tela é uma ListView com banner + até 3 cards + botão — no tamanho
  // padrão de teste, o card de PDF (o último) fica fora do viewport/cache
  // extent e nem chega a ser montado (mesmo gotcha de home_screen_test.dart).
  void useTallSurface(WidgetTester tester) {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
  }

  UserModel freeUser() => const UserModel(
    id: 'u1',
    email: 'viajante@example.com',
    onboardingCompleted: true,
    isPremium: false,
  );

  UserModel premiumUser(DateTime expiresAt) => UserModel(
    id: 'u1',
    email: 'viajante@example.com',
    onboardingCompleted: true,
    isPremium: true,
    premiumExpiresAt: expiresAt,
  );

  Widget buildScreen(UserModel? user) {
    return ProviderScope(
      overrides: [
        userProvider.overrideWith((ref) async => user),
        billingRepositoryProvider.overrideWithValue(mockRepo),
      ],
      child: const MaterialApp(home: PremiumScreen()),
    );
  }

  group('usuário free', () {
    testWidgets(
      'mostra as 2 opções de premium (cartão e Pix) + a opção de PDF avulso, '
      'e o banner de status mostra "Plano gratuito"',
      (tester) async {
        useTallSurface(tester);
        await tester.pumpWidget(buildScreen(freeUser()));
        await tester.pumpAndSettle();

        expect(find.text('Plano gratuito'), findsOneWidget);
        expect(find.text('Assinar com cartão'), findsOneWidget);
        expect(find.text('Pagar com Pix'), findsOneWidget);
        expect(find.text('Comprar via Pix'), findsOneWidget);
      },
    );

    testWidgets(
      'tocar em "Assinar com cartão" cria o checkout e abre a URL retornada',
      (tester) async {
        useTallSurface(tester);
        when(
          () => mockRepo.createSubscriptionCheckout(
            successUrl: any(named: 'successUrl'),
            cancelUrl: any(named: 'cancelUrl'),
          ),
        ).thenAnswer((_) async => 'https://checkout.stripe.com/pay/cs_card');
        when(
          () => mockUrlLauncher.launchUrl(any(), any()),
        ).thenAnswer((_) async => true);

        await tester.pumpWidget(buildScreen(freeUser()));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Assinar com cartão'));
        await tester.pumpAndSettle();

        final captured = verify(
          () => mockUrlLauncher.launchUrl(captureAny(), captureAny()),
        ).captured;
        expect(captured[0], 'https://checkout.stripe.com/pay/cs_card');
      },
    );

    testWidgets(
      'tocar em "Pagar com Pix" chama o checkout de premium via Pix (não o de cartão)',
      (tester) async {
        useTallSurface(tester);
        when(
          () => mockRepo.createPixPremiumCheckout(
            successUrl: any(named: 'successUrl'),
            cancelUrl: any(named: 'cancelUrl'),
          ),
        ).thenAnswer((_) async => 'https://checkout.stripe.com/pay/cs_pix');
        when(
          () => mockUrlLauncher.launchUrl(any(), any()),
        ).thenAnswer((_) async => true);

        await tester.pumpWidget(buildScreen(freeUser()));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Pagar com Pix'));
        await tester.pumpAndSettle();

        verify(
          () => mockRepo.createPixPremiumCheckout(
            successUrl: any(named: 'successUrl'),
            cancelUrl: any(named: 'cancelUrl'),
          ),
        ).called(1);
        verifyNever(
          () => mockRepo.createSubscriptionCheckout(
            successUrl: any(named: 'successUrl'),
            cancelUrl: any(named: 'cancelUrl'),
          ),
        );
      },
    );

    testWidgets(
      'quando o checkout falha (exceção do repositório), mostra mensagem de '
      'erro em vez de quebrar a tela',
      (tester) async {
        useTallSurface(tester);
        when(
          () => mockRepo.createPixPdfExportCheckout(
            successUrl: any(named: 'successUrl'),
            cancelUrl: any(named: 'cancelUrl'),
          ),
        ).thenThrow(Exception('falha de rede'));

        await tester.pumpWidget(buildScreen(freeUser()));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Comprar via Pix'));
        await tester.pumpAndSettle();

        expect(find.byType(SnackBar), findsOneWidget);
        expect(
          find.text('Não foi possível iniciar o pagamento. Tente novamente.'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'quando launchUrl retorna false, mostra mensagem de que não foi '
      'possível abrir a página de pagamento',
      (tester) async {
        useTallSurface(tester);
        when(
          () => mockRepo.createPixPdfExportCheckout(
            successUrl: any(named: 'successUrl'),
            cancelUrl: any(named: 'cancelUrl'),
          ),
        ).thenAnswer((_) async => 'https://checkout.stripe.com/pay/cs_pdf');
        when(
          () => mockUrlLauncher.launchUrl(any(), any()),
        ).thenAnswer((_) async => false);

        await tester.pumpWidget(buildScreen(freeUser()));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Comprar via Pix'));
        await tester.pumpAndSettle();

        expect(
          find.text('Não foi possível abrir a página de pagamento.'),
          findsOneWidget,
        );
      },
    );
  });

  group('usuário premium', () {
    testWidgets(
      'esconde as 2 opções de assinatura e mostra a validade no banner — '
      'só a opção de PDF avulso continua disponível pra todo mundo',
      (tester) async {
        useTallSurface(tester);
        final expiresAt = DateTime(2026, 10, 14);
        await tester.pumpWidget(buildScreen(premiumUser(expiresAt)));
        await tester.pumpAndSettle();

        expect(find.text('Você é Premium'), findsOneWidget);
        expect(find.text('Válido até 14/10/2026'), findsOneWidget);
        expect(find.text('Assinar com cartão'), findsNothing);
        expect(find.text('Pagar com Pix'), findsNothing);
        expect(find.text('Comprar via Pix'), findsOneWidget);
      },
    );
  });
}
