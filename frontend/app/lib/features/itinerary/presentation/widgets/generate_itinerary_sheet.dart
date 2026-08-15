import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../../core/themes/app_theme.dart';
import '../../data/itinerary_repository.dart';
import '../providers/itinerary_provider.dart';

// ── Public entry point ────────────────────────────────────────────────────────

class GenerateItinerarySheet extends ConsumerStatefulWidget {
  final String destinationId;
  final String destinationName;

  const GenerateItinerarySheet({
    super.key,
    required this.destinationId,
    required this.destinationName,
  });

  @override
  ConsumerState<GenerateItinerarySheet> createState() =>
      _GenerateItinerarySheetState();
}

// ── State ─────────────────────────────────────────────────────────────────────

class _GenerateItinerarySheetState
    extends ConsumerState<GenerateItinerarySheet> {
  int _step = 0;

  // Step 0 — dates
  DateTime? _startDate;
  DateTime? _endDate;

  // Step 1 — stay area
  final _areaController = TextEditingController();

  // Step 2 — trip vibe
  _TripVibe? _vibe;

  // Step 3 — companions
  _Companions? _companions;

  // Step 4 — budget
  _Budget? _budget;

  // Generation
  bool _loading = false;
  String? _error;

  static const _totalSteps = 5;

  static const _months = [
    '', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez',
  ];

  @override
  void dispose() {
    _areaController.dispose();
    super.dispose();
  }

  // ── Navigation ───────────────────────────────────────────────────────────────

  bool get _canAdvance {
    return switch (_step) {
      0 => _startDate != null && _endDate != null,
      1 => true, // area is optional
      2 => _vibe != null,
      3 => _companions != null,
      4 => _budget != null,
      _ => false,
    };
  }

  void _next() {
    if (_step < _totalSteps - 1) setState(() => _step++);
  }

  void _back() {
    if (_step > 0) setState(() => _step--);
  }

  // ── Date helpers ─────────────────────────────────────────────────────────────

  String _dateLabel(DateTime? d) {
    if (d == null) return 'Selecionar';
    return '${d.day} ${_months[d.month]} ${d.year}';
  }

  int get _nights =>
      (_startDate != null && _endDate != null)
          ? _endDate!.difference(_startDate!).inDays
          : 0;

  Future<void> _pickStart() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _startDate ?? DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked == null) return;
    setState(() {
      _startDate = picked;
      if (_endDate != null && !_endDate!.isAfter(picked)) _endDate = null;
    });
  }

  Future<void> _pickEnd() async {
    if (_startDate == null) return;
    final picked = await showDatePicker(
      context: context,
      initialDate: _endDate ?? _startDate!.add(const Duration(days: 1)),
      firstDate: _startDate!.add(const Duration(days: 1)),
      lastDate: _startDate!.add(const Duration(days: 14)),
    );
    if (picked == null) return;
    setState(() => _endDate = picked);
  }

  // ── Generate ─────────────────────────────────────────────────────────────────

  Future<void> _generate() async {
    if (_startDate == null || _endDate == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final itinerary = await ref.read(itineraryRepositoryProvider).generate(
            destinationId: widget.destinationId,
            startDate: _startDate!,
            endDate: _endDate!,
            stayArea: _areaController.text.trim().isEmpty
                ? null
                : _areaController.text.trim(),
            tripVibe: _vibe?.backendKey,
            companions: _companions?.backendKey,
            tripBudget: _budget?.backendKey,
          );
      ref.invalidate(itinerariesProvider);

      if (mounted) {
        Navigator.of(context).pop();
        context.push('/roteiro/${itinerary.id}');
      }
    } catch (_) {
      setState(() {
        _loading = false;
        _error = 'Não foi possível gerar o roteiro. Tente novamente.';
      });
    }
  }

  // ── Build ────────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding:
          EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Container(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
        decoration: BoxDecoration(
          color: AppTheme.background,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _Handle(),
            const SizedBox(height: 16),
            _ProgressDots(current: _step, total: _totalSteps),
            const SizedBox(height: 20),
            _Header(
              step: _step,
              destinationName: widget.destinationName,
            ),
            const SizedBox(height: 24),
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 200),
              child: KeyedSubtree(
                key: ValueKey(_step),
                child: _buildStepContent(),
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Colors.red[400],
                    ),
                textAlign: TextAlign.center,
              ),
            ],
            const SizedBox(height: 24),
            _NavRow(
              step: _step,
              totalSteps: _totalSteps,
              canAdvance: _canAdvance,
              loading: _loading,
              onBack: _back,
              onNext: _next,
              onGenerate: _generate,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepContent() {
    return switch (_step) {
      0 => _StepDates(
          startDate: _startDate,
          endDate: _endDate,
          nights: _nights,
          dateLabel: _dateLabel,
          onPickStart: _loading ? null : _pickStart,
          onPickEnd: _loading || _startDate == null ? null : _pickEnd,
        ),
      1 => _StepArea(controller: _areaController),
      2 => _StepVibe(
          selected: _vibe,
          onSelect: (v) => setState(() => _vibe = v),
        ),
      3 => _StepCompanions(
          selected: _companions,
          onSelect: (c) => setState(() => _companions = c),
        ),
      4 => _StepBudget(
          selected: _budget,
          onSelect: (b) => setState(() => _budget = b),
        ),
      _ => const SizedBox.shrink(),
    };
  }
}

