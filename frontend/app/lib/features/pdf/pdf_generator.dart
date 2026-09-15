import 'dart:typed_data';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import '../itinerary/data/itinerary_models.dart';

/// Gera o PDF do roteiro inteiramente no cliente, a partir do
/// [ItineraryModel] já carregado na tela — o backend só autoriza e conta
/// (ver PdfRepository.confirmExport), nunca guarda nem monta o arquivo.
/// Mesmos rótulos/ordem de dia e slot já usados em itinerary_screen.dart
/// (_ItineraryBody/_SlotSection), pra o PDF bater com o que a pessoa vê
/// no app.
class ItineraryPdfGenerator {
  static const _months = [
    '',
    'janeiro',
    'fevereiro',
    'março',
    'abril',
    'maio',
    'junho',
    'julho',
    'agosto',
    'setembro',
    'outubro',
    'novembro',
    'dezembro',
  ];
  static const _weekdays = [
    '',
    'segunda-feira',
    'terça-feira',
    'quarta-feira',
    'quinta-feira',
    'sexta-feira',
    'sábado',
    'domingo',
  ];
  static const _slotLabels = {
    'MORNING': 'Manhã',
    'AFTERNOON': 'Tarde',
    'EVENING': 'Noite',
  };
  static const _slotOrder = ['MORNING', 'AFTERNOON', 'EVENING'];

  static String _dayLabel(ItineraryDayModel day) {
    final d = day.date;
    // Hífen, não travessão "—": a fonte padrão do PDF (Helvetica, sem
    // fonte customizada embutida) só cobre Latin-1 — um travessão real
    // simplesmente não desenha nenhum glyph no arquivo final.
    return 'Dia ${day.dayNumber} - ${_weekdays[d.weekday]}, ${d.day} de ${_months[d.month]}';
  }

  static String _dateRangeLabel(ItineraryModel itinerary) {
    final s = itinerary.startDate;
    final e = itinerary.endDate;
    final dayCount = e.difference(s).inDays + 1;
    final label = dayCount == 1 ? '1 dia' : '$dayCount dias';
    return '${s.day} de ${_months[s.month]} a ${e.day} de ${_months[e.month]} · $label';
  }

  static Future<Uint8List> generate(ItineraryModel itinerary) async {
    final doc = pw.Document(title: itinerary.title, author: 'LUVOY');

    final days =
        (List.of(itinerary.days)
              ..sort((a, b) => a.dayNumber.compareTo(b.dayNumber)))
            .where((d) => d.activities.isNotEmpty)
            .toList();

    doc.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.fromLTRB(36, 48, 36, 36),
        header: (context) => context.pageNumber == 1
            ? pw.SizedBox.shrink()
            : pw.Container(
                alignment: pw.Alignment.centerRight,
                margin: const pw.EdgeInsets.only(bottom: 12),
                child: pw.Text(
                  itinerary.title,
                  style: const pw.TextStyle(
                    fontSize: 9,
                    color: PdfColors.grey500,
                  ),
                ),
              ),
        footer: (context) => pw.Container(
          alignment: pw.Alignment.centerRight,
          margin: const pw.EdgeInsets.only(top: 8),
          child: pw.Text(
            '${context.pageNumber} / ${context.pagesCount}',
            style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey500),
          ),
        ),
        build: (context) => [
          pw.Text(
            itinerary.title,
            style: pw.TextStyle(
              fontSize: 26,
              fontWeight: pw.FontWeight.bold,
              color: PdfColors.blueGrey900,
            ),
          ),
          pw.SizedBox(height: 4),
          pw.Text(
            itinerary.destinationName,
            style: const pw.TextStyle(fontSize: 14, color: PdfColors.grey700),
          ),
          pw.SizedBox(height: 2),
          pw.Text(
            _dateRangeLabel(itinerary),
            style: const pw.TextStyle(fontSize: 11, color: PdfColors.grey600),
          ),
          pw.SizedBox(height: 20),
          pw.Divider(color: PdfColors.grey300),
          for (final day in days) ...[
            pw.SizedBox(height: 16),
            pw.Text(
              _dayLabel(day),
              style: pw.TextStyle(
                fontSize: 15,
                fontWeight: pw.FontWeight.bold,
                color: PdfColors.blueGrey900,
              ),
            ),
            pw.SizedBox(height: 10),
            for (final slot in _slotOrder)
              if (day.activitiesForSlot(slot).isNotEmpty)
                pw.Padding(
                  padding: const pw.EdgeInsets.only(bottom: 10),
                  child: pw.Column(
                    crossAxisAlignment: pw.CrossAxisAlignment.start,
                    children: [
                      pw.Text(
                        _slotLabels[slot]!.toUpperCase(),
                        style: pw.TextStyle(
                          fontSize: 10,
                          fontWeight: pw.FontWeight.bold,
                          color: PdfColors.amber800,
                          letterSpacing: 1.2,
                        ),
                      ),
                      pw.SizedBox(height: 6),
                      for (final activity in day.activitiesForSlot(slot))
                        pw.Padding(
                          padding: const pw.EdgeInsets.only(left: 8, bottom: 8),
                          child: pw.Column(
                            crossAxisAlignment: pw.CrossAxisAlignment.start,
                            children: [
                              pw.Text(
                                activity.poi.title,
                                style: pw.TextStyle(
                                  fontSize: 12,
                                  fontWeight: pw.FontWeight.bold,
                                ),
                              ),
                              if (activity.poi.address != null &&
                                  activity.poi.address!.isNotEmpty)
                                pw.Text(
                                  activity.poi.address!,
                                  style: const pw.TextStyle(
                                    fontSize: 10,
                                    color: PdfColors.grey600,
                                  ),
                                ),
                              if (activity.poi.description != null &&
                                  activity.poi.description!.isNotEmpty)
                                pw.Padding(
                                  padding: const pw.EdgeInsets.only(top: 2),
                                  child: pw.Text(
                                    activity.poi.description!,
                                    style: const pw.TextStyle(
                                      fontSize: 10,
                                      color: PdfColors.grey700,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                    ],
                  ),
                ),
          ],
          pw.SizedBox(height: 24),
          pw.Divider(color: PdfColors.grey300),
          pw.SizedBox(height: 8),
          pw.Text(
            'Gerado pelo LUVOY',
            style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey500),
          ),
        ],
      ),
    );

    return doc.save();
  }
}
