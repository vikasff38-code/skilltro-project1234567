package org.skilltro;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.skilltro.Domain.*;

class MatchingRulesTest {
  @Test void calculatesRequiredSkillOverlap() {
    assertEquals(50, MatchingRules.requiredSkillOverlap(1, 2));
    assertEquals(67, MatchingRules.requiredSkillOverlap(2, 3));
  }

  @Test void handlesNoRequiredSkillsAndZeroOverlap() {
    assertEquals(100, MatchingRules.requiredSkillOverlap(0, 0));
    assertEquals(0, MatchingRules.requiredSkillOverlap(0, 3));
  }

  @Test void explainsSkillExperienceEvidenceAndLocationFit() {
    Job job=new Job();job.minimumExperienceYears=4;job.location="Hyderabad";
    Skill electrical=new Skill();electrical.id=1L;electrical.name="Electrical wiring";
    Skill plumbing=new Skill();plumbing.id=2L;plumbing.name="Plumbing";
    Skill solar=new Skill();solar.id=3L;solar.name="Solar panel installation";
    JobSkill requiredOne=new JobSkill();requiredOne.skill=electrical;requiredOne.requirementType="REQUIRED";
    JobSkill requiredTwo=new JobSkill();requiredTwo.skill=plumbing;requiredTwo.requirementType="REQUIRED";
    JobSkill preferred=new JobSkill();preferred.skill=solar;preferred.requirementType="PREFERRED";
    WorkerProfile worker=new WorkerProfile();worker.experienceYears=2;worker.location="Hyderabad";
    WorkerSkill evidence=new WorkerSkill();evidence.skill=electrical;evidence.state="CONFIRMED";evidence.evidence="Fictional evidence";
    WorkerSkill preferredEvidence=new WorkerSkill();preferredEvidence.skill=solar;preferredEvidence.state="CONFIRMED";preferredEvidence.evidence="Preferred demo evidence";

    MatchingRules.Evaluation result=MatchingRules.evaluate(job,worker,List.of(requiredOne,requiredTwo,preferred),List.of(evidence,preferredEvidence));

    assertEquals(55,result.matchPercentage());
    assertEquals(List.of("Electrical wiring"),result.matchedSkills());
    assertEquals(List.of("Plumbing"),result.missingSkills());
    assertEquals("Fictional evidence",result.relevantEvidence().get("Electrical wiring"));
    assertEquals("Preferred demo evidence",result.relevantEvidence().get("Solar panel installation"));
    assertFalse(result.experienceRequirementMet());
    assertTrue(result.locationMatched());
    assertTrue(result.explanation().contains("Matched 1 of 2 required skills"));
    assertTrue(result.explanation().contains("evidence is available for 1 matched required skill(s)"));
  }
}