// ── Small layout widgets ──────────────────────────────────────────────────────

class _Handle extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 40,
        height: 4,
        decoration: BoxDecoration(
          color: AppTheme.softBlack.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }
}

class _ProgressDots extends StatelessWidget {
  final int current;
  final int total;
  const _ProgressDots({required this.current, required this.total});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: List.generate(total, (i) {
        final active = i <= current;
        return AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          margin: const EdgeInsets.only(right: 4),
          width: active ? 20 : 6,
          height: 6,
          decoration: BoxDecoration(
            color: active
                ? AppTheme.primaryBlue
                : AppTheme.softBlack.withValues(alpha: 0.15),
            borderRadius: BorderRadius.circular(3),
          ),
        );
      }),
    );
  }
}

class _Header extends StatelessWidget {
  final int step;
  final String destinationName;
  const _Header({required this.step, required this.destinationName});

  static const _titles = [
    'Quando vai viajar?',
    'Onde vai se hospedar?',
    'Qual é a vibe da viagem?',
    'Com quem vai?',
    'Qual o budget desta viagem?',
  ];

  static const _subtitles = [
    null,
    'Opcional — ajuda a priorizar locais próximos',
    'Isso vai personalizar seu roteiro',
    'Pode ser diferente do que você configurou no perfil',
    'Pode ser diferente da sua preferência padrão',
  ];

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Gerar roteiro',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 28,
            fontWeight: FontWeight.w500,
            color: AppTheme.softBlack,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          destinationName,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: AppTheme.softBlack.withValues(alpha: 0.45),
              ),
        ),
        const SizedBox(height: 16),
        Text(
          _titles[step],
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
                color: AppTheme.softBlack,
              ),
        ),
        if (_subtitles[step] != null) ...[
          const SizedBox(height: 2),
          Text(
            _subtitles[step]!,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: AppTheme.softBlack.withValues(alpha: 0.45),
                ),
          ),
        ],
      ],
    );
  }
}

class _NavRow extends StatelessWidget {
  final int step;
  final int totalSteps;
  final bool canAdvance;
  final bool loading;
  final VoidCallback onBack;
  final VoidCallback onNext;
  final VoidCallback onGenerate;

  const _NavRow({
    required this.step,
    required this.totalSteps,
    required this.canAdvance,
    required this.loading,
    required this.onBack,
    required this.onNext,
    required this.onGenerate,
  });

