// Reproduz o problema reportado de "opacidade baixa demais" / contraste
// lavado nos ícones INATIVOS da bottom nav bar (Explorar, Para Você,
// Roteiros, Favoritos, Perfil).
//
// Hoje, `_NavItem.build()` em scaffold_with_nav_bar.dart usa:
//   final color = isActive ? AppTheme.goldenOrange : const Color(0xFFB8B3A9);
// — um bege lavado fixo para TODAS as 5 abas quando inativas.
//
// Contrato assumido (RED por design — a implementar por um próximo agente):
// a cor inativa deve passar a ser `AppTheme.softBlack.withValues(alpha: 0.45)`
// (Flutter 3.44 / Dart 3.12 neste projeto — `Color.withValues` já disponível,
// então não usamos a API depreciada `.withOpacity`). A cor ativa
// (AppTheme.goldenOrange) não muda.
//
// Este teste monta uma árvore mínima com StatefulShellRoute.indexedStack
// (mesmo padrão de app_router.dart: 5 branches, uma por aba real —
// /home, /para-voce, /roteiros, /favoritos, /perfil) usando telas
// placeholder triviais (não as telas reais, que dependem de providers/
// Supabase — mesma limitação documentada em about_dialog_test.dart) —
// e verifica, para cada uma das 5 abas como aba "ativa" corrente, a cor
// de todos os 5 ícones da nav bar.

import 'package:app/core/themes/app_theme.dart';
import 'package:app/shared/widgets/scaffold_with_nav_bar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

// Calculada em runtime (não hardcoded como literal hex) para não arriscar
// erro de arredondamento na conversão double->ARGB32 feita por withValues.
final Color _expectedInactiveColor = AppTheme.softBlack.withValues(alpha: 0.45);

class _NavTab {
  final String path;
  final IconData icon;
  final IconData activeIcon;
  final String label;
  const _NavTab(this.path, this.icon, this.activeIcon, this.label);
}

const _tabs = [
  _NavTab('/home', Icons.explore_outlined, Icons.explore, 'Explorar'),
  _NavTab('/para-voce', Icons.auto_awesome_outlined, Icons.auto_awesome, 'Para você'),
  _NavTab('/roteiros', Icons.map_outlined, Icons.map, 'Roteiros'),
  _NavTab('/favoritos', Icons.favorite_border, Icons.favorite, 'Favoritos'),
  _NavTab('/perfil', Icons.person_outline, Icons.person, 'Perfil'),
];

Widget _placeholderScreen(String label) => Scaffold(body: Center(child: Text(label)));

Widget _buildApp({required String initialLocation}) {
  final router = GoRouter(
    initialLocation: initialLocation,
    routes: [
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) =>
            ScaffoldWithNavBar(navigationShell: navigationShell),
        branches: _tabs
            .map(
              (tab) => StatefulShellBranch(
                routes: [
                  GoRoute(
                    path: tab.path,
                    builder: (context, state) => _placeholderScreen(tab.label),
                  ),
                ],
              ),
            )
            .toList(),
      ),
    ],
  );

  return MaterialApp.router(routerConfig: router);
}

void main() {
  for (var activeIndex = 0; activeIndex < _tabs.length; activeIndex++) {
    final activeTab = _tabs[activeIndex];

    testWidgets(
      'com "${activeTab.label}" ativa, ícone ativo fica goldenOrange e os outros 4 ficam softBlack a 45% de alpha',
      (tester) async {
        await tester.pumpWidget(_buildApp(initialLocation: activeTab.path));
        await tester.pumpAndSettle();

        for (var i = 0; i < _tabs.length; i++) {
          final tab = _tabs[i];
          final isActive = i == activeIndex;
          final iconData = isActive ? tab.activeIcon : tab.icon;

          final iconFinder = find.byIcon(iconData);
          expect(
            iconFinder,
            findsOneWidget,
            reason: 'Ícone ${isActive ? "ativo" : "inativo"} da aba "${tab.label}" não encontrado',
          );

          final iconWidget = tester.widget<Icon>(iconFinder);

          if (isActive) {
            expect(
              iconWidget.color,
              AppTheme.goldenOrange,
              reason: 'Aba ativa "${tab.label}" deve manter a cor AppTheme.goldenOrange',
            );
          } else {
            expect(
              iconWidget.color,
              _expectedInactiveColor,
              reason:
                  'Aba inativa "${tab.label}" deve usar AppTheme.softBlack.withValues(alpha: 0.45), '
                  'não o bege lavado fixo Color(0xFFB8B3A9)',
            );
            expect(
              iconWidget.color,
              isNot(const Color(0xFFB8B3A9)),
              reason: 'A cor inativa não deve mais ser o bege lavado fixo Color(0xFFB8B3A9)',
            );
          }
        }
      },
    );
  }
}
