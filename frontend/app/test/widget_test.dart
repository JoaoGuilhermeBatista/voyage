// Smoke test for the LUVOY app.
//
// The real root widget (LuvoyApp) uses GoRouter with a redirect that reads
// Supabase.instance.client.auth.currentSession, which requires
// Supabase.initialize() to have run first (platform channels aren't
// available in the widget-test environment). Instead of standing up the
// full app + Supabase, this test verifies the screen an unauthenticated
// user actually lands on — LoginScreen — renders correctly inside a
// minimal MaterialApp + ProviderScope, with no network calls triggered
// during build.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:app/features/auth/presentation/screens/login_screen.dart';

void main() {
  testWidgets('LoginScreen renders email/password form for a logged-out user',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(
          home: LoginScreen(),
        ),
      ),
    );
    await tester.pump();

    // Header and tabs.
    expect(find.text('LUVOY'), findsOneWidget);
    expect(find.text('Entrar'), findsWidgets);
    expect(find.text('Criar conta'), findsOneWidget);

    // Login form fields.
    expect(find.byType(TextField), findsNWidgets(2));
    expect(find.text('Email'), findsOneWidget);
    expect(find.text('Senha'), findsOneWidget);
  });
}
