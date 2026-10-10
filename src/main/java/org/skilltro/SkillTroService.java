package org.skilltro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.skilltro.Domain.*;

/** Application use cases shared by the REST controller and persistence adapters. */
@Service
@Transactional(readOnly=true)
class SkillTroService {
  private static final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder PASSWORDS=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
  private final UserRepo users; private final WorkerRepo workers; private final EmployerRepo employers;
  private final SkillRepo skills; private final WorkerSkillRepo workerSkills; private final QuestionRepo questions;
  private final AttemptRepo attempts; private final JobRepo jobs; private final JobSkillRepo jobSkills;
  private final ApplicationRepo applications; private final MatchResultRepo matchResults;
  private final ObjectMapper json;

  SkillTroService(UserRepo users,WorkerRepo workers,EmployerRepo employers,SkillRepo skills,
      WorkerSkillRepo workerSkills,QuestionRepo questions,AttemptRepo attempts,JobRepo jobs,
      JobSkillRepo jobSkills,ApplicationRepo applications,MatchResultRepo matchResults,ObjectMapper json) {
    this.users=users;this.workers=workers;this.employers=employers;this.skills=skills;
    this.workerSkills=workerSkills;this.questions=questions;this.attempts=attempts;this.jobs=jobs;
    this.jobSkills=jobSkills;this.applications=applications;this.matchResults=matchResults;this.json=json;
  }

