// Teste do provider unusualPlacesForDestinationProvider — confirma que o
// FutureProvider.family delega pro repositório com o slug correto (mesmo
// padrão de cobertura dado a templatesForDestinationProvider).

import 'package:app/features/unusualplaces/data/unusual_place_models.dart';
import 'package:app/features/unusualplaces/data/unusual_place_repository.dart';
import 'package:app/features/unusualplaces/presentation/providers/unusual_place_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockUnusualPlaceRepository extends Mock
    implements UnusualPlaceRepository {}

void main() {
  test('delega para getForDestination com o slug correto', () async {
    final mockRepo = MockUnusualPlaceRepository();
    when(() => mockRepo.getForDestination('paris')).thenAnswer(
      (_) async => [
        const UnusualPlaceModel(
          title: 'Bar Escondido',
          description: 'Descrição completa.',
          category: 'Bar escondido',
          locked: false,
        ),
      ],
    );

    final container = ProviderContainer(
      overrides: [
        unusualPlaceRepositoryProvider.overrideWithValue(mockRepo),
      ],
    );
    addTearDown(container.dispose);

    final result =
        await container.read(unusualPlacesForDestinationProvider('paris').future);

    expect(result, hasLength(1));
    expect(result.first.title, 'Bar Escondido');
    verify(() => mockRepo.getForDestination('paris')).called(1);
  });
}
