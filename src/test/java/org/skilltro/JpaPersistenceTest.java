package org.skilltro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;
import static org.skilltro.Domain.*;

@DataJpaTest(properties={
    "spring.datasource.url=jdbc:h2:mem:skilltro-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate"})
class JpaPersistenceTest {
  @Autowired JpaUserRepo users;
  @Autowired JpaWorkerRepo workers;
  @Autowired JpaEmployerRepo employers;
  @Autowired JpaSkillRepo skills;
  @Autowired JpaJobRepo jobs;
  @Autowired JpaMatchResultRepo matches;

  @Test void persistsProfilesOpportunitiesAndLatestMatchSnapshot() {
    User workerUser=new User();workerUser.email="worker@persistence.test";workerUser.name="Worker";workerUser.passwordHash="hashed";workerUser.role=Role.WORKER;users.saveAndFlush(workerUser);
    WorkerProfile worker=new WorkerProfile();worker.user=workerUser;worker.location="Pune";workers.saveAndFlush(worker);

    User employerUser=new User();employerUser.email="employer@persistence.test";employerUser.name="Employer";employerUser.passwordHash="hashed";employerUser.role=Role.EMPLOYER;users.saveAndFlush(employerUser);
    EmployerProfile employer=new EmployerProfile();employer.user=employerUser;employer.organization="Workshop";employers.saveAndFlush(employer);
    Skill skill=new Skill();skill.name="Carpentry";skill.category="Practical";skills.saveAndFlush(skill);
    Job job=new Job();job.employer=employer;job.title="Carpenter";job.location="Pune";job.open=true;jobs.saveAndFlush(job);

    MatchResult match=new MatchResult();match.workerProfile=worker;match.job=job;match.matchPercentage=100;
    match.matchedRequiredCount=1;match.totalRequiredCount=1;match.matchedSkillsJson="[\"Carpentry\"]";match.missingSkillsJson="[]";match.preferredSkillsJson="[]";
    match.experienceRequirementMet=true;match.locationMatched=true;match.explanation="Fictional test snapshot";match.relevantEvidenceJson="{\"Carpentry\":\"test evidence\"}";
    matches.saveAndFlush(match);

    MatchResult persisted=matches.findByWorkerProfileIdAndJobId(worker.id,job.id).orElseThrow();
    assertEquals(100,persisted.matchPercentage);
    assertEquals("[\"Carpentry\"]",persisted.matchedSkillsJson);
    assertEquals("{\"Carpentry\":\"test evidence\"}",persisted.relevantEvidenceJson);
    assertEquals("worker@persistence.test",users.findByEmailIgnoreCase("WORKER@persistence.test").orElseThrow().email);
  }
}
