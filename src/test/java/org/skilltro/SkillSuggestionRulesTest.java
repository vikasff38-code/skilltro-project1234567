package org.skilltro;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillSuggestionRulesTest {
  private static final List<String> CATALOG = List.of("Carpentry", "Welding", "Electrical wiring", "Plumbing");

  @Test void suggestsSkillsFromPracticalPhrases() {
    assertEquals(List.of("Carpentry", "Plumbing"),
        SkillSuggestionRules.suggest(CATALOG, "I build furniture and repair pipe fittings", Set.of()));
  }

  @Test void excludesSkillsAlreadyReviewedRegardlessOfCase() {
    assertEquals(List.of("Plumbing"),
        SkillSuggestionRules.suggest(CATALOG, "Furniture work and pipe fitting", Set.of("cArPeNtry")));
  }

  @Test void doesNotSuggestFromAnEmptyDescription() {
    assertEquals(List.of(), SkillSuggestionRules.suggest(CATALOG, null, Set.of()));
  }

  @Test void extractsTheSentenceAsSkillEvidence() {
    assertEquals("I build furniture and repair pipe fittings.",
        SkillSuggestionRules.evidenceFor("Carpentry", "I build furniture and repair pipe fittings."));
  }
}
