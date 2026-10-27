import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Paleta e tipografia da marca LUVOY — portada do spec oficial de design
/// (`docs/frontend` / Lovable "Lux Travel Journal", agosto de 2026): tokens
/// oklch convertidos para hex, tema claro geral + tokens "Noir" reservados
/// a blocos escuros (`ink`). Reskin de 2026-08-31, revisado no mesmo dia
/// contra o spec — backup do tema pré-reskin em memória
/// (`luvoy-flutter-reskin-backup`), commit de referência `b6880b4`.
///
/// Regra rígida do spec: os tokens Noir (`ink`/`navy`/`ivory`/`goldNoir`/
/// `goldSoftNoir`) só devem aparecer DENTRO de blocos com fundo `ink` — o
/// resto do app usa sempre o tema claro. Note que `gold`/`goldSoft` no tema
/// claro são ALIASES do azul forte (`primary`) e do pêssego (`accent`) —
/// não são dourados de verdade; dourado de verdade só existe em `goldNoir`.
class AppTheme {
  // ── Tema claro (tokens oficiais) ────────────────────────────────────────
  static const Color background = Color(0xFFF8F5EE);
  static const Color foreground = Color(0xFF1A1512);
  static const Color card = Color(0xFFFBF9F5);
  static const Color popover = Color(0xFFFEFDFB);
  static const Color primary = Color(0xFF005093);
  static const Color primaryForeground = Color(0xFFFDFCF8);
  static const Color secondary = Color(0xFFEEE9E1);
  static const Color secondaryForeground = Color(0xFF322D29);
  static const Color muted = Color(0xFFF0ECE5);
  static const Color mutedForeground = Color(0xFF6F6860);
  static const Color accent = Color(0xFFF6CFB0);
  static const Color accentForeground = Color(0xFF25211D);
  static const Color destructive = Color(0xFFC53732);
  static const Color border = Color(0xFFE2DDD5);
  static const Color ring = Color(0xFF4896D3);
  static const Color coral = Color(0xFFF2B58C);
  static const Color coralSoft = Color(0xFFFDE2CE);
  static const Color solara = Color(0xFFFEB261);
  static const Color naval = Color(0xFF183966);
  static const Color overlay = Color(0xFF0C0806);

  /// Alias do azul forte — ponto ativo da nav, eyebrows claros, badges.
  /// NÃO é um dourado de verdade (ver docstring da classe).
  static const Color gold = primary;
  static const Color goldSoft = accent;

  // ── Tokens "Noir" — só dentro de blocos com fundo `ink` ─────────────────
  static const Color ink = Color(0xFF183966);
  static const Color navy = Color(0xFF192A3C);
  static const Color ivory = Color(0xFFFCF9F0);
  static const Color goldNoir = Color(0xFFDBB06B);
  static const Color goldSoftNoir = Color(0xFFF0DCB1);

  /// Fundo do tema escuro — preto-azulado (não neutro) de propósito: é a
  /// mesma cor mais escura já usada nos degradês placeholder da Home
  /// (`_placeholderGradients` em home_screen.dart), pra o fundo da tela
  /// "conversar" com o hero "destino da semana" em vez de destoar dele.
  static const Color darkBase = Color(0xFF0E1217);

  /// Fundo do indicador de status premium (perfil e tela de planos) — texto
  /// em cima dele usa `foreground` (escuro), não `ivory`: claro demais pra
  /// ler sobre esse azul.
  static const Color luxy = Color(0xFF73AEE6);

  // ── Aliases legados ──────────────────────────────────────────────────────
  // Mantidos com os MESMOS NOMES de antes do reskin — usados em ~280 pontos
  // pelo app inteiro. Trocar só o VALOR (pra paleta oficial) em vez de
  // renomear faz toda tela fora do escopo deste reskin herdar os tokens
  // novos automaticamente, sem precisar tocar em cada arquivo. Telas novas
  // devem preferir os nomes oficiais acima em vez destes aliases.
  static const Color primaryBlue = primary;
  static const Color goldenOrange = gold;
  static const Color softWhite = card;
  static const Color softBlack = foreground;
  static const Color shimmerBase = muted;
  static const Color shimmerHigh = background;

