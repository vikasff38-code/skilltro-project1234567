package org.skilltro;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import static org.skilltro.Domain.*;

/* Application-facing persistence contracts. Api and seed logic depend only on these. */
interface UserRepo {
  <S extends User> S save(S entity); Optional<User> findById(Long id);
  Optional<User> findByEmailIgnoreCase(String email); long count();
}
interface WorkerRepo {
  <S extends WorkerProfile> S save(S entity); Optional<WorkerProfile> findByUserId(Long id);
  List<WorkerProfile> findAll();
}
interface EmployerRepo {
  <S extends EmployerProfile> S save(S entity); Optional<EmployerProfile> findByUserId(Long id);
}
interface SkillRepo {
  <S extends Skill> S save(S entity); Optional<Skill> findById(Long id);
  Optional<Skill> findByNameIgnoreCase(String name); List<Skill> findAll();
}
interface WorkerSkillRepo {
  <S extends WorkerSkill> S save(S entity); List<WorkerSkill> findByWorkerProfileIdAndState(Long id,String state);
  List<WorkerSkill> findByWorkerProfileId(Long id);
}
interface QuestionRepo {
  <S extends Question> S save(S entity); List<Question> findBySkillId(Long id); long count();
}
interface AttemptRepo {
  <S extends Attempt> S save(S entity); List<Attempt> findByWorkerProfileIdOrderByCreatedAtDesc(Long id);
}
interface JobRepo {
  <S extends Job> S save(S entity); Optional<Job> findById(Long id);
  List<Job> findByOpenTrueOrderByCreatedAtDesc(); List<Job> findByEmployerId(Long id);
}
interface JobSkillRepo {
  <S extends JobSkill> S save(S entity); List<JobSkill> findByJobId(Long id);
  void deleteAll(Iterable<? extends JobSkill> entities); void flush();
}
interface ApplicationRepo {
  <S extends Application> S save(S entity); Optional<Application> findById(Long id);
  List<Application> findByWorkerProfileId(Long id); List<Application> findByJobEmployerId(Long id);
  boolean existsByJobIdAndWorkerProfileId(Long jobId,Long workerId);
}
interface MatchResultRepo {
  <S extends MatchResult> S save(S entity);
  Optional<MatchResult> findByWorkerProfileIdAndJobId(Long workerId,Long jobId);
}

/* Spring Data JPA implementations use the configured H2 datasource. */
interface JpaUserRepo extends JpaRepository<User,Long>,UserRepo { Optional<User> findByEmailIgnoreCase(String email); }
interface JpaWorkerRepo extends JpaRepository<WorkerProfile,Long>,WorkerRepo { Optional<WorkerProfile> findByUserId(Long id); }
interface JpaEmployerRepo extends JpaRepository<EmployerProfile,Long>,EmployerRepo { Optional<EmployerProfile> findByUserId(Long id); }
interface JpaSkillRepo extends JpaRepository<Skill,Long>,SkillRepo { Optional<Skill> findByNameIgnoreCase(String name); }
interface JpaWorkerSkillRepo extends JpaRepository<WorkerSkill,Long>,WorkerSkillRepo {
  List<WorkerSkill> findByWorkerProfileIdAndState(Long id,String state); List<WorkerSkill> findByWorkerProfileId(Long id);
}
interface JpaQuestionRepo extends JpaRepository<Question,Long>,QuestionRepo { List<Question> findBySkillId(Long id); }
interface JpaAttemptRepo extends JpaRepository<Attempt,Long>,AttemptRepo { List<Attempt> findByWorkerProfileIdOrderByCreatedAtDesc(Long id); }
interface JpaJobRepo extends JpaRepository<Job,Long>,JobRepo {
  List<Job> findByOpenTrueOrderByCreatedAtDesc(); List<Job> findByEmployerId(Long id);
}
interface JpaJobSkillRepo extends JpaRepository<JobSkill,Long>,JobSkillRepo { List<JobSkill> findByJobId(Long id); }
interface JpaApplicationRepo extends JpaRepository<Application,Long>,ApplicationRepo {
  List<Application> findByWorkerProfileId(Long id); List<Application> findByJobEmployerId(Long id);
  boolean existsByJobIdAndWorkerProfileId(Long jobId,Long workerId);
}
interface JpaMatchResultRepo extends JpaRepository<MatchResult,Long>,MatchResultRepo {
  Optional<MatchResult> findByWorkerProfileIdAndJobId(Long workerId,Long jobId);
}
