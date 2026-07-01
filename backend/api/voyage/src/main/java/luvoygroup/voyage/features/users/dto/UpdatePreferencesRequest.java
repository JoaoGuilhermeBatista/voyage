package luvoygroup.voyage.features.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.GroupType;
import luvoygroup.voyage.domain.enums.TravelRhythm;

import java.util.List;

public record UpdatePreferencesRequest(
    @NotNull(message = "Budget é obrigatório")
    BudgetTier budget,

    @NotNull(message = "Rhythm é obrigatório")
    TravelRhythm rhythm,

    @NotNull(message = "GroupType é obrigatório")
    GroupType groupType,

    @NotEmpty(message = "Interests não pode ser vazio")
    List<String> interests
) {}
