package org.skilltro;

import java.util.*;
import java.util.stream.Collectors;
import static org.skilltro.Domain.*;

/** Single deterministic match calculation used by API responses and stored snapshots. */
public final class MatchingRules {
  private MatchingRules() {}

  public record Evaluation(List<String> matchedSkills, List<String> missingSkills,
      List<String> preferredSkills, Map<String,String> relevantEvidence, int matchPercentage,
      boolean experienceRequirementMet, boolean locationMatched, String explanation) {}

  public static Evaluation evaluate(Job job, WorkerProfile worker, List<JobSkill> requirements,
      List<WorkerSkill> workerSkills) {
    Set<Long> confirmed = workerSkills.stream().filter(s -> "CONFIRMED".equals(s.state))
        .map(s -> s.skill.id).collect(Collectors.toSet());
    Map<Long,String> evidenceBySkill = workerSkills.stream().filter(s -> "CONFIRMED".equals(s.state))
        .filter(s -> s.evidence != null && !s.evidence.isBlank())
        .collect(Collectors.toMap(s -> s.skill.id, s -> s.evidence, (first, ignored) -> first));
    List<JobSkill> required = requirements.stream().filter(s -> "REQUIRED".equals(s.requirementType)).toList();
    List<JobSkill> preferred = requirements.stream().filter(s -> "PREFERRED".equals(s.requirementType)).toList();
    List<String> matched = required.stream().filter(s -> confirmed.contains(s.skill.id)).map(s -> s.skill.name).toList();
    List<String> missing = required.stream().filter(s -> !confirmed.contains(s.skill.id)).map(s -> s.skill.name).toList();
    List<String> preferredNames = preferred.stream().map(s -> s.skill.name).toList();
    Map<String,String> evidence = new LinkedHashMap<>();
    for (JobSkill requirement : required) {
      String text = evidenceBySkill.get(requirement.skill.id);
      if (text != null) evidence.put(requirement.skill.name, text);
    }
    for (JobSkill requirement : preferred) {
      String text = evidenceBySkill.get(requirement.skill.id);
      if (text != null) evidence.putIfAbsent(requirement.skill.name, text);
    }

    int requiredCount = required.size();
    double skillFit = requiredCount == 0 ? 1 : (double) matched.size() / requiredCount;
    int minimumYears = Math.max(0, job.minimumExperienceYears);
    boolean experienceMet = worker.experienceYears >= minimumYears;
    double experienceFit = minimumYears == 0 ? 1 : Math.min(1, (double) worker.experienceYears / minimumYears);
    long evidenceForRequired = evidence.keySet().stream().filter(matched::contains).count();
    double evidenceFit = requiredCount == 0 ? 1 : (double) evidenceForRequired / requiredCount;
    boolean locationMatched = locationsMatch(worker.location, job.location);
    int percentage = (int) Math.round(60 * skillFit + 15 * experienceFit + 15 * evidenceFit + 10 * (locationMatched ? 1 : 0));

    String experienceNote = minimumYears == 0 ? "no minimum experience requirement" :
        (experienceMet ? "meets the " + minimumYears + "+ year experience requirement" :
            "has " + worker.experienceYears + " of " + minimumYears + " required years of experience");
    String locationNote = locationMatched ? "location matches" : "location differs from the listing";
    String explanation = "Matched " + matched.size() + " of " + requiredCount + " required skills; " +
        experienceNote + "; evidence is available for " + evidenceForRequired + " matched required skill(s); " + locationNote + ".";
    return new Evaluation(matched, missing, preferredNames, evidence, percentage, experienceMet, locationMatched, explanation);
  }

  public static int requiredSkillOverlap(int matchedRequired, int totalRequired) {
    if (totalRequired <= 0) return 100;
    return (int) Math.round(100.0 * matchedRequired / totalRequired);
  }

  private static boolean locationsMatch(String worker, String job) {
    String w = normalize(worker), j = normalize(job);
    if (w.isBlank() || j.isBlank()) return false;
    if (w.contains("remote") || j.contains("remote")) return true;
    return w.equals(j) || w.contains(j) || j.contains(w);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }
}