  @override
  Widget build(BuildContext context) {
    final isLast = step == totalSteps - 1;
    return Row(
      children: [
        if (step > 0) ...[
          OutlinedButton(
            onPressed: loading ? null : onBack,
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
              side: BorderSide(
                  color: AppTheme.softBlack.withValues(alpha: 0.2)),
            ),
            child: const Icon(Icons.arrow_back_rounded, size: 18),
          ),
          const SizedBox(width: 12),
        ],
        Expanded(
          child: SizedBox(
            height: 50,
            child: ElevatedButton(
              onPressed: canAdvance && !loading
                  ? (isLast ? onGenerate : onNext)
                  : null,
              child: loading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : Text(isLast ? 'Gerar Roteiro' : 'Continuar'),
            ),
          ),
        ),
      ],
    );
  }
}

// ── Step 0: Dates ─────────────────────────────────────────────────────────────

class _StepDates extends StatelessWidget {
  final DateTime? startDate;
  final DateTime? endDate;
  final int nights;
  final String Function(DateTime?) dateLabel;
  final VoidCallback? onPickStart;
  final VoidCallback? onPickEnd;

  const _StepDates({
    required this.startDate,
    required this.endDate,
    required this.nights,
    required this.dateLabel,
    required this.onPickStart,
    required this.onPickEnd,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _DateField(
                label: 'Chegada',
                value: dateLabel(startDate),
                onTap: onPickStart,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _DateField(
                label: 'Partida',
                value: dateLabel(endDate),
                onTap: onPickEnd,
                dimmed: startDate == null,
              ),
            ),
          ],
        ),
        if (nights > 0) ...[
          const SizedBox(height: 10),
          Center(
            child: Text(
              '$nights ${nights == 1 ? 'noite' : 'noites'}',
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: AppTheme.primaryBlue,
                  ),
            ),
          ),
        ],
      ],
    );
  }
}

class _DateField extends StatelessWidget {
  final String label;
  final String value;
  final VoidCallback? onTap;
  final bool dimmed;

  const _DateField({
    required this.label,
    required this.value,
    this.onTap,
    this.dimmed = false,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
        decoration: BoxDecoration(
          color: AppTheme.softBlack.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: AppTheme.softBlack.withValues(alpha: 0.1),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label.toUpperCase(),
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.4),
                    letterSpacing: 1.1,
                    fontSize: 10,
                  ),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: dimmed
                        ? AppTheme.softBlack.withValues(alpha: 0.3)
                        : AppTheme.softBlack,
                    fontWeight: FontWeight.w500,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Step 1: Stay area ─────────────────────────────────────────────────────────

class _StepArea extends StatelessWidget {
  final TextEditingController controller;
  const _StepArea({required this.controller});

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      autofocus: true,
      textCapitalization: TextCapitalization.words,
      decoration: InputDecoration(
        hintText: 'Ex: Marais, Eixample, Centro…',
        prefixIcon: Icon(Icons.location_on_outlined,
            color: AppTheme.softBlack.withValues(alpha: 0.4)),
        filled: true,
        fillColor: AppTheme.softBlack.withValues(alpha: 0.04),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide:
              BorderSide(color: AppTheme.softBlack.withValues(alpha: 0.1)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide:
              BorderSide(color: AppTheme.softBlack.withValues(alpha: 0.1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppTheme.primaryBlue),
        ),
      ),
    );
  }
}

// ── Step 2: Vibe ──────────────────────────────────────────────────────────────

enum _TripVibe {
  romantic('Lua de mel', Icons.favorite_border_rounded, 'ROMANTIC'),
  adventure('Aventura', Icons.terrain_outlined, 'ADVENTURE'),
  culture('Cultura & Arte', Icons.museum_outlined, 'CULTURE'),
  wellness('Relaxar', Icons.spa_outlined, 'WELLNESS'),
  business('Trabalho + lazer', Icons.work_outline_rounded, 'BUSINESS');

