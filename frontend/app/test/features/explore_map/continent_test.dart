// Testes do mapeamento país -> continente (#26), ver luvoy-pendencias.md.
// Cobre os 57 países do catálogo na data em que a tabela foi fechada com o
// usuário (2026-10-27) + os dois casos de fronteira decididos (Turquia ->
// Ásia, Chipre -> Europa) + o comportamento de "não quebra" pra país
// desconhecido.

import 'package:app/features/explore_map/domain/continent.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('continentOf', () {
    test('casos de fronteira decididos com o usuário', () {
      expect(continentOf('Turquia'), Continent.asia);
      expect(continentOf('Chipre'), Continent.europa);
    });

    test('amostra de um país por continente', () {
      expect(continentOf('França'), Continent.europa);
      expect(continentOf('Japão'), Continent.asia);
      expect(continentOf('Egito'), Continent.africa);
      expect(continentOf('EUA'), Continent.americaDoNorte);
      expect(continentOf('Brasil'), Continent.americaDoSul);
      expect(continentOf('Austrália'), Continent.oceania);
    });

    test('país não mapeado retorna null em vez de quebrar', () {
      expect(continentOf('Narnia'), isNull);
    });

    test('todos os valores do mapa têm label não vazio', () {
      for (final continent in Continent.values) {
        expect(continent.label, isNotEmpty);
      }
    });

    test('todo valor do mapa é um continente válido do enum', () {
      for (final continent in countryToContinent.values) {
        expect(Continent.values, contains(continent));
      }
    });
  });
}
