package org.skilltro;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.time.Instant;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import static org.skilltro.MemoryValidation.required;
import static org.skilltro.Domain.*;

/** Process-local repository implementations. Only enabled by the explicit dev-memory profile. */
@Configuration
@Profile("dev-memory")
class MemoryRepositoriesConfiguration {
  @Bean PlatformTransactionManager memoryTransactionManager(){return new AbstractPlatformTransactionManager(){
    @Override protected Object doGetTransaction(){return new Object();}
    @Override protected void doBegin(Object transaction,TransactionDefinition definition){}
    @Override protected void doCommit(DefaultTransactionStatus status){}
    @Override protected void doRollback(DefaultTransactionStatus status){}
  };}
  @Bean MemoryTable<User> userTable(){return new MemoryTable<>();}
  @Bean MemoryTable<WorkerProfile> workerTable(){return new MemoryTable<>();}
  @Bean MemoryTable<EmployerProfile> employerTable(){return new MemoryTable<>();}
  @Bean MemoryTable<Skill> skillTable(){return new MemoryTable<>();}
  @Bean MemoryTable<WorkerSkill> workerSkillTable(){return new MemoryTable<>();}
  @Bean MemoryTable<Question> questionTable(){return new MemoryTable<>();}
  @Bean MemoryTable<Attempt> attemptTable(){return new MemoryTable<>();}
  @Bean MemoryTable<Job> jobTable(){return new MemoryTable<>();}
  @Bean MemoryTable<JobSkill> jobSkillTable(){return new MemoryTable<>();}
  @Bean MemoryTable<Application> applicationTable(){return new MemoryTable<>();}
  @Bean MemoryTable<MatchResult> matchResultTable(){return new MemoryTable<>();}

  @Bean UserRepo userRepo(MemoryTable<User> t){return new MemoryUserRepo(t);}
  @Bean WorkerRepo workerRepo(MemoryTable<WorkerProfile> t){return new MemoryWorkerRepo(t);}
  @Bean EmployerRepo employerRepo(MemoryTable<EmployerProfile> t){return new MemoryEmployerRepo(t);}
  @Bean SkillRepo skillRepo(MemoryTable<Skill> t){return new MemorySkillRepo(t);}
  @Bean WorkerSkillRepo workerSkillRepo(MemoryTable<WorkerSkill> t){return new MemoryWorkerSkillRepo(t);}
  @Bean QuestionRepo questionRepo(MemoryTable<Question> t){return new MemoryQuestionRepo(t);}
  @Bean AttemptRepo attemptRepo(MemoryTable<Attempt> t){return new MemoryAttemptRepo(t);}
  @Bean JobRepo jobRepo(MemoryTable<Job> t){return new MemoryJobRepo(t);}
  @Bean JobSkillRepo jobSkillRepo(MemoryTable<JobSkill> t){return new MemoryJobSkillRepo(t);}
  @Bean ApplicationRepo applicationRepo(MemoryTable<Application> t){return new MemoryApplicationRepo(t);}
  @Bean MatchResultRepo matchResultRepo(MemoryTable<MatchResult> t){return new MemoryMatchResultRepo(t);}
}

/** Small synchronized table with generated IDs, timestamps, updates, deletion, and unique checks. */
final class MemoryTable<T extends Timed> {
  private final LinkedHashMap<Long,T> rows=new LinkedHashMap<>();
  private long nextId=1;
  synchronized <S extends T> S save(S entity){return save(entity,(stored,incoming)->false);}
  synchronized <S extends T> S save(S entity,BiPredicate<T,S> duplicate){
    Objects.requireNonNull(entity,"entity");
    if(duplicate!=null&&rows.values().stream().anyMatch(old->!Objects.equals(old.id,entity.id)&&duplicate.test(old,entity)))
      throw new DataIntegrityViolationException("In-memory uniqueness constraint violated");
    Instant now=Instant.now();
    if(entity.id==null)entity.id=nextId++;
    else nextId=Math.max(nextId,entity.id+1);
    if(entity.createdAt==null)entity.createdAt=now;
    entity.updatedAt=now;
    rows.put(entity.id,entity);
    return entity;
  }
  synchronized Optional<T> findById(Long id){return Optional.ofNullable(rows.get(id));}
  synchronized List<T> findAll(){return new ArrayList<>(rows.values());}
  synchronized long count(){return rows.size();}
  synchronized List<T> find(Predicate<T> predicate){return rows.values().stream().filter(predicate).toList();}
  synchronized boolean exists(Predicate<T> predicate){return rows.values().stream().anyMatch(predicate);}
  synchronized void deleteAll(Iterable<? extends T> entities){for(T e:entities)if(e!=null&&e.id!=null)rows.remove(e.id);}
}

