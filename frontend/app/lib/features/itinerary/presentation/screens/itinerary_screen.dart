import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:printing/printing.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../currency/presentation/providers/currency_provider.dart';
import '../../../maps/presentation/widgets/open_in_maps_button.dart';
import '../../../maps/presentation/widgets/route_info_bar.dart';
import '../../../pdf/data/pdf_repository.dart';
import '../../../pdf/pdf_generator.dart';
import '../../data/itinerary_models.dart';
import '../../data/itinerary_repository.dart';
import '../providers/itinerary_provider.dart';

class ItineraryScreen extends ConsumerWidget {
  final String id;
  const ItineraryScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncValue = ref.watch(itineraryDetailProvider(id));

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: asyncValue.when(
        loading: () => const CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: AppTheme.primaryBlue),
              ),
            ),
          ],
        ),
        error: (_, _) => CustomScrollView(
          slivers: [
            SliverFillRemaining(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'Não foi possível carregar o roteiro.',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.jost(
                          fontSize: 15,
                          color: AppTheme.softBlack,
                        ),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () =>
                            ref.invalidate(itineraryDetailProvider(id)),
                        child: const Text('Tentar novamente'),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
        data: (itinerary) => _ItineraryBody(itinerary: itinerary),
      ),
    );
  }
}

// ── Body ──────────────────────────────────────────────────────────────────────

class _ItineraryBody extends StatelessWidget {
  final ItineraryModel itinerary;
  const _ItineraryBody({required this.itinerary});

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
  static const _weekdays = [
    '',
    'Seg',
    'Ter',
    'Qua',
    'Qui',
    'Sex',
    'Sáb',
    'Dom',
  ];

  String _dayLabel(ItineraryDayModel day) {
    final d = day.date;
    final weekday = _weekdays[d.weekday];
    final month = _months[d.month];
    return 'Dia ${day.dayNumber} — $weekday, ${d.day} de $month';
  }

  String _dateRange() {
    final s = itinerary.startDate;
    final e = itinerary.endDate;
    final dayCount = e.difference(s).inDays + 1;
    final label = dayCount == 1 ? '1 dia' : '$dayCount dias';
    if (s.month == e.month) {
      return '${s.day}–${e.day} ${_months[s.month]} · $label';
    }
    return '${s.day} ${_months[s.month]} – ${e.day} ${_months[e.month]} · $label';
  }

  @override
  Widget build(BuildContext context) {
    final days =
        (List.of(itinerary.days)
              ..sort((a, b) => a.dayNumber.compareTo(b.dayNumber)))
            .where((d) => d.activities.isNotEmpty)
            .toList();

    return CustomScrollView(
      slivers: [
        _HeroAppBar(itinerary: itinerary),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  itinerary.title,
                  style: GoogleFonts.cormorantGaramond(
                    fontSize: 32,
                    fontWeight: FontWeight.w500,
                    color: AppTheme.softBlack,
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _dateRange(),
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTheme.softBlack.withValues(alpha: 0.5),
                  ),
                ),
                const SizedBox(height: 12),
                const _CurrencyRatesBar(),
              ],
            ),
          ),
        ),
        for (final day in days) ...[
          SliverToBoxAdapter(child: _DayHeader(label: _dayLabel(day))),
          for (final slot in const ['MORNING', 'AFTERNOON', 'EVENING'])
            if (day.activitiesForSlot(slot).isNotEmpty)
              _SlotSection(
                itineraryId: itinerary.id,
                slot: slot,
                activities: day.activitiesForSlot(slot),
              ),
        ],
        const SliverToBoxAdapter(child: SizedBox(height: 48)),
      ],
    );
  }
}

// ── Currency rates bar ───────────────────────────────────────────────────────
//
// Indicador discreto do câmbio do dia (EUR/USD/GBP → BRL), útil para
// orçamento de viagem. Fallback gracioso: enquanto carrega ou se a chamada
// falhar, a seção simplesmente não aparece — nunca quebra a tela.

