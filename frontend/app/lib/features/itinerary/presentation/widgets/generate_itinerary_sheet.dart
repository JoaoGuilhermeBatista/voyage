import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../../core/themes/app_theme.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';
import '../../../maps/data/maps_models.dart';
import '../../../maps/presentation/providers/maps_provider.dart';
import '../../../onboarding/presentation/widgets/preference_steps.dart';
import '../../data/itinerary_models.dart';
import '../../data/itinerary_repository.dart';
import '../providers/itinerary_provider.dart';

// ── Logical step names (index shifts depending on the mode) ────────────────

enum _StepName {
  city,
  dates,
  area,
  vibe,
  interests,
  rhythm,
  companions,
  budget,
}

// ── Public entry point ────────────────────────────────────────────────────────

class GenerateItinerarySheet extends ConsumerStatefulWidget {
  final String? destinationId;
  final String? destinationName;

  const GenerateItinerarySheet({
    super.key,
    this.destinationId,
    this.destinationName,
  });

  @override
  ConsumerState<GenerateItinerarySheet> createState() =>
      _GenerateItinerarySheetState();
}

// ── State ─────────────────────────────────────────────────────────────────────

class _GenerateItinerarySheetState
    extends ConsumerState<GenerateItinerarySheet> {
  int _step = 0;

  // City step (only in "sem destino" mode)
  final _cityController = TextEditingController();
  String? _selectedDestinationId;
  String? _selectedDestinationName;

  // Step — dates
  DateTime? _startDate;
  DateTime? _endDate;

  // Step — stay area
  final _areaController = TextEditingController();

  // Step — trip vibe
  _TripVibe? _vibe;

  // Step — interests
  final Set<String> _interests = {};

  // Step — rhythm
  String? _rhythm;

  // Step — companions
  _Companions? _companions;

  // Step — budget
  _Budget? _budget;

  // Generation
  bool _loading = false;
  String? _error;

  static const _months = [
    '', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez',
  ];

  /// Ordem lógica das etapas — desloca conforme o modo (com ou sem
  /// destino pré-definido).
  List<_StepName> get _steps => widget.destinationId == null
      ? const [
          _StepName.city,
          _StepName.dates,
          _StepName.budget,
          _StepName.area,
          _StepName.vibe,
          _StepName.interests,
          _StepName.rhythm,
          _StepName.companions,
        ]
      : const [
          _StepName.dates,
          _StepName.budget,
          _StepName.area,
          _StepName.vibe,
          _StepName.interests,
          _StepName.rhythm,
          _StepName.companions,
        ];

  int get _totalSteps => _steps.length;

  _StepName get _currentStepName => _steps[_step];

  @override
  void initState() {
    super.initState();
    _cityController.addListener(_onCityQueryChanged);
  }

  void _onCityQueryChanged() {
    setState(() {});
  }

  @override
  void dispose() {
    _cityController.removeListener(_onCityQueryChanged);
    _cityController.dispose();
    _areaController.dispose();
    super.dispose();
  }

  // ── Navigation ───────────────────────────────────────────────────────────────

  bool get _canAdvance {
    return switch (_currentStepName) {
      _StepName.city => _selectedDestinationId != null,
      _StepName.dates => _startDate != null && _endDate != null,
      _StepName.area => true, // area is optional
      _StepName.vibe => _vibe != null,
      _StepName.interests => true, // interests are optional
      _StepName.rhythm => true, // rhythm is optional
      _StepName.companions => _companions != null,
      _StepName.budget => _budget != null,
    };
  }

  void _next() {
    if (_step < _totalSteps - 1) setState(() => _step++);
  }

  void _selectDestination(DestinationSummaryModel destination) {
    setState(() {
      _selectedDestinationId = destination.id;
      _selectedDestinationName = destination.name;
      if (_step < _totalSteps - 1) _step++;
    });
  }

  void _selectVibe(_TripVibe vibe) {
    setState(() {
      _vibe = vibe;
      if (vibe == _TripVibe.romantic && _companions == _Companions.solo) {
        _companions = null;
      }
    });
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
    final effectiveDestinationId = widget.destinationId ?? _selectedDestinationId;
    if (effectiveDestinationId == null) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final itinerary = await _callGenerate(effectiveDestinationId);
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

  /// Chama o repositório resolvendo tripInterests/tripRhythm — mantém a
  /// chamada "enxuta" (sem esses dois parâmetros nomeados) quando nada foi
  /// selecionado, preservando compatibilidade com o formato de chamada já
  /// existente.
  Future<ItineraryModel> _callGenerate(String destinationId) {
    final repo = ref.read(itineraryRepositoryProvider);
    final stayArea = _areaController.text.trim().isEmpty
        ? null
        : _areaController.text.trim();
    final tripInterests = _interests.isEmpty ? null : _interests.toList();

    if (tripInterests == null && _rhythm == null) {
      return repo.generate(
        destinationId: destinationId,
        startDate: _startDate!,
        endDate: _endDate!,
        stayArea: stayArea,
        tripVibe: _vibe?.backendKey,
        companions: _companions?.backendKey,
        tripBudget: _budget?.backendKey,
      );
    }

    return repo.generate(
      destinationId: destinationId,
      startDate: _startDate!,
      endDate: _endDate!,
      stayArea: stayArea,
      tripVibe: _vibe?.backendKey,
      companions: _companions?.backendKey,
      tripBudget: _budget?.backendKey,
      tripInterests: tripInterests,
      tripRhythm: _rhythm,
    );
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
            // O conteúdo cresceu (etapa de Cidade, Interesses, Ritmo) e pode
            // não caber na altura disponível em telas menores — torna essa
            // parte rolável para nunca estourar o layout, mantendo os
            // botões de navegação sempre visíveis fora da rolagem.
            Flexible(
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _ProgressDots(current: _step, total: _totalSteps),
                    const SizedBox(height: 20),
                    _Header(
                      stepName: _currentStepName,
                      destinationName: widget.destinationName ??
                          _selectedDestinationName ??
                          '',
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
                  ],
                ),
              ),
            ),
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
    return switch (_currentStepName) {
      _StepName.city => _StepCity(
          controller: _cityController,
          selectedId: _selectedDestinationId,
          onSelect: _selectDestination,
        ),
      _StepName.dates => _StepDates(
          startDate: _startDate,
          endDate: _endDate,
          nights: _nights,
          dateLabel: _dateLabel,
          onPickStart: _loading ? null : _pickStart,
          onPickEnd: _loading || _startDate == null ? null : _pickEnd,
        ),
      _StepName.area => _StepArea(
          controller: _areaController,
          destinationId: widget.destinationId ?? _selectedDestinationId,
          budgetTier: _budget?.backendKey,
          onSuggestionSelected: (name) =>
              setState(() => _areaController.text = name),
        ),
      _StepName.vibe => _StepVibe(
          selected: _vibe,
          onSelect: _selectVibe,
        ),
      _StepName.interests => InterestsStep(
          selected: _interests,
          onToggle: (i) => setState(() {
            if (_interests.contains(i)) {
              _interests.remove(i);
            } else {
              _interests.add(i);
            }
          }),
          showStepNumber: false,
          scrollable: false,
        ),
      _StepName.rhythm => RhythmStep(
          selected: _rhythm,
          onSelected: (r) => setState(() => _rhythm = r),
          showStepNumber: false,
          scrollable: false,
        ),
      _StepName.companions => _StepCompanions(
          selected: _companions,
          onSelect: (c) => setState(() => _companions = c),
          hideSolo: _vibe == _TripVibe.romantic,
        ),
      _StepName.budget => _StepBudget(
          selected: _budget,
          onSelect: (b) => setState(() => _budget = b),
        ),
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
  final _StepName stepName;
  final String destinationName;
  const _Header({required this.stepName, required this.destinationName});

  // Interesses e Ritmo reaproveitam InterestsStep/RhythmStep do onboarding,
  // que já trazem seu próprio título grande (via StepScaffold) — por isso
  // ficam de fora deste mapa: mostrar um segundo título aqui duplicaria a
  // pergunta com uma redação diferente, empilhando dois headers na tela.
  static const _titles = {
    _StepName.city: 'Para onde você vai?',
    _StepName.dates: 'Quando vai viajar?',
    _StepName.area: 'Onde vai se hospedar?',
    _StepName.vibe: 'Qual é o clima da viagem?',
    _StepName.companions: 'Com quem vai?',
    _StepName.budget: 'Qual o budget desta viagem?',
  };

  static const _subtitles = {
    _StepName.city: 'Escolha o destino da sua próxima viagem',
    _StepName.dates: null,
    _StepName.area: 'Opcional — ajuda a priorizar locais próximos',
    _StepName.vibe: 'Isso vai personalizar seu roteiro',
    _StepName.companions: 'Pode ser diferente do que você configurou no perfil',
    _StepName.budget: 'Pode ser diferente da sua preferência padrão',
  };

  @override
  Widget build(BuildContext context) {
    final title = _titles[stepName];
    final subtitle = _subtitles[stepName];
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
        if (title != null) ...[
          const SizedBox(height: 16),
          Text(
            title,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: AppTheme.softBlack,
                ),
          ),
          if (subtitle != null) ...[
            const SizedBox(height: 2),
            Text(
              subtitle,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.45),
                  ),
            ),
          ],
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

// ── Step: City (modo "sem destino pré-definido") ────────────────────────────

class _StepCity extends ConsumerWidget {
  final TextEditingController controller;
  final String? selectedId;
  final ValueChanged<DestinationSummaryModel> onSelect;

  const _StepCity({
    required this.controller,
    required this.selectedId,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final query = controller.text.trim();
    final asyncDestinations = query.isEmpty
        ? ref.watch(destinationsProvider)
        : ref.watch(destinationSearchProvider(query));

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: controller,
          autofocus: true,
          decoration: InputDecoration(
            hintText: 'Buscar cidade ou destino…',
            prefixIcon: Icon(Icons.search_rounded,
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
        ),
        const SizedBox(height: 16),
        ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 320),
          child: asyncDestinations.when(
            data: (destinations) {
              if (destinations.isEmpty) {
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 24),
                  child: Text(
                    'Nenhum destino encontrado',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: AppTheme.softBlack.withValues(alpha: 0.45),
                        ),
                  ),
                );
              }
              return ListView.builder(
                shrinkWrap: true,
                itemCount: destinations.length,
                itemBuilder: (context, index) {
                  final destination = destinations[index];
                  return _DestinationTile(
                    destination: destination,
                    selected: destination.id == selectedId,
                    onTap: () => onSelect(destination),
                  );
                },
              );
            },
            loading: () => const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            ),
            error: (_, _) => Padding(
              padding: const EdgeInsets.symmetric(vertical: 24),
              child: Text(
                'Não foi possível carregar os destinos',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppTheme.softBlack.withValues(alpha: 0.45),
                    ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _DestinationTile extends StatelessWidget {
  final DestinationSummaryModel destination;
  final bool selected;
  final VoidCallback onTap;

  const _DestinationTile({
    required this.destination,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
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
          children: [
            Icon(
              Icons.location_city_rounded,
              size: 18,
              color: selected
                  ? AppTheme.primaryBlue
                  : AppTheme.softBlack.withValues(alpha: 0.5),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    destination.name,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: selected
                              ? AppTheme.primaryBlue
                              : AppTheme.softBlack,
                        ),
                  ),
                  Text(
                    destination.country,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppTheme.softBlack.withValues(alpha: 0.45),
                        ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Step 1: Stay area ─────────────────────────────────────────────────────────

class _StepArea extends ConsumerWidget {
  final TextEditingController controller;
  final String? destinationId;
  final String? budgetTier;
  final ValueChanged<String> onSuggestionSelected;

  const _StepArea({
    required this.controller,
    required this.destinationId,
    required this.budgetTier,
    required this.onSuggestionSelected,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildSuggestions(context, ref),
        TextField(
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
              borderSide: BorderSide(
                  color: AppTheme.softBlack.withValues(alpha: 0.1)),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                  color: AppTheme.softBlack.withValues(alpha: 0.1)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: AppTheme.primaryBlue),
            ),
          ),
        ),
      ],
    );
  }

  /// Chips com sugestões de bairro (via
  /// GET /api/v1/maps/neighborhood-suggestions), acima do campo de texto.
  /// Mesmo espírito gracioso de RouteInfoBar/_CurrencyRatesBar: enquanto
  /// carrega, em erro, sem `destinationId`/`budgetTier`, com
  /// `configured == false` ou lista vazia — não mostra nada, só o campo de
  /// texto de sempre.
  Widget _buildSuggestions(BuildContext context, WidgetRef ref) {
    final destinationId = this.destinationId;
    final budgetTier = this.budgetTier;
    if (destinationId == null || budgetTier == null) {
      return const SizedBox.shrink();
    }

    final asyncSuggestions = ref.watch(
      neighborhoodSuggestionsProvider((destinationId, budgetTier)),
    );

    return asyncSuggestions.when(
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const SizedBox.shrink(),
      data: (suggestions) {
        if (!suggestions.configured || suggestions.neighborhoods.isEmpty) {
          return const SizedBox.shrink();
        }

        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Sugestões:',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: AppTheme.softBlack.withValues(alpha: 0.45),
                    ),
              ),
              const SizedBox(height: 8),
              Column(
                children: suggestions.neighborhoods
                    .map((neighborhood) => Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: _NeighborhoodSuggestionCard(
                            neighborhood: neighborhood,
                            selected: controller.text == neighborhood.name,
                            onTap: () =>
                                onSuggestionSelected(neighborhood.name),
                          ),
                        ))
                    .toList(),
              ),
            ],
          ),
        );
      },
    );
  }
}

