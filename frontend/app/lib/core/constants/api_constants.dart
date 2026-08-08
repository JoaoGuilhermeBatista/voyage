import 'dart:io';

class ApiConstants {
  static String get baseUrl {
    if (Platform.isAndroid) return 'http://10.0.2.2:8080/api/v1';
    return 'http://localhost:8080/api/v1';
  }

  static const String supabaseUrl = 'https://jehszyovmhaqtvrzzful.supabase.co';
  static const String supabaseAnonKey =
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplaHN6eW92bWhhcXR2cnp6ZnVsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI3MzUxODgsImV4cCI6MjA5ODMxMTE4OH0.jLhBIrkwIaoptuMFO0HRHqGlwhsyJIo7QHtW8TI5LfY';

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);
}