  /// Label "eyebrow": uppercase, espaçado (0.28em), usado acima de títulos
  /// serifados em todo o design (ex.: "Destino da semana", "Curadoria").
  /// Cor default é `gold` (= azul, tema claro) — passar `AppTheme.goldNoir`
  /// explicitamente quando o eyebrow estiver dentro de um bloco Noir.
  /// Chamador é responsável por aplicar `.toUpperCase()`.
  static TextStyle eyebrow({Color color = gold, double? letterSpacing}) =>
      GoogleFonts.jost(
        fontSize: 11,
        fontWeight: FontWeight.w500,
        letterSpacing: letterSpacing ?? 3.1, // 0.28em de 11px
        color: color,
      );

  static ThemeData get lightTheme => ThemeData(
    useMaterial3: true,
    colorScheme: const ColorScheme.light(
      primary: primary,
      secondary: secondary,
      surface: card,
      onPrimary: primaryForeground,
      onSecondary: secondaryForeground,
      onSurface: foreground,
      error: destructive,
    ),
    scaffoldBackgroundColor: background,
    textTheme: GoogleFonts.jostTextTheme().copyWith(
      displayLarge: GoogleFonts.cormorantGaramond(
        fontSize: 48,
        fontWeight: FontWeight.w500,
        color: foreground,
        height: 1.1,
      ),
      displayMedium: GoogleFonts.cormorantGaramond(
        fontSize: 36,
        fontWeight: FontWeight.w500,
        color: foreground,
        height: 1.2,
      ),
      headlineMedium: GoogleFonts.jost(
        fontSize: 22,
        fontWeight: FontWeight.w500,
        color: foreground,
      ),
      headlineSmall: GoogleFonts.jost(
        fontSize: 18,
        fontWeight: FontWeight.w500,
        color: foreground,
      ),
      bodyLarge: GoogleFonts.jost(
        fontSize: 16,
        fontWeight: FontWeight.w300,
        color: foreground,
      ),
      bodyMedium: GoogleFonts.jost(
        fontSize: 14,
        fontWeight: FontWeight.w300,
        color: foreground,
      ),
      labelSmall: GoogleFonts.jost(
        fontSize: 11,
        fontWeight: FontWeight.w500,
        letterSpacing: 1.6,
        color: foreground,
      ),
    ),
    cardTheme: CardThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
      elevation: 0,
      color: card,
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: BorderSide.none,
      ),
      filled: true,
      fillColor: card,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primary,
        foregroundColor: primaryForeground,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
        padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
        textStyle: eyebrow(color: primaryForeground, letterSpacing: 1.4),
        elevation: 0,
      ),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: background,
      elevation: 0,
      centerTitle: true,
      titleTextStyle: GoogleFonts.cormorantGaramond(
        fontSize: 22,
        fontWeight: FontWeight.w500,
        color: foreground,
      ),
      iconTheme: const IconThemeData(color: foreground),
    ),
    dividerTheme: const DividerThemeData(color: border, thickness: 1),
    extensions: const [AppColors.light],
  );

  /// Reaproveita a família "Noir" (ink/navy/ivory/goldNoir) já usada nos
  /// blocos escuros do app (plano premium, bloco "Noite" dos roteiros) como
  /// base do tema escuro — mesma linguagem visual, sem criar paleta nova.
  static ThemeData get darkTheme => ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: ColorScheme.dark(
      primary: goldNoir,
      secondary: navy,
      surface: navy,
      onPrimary: ink,
      onSecondary: ivory,
      onSurface: ivory,
      error: destructive,
    ),
    scaffoldBackgroundColor: darkBase,
    textTheme: GoogleFonts.jostTextTheme(ThemeData.dark().textTheme).copyWith(
      displayLarge: GoogleFonts.cormorantGaramond(
        fontSize: 48,
        fontWeight: FontWeight.w500,
        color: ivory,
        height: 1.1,
      ),
      displayMedium: GoogleFonts.cormorantGaramond(
        fontSize: 36,
        fontWeight: FontWeight.w500,
        color: ivory,
        height: 1.2,
      ),
      headlineMedium: GoogleFonts.jost(
        fontSize: 22,
        fontWeight: FontWeight.w500,
        color: ivory,
      ),
      headlineSmall: GoogleFonts.jost(
        fontSize: 18,
        fontWeight: FontWeight.w500,
        color: ivory,
      ),
      bodyLarge: GoogleFonts.jost(
        fontSize: 16,
        fontWeight: FontWeight.w300,
        color: ivory,
      ),
      bodyMedium: GoogleFonts.jost(
        fontSize: 14,
        fontWeight: FontWeight.w300,
        color: ivory,
      ),
      labelSmall: GoogleFonts.jost(
        fontSize: 11,
        fontWeight: FontWeight.w500,
        letterSpacing: 1.6,
        color: ivory,
      ),
    ),
    cardTheme: CardThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
      elevation: 0,
      color: navy,
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: BorderSide.none,
      ),
      filled: true,
      fillColor: navy,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: goldNoir,
        foregroundColor: ink,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
        padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
        textStyle: eyebrow(color: ink, letterSpacing: 1.4),
        elevation: 0,
      ),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: darkBase,
      elevation: 0,
      centerTitle: true,
      titleTextStyle: GoogleFonts.cormorantGaramond(
        fontSize: 22,
        fontWeight: FontWeight.w500,
        color: ivory,
      ),
      iconTheme: const IconThemeData(color: ivory),
    ),
    dividerTheme: DividerThemeData(color: navy, thickness: 1),
    extensions: const [AppColors.dark],
  );
}

