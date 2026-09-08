import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../destinations/data/destination_summary_model.dart';
import '../../../destinations/presentation/providers/destination_provider.dart';
import '../../data/itinerary_models.dart';
import '../../data/itinerary_repository.dart';
import '../providers/itinerary_provider.dart';

/// Tela de criação de viagem multi-destino (spec item 7 — "N cidades +
/// datas por trecho"). Decisão de escopo: cada trecho pede só destino +
/// datas (não repete o wizard inteiro de preferências por trecho) — o
/// motor de geração já cai de volta nas preferências do onboarding quando
/// esses campos opcionais não são enviados, mesmo comportamento de um
/// roteiro avulso gerado sem preencher vibe/interesses/etc.
class CreateTripScreen extends ConsumerStatefulWidget {
  const CreateTripScreen({super.key});

  @override
  ConsumerState<CreateTripScreen> createState() => _CreateTripScreenState();
}

class _LegDraft {
  DestinationSummaryModel? destination;
  DateTime? startDate;
  DateTime? endDate;
}

class _CreateTripScreenState extends ConsumerState<CreateTripScreen> {
  final List<_LegDraft> _legs = [_LegDraft(), _LegDraft()];
  bool _loading = false;
  String? _error;

  static const _months = [
    '',
    'Jan',
    'Fev',
    'Mar',
    'Abr',
    'Mai',
    'Jun',
    'Jul',
    'Ago',
    'Set',
    'Out',
    'Nov',
    'Dez',
  ];

  String _dateLabel(DateTime? d) {
    if (d == null) return 'Selecionar';
    return '${d.day} ${_months[d.month]} ${d.year}';
  }

  /// Data mínima permitida pro início deste trecho: o dia seguinte ao fim
  /// do trecho anterior (spec: "permitir folga, proibir sobreposição") —
  /// ou hoje, se for o primeiro trecho ou o anterior ainda não tem data.
  DateTime _minStartFor(int index) {
    if (index == 0) return DateTime.now();
    final previousEnd = _legs[index - 1].endDate;
    if (previousEnd == null) return DateTime.now();
    return previousEnd.add(const Duration(days: 1));
  }

  bool get _canSubmit =>
      !_loading &&
      _legs.every(
        (l) =>
            l.destination != null && l.startDate != null && l.endDate != null,
      );

