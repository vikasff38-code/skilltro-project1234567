package org.skilltro;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.skilltro.Domain.*;

class MemoryRepositoriesTest {
  @Test void generatesStableIdsUpdatesTimestampsAndFindsEmailCaseInsensitively() throws Exception {
    MemoryTable<User> table=new MemoryTable<>();
    MemoryUserRepo users=new MemoryUserRepo(table);
    User u=user("Demo@Example.test");

    users.save(u);
    Long originalId=u.id;
    Instant originalCreated=u.createdAt;
    Thread.sleep(2);
    u.name="Renamed";
    users.save(u);

    assertEquals(originalId,u.id);
    assertEquals(originalCreated,u.createdAt);
    assertTrue(u.updatedAt.isAfter(originalCreated));
    assertEquals("Renamed",users.findByEmailIgnoreCase("demo@example.test").orElseThrow().name);
    assertEquals(1,users.count());
  }

  @Test void enforcesUniqueEmailAndSkillRelationshipsAndSupportsDeleteAll() {
    MemoryUserRepo users=new MemoryUserRepo(new MemoryTable<>());
    User u=user("worker@example.test"); users.save(u);
    User duplicate=user("WORKER@example.test");
    assertThrows(DataIntegrityViolationException.class,()->users.save(duplicate));

    MemoryWorkerRepo workers=new MemoryWorkerRepo(new MemoryTable<>());
    WorkerProfile worker=new WorkerProfile();worker.user=u;worker.location="Pune";workers.save(worker);
    MemorySkillRepo skills=new MemorySkillRepo(new MemoryTable<>());
    Skill skill=new Skill();skill.name="Carpentry";skills.save(skill);
    MemoryWorkerSkillRepo workerSkills=new MemoryWorkerSkillRepo(new MemoryTable<>());
    WorkerSkill confirmed=new WorkerSkill();confirmed.workerProfile=worker;confirmed.skill=skill;confirmed.state="CONFIRMED";workerSkills.save(confirmed);
    assertEquals(List.of(confirmed),workerSkills.findByWorkerProfileIdAndState(worker.id,"CONFIRMED"));
    assertTrue(workerSkills.findByWorkerProfileIdAndState(worker.id,"REJECTED").isEmpty());

    MemoryJobSkillRepo jobSkills=new MemoryJobSkillRepo(new MemoryTable<>());
    EmployerProfile employer=new EmployerProfile();employer.id=9L;employer.organization="Employer";
    Job job=new Job();job.id=4L;job.employer=employer;job.title="Demo";job.location="Pune";
    JobSkill required=new JobSkill();required.job=job;required.skill=skill;required.requirementType="REQUIRED";jobSkills.save(required);
    assertEquals(List.of(required),jobSkills.findByJobId(job.id));
    jobSkills.deleteAll(List.of(required));
    assertTrue(jobSkills.findByJobId(job.id).isEmpty());
  }

  private static User user(String email){User u=new User();u.email=email;u.name="Worker";u.passwordHash="hash";u.role=Role.WORKER;return u;}
}
