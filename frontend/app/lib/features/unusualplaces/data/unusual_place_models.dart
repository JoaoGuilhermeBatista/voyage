/// Modelo espelhando UnusualPlaceDTO do backend (feature premium "Lugares
/// Incomuns" — escopo definido numa sessão de implementação, não spec
/// oficial da contratante, ver resumo entregue ao usuário).
///
/// GET /api/v1/unusual-places/{slug} (autenticado):
///   -> List<{title, description, category, locked}>
/// `locked=true`: usuário sem premium — description/category vêm null,
/// só o título (teaser de upsell). `locked=false`: conteúdo completo.
class UnusualPlaceModel {
  final String title;
  final String? description;
  final String? category;
  final bool locked;

  const UnusualPlaceModel({
    required this.title,
    required this.description,
    required this.category,
    required this.locked,
  });

  factory UnusualPlaceModel.fromJson(Map<String, dynamic> json) {
    return UnusualPlaceModel(
      title: json['title'] as String,
      description: json['description'] as String?,
      category: json['category'] as String?,
      locked: json['locked'] as bool,
    );
  }
}
