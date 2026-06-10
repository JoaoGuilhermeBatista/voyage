class ApiConstants {
  // 10.0.2.2 é o IP que o Android Emulator usa para acessar o localhost da máquina host
  static const String baseUrl = 'http://10.0.2.2:8080/api/v1';

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);
}
