// Testes de ItineraryPdfGenerator — gera o PDF inteiramente no cliente a
// partir do ItineraryModel já carregado (o backend só autoriza/conta, ver
// PdfRepository.confirmExport). Não valida o layout visual (fora do
// escopo de um teste automatizado) — só que o gerador produz bytes de PDF
// válidos e não quebra com dados incompletos (POI sem endereço/descrição,
// dia sem atividades).

import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/pdf/pdf_generator.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const poiCompleto = PoiDetailModel(
    id: 'poi-1',
    title: 'Museu de Arte Moderna',
    category: 'MUSEUM',
    budgetTier: 'MODERATE',
    address: 'Av. Paulista, 1578',
    description: 'Um dos principais museus de arte moderna do país.',
  );

  const poiSemAddressNemDescricao = PoiDetailModel(
    id: 'poi-2',
    title: 'Feira de Rua',
    category: 'ACTIVITY',
    budgetTier: 'BUDGET',
  );

  ItineraryModel sampleItinerary() {
    return ItineraryModel(
      id: 'itin-1',
      title: 'Viagem a São Paulo',
      destinationName: 'São Paulo',
      destinationHeroImageUrl: '',
      startDate: DateTime(2026, 9, 1),
      endDate: DateTime(2026, 9, 2),
      days: [
        ItineraryDayModel(
          dayNumber: 1,
          date: DateTime(2026, 9, 1),
          activities: const [
            ItineraryActivityModel(
              id: 'act-1',
              slot: 'MORNING',
              orderInSlot: 1,
              poi: poiCompleto,
            ),
            ItineraryActivityModel(
              id: 'act-2',
              slot: 'EVENING',
              orderInSlot: 1,
              poi: poiSemAddressNemDescricao,
            ),
          ],
        ),
        // Dia sem atividade nenhuma — não deve aparecer no PDF nem quebrar
        // a geração (mesmo filtro que a tela usa: .where((d) =>
        // d.activities.isNotEmpty)).
        ItineraryDayModel(
          dayNumber: 2,
          date: DateTime(2026, 9, 2),
          activities: const [],
        ),
      ],
    );
  }

  test(
    'gera bytes de PDF válidos (assinatura %PDF) pra um roteiro com dias e slots',
    () async {
      final bytes = await ItineraryPdfGenerator.generate(sampleItinerary());

      expect(bytes, isNotEmpty);
      // Todo arquivo PDF começa com essa assinatura de 4 bytes.
      expect(String.fromCharCodes(bytes.take(4)), '%PDF');
    },
  );

  test(
    'não lança exceção quando o POI não tem address nem description',
    () async {
      await expectLater(
        ItineraryPdfGenerator.generate(sampleItinerary()),
        completes,
      );
    },
  );
}