final class MemoryUserRepo implements UserRepo {
  private final MemoryTable<User> t; MemoryUserRepo(MemoryTable<User> t){this.t=t;}
  public <S extends User>S save(S e){required(e.email!=null&&!e.email.isBlank()&&e.passwordHash!=null&&e.name!=null&&e.role!=null,"user fields");return t.save(e,(a,b)->a.email.equalsIgnoreCase(b.email));}
  public Optional<User> findById(Long id){return t.findById(id);}
  public Optional<User> findByEmailIgnoreCase(String email){return t.findAll().stream().filter(x->x.email.equalsIgnoreCase(email)).findFirst();}
  public long count(){return t.count();}
}
final class MemoryWorkerRepo implements WorkerRepo {
  private final MemoryTable<WorkerProfile> t; MemoryWorkerRepo(MemoryTable<WorkerProfile> t){this.t=t;}
  public <S extends WorkerProfile>S save(S e){required(e.user!=null&&e.user.id!=null,"worker user");return t.save(e,(a,b)->Objects.equals(a.user.id,b.user.id));}
  public Optional<WorkerProfile> findByUserId(Long id){return t.findAll().stream().filter(x->Objects.equals(x.user.id,id)).findFirst();}
  public List<WorkerProfile> findAll(){return t.findAll();}
}
final class MemoryEmployerRepo implements EmployerRepo {
  private final MemoryTable<EmployerProfile> t; MemoryEmployerRepo(MemoryTable<EmployerProfile> t){this.t=t;}
  public <S extends EmployerProfile>S save(S e){required(e.user!=null&&e.user.id!=null,"employer user");return t.save(e,(a,b)->Objects.equals(a.user.id,b.user.id));}
  public Optional<EmployerProfile> findByUserId(Long id){return t.findAll().stream().filter(x->Objects.equals(x.user.id,id)).findFirst();}
}
final class MemorySkillRepo implements SkillRepo {
  private final MemoryTable<Skill> t; MemorySkillRepo(MemoryTable<Skill> t){this.t=t;}
  public <S extends Skill>S save(S e){required(e.name!=null&&!e.name.isBlank(),"skill name");return t.save(e,(a,b)->a.name.equalsIgnoreCase(b.name));}
  public Optional<Skill> findById(Long id){return t.findById(id);}
  public Optional<Skill> findByNameIgnoreCase(String name){return t.findAll().stream().filter(x->x.name.equalsIgnoreCase(name)).findFirst();}
  public List<Skill> findAll(){return t.findAll();}
}
final class MemoryWorkerSkillRepo implements WorkerSkillRepo {
  private final MemoryTable<WorkerSkill> t; MemoryWorkerSkillRepo(MemoryTable<WorkerSkill> t){this.t=t;}
  public <S extends WorkerSkill>S save(S e){required(e.workerProfile!=null&&e.workerProfile.id!=null&&e.skill!=null&&e.skill.id!=null,"worker skill relations");return t.save(e,(a,b)->Objects.equals(a.workerProfile.id,b.workerProfile.id)&&Objects.equals(a.skill.id,b.skill.id));}
  public List<WorkerSkill> findByWorkerProfileIdAndState(Long id,String state){return t.find(x->Objects.equals(x.workerProfile.id,id)&&Objects.equals(x.state,state));}
  public List<WorkerSkill> findByWorkerProfileId(Long id){return t.find(x->Objects.equals(x.workerProfile.id,id));}
}
final class MemoryQuestionRepo implements QuestionRepo {
  private final MemoryTable<Question> t; MemoryQuestionRepo(MemoryTable<Question> t){this.t=t;}
  public <S extends Question>S save(S e){required(e.skill!=null&&e.skill.id!=null&&e.prompt!=null,"question fields");return t.save(e);}
  public List<Question> findBySkillId(Long id){return t.find(x->Objects.equals(x.skill.id,id));}
  public long count(){return t.count();}
}
final class MemoryAttemptRepo implements AttemptRepo {
  private final MemoryTable<Attempt> t; MemoryAttemptRepo(MemoryTable<Attempt> t){this.t=t;}
  public <S extends Attempt>S save(S e){required(e.workerProfile!=null&&e.workerProfile.id!=null&&e.skill!=null&&e.skill.id!=null,"assessment relations");return t.save(e);}
  public List<Attempt> findByWorkerProfileIdOrderByCreatedAtDesc(Long id){return t.find(x->Objects.equals(x.workerProfile.id,id)).stream().sorted(Comparator.comparing((Attempt x)->x.createdAt).reversed()).toList();}
}
final class MemoryJobRepo implements JobRepo {
  private final MemoryTable<Job> t; MemoryJobRepo(MemoryTable<Job> t){this.t=t;}
  public <S extends Job>S save(S e){required(e.employer!=null&&e.employer.id!=null&&e.title!=null&&!e.title.isBlank()&&e.location!=null,"job fields");return t.save(e);}
  public Optional<Job> findById(Long id){return t.findById(id);}
  public List<Job> findByOpenTrueOrderByCreatedAtDesc(){return t.findAll().stream().filter(x->x.open).sorted(Comparator.comparing((Job x)->x.createdAt).reversed()).toList();}
  public List<Job> findByEmployerId(Long id){return t.find(x->Objects.equals(x.employer.id,id));}
}
final class MemoryJobSkillRepo implements JobSkillRepo {
  private final MemoryTable<JobSkill> t; MemoryJobSkillRepo(MemoryTable<JobSkill> t){this.t=t;}
  public <S extends JobSkill>S save(S e){required(e.job!=null&&e.job.id!=null&&e.skill!=null&&e.skill.id!=null&&e.requirementType!=null,"job skill relations");return t.save(e,(a,b)->Objects.equals(a.job.id,b.job.id)&&Objects.equals(a.skill.id,b.skill.id)&&Objects.equals(a.requirementType,b.requirementType));}
  public List<JobSkill> findByJobId(Long id){return t.find(x->Objects.equals(x.job.id,id));}
  public void deleteAll(Iterable<? extends JobSkill> entities){t.deleteAll(entities);}
  public void flush(){}
}
final class MemoryApplicationRepo implements ApplicationRepo {
  private final MemoryTable<Application> t; MemoryApplicationRepo(MemoryTable<Application> t){this.t=t;}
  public <S extends Application>S save(S e){required(e.job!=null&&e.job.id!=null&&e.workerProfile!=null&&e.workerProfile.id!=null&&e.status!=null,"application relations");return t.save(e,(a,b)->Objects.equals(a.job.id,b.job.id)&&Objects.equals(a.workerProfile.id,b.workerProfile.id));}
  public Optional<Application> findById(Long id){return t.findById(id);}
  public List<Application> findByWorkerProfileId(Long id){return t.find(x->Objects.equals(x.workerProfile.id,id));}
  public List<Application> findByJobEmployerId(Long id){return t.find(x->Objects.equals(x.job.employer.id,id));}
  public boolean existsByJobIdAndWorkerProfileId(Long jobId,Long workerId){return t.exists(x->Objects.equals(x.job.id,jobId)&&Objects.equals(x.workerProfile.id,workerId));}
}
final class MemoryMatchResultRepo implements MatchResultRepo {
  private final MemoryTable<MatchResult> t; MemoryMatchResultRepo(MemoryTable<MatchResult> t){this.t=t;}
  public <S extends MatchResult>S save(S e){required(e.workerProfile!=null&&e.workerProfile.id!=null&&e.job!=null&&e.job.id!=null,"match result relations");return t.save(e,(a,b)->Objects.equals(a.workerProfile.id,b.workerProfile.id)&&Objects.equals(a.job.id,b.job.id));}
  public Optional<MatchResult> findByWorkerProfileIdAndJobId(Long workerId,Long jobId){return t.findAll().stream().filter(x->Objects.equals(x.workerProfile.id,workerId)&&Objects.equals(x.job.id,jobId)).findFirst();}
}

final class MemoryValidation {
  private MemoryValidation(){}
  static void required(boolean valid,String what){if(!valid)throw new DataIntegrityViolationException("Invalid in-memory record: "+what);}
}

