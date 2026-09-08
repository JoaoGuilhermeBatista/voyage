// Testes de freeTierLimitMessage — mapeia o 402 (PaymentRequiredException do
// backend, limite de 1 roteiro/mês do plano gratuito) pra uma mensagem
// amigável em pt-BR, ver luvoy-pendencias.md.

import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DioException dioErrorWithStatus(int? statusCode) {
    final requestOptions = RequestOptions(path: '/itineraries');
    return DioException(
      requestOptions: requestOptions,
      response: statusCode == null
          ? null
          : Response(requestOptions: requestOptions, statusCode: statusCode),
    );
  }

  test('retorna a mensagem de limite gratuito quando o erro é 402', () {
    final message = freeTierLimitMessage(dioErrorWithStatus(402));

    expect(message, isNotNull);
    expect(message, contains('Premium'));
  });

  test('retorna null pra outros códigos HTTP (ex.: 500)', () {
    expect(freeTierLimitMessage(dioErrorWithStatus(500)), isNull);
  });

  test('retorna null pra DioException sem response (ex.: falha de rede)', () {
    expect(freeTierLimitMessage(dioErrorWithStatus(null)), isNull);
  });

  test('retorna null pra erros que não são DioException', () {
    expect(freeTierLimitMessage(Exception('erro qualquer')), isNull);
  });
}