class _CurrencyRatesBar extends ConsumerWidget {
  const _CurrencyRatesBar();

  String _fmt(double value) => value.toStringAsFixed(2).replaceAll('.', ',');

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ratesAsync = ref.watch(currencyRatesProvider);

    return ratesAsync.when(
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const SizedBox.shrink(),
      data: (rates) {
        final style = Theme.of(context).textTheme.bodySmall?.copyWith(
          color: AppTheme.softBlack.withValues(alpha: 0.5),
        );
        return Row(
          children: [
            Icon(
              Icons.currency_exchange,
              size: 13,
              color: AppTheme.softBlack.withValues(alpha: 0.35),
            ),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                'Câmbio hoje: 1 EUR = R\$ ${_fmt(rates.eurToBrl)} · '
                '1 USD = R\$ ${_fmt(rates.usdToBrl)} · '
                '1 GBP = R\$ ${_fmt(rates.gbpToBrl)}',
                style: style,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        );
      },
    );
  }
}

// ── Hero app bar ──────────────────────────────────────────────────────────────

class _HeroAppBar extends ConsumerWidget {
  final ItineraryModel itinerary;
  const _HeroAppBar({required this.itinerary});

  Future<void> _confirmDelete(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Excluir roteiro'),
        content: const Text(
          'Tem certeza que deseja excluir este roteiro? Esta ação não pode ser desfeita.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Excluir'),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    try {
      await ref.read(itineraryRepositoryProvider).delete(itinerary.id);
      ref.invalidate(itinerariesProvider);
      if (context.mounted) context.pop();
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Não foi possível excluir o roteiro.')),
        );
      }
    }
  }

  /// PdfRepository.confirmExport só autoriza e conta (1ª grátis, depois
  /// premium ilimitado ou 1 crédito avulso via Pix) — o PDF em si é
  /// montado aqui no cliente a partir do [itinerary] já carregado na
  /// tela, sem precisar de nova chamada ao backend pro conteúdo.
  Future<void> _exportPdf(BuildContext context, WidgetRef ref) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(pdfRepositoryProvider).confirmExport(itinerary.id);
      final bytes = await ItineraryPdfGenerator.generate(itinerary);
      await Printing.sharePdf(bytes: bytes, filename: '${itinerary.title}.pdf');
    } catch (e) {
      if (!context.mounted) return;
      final limitMessage = pdfLimitMessage(e);
      if (limitMessage != null) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(limitMessage),
            action: SnackBarAction(
              label: 'Ver planos',
              onPressed: () => context.push('/premium'),
            ),
          ),
        );
      } else {
        messenger.showSnackBar(
          const SnackBar(
            content: Text('Não foi possível exportar o PDF. Tente novamente.'),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final hasImage = itinerary.destinationHeroImageUrl.isNotEmpty;

    return SliverAppBar(
      expandedHeight: 240,
      pinned: true,
      backgroundColor: AppTheme.background,
      elevation: 0,
      scrolledUnderElevation: 0,
      leading: Padding(
        padding: const EdgeInsets.all(8),
        child: CircleAvatar(
          backgroundColor: Colors.black.withValues(alpha: 0.35),
          child: IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white, size: 20),
            padding: EdgeInsets.zero,
            onPressed: () => context.pop(),
          ),
        ),
      ),
      actions: [
        Padding(
          padding: const EdgeInsets.all(8),
          child: CircleAvatar(
            backgroundColor: Colors.black.withValues(alpha: 0.35),
            child: PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, color: Colors.white, size: 20),
              padding: EdgeInsets.zero,
              color: AppTheme.background,
              onSelected: (value) {
                if (value == 'delete') _confirmDelete(context, ref);
                if (value == 'export') _exportPdf(context, ref);
              },
              itemBuilder: (_) => const [
                PopupMenuItem(
                  value: 'export',
                  child: Row(
                    children: [
                      Icon(Icons.picture_as_pdf_outlined, size: 18),
                      SizedBox(width: 8),
                      Text('Exportar PDF'),
                    ],
                  ),
                ),
                PopupMenuItem(
                  value: 'delete',
                  child: Row(
                    children: [
                      Icon(Icons.delete_outline, color: Colors.red, size: 18),
                      SizedBox(width: 8),
                      Text(
                        'Excluir roteiro',
                        style: TextStyle(color: Colors.red),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
      flexibleSpace: FlexibleSpaceBar(
        collapseMode: CollapseMode.parallax,
        background: Stack(
          fit: StackFit.expand,
          children: [
            if (hasImage)
              CachedNetworkImage(
                imageUrl: itinerary.destinationHeroImageUrl,
                fit: BoxFit.cover,
                placeholder: (_, _) =>
                    const ColoredBox(color: Color(0xFF1A252F)),
                errorWidget: (_, _, _) =>
                    const ColoredBox(color: Color(0xFF1A252F)),
              )
            else
              const ColoredBox(color: Color(0xFF1A252F)),
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              height: 100,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      AppTheme.background,
                      AppTheme.background.withValues(alpha: 0),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Day header ────────────────────────────────────────────────────────────────

class _DayHeader extends StatelessWidget {
  final String label;
  const _DayHeader({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: GoogleFonts.cormorantGaramond(
              fontSize: 24,
              fontWeight: FontWeight.w500,
              color: AppTheme.foreground,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 8),
          const Divider(),
        ],
      ),
    );
  }
}

// ── Slot section ──────────────────────────────────────────────────────────────

class _SlotSection extends StatelessWidget {
  final String itineraryId;
  final String slot;
  final List<ItineraryActivityModel> activities;
  const _SlotSection({
    required this.itineraryId,
    required this.slot,
    required this.activities,
  });

  static const _slotLabels = {
    'MORNING': 'Manhã',
    'AFTERNOON': 'Tarde',
    'EVENING': 'Noite',
  };

  // Glyphs tipográficos (spec §6) em vez de ícone Material — ☀︎ manhã, ◐
  // tarde, ☾ noite.
  static const _slotGlyphs = {
    'MORNING': '☀︎',
    'AFTERNOON': '◐',
    'EVENING': '☾',
  };

  @override
  Widget build(BuildContext context) {
    // A Noite ganha o tratamento "Noir" (fundo escuro, dourado de verdade)
    // — mesmo padrão do spec de design (§6): os 3 períodos usam o mesmo
    // "bloco com borda", só a Noite inverte pra bg-ink/gold-noir.
    final isNoir = slot == 'EVENING';
    final bg = isNoir ? AppTheme.ink : AppTheme.card;
    final borderColor = isNoir
        ? AppTheme.goldNoir.withValues(alpha: 0.25)
        : AppTheme.border;
    final labelColor = isNoir ? AppTheme.goldNoir : AppTheme.mutedForeground;

    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: bg,
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: borderColor),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  border: Border(bottom: BorderSide(color: borderColor)),
                ),
                child: Row(
                  children: [
                    Text(
                      _slotGlyphs[slot] ?? '•',
                      style: GoogleFonts.cormorantGaramond(
                        fontSize: 18,
                        color: labelColor,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      (_slotLabels[slot] ?? slot).toUpperCase(),
                      style: AppTheme.eyebrow(color: labelColor),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    for (var i = 0; i < activities.length; i++) ...[
                      _ActivityTile(
                        itineraryId: itineraryId,
                        activity: activities[i],
                        isNoir: isNoir,
                      ),
                      if (i < activities.length - 1)
                        _RouteBetweenActivities(
                          from: activities[i].poi,
                          to: activities[i + 1].poi,
                        ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// Distância/duração de rota entre duas atividades consecutivas do mesmo
// slot, via RouteInfoBar (lib/features/maps/...). Some graciosamente (sem
// renderizar nada) quando falta lat/lng em algum dos dois POIs — mesmo
// espírito de fallback do resto da tela (_CurrencyRatesBar, OpenInMapsButton).
class _RouteBetweenActivities extends StatelessWidget {
  final PoiDetailModel from;
  final PoiDetailModel to;
  const _RouteBetweenActivities({required this.from, required this.to});

  @override
  Widget build(BuildContext context) {
    final originLat = from.latitude;
    final originLng = from.longitude;
    final destLat = to.latitude;
    final destLng = to.longitude;
    if (originLat == null ||
        originLng == null ||
        destLat == null ||
        destLng == null) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.only(left: 4, top: 6, bottom: 14),
      child: RouteInfoBar(
        originLat: originLat,
        originLng: originLng,
        destLat: destLat,
        destLng: destLng,
      ),
    );
  }
}

// ── Activity tile ─────────────────────────────────────────────────────────────

class _ActivityTile extends ConsumerWidget {
  final String itineraryId;
  final ItineraryActivityModel activity;
  final bool isNoir;
  const _ActivityTile({
    required this.itineraryId,
    required this.activity,
    this.isNoir = false,
  });

  static const _categoryLabels = {
    'MUSEUM': 'Museu',
    'RESTAURANT': 'Restaurante',
    'HOTEL': 'Hotel',
    'BEACH': 'Praia',
    'PARK': 'Parque',
    'SHOPPING': 'Shopping',
    'NIGHTLIFE': 'Vida Noturna',
  };

  static const _budgetLabels = {
    'ECONOMY': '€',
    'MODERATE': '€€',
    'LUXURY': '€€€',
  };

  void _openEditSheet(BuildContext context, WidgetRef ref) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.background,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) =>
          _AlternativesSheet(itineraryId: itineraryId, activity: activity),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final poi = activity.poi;
    final categoryLabel = _categoryLabels[poi.category] ?? poi.category;
    final budgetLabel = _budgetLabels[poi.budgetTier] ?? '';
    final duration = poi.averageDurationMinutes != null
        ? '${poi.averageDurationMinutes} min'
        : null;

    // Paleta local: dentro do bloco "Noite" (Noir), tudo inverte pra
    // fundo escuro/texto ivory/acento dourado de verdade — fora dele
    // (Manhã/Tarde), segue o tema claro normal.
    final accent = isNoir ? AppTheme.goldNoir : AppTheme.primary;
    final textColor = isNoir ? AppTheme.ivory : AppTheme.foreground;
    final mutedColor = isNoir
        ? AppTheme.ivory.withValues(alpha: 0.5)
        : AppTheme.mutedForeground;
    final faintColor = isNoir
        ? AppTheme.ivory.withValues(alpha: 0.35)
        : AppTheme.mutedForeground.withValues(alpha: 0.7);

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Order indicator
          Container(
            width: 28,
            height: 28,
            margin: const EdgeInsets.only(top: 2),
            decoration: BoxDecoration(
              color: accent.withValues(alpha: isNoir ? 0.15 : 0.08),
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                '${activity.orderInSlot}',
                style: GoogleFonts.jost(
                  fontSize: 11,
                  color: accent,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),

          // Thumbnail
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: poi.images.isNotEmpty
                ? CachedNetworkImage(
                    imageUrl: poi.images.first,
                    width: 56,
                    height: 56,
                    fit: BoxFit.cover,
                    placeholder: (_, _) => ColoredBox(
                      color: AppTheme.shimmerBase,
                      child: const SizedBox(width: 56, height: 56),
                    ),
                    errorWidget: (_, _, _) => ColoredBox(
                      color: AppTheme.shimmerBase,
                      child: SizedBox(
                        width: 56,
                        height: 56,
                        child: Icon(Icons.image_outlined, color: faintColor),
                      ),
                    ),
                  )
                : ColoredBox(
                    color: isNoir ? AppTheme.navy : AppTheme.shimmerBase,
                    child: SizedBox(
                      width: 56,
                      height: 56,
                      child: Icon(Icons.image_outlined, color: faintColor),
                    ),
                  ),
          ),
          const SizedBox(width: 12),

          // Content
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  poi.title,
                  style: GoogleFonts.jost(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: textColor,
                  ),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Text(
                      categoryLabel,
                      style: GoogleFonts.jost(fontSize: 12, color: mutedColor),
                    ),
                    if (budgetLabel.isNotEmpty) ...[
                      Text(
                        ' · ',
                        style: GoogleFonts.jost(
                          fontSize: 12,
                          color: faintColor,
                        ),
                      ),
                      Text(
                        budgetLabel,
                        style: GoogleFonts.jost(
                          fontSize: 12,
                          color: accent,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                    if (duration != null) ...[
                      Text(
                        ' · ',
                        style: GoogleFonts.jost(
                          fontSize: 12,
                          color: faintColor,
                        ),
                      ),
                      Text(
                        duration,
                        style: GoogleFonts.jost(
                          fontSize: 12,
                          color: mutedColor,
                        ),
                      ),
                    ],
                  ],
                ),
                if (poi.description != null && poi.description!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    poi.description!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: GoogleFonts.jost(
                      fontSize: 12,
                      fontWeight: FontWeight.w300,
                      color: mutedColor,
                      height: 1.5,
                    ),
                  ),
                ],
                if (poi.address != null && poi.address!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(
                        Icons.location_on_outlined,
                        size: 12,
                        color: faintColor,
                      ),
                      const SizedBox(width: 3),
                      Expanded(
                        child: Text(
                          poi.address!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: GoogleFonts.jost(
                            fontSize: 12,
                            color: faintColor,
                          ),
                        ),
                      ),
                      if (poi.latitude != null && poi.longitude != null)
                        OpenInMapsButton(
                          lat: poi.latitude!,
                          lng: poi.longitude!,
                        ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          IconButton(
            icon: Icon(Icons.edit_outlined, size: 20, color: mutedColor),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(),
            onPressed: () => _openEditSheet(context, ref),
          ),
        ],
      ),
    );
  }
}

// ── Alternatives bottom sheet ────────────────────────────────────────────────
//
// Aberto ao tocar no ícone de editar de um _ActivityTile. Busca até 3 POIs
// alternativos da mesma categoria e permite trocar o POI da atividade.
//
// Decisões de UX confirmadas:
// 1. Erro ao buscar alternativas → mensagem de erro dentro do próprio sheet
//    (não fecha), com botão "Tentar novamente".
// 2. Seleção de alternativa → o card inteiro é tocável (tap já seleciona e
//    dispara a troca, sem botão "Escolher" separado).
// 3. Erro ao trocar (substitute) → mantém o sheet aberto, mostra snackbar de
//    erro, permite tentar de novo sem refazer a busca.

class _AlternativesSheet extends ConsumerStatefulWidget {
  final String itineraryId;
  final ItineraryActivityModel activity;
  const _AlternativesSheet({required this.itineraryId, required this.activity});

  @override
  ConsumerState<_AlternativesSheet> createState() => _AlternativesSheetState();
}

class _AlternativesSheetState extends ConsumerState<_AlternativesSheet> {
  bool _loading = true;
  String? _error;
  List<PoiDetailModel> _alternatives = const [];
  String? _substitutingPoiId;

  @override
  void initState() {
    super.initState();
    _loadAlternatives();
  }

  Future<void> _loadAlternatives() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await ref
          .read(itineraryRepositoryProvider)
          .getAlternatives(widget.itineraryId, widget.activity.id);
      if (!mounted) return;
      setState(() {
        _alternatives = result;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Não foi possível carregar as alternativas.';
        _loading = false;
      });
    }
  }

  Future<void> _selectAlternative(PoiDetailModel poi) async {
    if (_substitutingPoiId != null) return;
    setState(() => _substitutingPoiId = poi.id);
    try {
      await ref
          .read(itineraryRepositoryProvider)
          .substituteActivity(widget.itineraryId, widget.activity.id, poi.id);
      if (!mounted) return;
      ref.invalidate(itineraryDetailProvider(widget.itineraryId));
      Navigator.of(context).pop();
    } catch (_) {
      if (!mounted) return;
      setState(() => _substitutingPoiId = null);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Não foi possível trocar o local.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Trocar local',
              style: GoogleFonts.cormorantGaramond(
                fontSize: 24,
                fontWeight: FontWeight.w500,
                color: AppTheme.softBlack,
              ),
            ),
            const SizedBox(height: 16),
            _buildContent(context),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context) {
    if (_loading) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 32),
        child: Center(
          child: CircularProgressIndicator(color: AppTheme.primaryBlue),
        ),
      );
    }

    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: GoogleFonts.jost(fontSize: 15, color: AppTheme.softBlack),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadAlternatives,
              child: const Text('Tentar novamente'),
            ),
          ],
        ),
      );
    }

    if (_alternatives.isEmpty) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 24),
        child: Text(
          'Nenhuma alternativa encontrada para esta categoria.',
          textAlign: TextAlign.center,
          style: GoogleFonts.jost(
            fontSize: 15,
            color: AppTheme.softBlack.withValues(alpha: 0.6),
          ),
        ),
      );
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (final poi in _alternatives)
          _AlternativeCard(
            poi: poi,
            isSubstituting: _substitutingPoiId == poi.id,
            disabled: _substitutingPoiId != null,
            onTap: () => _selectAlternative(poi),
          ),
      ],
    );
  }
}