  @Transactional User register(RegisterRequest in) {
    String email=in.email().trim().toLowerCase(Locale.ROOT);
    if(users.findByEmailIgnoreCase(email).isPresent())throw problem(HttpStatus.CONFLICT,"Email already registered");
    User u=new User();u.email=email;u.passwordHash=PASSWORDS.encode(in.password());u.name=in.name().trim();u.role=in.role();users.save(u);
    if(u.role==Role.WORKER){WorkerProfile p=new WorkerProfile();p.user=u;p.location="";p.occupation="";p.workDescription="";workers.save(p);}
    else{EmployerProfile p=new EmployerProfile();p.user=u;p.organization=u.name;p.location="";p.description="";employers.save(p);}
    return u;
  }
  User authenticate(LoginRequest in) {
    User u=users.findByEmailIgnoreCase(in.email().trim()).orElseThrow(()->problem(HttpStatus.UNAUTHORIZED,"Email or password is incorrect"));
    if(!PASSWORDS.matches(in.password(),u.passwordHash))throw problem(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");
    return u;
  }
  User userById(Long id){return users.findById(id).orElseThrow(()->problem(HttpStatus.UNAUTHORIZED,"Please sign in"));}
  WorkerProfile workerProfileByUserId(Long id){return workers.findByUserId(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Worker profile not found"));}
  EmployerProfile employerProfileByUserId(Long id){return employers.findByUserId(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Employer profile not found"));}

  List<Map<String,Object>> allSkills(){return skills.findAll().stream().map(x->Map.<String,Object>of("id",x.id,"name",x.name,"category",safe(x.category))).toList();}
  Map<String,Object> workerProfile(WorkerProfile p){return Map.of("id",p.id,"name",p.user.name,"occupation",safe(p.occupation),"experienceYears",p.experienceYears,"location",safe(p.location),"workDescription",safe(p.workDescription),"demo",p.demo,"skills",workerSkills.findByWorkerProfileIdAndState(p.id,"CONFIRMED").stream().map(x->x.skill.name).toList());}
  @Transactional Map<String,Object> saveWorkerProfile(WorkerProfile p,ProfileRequest in){p.occupation=safe(in.occupation());p.experienceYears=in.experienceYears();p.location=safe(in.location());p.workDescription=safe(in.workDescription());workers.save(p);return workerProfile(p);}
  List<Map<String,Object>> suggestions(WorkerProfile p){String text=safe(p.occupation)+" "+safe(p.workDescription);Set<String> reviewed=workerSkills.findByWorkerProfileId(p.id).stream().map(x->x.skill.name).collect(Collectors.toSet());return SkillSuggestionRules.suggest(skills.findAll().stream().map(sk->sk.name).toList(),text,reviewed).stream().map(name->{Skill sk=skill(name);String evidence=SkillSuggestionRules.evidenceFor(name,safe(p.workDescription));if(evidence.isBlank())evidence=SkillSuggestionRules.evidenceFor(name,safe(p.occupation));return Map.<String,Object>of("id",sk.id,"name",sk.name,"evidence",evidence);}).toList();}
  @Transactional Map<String,Object> updateSkill(WorkerProfile p,WorkerSkillRequest in){
    if(in.id()==null&&(in.name()==null||in.name().isBlank()))throw problem(HttpStatus.BAD_REQUEST,"Skill id or name is required");
    Skill sk=in.id()!=null?skills.findById(in.id()).orElseThrow(()->problem(HttpStatus.BAD_REQUEST,"Unknown skill id")):skill(in.name());
    String state="REJECTED".equalsIgnoreCase(in.state())?"REJECTED":"CONFIRMED";
    WorkerSkill ws=workerSkills.findByWorkerProfileId(p.id).stream().filter(x->x.skill.id.equals(sk.id)).findFirst().orElseGet(WorkerSkill::new);
    ws.workerProfile=p;ws.skill=sk;ws.state=state;String evidenceText=SkillSuggestionRules.evidenceFor(sk.name,safe(p.workDescription));if(evidenceText.isBlank())evidenceText=SkillSuggestionRules.evidenceFor(sk.name,safe(p.occupation));ws.evidence=in.evidence()!=null&&!in.evidence().isBlank()?in.evidence():(state.equals("CONFIRMED")&&!evidenceText.isBlank()?evidenceText:(state.equals("CONFIRMED")?"Worker confirmed after profile review":"Worker rejected suggestion"));workerSkills.save(ws);
    return Map.of("message",state.equals("CONFIRMED")?"Skill confirmed":"Suggestion rejected","skill",sk.name,"state",state);
  }
  List<Map<String,Object>> assessmentList(){return skills.findAll().stream().map(sk->Map.<String,Object>of("skill",sk.name,"skillId",sk.id,"questions",questions.findBySkillId(sk.id).size())).filter(x->(int)x.get("questions")>0).toList();}
  List<Map<String,Object>> assessment(Long skillId){skills.findById(skillId).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Assessment skill not found"));return questions.findBySkillId(skillId).stream().map(q->Map.<String,Object>of("id",q.id,"prompt",q.prompt,"options",List.of(q.optionA,q.optionB,q.optionC,q.optionD))).toList();}
  @Transactional Map<String,Object> submitAssessment(WorkerProfile p,Long skillId,List<AnswerRequest> answers){
    Skill sk=skills.findById(skillId).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Assessment skill not found"));List<Question> qs=questions.findBySkillId(skillId);
    if(qs.isEmpty())throw problem(HttpStatus.NOT_FOUND,"No assessment is available for this skill");
    if(answers==null||answers.size()!=qs.size())throw problem(HttpStatus.BAD_REQUEST,"Submit exactly one answer for each question");
    Set<Long> valid=qs.stream().map(q->q.id).collect(Collectors.toSet()),seen=new HashSet<>();
    for(AnswerRequest a:answers)if(a.questionId()==null||!valid.contains(a.questionId())||!seen.add(a.questionId())||a.option()<0||a.option()>3)throw problem(HttpStatus.BAD_REQUEST,"Each question needs one valid option");
    int score=(int)answers.stream().filter(a->qs.stream().anyMatch(q->q.id.equals(a.questionId())&&q.correctOption==a.option())).count();
    Attempt attempt=new Attempt();attempt.workerProfile=p;attempt.skill=sk;attempt.score=score;attempt.total=qs.size();attempt.answersJson=writeJson(answers);attempts.save(attempt);
    return Map.of("score",score,"total",qs.size(),"notice","Quiz results indicate knowledge only; they do not prove practical competence.");
  }
  List<Map<String,Object>> results(WorkerProfile p){return attempts.findByWorkerProfileIdOrderByCreatedAtDesc(p.id).stream().map(a->Map.<String,Object>of("skill",a.skill.name,"score",a.score,"total",a.total,"createdAt",a.createdAt,"demo",a.demo,"notice",a.demo?"Synthetic demo knowledge check; not a real assessment or proof of practical competence.":"Knowledge check only; not proof of practical competence.")).toList();}

  @Transactional List<Map<String,Object>> discoverJobs(WorkerProfile p,String search,String location){
    String query=normalize(search),place=normalize(location);
    return jobs.findByOpenTrueOrderByCreatedAtDesc().stream()
        .filter(j->place.isBlank()||normalize(j.location).contains(place)||normalize(j.location).contains("remote"))
        .filter(j->query.isBlank()||normalize(j.title).contains(query)||normalize(j.description).contains(query)||
            normalize(j.employer.organization).contains(query)||jobSkills.findByJobId(j.id).stream().anyMatch(s->normalize(s.skill.name).contains(query)))
        .map(j->match(j,p,true)).toList();
  }
  @Transactional int refreshDemoMatchSnapshots(List<WorkerProfile> demoWorkers,List<Job> demoJobs){
    int count=0;for(WorkerProfile worker:demoWorkers)for(Job job:demoJobs){match(job,worker,true);count++;}return count;
  }
  @Transactional Map<String,Object> apply(WorkerProfile p,Long id){Job j=jobs.findById(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Opportunity not found"));if(!j.open)throw problem(HttpStatus.CONFLICT,"This opportunity is closed");if(applications.existsByJobIdAndWorkerProfileId(id,p.id))throw problem(HttpStatus.CONFLICT,"You have already applied");Application a=new Application();a.job=j;a.workerProfile=p;a.status="SUBMITTED";applications.save(a);return Map.of("message","Application submitted","id",a.id);}
  List<Map<String,Object>> myApplications(WorkerProfile p){return applications.findByWorkerProfileId(p.id).stream().map(a->Map.<String,Object>of("id",a.id,"jobId",a.job.id,"title",a.job.title,"employer",a.job.employer.organization,"status",a.status,"createdAt",a.createdAt)).toList();}
  Map<String,Object> employerProfile(EmployerProfile p){return Map.of("id",p.id,"name",p.user.name,"organization",safe(p.organization),"location",safe(p.location),"description",safe(p.description),"demo",p.demo);}
  @Transactional Map<String,Object> saveEmployerProfile(EmployerProfile p,ProfileRequest in){p.organization=safe(in.organization());p.location=safe(in.location());p.description=safe(in.description());employers.save(p);return employerProfile(p);}
  List<Map<String,Object>> employerJobs(EmployerProfile e){return jobs.findByEmployerId(e.id).stream().map(j->{Map<String,Object> m=new LinkedHashMap<>(match(j,null,false));m.put("requiredSkills",jobSkills.findByJobId(j.id).stream().filter(x->x.requirementType.equals("REQUIRED")).map(x->x.skill.name).toList());m.put("preferredSkills",jobSkills.findByJobId(j.id).stream().filter(x->x.requirementType.equals("PREFERRED")).map(x->x.skill.name).toList());return m;}).toList();}
  @Transactional Map<String,Object> createJob(EmployerProfile e,JobRequest in){Job j=saveJob(new Job(),in,e);return Map.of("id",j.id,"message","Demo opportunity created");}
  @Transactional Map<String,Object> editJob(EmployerProfile e,Long id,JobRequest in){Job j=jobs.findById(id).filter(x->x.employer.id.equals(e.id)).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Opportunity not found"));saveJob(j,in,e);return Map.of("message","Opportunity updated");}
  @Transactional Map<String,Object> closeJob(EmployerProfile e,Long id){Job j=jobs.findById(id).filter(x->x.employer.id.equals(e.id)).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Opportunity not found"));j.open=false;jobs.save(j);return Map.of("message","Opportunity closed");}
  List<Map<String,Object>> discoverWorkers(String skill,String location){
    String selectedSkill=skill==null||skill.isBlank()?null:skill.trim(),selectedLocation=location==null||location.isBlank()?null:location.trim();
    return workers.findAll().stream().filter(w->selectedLocation==null||safe(w.location).toLowerCase(Locale.ROOT).contains(selectedLocation.toLowerCase(Locale.ROOT))).filter(w->selectedSkill==null||workerSkills.findByWorkerProfileIdAndState(w.id,"CONFIRMED").stream().anyMatch(x->x.skill.name.equalsIgnoreCase(selectedSkill))).map(w->workerCard(w,selectedSkill)).toList();
  }
  @Transactional List<Map<String,Object>> employerApplications(EmployerProfile e){return applications.findByJobEmployerId(e.id).stream().map(a->{Map<String,Object> m=new LinkedHashMap<>(workerCard(a.workerProfile,null));Map<String,Object> result=match(a.job,a.workerProfile,true);m.put("applicationId",a.id);m.put("jobId",a.job.id);m.put("jobTitle",a.job.title);m.put("status",a.status);m.put("missingSkills",result.get("missingSkills"));m.put("matchPercentage",result.get("matchPercentage"));return m;}).toList();}
  @Transactional Map<String,Object> updateApplication(EmployerProfile e,Long id,ApplicationStatusRequest in){Application a=applications.findById(id).filter(x->x.job.employer.id.equals(e.id)).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Application not found"));a.status=in.status();applications.save(a);return Map.of("id",a.id,"status",a.status);}

  private Job saveJob(Job j,JobRequest in,EmployerProfile e){
    List<Skill> required=resolveJobSkills(Optional.ofNullable(in.required()).orElse(List.of()));List<Skill> preferred=resolveJobSkills(Optional.ofNullable(in.preferred()).orElse(List.of()));
    Set<String> requiredNames=required.stream().map(x->x.name.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());if(requiredNames.size()!=required.size())throw problem(HttpStatus.BAD_REQUEST,"Required skills must not contain duplicates");
    Set<String> preferredNames=preferred.stream().map(x->x.name.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());if(preferredNames.size()!=preferred.size())throw problem(HttpStatus.BAD_REQUEST,"Preferred skills must not contain duplicates");if(preferredNames.stream().anyMatch(requiredNames::contains))throw problem(HttpStatus.BAD_REQUEST,"A skill cannot be both required and preferred");
    boolean creating=j.id==null;
    if(in.payMin()!=null&&in.payMax()!=null&&in.payMin()>in.payMax())throw problem(HttpStatus.BAD_REQUEST,"Minimum pay must not exceed maximum pay");
    j.employer=e;j.title=in.title().trim();j.location=in.location().trim();j.description=safe(in.description());j.safetyRequirements=safe(in.safetyRequirements());j.demo=true;
    if(in.minimumExperienceYears()!=null)j.minimumExperienceYears=in.minimumExperienceYears();if(in.payMin()!=null)j.payMin=in.payMin();if(in.payMax()!=null)j.payMax=in.payMax();if(in.payPeriod()!=null)j.payPeriod=in.payPeriod().toUpperCase(Locale.ROOT);if(creating)j.open=true;jobs.save(j);
    jobSkills.deleteAll(jobSkills.findByJobId(j.id));jobSkills.flush();for(Skill sk:required){JobSkill row=new JobSkill();row.job=j;row.skill=sk;row.requirementType="REQUIRED";jobSkills.save(row);}for(Skill sk:preferred){JobSkill row=new JobSkill();row.job=j;row.skill=sk;row.requirementType="PREFERRED";jobSkills.save(row);}return j;
  }
  private List<Skill> resolveJobSkills(List<String> names){return names.stream().map(name->{if(name==null||name.isBlank())throw problem(HttpStatus.BAD_REQUEST,"Skill names must not be empty");return skill(name);}).toList();}
  private Map<String,Object> match(Job j,WorkerProfile w,boolean store){
    List<JobSkill> requirements=jobSkills.findByJobId(j.id);
    MatchingRules.Evaluation evaluation=w==null?null:MatchingRules.evaluate(j,w,requirements,workerSkills.findByWorkerProfileId(w.id));
    List<JobSkill> required=requirements.stream().filter(x->x.requirementType.equals("REQUIRED")).toList();
    List<JobSkill> preferred=requirements.stream().filter(x->x.requirementType.equals("PREFERRED")).toList();
    List<String> matched=evaluation==null?List.of():evaluation.matchedSkills();
    List<String> missing=evaluation==null?required.stream().map(x->x.skill.name).toList():evaluation.missingSkills();
    List<String> preferredNames=preferred.stream().map(x->x.skill.name).toList();
    int pct=evaluation==null?0:evaluation.matchPercentage();
    Map<String,String> evidence=evaluation==null?Map.of():evaluation.relevantEvidence();
    boolean experienceMet=evaluation!=null&&evaluation.experienceRequirementMet();
    boolean locationMatched=evaluation!=null&&evaluation.locationMatched();
    String explanation=evaluation==null?"Sign in as a worker to get an individualized match.":evaluation.explanation();
    if(store&&w!=null&&w.id!=null){
      MatchResult row=matchResults.findByWorkerProfileIdAndJobId(w.id,j.id).orElseGet(MatchResult::new);row.workerProfile=w;row.job=j;
      row.matchPercentage=pct;row.matchedRequiredCount=matched.size();row.totalRequiredCount=required.size();
      row.matchedSkillsJson=writeJson(matched);row.missingSkillsJson=writeJson(missing);row.preferredSkillsJson=writeJson(preferredNames);
      row.experienceRequirementMet=experienceMet;row.locationMatched=locationMatched;row.explanation=explanation;row.relevantEvidenceJson=writeJson(evidence);matchResults.save(row);
    }
    Map<String,Object> result=new LinkedHashMap<>();result.put("jobId",j.id);result.put("title",j.title);result.put("location",j.location);result.put("description",safe(j.description));result.put("demo",j.demo);result.put("open",j.open);result.put("employer",j.employer.organization);
    result.put("matchedSkills",matched);result.put("missingSkills",missing);result.put("preferredSkills",preferredNames);result.put("relevantEvidence",evidence);result.put("matchPercentage",pct);result.put("explanation",explanation);
    result.put("experienceRequirementMet",experienceMet);result.put("locationMatched",locationMatched);result.put("minimumExperienceYears",j.minimumExperienceYears);result.put("payMin",j.payMin);result.put("payMax",j.payMax);result.put("payPeriod",j.payPeriod);
    result.put("safetyRequirements",safe(j.safetyRequirements));result.put("mandatoryRequirementsMet",evaluation!=null&&missing.isEmpty()&&experienceMet);return result;
  }
  private Map<String,Object> workerCard(WorkerProfile w,String search){
    List<WorkerSkill> confirmed=workerSkills.findByWorkerProfileIdAndState(w.id,"CONFIRMED");
    List<String> names=confirmed.stream().map(x->x.skill.name).toList();
    List<Map<String,String>> evidence=confirmed.stream().filter(x->x.evidence!=null&&!x.evidence.isBlank()).map(x->Map.of("skill",x.skill.name,"evidence",x.evidence)).toList();
    List<String> checks=attempts.findByWorkerProfileIdOrderByCreatedAtDesc(w.id).stream().map(a->a.skill.name+" "+a.score+"/"+a.total+(a.demo?" (fictional demo knowledge check)":" (knowledge check)")).toList();
    Map<String,Object> card=new LinkedHashMap<>();card.put("id",w.id);card.put("name",w.user.name);card.put("occupation",safe(w.occupation));card.put("experienceYears",w.experienceYears);card.put("location",safe(w.location));card.put("workDescription",safe(w.workDescription));card.put("demo",w.demo);card.put("confirmedSkills",names);card.put("skillEvidence",evidence);card.put("assessmentResults",checks);card.put("matchingSkills",search==null?List.of():names.stream().filter(x->x.equalsIgnoreCase(search)).toList());return card;
  }
  private Skill skill(String name){return skills.findByNameIgnoreCase(name.trim()).orElseThrow(()->problem(HttpStatus.BAD_REQUEST,"Unknown skill: "+name));}
  private static String safe(String value){return value==null?"":value;}
  private static String normalize(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT);}
  private String writeJson(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize stored result",e);}}
  private static ResponseStatusException problem(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