/// Tokens semânticos (`background`/`foreground`/`card`/etc.) que reagem ao
/// tema claro/escuro — telas que precisam de dark mode de verdade devem ler
/// `context.colors.X` em vez de `AppTheme.X` fixo. Registrado como
/// `ThemeExtension` em `lightTheme`/`darkTheme`; ver [AppColorsContext].
@immutable
class AppColors extends ThemeExtension<AppColors> {
  final Color background;
  final Color foreground;
  final Color card;
  final Color mutedForeground;
  final Color secondary;
  final Color border;
  final Color primary;

  const AppColors({
    required this.background,
    required this.foreground,
    required this.card,
    required this.mutedForeground,
    required this.secondary,
    required this.border,
    required this.primary,
  });

  static const light = AppColors(
    background: AppTheme.background,
    foreground: AppTheme.foreground,
    card: AppTheme.card,
    mutedForeground: AppTheme.mutedForeground,
    secondary: AppTheme.secondary,
    border: AppTheme.border,
    primary: AppTheme.primary,
  );

  static const dark = AppColors(
    background: AppTheme.darkBase,
    foreground: AppTheme.ivory,
    card: AppTheme.navy,
    mutedForeground: Color(0x99FCF9F0), // ivory ~60% alpha
    secondary: AppTheme.navy,
    border: AppTheme.navy,
    primary: AppTheme.goldNoir,
  );

  @override
  AppColors copyWith({
    Color? background,
    Color? foreground,
    Color? card,
    Color? mutedForeground,
    Color? secondary,
    Color? border,
    Color? primary,
  }) => AppColors(
    background: background ?? this.background,
    foreground: foreground ?? this.foreground,
    card: card ?? this.card,
    mutedForeground: mutedForeground ?? this.mutedForeground,
    secondary: secondary ?? this.secondary,
    border: border ?? this.border,
    primary: primary ?? this.primary,
  );

  @override
  AppColors lerp(ThemeExtension<AppColors>? other, double t) {
    if (other is! AppColors) return this;
    return AppColors(
      background: Color.lerp(background, other.background, t)!,
      foreground: Color.lerp(foreground, other.foreground, t)!,
      card: Color.lerp(card, other.card, t)!,
      mutedForeground: Color.lerp(mutedForeground, other.mutedForeground, t)!,
      secondary: Color.lerp(secondary, other.secondary, t)!,
      border: Color.lerp(border, other.border, t)!,
      primary: Color.lerp(primary, other.primary, t)!,
    );
  }
}

extension AppColorsContext on BuildContext {
  /// Cai pro tema claro quando a extensão não está registrada (ex.: testes
  /// de widget que montam `MaterialApp(home: ...)` sem `AppTheme.lightTheme`)
  /// em vez de lançar — nenhuma tela deveria quebrar por causa de tema.
  AppColors get colors =>
      Theme.of(this).extension<AppColors>() ?? AppColors.light;
}
