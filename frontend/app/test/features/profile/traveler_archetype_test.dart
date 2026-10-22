import 'package:app/features/profile/domain/traveler_archetype.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('computeTravelerArchetype', () {
    test('interesses que votam todos no mesmo arquétipo vencem claramente', () {
      expect(
        computeTravelerArchetype(['gastronomia', 'vinhos']),
        TravelerArchetype.apreciadorDeSabores,
      );
      expect(
        computeTravelerArchetype(['cultura', 'moda', 'musica']),
        TravelerArchetype.apaixonadoPorCultura,
      );
      expect(
        computeTravelerArchetype(['natureza', 'arquitetura']),
        TravelerArchetype.exploradorCurioso,
      );
    });

    test('maioria clara entre arquétipos diferentes vence pelo número de votos', () {
      // 2 votos pra Apreciador de Sabores, 1 pra Apaixonado por Cultura
      expect(
        computeTravelerArchetype(['gastronomia', 'vinhos', 'cultura']),
        TravelerArchetype.apreciadorDeSabores,
      );
    });

    test('empate entre arquétipos cai no Viajante Equilibrado', () {
      // 1 voto pra cada um de dois arquétipos diferentes
      expect(
        computeTravelerArchetype(['gastronomia', 'cultura']),
        TravelerArchetype.equilibrado,
      );
    });

    test('lista de interesses vazia cai no Viajante Equilibrado', () {
      expect(computeTravelerArchetype([]), TravelerArchetype.equilibrado);
    });

    test('só interesses sem voto definido (nenhum reconhecido) cai no '
        'Viajante Equilibrado', () {
      expect(
        computeTravelerArchetype(['interesse-desconhecido']),
        TravelerArchetype.equilibrado,
      );
    });

    test('rhythm RELAXED dá um voto extra pro Buscador de Descanso, '
        'suficiente pra desempatar', () {
      // sem rhythm: gastronomia(1) x bem-estar(1) -> empate -> equilibrado
      expect(
        computeTravelerArchetype(['gastronomia', 'bem-estar']),
        TravelerArchetype.equilibrado,
      );
      // com RELAXED: gastronomia(1) x bem-estar(2) -> Buscador de Descanso
      expect(
        computeTravelerArchetype(
          ['gastronomia', 'bem-estar'],
          rhythm: 'RELAXED',
        ),
        TravelerArchetype.buscadorDeDescanso,
      );
    });

    test('rhythm RELAXED sozinho (sem nenhum interesse) já basta pro '
        'Buscador de Descanso', () {
      expect(
        computeTravelerArchetype([], rhythm: 'RELAXED'),
        TravelerArchetype.buscadorDeDescanso,
      );
    });

    test('rhythm RELAXED não domina sobre uma maioria clara de outro '
        'arquétipo', () {
      expect(
        computeTravelerArchetype(
          ['cultura', 'moda', 'musica'],
          rhythm: 'RELAXED',
        ),
        TravelerArchetype.apaixonadoPorCultura,
      );
    });

    test('cada arquétipo tem título e descrição não vazios', () {
      for (final archetype in TravelerArchetype.values) {
        expect(archetype.title, isNotEmpty);
        expect(archetype.description, isNotEmpty);
      }
    });
  });
}
