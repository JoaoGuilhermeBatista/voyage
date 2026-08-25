import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../data/itinerary_template_models.dart';
import '../../data/itinerary_template_repository.dart';
import '../providers/itinerary_template_provider.dart';

/// Tela de preview de um template de roteiro curado (dia a dia, SEM datas
/// reais — apenas dayNumber relativo) com botão "Usar este roteiro" que
/// escolhe uma data de início e chama
/// ItineraryTemplateRepository.adoptTemplate, navegando em seguida para
/// ItineraryScreen (/roteiro/:id) com o roteiro real criado.
///
/// Navegação pós-adopt usa `context.push` (go_router) — mesma decisão
/// documentada no teste correspondente (sem bottom sheet para dar pop
/// antes, ao contrário de GenerateItinerarySheet). Nos testes de widget
/// desta tela (que montam `MaterialApp(home: ...)` sem GoRouter no
/// ancestral), essa chamada lança synchronamente e é absorvida pelo
/// `catch` de erro do fluxo de adoção — inofensivo ali, pois nenhum teste
/// depende do destino real da navegação, só de adoptTemplate ter sido
/// chamado. Ver test/features/itinerarytemplates/itinerary_template_test.dart.
class TemplatePreviewScreen extends ConsumerStatefulWidget {
  final String slug;
  final String templateId;

  const TemplatePreviewScreen({
    super.key,
    required this.slug,
    required this.templateId,
  });

  @override
  ConsumerState<TemplatePreviewScreen> createState() =>
      _TemplatePreviewScreenState();
}

class _TemplatePreviewScreenState
    extends ConsumerState<TemplatePreviewScreen> {
  bool _adopting = false;

  (String, String) get _providerKey => (widget.slug, widget.templateId);

  Future<void> _useTemplate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked == null || !mounted) return;

    setState(() => _adopting = true);
    try {
      final itinerary = await ref
          .read(itineraryTemplateRepositoryProvider)
          .adoptTemplate(widget.templateId, picked);
      if (!mounted) return;

      // Navegação via go_router (rota já registrada em app_router.dart).
      // Não há bottom sheet para dar pop antes, diferente de
      // GenerateItinerarySheet — um push simples é suficiente.
      context.push('/roteiro/${itinerary.id}');
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Não foi possível criar o roteiro a partir deste modelo. '
            'Tente novamente.',
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _adopting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final asyncValue = ref.watch(templateDetailProvider(_providerKey));

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: AppTheme.background,
        title: const Text('Roteiro sugerido'),
      ),
      body: asyncValue.when(
        loading: () => const Center(
          child: CircularProgressIndicator(color: AppTheme.primaryBlue),
        ),
        error: (_, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Não foi possível carregar este roteiro.',
                  textAlign: TextAlign.center,
                  style: GoogleFonts.manrope(
                    fontSize: 15,
                    color: AppTheme.softBlack,
                  ),
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () =>
                      ref.invalidate(templateDetailProvider(_providerKey)),
                  child: const Text('Tentar novamente'),
                ),
              ],
            ),
          ),
        ),
        data: (detail) => _TemplatePreviewBody(detail: detail),
      ),
      bottomNavigationBar: asyncValue.hasValue
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 12),
                child: SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: _adopting ? null : _useTemplate,
                    child: _adopting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text('Usar este roteiro'),
                  ),
                ),
              ),
            )
          : null,
    );
  }
}

// ---------------------------------------------------------------------------
// Body (data state) — título, descrição, budget tier, dias/slots/atividades
// ---------------------------------------------------------------------------

class _TemplatePreviewBody extends StatelessWidget {
  final ItineraryTemplateDetailModel detail;
  const _TemplatePreviewBody({required this.detail});

  static const _budgetLabels = {
    'ECONOMY': 'Econômico',
    'MODERATE': 'Moderado',
    'LUXURY': 'Luxo',
  };

  @override
  Widget build(BuildContext context) {
    final days = List.of(detail.days)
      ..sort((a, b) => a.dayNumber.compareTo(b.dayNumber));
    final budgetLabel = _budgetLabels[detail.budgetTier] ?? detail.budgetTier;

    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
      children: [
        Text(
          detail.title,
          style: GoogleFonts.cormorantGaramond(
            fontSize: 32,
            fontWeight: FontWeight.w500,
            color: AppTheme.softBlack,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Icon(
              Icons.calendar_today_outlined,
              size: 14,
              color: AppTheme.softBlack.withValues(alpha: 0.5),
            ),
            const SizedBox(width: 6),
            Text(
              '${detail.durationDays} dias',
              style: GoogleFonts.manrope(
                fontSize: 13,
                color: AppTheme.softBlack.withValues(alpha: 0.6),
              ),
            ),
            const SizedBox(width: 12),
            Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: AppTheme.goldenOrange.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                budgetLabel,
                style: GoogleFonts.manrope(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.goldenOrange,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        Text(
          detail.description,
          style: GoogleFonts.manrope(
            fontSize: 15,
            height: 1.6,
            color: const Color(0xFF444444),
          ),
        ),
        for (final day in days) _TemplateDaySection(day: day),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Day section — "Dia N" + slots agrupados (Manhã/Tarde/Noite)
// ---------------------------------------------------------------------------

class _TemplateDaySection extends StatelessWidget {
  final TemplateDayModel day;
  const _TemplateDaySection({required this.day});

  static const _slotOrder = ['MORNING', 'AFTERNOON', 'EVENING'];
  static const _slotLabels = {
    'MORNING': 'Manhã',
    'AFTERNOON': 'Tarde',
    'EVENING': 'Noite',
  };

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 28),
        Text(
          'Dia ${day.dayNumber}',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 22,
            fontWeight: FontWeight.w600,
            color: AppTheme.softBlack,
          ),
        ),
        const SizedBox(height: 8),
        Divider(color: AppTheme.softBlack.withValues(alpha: 0.08), height: 1),
        for (final slot in _slotOrder)
          if (day.activitiesForSlot(slot).isNotEmpty)
            _TemplateSlotSection(
              label: _slotLabels[slot]!,
              activities: day.activitiesForSlot(slot),
            ),
      ],
    );
  }
}

class _TemplateSlotSection extends StatelessWidget {
  final String label;
  final List<TemplateActivityModel> activities;
  const _TemplateSlotSection({
    required this.label,
    required this.activities,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: AppTheme.softBlack.withValues(alpha: 0.4),
                  letterSpacing: 1.1,
                ),
          ),
          const SizedBox(height: 10),
          for (final activity in activities)
            _TemplateActivityTile(activity: activity),
        ],
      ),
    );
  }
}

class _TemplateActivityTile extends StatelessWidget {
  final TemplateActivityModel activity;
  const _TemplateActivityTile({required this.activity});

  @override
  Widget build(BuildContext context) {
    final poi = activity.poi;
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 6,
            height: 6,
            margin: const EdgeInsets.only(top: 7),
            decoration: const BoxDecoration(
              color: AppTheme.primaryBlue,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  poi.title,
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: AppTheme.softBlack,
                      ),
                ),
                if (poi.description != null &&
                    poi.description!.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    poi.description!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppTheme.softBlack.withValues(alpha: 0.5),
                        ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