  Future<void> _pickDestination(int index) async {
    final selected = await showModalBottomSheet<DestinationSummaryModel>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const _DestinationPickerSheet(),
    );
    if (selected == null) return;
    setState(() {
      _legs[index].destination = selected;
    });
  }

  Future<void> _pickStart(int index) async {
    final minDate = _minStartFor(index);
    final picked = await showDatePicker(
      context: context,
      initialDate: _legs[index].startDate ?? minDate,
      firstDate: minDate,
      lastDate: minDate.add(const Duration(days: 365)),
    );
    if (picked == null) return;
    setState(() {
      _legs[index].startDate = picked;
      final end = _legs[index].endDate;
      if (end != null && !end.isAfter(picked)) _legs[index].endDate = null;
    });
  }

  Future<void> _pickEnd(int index) async {
    final start = _legs[index].startDate;
    if (start == null) return;
    final picked = await showDatePicker(
      context: context,
      initialDate: _legs[index].endDate ?? start.add(const Duration(days: 1)),
      firstDate: start.add(const Duration(days: 1)),
      lastDate: start.add(const Duration(days: 14)),
    );
    if (picked == null) return;
    setState(() => _legs[index].endDate = picked);
  }

  void _addLeg() {
    setState(() => _legs.add(_LegDraft()));
  }

  void _removeLeg(int index) {
    if (_legs.length <= 2) return;
    setState(() => _legs.removeAt(index));
  }

  Future<void> _submit() async {
    if (!_canSubmit) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final trip = await ref
          .read(itineraryRepositoryProvider)
          .createTrip(
            _legs
                .map(
                  (l) => TripLegInput(
                    destinationId: l.destination!.id,
                    startDate: l.startDate!,
                    endDate: l.endDate!,
                  ),
                )
                .toList(),
          );
      ref.invalidate(tripsProvider);

      if (mounted) {
        context.pop();
        if (trip.tripGroupId != null) {
          context.push('/roteiros/viagem/${trip.tripGroupId}');
        }
      }
    } catch (e) {
      setState(() {
        _loading = false;
        _error =
            freeTierLimitMessage(e) ??
            'Não foi possível criar a viagem. Confira se as datas não se sobrepõem e tente novamente.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: AppTheme.background,
        elevation: 0,
        title: Text(
          'Nova viagem',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 22,
            fontWeight: FontWeight.w600,
            color: AppTheme.foreground,
          ),
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 8, 24, 0),
              child: Text(
                'Monte a sequência de destinos e datas da sua viagem — pode ter folga entre os trechos, mas as datas não podem se sobrepor.',
                style: GoogleFonts.jost(
                  fontSize: 13,
                  fontWeight: FontWeight.w300,
                  color: AppTheme.mutedForeground,
                  height: 1.5,
                ),
              ),
            ),
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
                itemCount: _legs.length,
                itemBuilder: (context, index) => Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: _LegCard(
                    index: index,
                    leg: _legs[index],
                    dateLabel: _dateLabel,
                    onPickDestination: () => _pickDestination(index),
                    onPickStart: () => _pickStart(index),
                    onPickEnd: () => _pickEnd(index),
                    onRemove: _legs.length > 2 ? () => _removeLeg(index) : null,
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
              child: OutlinedButton.icon(
                onPressed: _addLeg,
                icon: const Icon(Icons.add, size: 18),
                label: const Text('Adicionar trecho'),
              ),
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 0, 24, 12),
                child: Text(
                  _error!,
                  style: GoogleFonts.jost(fontSize: 12, color: Colors.red[400]),
                  textAlign: TextAlign.center,
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
              child: SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _canSubmit ? _submit : null,
                  child: _loading
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Criar viagem'),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Leg card ─────────────────────────────────────────────────────────────────

class _LegCard extends StatelessWidget {
  final int index;
  final _LegDraft leg;
  final String Function(DateTime?) dateLabel;
  final VoidCallback onPickDestination;
  final VoidCallback onPickStart;
  final VoidCallback onPickEnd;
  final VoidCallback? onRemove;

  const _LegCard({
    required this.index,
    required this.leg,
    required this.dateLabel,
    required this.onPickDestination,
    required this.onPickStart,
    required this.onPickEnd,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.card,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Trecho ${index + 1}',
                  style: GoogleFonts.jost(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.6,
                    color: AppTheme.mutedForeground,
                  ),
                ),
              ),
              if (onRemove != null)
                GestureDetector(
                  onTap: onRemove,
                  child: Icon(
                    Icons.close_rounded,
                    size: 18,
                    color: AppTheme.mutedForeground.withValues(alpha: 0.6),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 10),
          GestureDetector(
            onTap: onPickDestination,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
              decoration: BoxDecoration(
                color: AppTheme.background,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.border),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.place_outlined,
                    size: 18,
                    color: AppTheme.mutedForeground,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      leg.destination?.name ?? 'Selecionar destino',
                      style: GoogleFonts.jost(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: leg.destination != null
                            ? AppTheme.foreground
                            : AppTheme.mutedForeground,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: _DateField(
                  label: 'Início',
                  value: dateLabel(leg.startDate),
                  onTap: onPickStart,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _DateField(
                  label: 'Fim',
                  value: dateLabel(leg.endDate),
                  onTap: leg.startDate == null ? null : onPickEnd,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _DateField extends StatelessWidget {
  final String label;
  final String value;
  final VoidCallback? onTap;

  const _DateField({
    required this.label,
    required this.value,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: AppTheme.background,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppTheme.border),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: GoogleFonts.jost(
                fontSize: 10,
                color: AppTheme.mutedForeground,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              value,
              style: GoogleFonts.jost(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: onTap == null
                    ? AppTheme.mutedForeground.withValues(alpha: 0.5)
                    : AppTheme.foreground,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Destination picker sheet ────────────────────────────────────────────────

class _DestinationPickerSheet extends ConsumerStatefulWidget {
  const _DestinationPickerSheet();

  @override
  ConsumerState<_DestinationPickerSheet> createState() =>
      _DestinationPickerSheetState();
}

class _DestinationPickerSheetState
    extends ConsumerState<_DestinationPickerSheet> {
  final _controller = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final query = _controller.text.trim();
    final asyncDestinations = query.isEmpty
        ? ref.watch(destinationsProvider)
        : ref.watch(destinationSearchProvider(query));

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Material(
        color: AppTheme.background,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: _controller,
                autofocus: true,
                decoration: InputDecoration(
                  hintText: 'Buscar cidade ou destino…',
                  prefixIcon: Icon(
                    Icons.search_rounded,
                    color: AppTheme.mutedForeground.withValues(alpha: 0.6),
                  ),
                  filled: true,
                  fillColor: AppTheme.mutedForeground.withValues(alpha: 0.06),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              ConstrainedBox(
                constraints: const BoxConstraints(maxHeight: 360),
                child: asyncDestinations.when(
                  data: (destinations) {
                    if (destinations.isEmpty) {
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 24),
                        child: Text(
                          'Nenhum destino encontrado',
                          style: GoogleFonts.jost(
                            fontSize: 13,
                            color: AppTheme.mutedForeground,
                          ),
                        ),
                      );
                    }
                    return ListView.builder(
                      shrinkWrap: true,
                      itemCount: destinations.length,
                      itemBuilder: (context, index) {
                        final destination = destinations[index];
                        return ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Text(
                            destination.name,
                            style: GoogleFonts.jost(
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                          subtitle: Text(
                            destination.country,
                            style: GoogleFonts.jost(
                              fontSize: 12,
                              color: AppTheme.mutedForeground,
                            ),
                          ),
                          onTap: () => Navigator.of(context).pop(destination),
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
                      style: GoogleFonts.jost(
                        fontSize: 13,
                        color: AppTheme.mutedForeground,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
