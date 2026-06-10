import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const Color primaryBlue  = Color(0xFF1D407A);
  static const Color goldenOrange = Color(0xFFF2B15A);
  static const Color background   = Color(0xFFF6F2EB);
  static const Color softWhite    = Color(0xFFFFFDF9);
  static const Color softBlack    = Color(0xFF111111);
  static const Color shimmerBase  = Color(0xFFE8E4DC);
  static const Color shimmerHigh  = Color(0xFFF6F2EB);

  static ThemeData get lightTheme => ThemeData(
    useMaterial3: true,
    colorScheme: const ColorScheme.light(
      primary: primaryBlue,
      secondary: goldenOrange,
      surface: softWhite,
      onPrimary: softWhite,
      onSecondary: softBlack,
      onSurface: softBlack,
    ),
    scaffoldBackgroundColor: background,
    textTheme: GoogleFonts.manropeTextTheme().copyWith(
      displayLarge: GoogleFonts.cormorantGaramond(
        fontSize: 48,
        fontWeight: FontWeight.w600,
        color: softBlack,
        height: 1.1,
      ),
      displayMedium: GoogleFonts.cormorantGaramond(
        fontSize: 36,
        fontWeight: FontWeight.w500,
        color: softBlack,
        height: 1.2,
      ),
      headlineMedium: GoogleFonts.manrope(
        fontSize: 22,
        fontWeight: FontWeight.w700,
        color: softBlack,
      ),
      headlineSmall: GoogleFonts.manrope(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        color: softBlack,
      ),
      bodyLarge: GoogleFonts.manrope(
        fontSize: 16,
        color: softBlack,
      ),
      bodyMedium: GoogleFonts.manrope(
        fontSize: 14,
        color: softBlack,
      ),
      labelSmall: GoogleFonts.manrope(
        fontSize: 11,
        fontWeight: FontWeight.w600,
        letterSpacing: 1.2,
        color: softBlack,
      ),
    ),
    cardTheme: CardTheme(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
      ),
      elevation: 0,
      color: softWhite,
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
      filled: true,
      fillColor: softWhite,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primaryBlue,
        foregroundColor: softWhite,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
        textStyle: GoogleFonts.manrope(
          fontSize: 15,
          fontWeight: FontWeight.w600,
        ),
        elevation: 0,
      ),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: background,
      elevation: 0,
      centerTitle: true,
      titleTextStyle: GoogleFonts.cormorantGaramond(
        fontSize: 22,
        fontWeight: FontWeight.w600,
        color: softBlack,
      ),
      iconTheme: const IconThemeData(color: softBlack),
    ),
    dividerTheme: const DividerThemeData(
      color: Color(0xFFE8E4DC),
      thickness: 1,
    ),
  );
}
