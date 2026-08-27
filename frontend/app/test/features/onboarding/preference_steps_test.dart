// Testes simples para os novos interesses "Vinhos" e "Neve" adicionados a
// preference_steps.dart.

import 'package:app/features/onboarding/presentation/widgets/preference_steps.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('interestOptions — novos interesses Vinhos e Neve', () {
    test('interestOptions contém "vinhos" e "neve"', () {
      expect(interestOptions, contains('vinhos'));
      expect(interestOptions, contains('neve'));
    });

    test('interestLabels mapeia os rótulos esperados', () {
      expect(interestLabels['vinhos'], 'Vinhos');
      expect(interestLabels['neve'], 'Neve');
    });
  });

  testWidgets(
    'InterestsStep renderiza os rótulos "Vinhos" e "Neve"',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InterestsStep(
              selected: const {},
              onToggle: (_) {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Vinhos'), findsOneWidget);
      expect(find.text('Neve'), findsOneWidget);
    },
  );
}
