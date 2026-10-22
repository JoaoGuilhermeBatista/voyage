/// Perfil do viajante (#8/9) — 5 arquétipos calculados a partir dos
/// interesses escolhidos no onboarding (+ ritmo da viagem como desempate
/// leve), sem precisar de LLM: cada interesse "vota" num arquétipo, o mais
/// votado vence. Empate ou nenhum interesse reconhecido → Viajante
/// Equilibrado (fallback neutro, nunca deixa o usuário sem um perfil).
enum TravelerArchetype {
  apreciadorDeSabores(
    'O Apreciador de Sabores',
    'Para você, viajar é uma experiência que se prova. Valoriza a boa '
        'gastronomia, os sabores locais e a arte de compartilhar a mesa. '
        'Cada refeição vira uma lembrança.',
  ),
  buscadorDeDescanso(
    'O Buscador de Descanso',
    'Sua viagem ideal é um refúgio. Prefere roteiros sem pressa, onde o '
        'conforto e o tempo são os maiores luxos. Viajar, para você, é '
        'recarregar as energias.',
  ),
  exploradorCurioso(
    'O Explorador Curioso',
    'Movido pela curiosidade, você gosta de ir além do óbvio. Cada '
        'destino é uma descoberta nova, uma paisagem inédita, uma '
        'história para colecionar.',
  ),
  apaixonadoPorCultura(
    'O Apaixonado por Cultura',
    'Você viaja para se conectar com a alma de cada lugar. Museus, '
        'arquitetura e tradições. A sua viagem é uma jornada pelo tempo e '
        'pela história.',
  ),
  equilibrado(
    'O Viajante Equilibrado',
    'Você gosta de um pouco de tudo: descanso, cultura, sabores e '
        'aventura na medida certa. A sua viagem ideal é uma harmonia de '
        'experiências bem planejadas.',
  );

  final String title;
  final String description;
  const TravelerArchetype(this.title, this.description);
}

/// Interesse (chave de interestOptions, ver preference_steps.dart) -> qual
/// arquétipo ele vota.
const _archetypeVotes = {
  'gastronomia': TravelerArchetype.apreciadorDeSabores,
  'vinhos': TravelerArchetype.apreciadorDeSabores,
  'bem-estar': TravelerArchetype.buscadorDeDescanso,
  'praia': TravelerArchetype.buscadorDeDescanso,
  'neve': TravelerArchetype.buscadorDeDescanso,
  'natureza': TravelerArchetype.exploradorCurioso,
  'arquitetura': TravelerArchetype.exploradorCurioso,
  'vida-local': TravelerArchetype.exploradorCurioso,
  'cultura': TravelerArchetype.apaixonadoPorCultura,
  'moda': TravelerArchetype.apaixonadoPorCultura,
  'musica': TravelerArchetype.apaixonadoPorCultura,
  'vida-noturna': TravelerArchetype.apaixonadoPorCultura,
};

/// Calcula o arquétipo a partir dos interesses do onboarding. `rhythm`
/// (ECONOMY/MODERATE/LUXURY não entra — é budget; aqui é RELAXED/MODERATE/
/// INTENSE) dá um voto extra pro Buscador de Descanso quando RELAXED, só
/// como desempate leve — nunca domina sozinho sobre interesses reais.
TravelerArchetype computeTravelerArchetype(
  List<String> interests, {
  String? rhythm,
}) {
  final votes = <TravelerArchetype, int>{};
  void vote(TravelerArchetype a) => votes[a] = (votes[a] ?? 0) + 1;

  for (final interest in interests) {
    final archetype = _archetypeVotes[interest];
    if (archetype != null) vote(archetype);
  }
  if (rhythm == 'RELAXED') vote(TravelerArchetype.buscadorDeDescanso);

  if (votes.isEmpty) return TravelerArchetype.equilibrado;

  final maxVotes = votes.values.reduce((a, b) => a > b ? a : b);
  final winners = votes.entries
      .where((e) => e.value == maxVotes)
      .map((e) => e.key)
      .toList();

  return winners.length == 1 ? winners.single : TravelerArchetype.equilibrado;
}
