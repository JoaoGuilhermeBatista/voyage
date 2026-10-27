import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _prefsKey = 'theme_mode';

/// Persiste a escolha de tema (claro/escuro/sistema) localmente — `build()`
/// devolve `ThemeMode.system` de cara (sem travar o primeiro frame) e troca
/// pro valor salvo assim que o SharedPreferences responder.
class ThemeModeNotifier extends Notifier<ThemeMode> {
  @override
  ThemeMode build() {
    _loadPersisted();
    return ThemeMode.system;
  }

  Future<void> _loadPersisted() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final saved = prefs.getString(_prefsKey);
      if (saved == null) return;
      state = ThemeMode.values.firstWhere(
        (m) => m.name == saved,
        orElse: () => ThemeMode.system,
      );
    } catch (_) {
      // Sem storage disponível (ex.: widget tests sem mock de plugin) —
      // mantém ThemeMode.system, nunca derruba a tela por causa disso.
    }
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    state = mode;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_prefsKey, mode.name);
    } catch (_) {
      // Mesmo raciocínio de _loadPersisted — a troca de tema na sessão
      // atual já aconteceu (state = mode acima); só a persistência falhou.
    }
  }
}

final themeModeProvider = NotifierProvider<ThemeModeNotifier, ThemeMode>(
  ThemeModeNotifier.new,
);
