// Testes de parsing de UnusualPlaceModel (feature premium "Lugares
// Incomuns" — GET /api/v1/unusual-places/{slug}).

import 'package:app/features/unusualplaces/data/unusual_place_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parseia um lugar desbloqueado (locked=false) com conteúdo completo', () {
    final model = UnusualPlaceModel.fromJson({
      'title': 'Bar Escondido',
      'description': 'Um bar sem placa, só quem mora perto conhece.',
      'category': 'Bar escondido',
      'locked': false,
    });

    expect(model.title, 'Bar Escondido');
    expect(model.description, 'Um bar sem placa, só quem mora perto conhece.');
    expect(model.category, 'Bar escondido');
    expect(model.locked, isFalse);
  });

  test('parseia um teaser (locked=true) sem description/category', () {
    final model = UnusualPlaceModel.fromJson({
      'title': 'Bar Escondido',
      'description': null,
      'category': null,
      'locked': true,
    });

    expect(model.title, 'Bar Escondido');
    expect(model.description, isNull);
    expect(model.category, isNull);
    expect(model.locked, isTrue);
  });
}
