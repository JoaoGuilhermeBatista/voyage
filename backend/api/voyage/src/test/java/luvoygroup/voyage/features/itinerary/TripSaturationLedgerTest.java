package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes "standalone" (sem Mockito/Spring) da classe pura
 * TripSaturationLedger — mesmo padrão de AdminServiceTest/LlmServiceTest.
 */
class TripSaturationLedgerTest {

    private final Destination destination = Destination.builder()
        .id(UUID.randomUUID()).name("Paris").city("Paris").country("França")
        .slug("paris").tagline("").active(true).build();

    private Poi poi(PoiCategory category, boolean mustSee) {
        return Poi.builder()
            .id(UUID.randomUUID()).destination(destination)
            .title("POI").category(category).budgetTier(BudgetTier.MODERATE)
            .active(true).mustSee(mustSee).build();
    }

    @Test
    void categoryPenalty_shouldBeZero_whenCategoryNeverRecorded() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        assertEquals(0, ledger.categoryPenalty(PoiCategory.MUSEUM));
    }

    @Test
    void categoryPenalty_shouldGrowLinearly_withEachRecordedOccurrence() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        ledger.record(poi(PoiCategory.MUSEUM, false));
        assertEquals(2, ledger.categoryPenalty(PoiCategory.MUSEUM));

        ledger.record(poi(PoiCategory.MUSEUM, false));
        assertEquals(4, ledger.categoryPenalty(PoiCategory.MUSEUM));

        ledger.record(poi(PoiCategory.MUSEUM, false));
        assertEquals(6, ledger.categoryPenalty(PoiCategory.MUSEUM));
    }

    @Test
    void categoryPenalty_shouldBeCapped_atTen() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        for (int i = 0; i < 10; i++) ledger.record(poi(PoiCategory.MUSEUM, false));
        assertEquals(10, ledger.categoryPenalty(PoiCategory.MUSEUM));
    }

    @Test
    void categoryPenalty_shouldBeIndependent_perCategory() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        ledger.record(poi(PoiCategory.MUSEUM, false));
        ledger.record(poi(PoiCategory.MUSEUM, false));

        assertEquals(4, ledger.categoryPenalty(PoiCategory.MUSEUM));
        assertEquals(0, ledger.categoryPenalty(PoiCategory.RESTAURANT));
    }

    @Test
    void isMustSeeSaturated_shouldBeFalse_whenSampleIsTooSmall() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        for (int i = 0; i < 3; i++) ledger.record(poi(PoiCategory.MUSEUM, true));
        assertFalse(ledger.isMustSeeSaturated(), "Amostra < 4 não deve disparar saturação, mesmo 100% mustSee");
    }

    @Test
    void isMustSeeSaturated_shouldBeFalse_whenRatioIsNotAboveHalf() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        ledger.record(poi(PoiCategory.MUSEUM, true));
        ledger.record(poi(PoiCategory.MUSEUM, true));
        ledger.record(poi(PoiCategory.RESTAURANT, false));
        ledger.record(poi(PoiCategory.RESTAURANT, false));
        assertFalse(ledger.isMustSeeSaturated(), "Exatamente 50% não deve contar como saturado (precisa ser > 50%)");
    }

    @Test
    void isMustSeeSaturated_shouldBeTrue_whenSampleIsSufficientAndMajorityMustSee() {
        TripSaturationLedger ledger = new TripSaturationLedger();
        ledger.record(poi(PoiCategory.MUSEUM, true));
        ledger.record(poi(PoiCategory.MUSEUM, true));
        ledger.record(poi(PoiCategory.MUSEUM, true));
        ledger.record(poi(PoiCategory.RESTAURANT, false));
        assertTrue(ledger.isMustSeeSaturated());
    }
}