  final String label;
  final IconData icon;
  final String backendKey;
  const _TripVibe(this.label, this.icon, this.backendKey);
}

class _StepVibe extends StatelessWidget {
  final _TripVibe? selected;
  final ValueChanged<_TripVibe> onSelect;
  const _StepVibe({required this.selected, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: _TripVibe.values
          .map((v) => _OptionChip(
                label: v.label,
                icon: v.icon,
                selected: selected == v,
                onTap: () => onSelect(v),
              ))
          .toList(),
    );
  }
}

// ── Step 3: Companions ────────────────────────────────────────────────────────

enum _Companions {
  solo('Sozinho', Icons.person_outline_rounded, 'SOLO'),
  couple('Casal', Icons.favorite_border_rounded, 'COUPLE'),
  family('Família', Icons.family_restroom_outlined, 'FAMILY'),
  group('Grupo', Icons.group_outlined, 'GROUP');

  final String label;
  final IconData icon;
  final String backendKey;
  const _Companions(this.label, this.icon, this.backendKey);
}

class _StepCompanions extends StatelessWidget {
  final _Companions? selected;
  final ValueChanged<_Companions> onSelect;
  const _StepCompanions({required this.selected, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: _Companions.values
          .map((c) => _OptionChip(
                label: c.label,
                icon: c.icon,
                selected: selected == c,
                onTap: () => onSelect(c),
              ))
          .toList(),
    );
  }
}

// ── Step 4: Budget ────────────────────────────────────────────────────────────

enum _Budget {
  economy('Econômico', '€', 'ECONOMY'),
  moderate('Moderado', '€€', 'MODERATE'),
  luxury('Luxo', '€€€', 'LUXURY');

  final String label;
  final String symbol;
  final String backendKey;
  const _Budget(this.label, this.symbol, this.backendKey);
}

class _StepBudget extends StatelessWidget {
  final _Budget? selected;
  final ValueChanged<_Budget> onSelect;
  const _StepBudget({required this.selected, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: _Budget.values
          .map((b) => Expanded(
                child: Padding(
                  padding: EdgeInsets.only(
                      right: b == _Budget.luxury ? 0 : 10),
                  child: _BudgetCard(
                    budget: b,
                    selected: selected == b,
                    onTap: () => onSelect(b),
                  ),
                ),
              ))
          .toList(),
    );
  }
}

class _BudgetCard extends StatelessWidget {
  final _Budget budget;
  final bool selected;
  final VoidCallback onTap;
  const _BudgetCard(
      {required this.budget,
      required this.selected,
      required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: selected
              ? AppTheme.primaryBlue.withValues(alpha: 0.08)
              : AppTheme.softBlack.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selected
                ? AppTheme.primaryBlue
                : AppTheme.softBlack.withValues(alpha: 0.1),
            width: selected ? 1.5 : 1,
          ),
        ),
        child: Column(
          children: [
            Text(
              budget.symbol,
              style: GoogleFonts.manrope(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: selected
                    ? AppTheme.primaryBlue
                    : AppTheme.goldenOrange,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              budget.label,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: selected
                        ? AppTheme.primaryBlue
                        : AppTheme.softBlack.withValues(alpha: 0.55),
                    fontSize: 11,
                  ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

// ── Shared: option chip ───────────────────────────────────────────────────────

class _OptionChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;
  const _OptionChip(
      {required this.label,
      required this.icon,
      required this.selected,
      required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: selected
              ? AppTheme.primaryBlue.withValues(alpha: 0.08)
              : AppTheme.softBlack.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selected
                ? AppTheme.primaryBlue
                : AppTheme.softBlack.withValues(alpha: 0.1),
            width: selected ? 1.5 : 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 16,
              color: selected
                  ? AppTheme.primaryBlue
                  : AppTheme.softBlack.withValues(alpha: 0.5),
            ),
            const SizedBox(width: 8),
            Text(
              label,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: selected
                        ? AppTheme.primaryBlue
                        : AppTheme.softBlack,
                    fontWeight:
                        selected ? FontWeight.w600 : FontWeight.w400,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}
