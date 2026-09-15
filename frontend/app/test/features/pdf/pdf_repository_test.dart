// Testes de pdfLimitMessage — mapeia o 402 (PaymentRequiredException do
// backend, 1ª exportação grátis vitalícia esgotada) pra uma mensagem
// amigável em pt-BR, mesmo padrão de freeTierLimitMessage
// (itinerary_repository_test.dart).

import 'package:app/features/pdf/data/pdf_repository.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DioException dioErrorWithStatus(int? statusCode) {
    final requestOptions = RequestOptions(path: '/pdf/export/itin-1');
    return DioException(
      requestOptions: requestOptions,
      response: statusCode == null
          ? null
          : Response(requestOptions: requestOptions, statusCode: statusCode),
    );
  }

  test('retorna a mensagem de limite gratuito quando o erro é 402', () {
    final message = pdfLimitMessage(dioErrorWithStatus(402));

    expect(message, isNotNull);
    expect(message, contains('Premium'));
  });

  test(
    'retorna null pra outros códigos HTTP (ex.: 403 — roteiro de outro usuário)',
    () {
      expect(pdfLimitMessage(dioErrorWithStatus(403)), isNull);
    },
  );

  test('retorna null pra DioException sem response (ex.: falha de rede)', () {
    expect(pdfLimitMessage(dioErrorWithStatus(null)), isNull);
  });

  test('retorna null pra erros que não são DioException', () {
    expect(pdfLimitMessage(Exception('erro qualquer')), isNull);
  });
}