class _AlternativeCard extends StatelessWidget {
  final PoiDetailModel poi;
  final bool isSubstituting;
  final bool disabled;
  final VoidCallback onTap;

  static const _categoryLabels = {
    'MUSEUM': 'Museu',
    'RESTAURANT': 'Restaurante',
    'HOTEL': 'Hotel',
    'BEACH': 'Praia',
    'PARK': 'Parque',
    'SHOPPING': 'Shopping',
    'NIGHTLIFE': 'Vida Noturna',
  };

  const _AlternativeCard({
    required this.poi,
    required this.isSubstituting,
    required this.disabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final categoryLabel = _categoryLabels[poi.category] ?? poi.category;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: AppTheme.softBlack.withValues(alpha: 0.03),
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: disabled ? null : onTap,
          child: Opacity(
            opacity: disabled && !isSubstituting ? 0.5 : 1,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: poi.images.isNotEmpty
                        ? CachedNetworkImage(
                            imageUrl: poi.images.first,
                            width: 56,
                            height: 56,
                            fit: BoxFit.cover,
                            placeholder: (_, _) => ColoredBox(
                              color: AppTheme.shimmerBase,
                              child: const SizedBox(width: 56, height: 56),
                            ),
                            errorWidget: (_, _, _) => ColoredBox(
                              color: AppTheme.shimmerBase,
                              child: SizedBox(
                                width: 56,
                                height: 56,
                                child: Icon(
                                  Icons.image_outlined,
                                  color: AppTheme.softBlack.withValues(
                                    alpha: 0.3,
                                  ),
                                ),
                              ),
                            ),
                          )
                        : ColoredBox(
                            color: AppTheme.shimmerBase,
                            child: SizedBox(
                              width: 56,
                              height: 56,
                              child: Icon(
                                Icons.image_outlined,
                                color: AppTheme.softBlack.withValues(
                                  alpha: 0.3,
                                ),
                              ),
                            ),
                          ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          poi.title,
                          style: Theme.of(context).textTheme.bodyLarge
                              ?.copyWith(
                                fontWeight: FontWeight.w600,
                                color: AppTheme.softBlack,
                              ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          categoryLabel,
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: AppTheme.softBlack.withValues(
                                  alpha: 0.5,
                                ),
                              ),
                        ),
                      ],
                    ),
                  ),
                  if (isSubstituting)
                    const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AppTheme.primaryBlue,
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