// ── Shared: neighborhood suggestion card ────────────────────────────────────

/// Card tocável com o nome do bairro sugerido e a frase de vantagem (por que
/// vale a pena se hospedar ali, pro budget escolhido). Substitui o antigo
/// `_OptionChip` nessa etapa porque precisa de espaço para duas linhas de
/// texto — mesmo padrão de card inteiro tocável de `_DestinationTile`.
class _NeighborhoodSuggestionCard extends StatelessWidget {
  final NeighborhoodRecommendation neighborhood;
  final bool selected;
  final VoidCallback onTap;

  const _NeighborhoodSuggestionCard({
    required this.neighborhood,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
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
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              Icons.location_on_outlined,
              size: 18,
              color: selected
                  ? AppTheme.primaryBlue
                  : AppTheme.softBlack.withValues(alpha: 0.5),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    neighborhood.name,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: selected
                              ? AppTheme.primaryBlue
                              : AppTheme.softBlack,
                        ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    neighborhood.advantage,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppTheme.softBlack.withValues(alpha: 0.45),
                        ),
                  ),
                ],
              ),
            ),
          ],
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
  business('Trabalho + lazer', Icons.work_outline_rounded, 'BUSINESS'),
  silence('Silêncio & Contemplação', Icons.self_improvement_outlined, 'SILENCE');

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
  final bool hideSolo;
  const _StepCompanions({
    required this.selected,
    required this.onSelect,
    this.hideSolo = false,
  });

  @override
  Widget build(BuildContext context) {
    final options = hideSolo
        ? _Companions.values.where((c) => c != _Companions.solo)
        : _Companions.values;
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: options
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
              style: GoogleFonts.jost(
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
