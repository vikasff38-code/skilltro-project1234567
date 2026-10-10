package org.skilltro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.skilltro.Domain.*;

/** Exercises the real controller and in-memory repository adapters without a DB or socket. */
class MemoryApiIntegrationTest {
  private final ObjectMapper json=new ObjectMapper();
  private MockMvc mvc;
  private UserRepo users; private WorkerRepo workers; private EmployerRepo employers; private SkillRepo skills;
  private WorkerSkillRepo workerSkills; private QuestionRepo questions; private AttemptRepo attempts;
  private JobRepo jobs; private JobSkillRepo jobSkills; private ApplicationRepo applications; private MatchResultRepo matchResults;

  @BeforeEach void setUp(){
    users=new MemoryUserRepo(new MemoryTable<>());workers=new MemoryWorkerRepo(new MemoryTable<>());
    employers=new MemoryEmployerRepo(new MemoryTable<>());skills=new MemorySkillRepo(new MemoryTable<>());
    workerSkills=new MemoryWorkerSkillRepo(new MemoryTable<>());questions=new MemoryQuestionRepo(new MemoryTable<>());
    attempts=new MemoryAttemptRepo(new MemoryTable<>());jobs=new MemoryJobRepo(new MemoryTable<>());
    jobSkills=new MemoryJobSkillRepo(new MemoryTable<>());applications=new MemoryApplicationRepo(new MemoryTable<>());matchResults=new MemoryMatchResultRepo(new MemoryTable<>());
    String[] names={"Carpentry","Welding","Electrical wiring","Plumbing","Masonry","Painting","Cooking","Driving","Machine operation","First aid"};
    for(String name:names){Skill sk=new Skill();sk.name=name;sk.category="Practical";skills.save(sk);}

    User workerUser=new User();workerUser.email="worker.demo@skilltro.local";workerUser.name="Demo Worker";
    workerUser.passwordHash=new BCryptPasswordEncoder().encode("worker-demo-123");workerUser.role=Role.WORKER;users.save(workerUser);
    WorkerProfile worker=new WorkerProfile();worker.user=workerUser;worker.occupation="Carpenter";worker.experienceYears=5;
    worker.location="Pune";worker.workDescription="I build furniture and repair doors.";workers.save(worker);
    User employerUser=new User();employerUser.email="employer.demo@skilltro.local";employerUser.name="Demo Employer";
    employerUser.passwordHash=new BCryptPasswordEncoder().encode("employer-demo-123");employerUser.role=Role.EMPLOYER;users.save(employerUser);
    EmployerProfile employer=new EmployerProfile();employer.user=employerUser;employer.organization="SkillTro Demo Works";
    employer.location="Pune";employers.save(employer);

    addQuestion("Electrical wiring","Before replacing a wall switch, what is the first safety step?","Turn off circuit and verify power is off");
    addQuestion("Welding","What helps prevent a welded joint from cracking?","Correct heat and clean base metal");
    addQuestion("Plumbing","What should you do before loosening a water supply fitting?","Shut off and relieve pressure");
    addJob(employer,"Workshop Carpenter","Pune","Build and repair furniture with a small workshop team.","Carpentry","Painting","");
    addJob(employer,"Site Electrical Helper","Pune","Assist a licensed electrician with safe wiring installation.","Electrical wiring","First aid","Licensed electrician supervision required.");
    addJob(employer,"Community Kitchen Cook","Mumbai","Prepare meals for a neighborhood kitchen.","Cooking","Driving","");
    SkillTroService service=new SkillTroService(users,workers,employers,skills,workerSkills,questions,attempts,jobs,jobSkills,applications,matchResults,json);
    mvc=MockMvcBuilders.standaloneSetup(new Api(service)).build();
  }

  @Test void workerCanApplyAndEmployerCanUpdateStatus() throws Exception {
    MockHttpSession worker=login("worker.demo@skilltro.local","worker-demo-123");
    mvc.perform(get("/api/worker/profile").session(worker)).andExpect(status().isOk());
    mvc.perform(put("/api/worker/profile").session(worker).contentType("application/json")
        .content(json.writeValueAsString(Map.of("occupation","Carpenter","experienceYears",5,"location","Pune",
            "workDescription","I build furniture and repair doors."))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.occupation").value("Carpenter"));

    MvcResult suggestionResult=mvc.perform(get("/api/worker/suggestions").session(worker))
        .andExpect(status().isOk()).andReturn();
    JsonNode suggestions=json.readTree(suggestionResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    long carpentryId=-1;
    for(JsonNode suggestion:suggestions)if("Carpentry".equals(suggestion.path("name").asText()))carpentryId=suggestion.path("id").asLong();
    assertTrue(carpentryId>0,"Carpentry should be suggested from the profile");
    mvc.perform(post("/api/worker/skills").session(worker).contentType("application/json")
        .content(json.writeValueAsString(Map.of("id",carpentryId,"state","CONFIRMED"))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CONFIRMED"));

    JsonNode assessmentList=json.readTree(mvc.perform(get("/api/worker/assessments").session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    long assessmentId=assessmentList.get(0).path("skillId").asLong();
    JsonNode questionList=json.readTree(mvc.perform(get("/api/worker/assessments/{id}",assessmentId).session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    long questionId=questionList.get(0).path("id").asLong();
    mvc.perform(post("/api/worker/assessments/{id}/submit",assessmentId).session(worker)
        .contentType("application/json").content(json.writeValueAsString(List.of(Map.of("questionId",questionId,"option",0)))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));

    JsonNode openJobs=json.readTree(mvc.perform(get("/api/worker/jobs").session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    WorkerProfile persistedWorker=workers.findByUserId(users.findByEmailIgnoreCase("worker.demo@skilltro.local").orElseThrow().id).orElseThrow();
    assertTrue(matchResults.findByWorkerProfileIdAndJobId(persistedWorker.id,1L).isPresent(),"worker opportunity matches should be persisted");
    long jobId=-1;
    for(JsonNode job:openJobs)if("Workshop Carpenter".equals(job.path("title").asText()))jobId=job.path("jobId").asLong();
    assertTrue(jobId>0);
    mvc.perform(post("/api/worker/jobs/{id}/apply",jobId).session(worker)).andExpect(status().isOk());

    MockHttpSession employer=login("employer.demo@skilltro.local","employer-demo-123");
    JsonNode discovered=json.readTree(mvc.perform(get("/api/employer/workers?skill=Carpentry&location=Pune").session(employer))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    assertTrue(discovered.toString().contains("Demo Worker"));
    JsonNode received=json.readTree(mvc.perform(get("/api/employer/applications").session(employer))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    long applicationId=-1;
    for(JsonNode application:received)if(application.path("jobId").asLong()==jobId)applicationId=application.path("applicationId").asLong();
    assertTrue(applicationId>0,"Employer should see the worker application");
    mvc.perform(put("/api/employer/applications/{id}",applicationId).session(employer)
        .contentType("application/json").content("{\"status\":\"SHORTLISTED\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHORTLISTED"));

    MvcResult created=mvc.perform(post("/api/employer/jobs").session(employer).contentType("application/json")
        .content("{\"title\":\"Demo Welder\",\"location\":\"Pune\",\"description\":\"Fit metal frames\",\"required\":[\"Welding\"],\"preferred\":[\"First aid\"]}"))
        .andExpect(status().isCreated()).andReturn();
    long newJobId=json.readTree(created.getResponse().getContentAsString()).path("id").asLong();
    mvc.perform(put("/api/employer/jobs/{id}",newJobId).session(employer).contentType("application/json")
        .content("{\"title\":\"Workshop Welder\",\"location\":\"Pune\",\"required\":[\"Welding\"],\"preferred\":[\"First aid\"]}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/employer/jobs/{id}/close",newJobId).session(employer)).andExpect(status().isOk());
    mvc.perform(get("/api/worker/jobs").session(worker)).andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.jobId == "+newJobId+")]").isEmpty());
    MvcResult otherAccount=mvc.perform(post("/api/auth/register").contentType("application/json")
        .content("{\"email\":\"other-employer@example.test\",\"password\":\"employer-password\",\"name\":\"Other Employer\",\"role\":\"EMPLOYER\"}"))
        .andExpect(status().isCreated()).andReturn();
    MockHttpSession otherEmployer=(MockHttpSession)otherAccount.getRequest().getSession(false);
    mvc.perform(post("/api/employer/jobs/{id}/close",newJobId).session(otherEmployer)).andExpect(status().isNotFound());

    mvc.perform(post("/api/auth/logout").session(employer)).andExpect(status().isOk());
    worker=login("worker.demo@skilltro.local","worker-demo-123");
    mvc.perform(get("/api/worker/applications").session(worker)).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("SHORTLISTED"));
  }

  @Test void rejectsUnauthenticatedWrongRoleInvalidCredentialsAndDuplicateApply() throws Exception {
    mvc.perform(get("/api/worker/profile")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/login").contentType("application/json")
        .content("{\"email\":\"nobody@example.test\",\"password\":\"not-a-real-password\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/register").contentType("application/json")
        .content("{\"email\":\"bad-email\",\"password\":\"short\",\"name\":\"Test\",\"role\":\"WORKER\"}"))
        .andExpect(status().isBadRequest());
    MockHttpSession worker=login("worker.demo@skilltro.local","worker-demo-123");
    mvc.perform(get("/api/employer/jobs").session(worker)).andExpect(status().isForbidden());
    mvc.perform(put("/api/employer/applications/999999").session(worker)
        .contentType("application/json").content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/worker/skills").session(worker).contentType("application/json")
        .content("{\"name\":\"Not in catalog\",\"state\":\"CONFIRMED\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test void rejectedSkillsDoNotMatchAndInvalidAssessmentOrJobPayloadsAreRejected() throws Exception {
    MockHttpSession worker=login("worker.demo@skilltro.local","worker-demo-123");
    JsonNode suggestions=json.readTree(mvc.perform(get("/api/worker/suggestions").session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    long carpentryId=-1;
    for(JsonNode item:suggestions)if("Carpentry".equals(item.path("name").asText()))carpentryId=item.path("id").asLong();
    assertTrue(carpentryId>0);
    mvc.perform(post("/api/worker/skills").session(worker).contentType("application/json")
        .content(json.writeValueAsString(Map.of("id",carpentryId,"state","REJECTED"))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("REJECTED"));
    JsonNode remaining=json.readTree(mvc.perform(get("/api/worker/suggestions").session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    for(JsonNode item:remaining)org.junit.jupiter.api.Assertions.assertNotEquals("Carpentry",item.path("name").asText());
    JsonNode jobsJson=json.readTree(mvc.perform(get("/api/worker/jobs").session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    JsonNode carpenterJob=null;for(JsonNode job:jobsJson)if("Workshop Carpenter".equals(job.path("title").asText()))carpenterJob=job;
    org.junit.jupiter.api.Assertions.assertNotNull(carpenterJob);
    org.junit.jupiter.api.Assertions.assertEquals(25,carpenterJob.path("matchPercentage").asInt());
    org.junit.jupiter.api.Assertions.assertTrue(carpenterJob.path("missingSkills").toString().contains("Carpentry"));

    JsonNode assessment=json.readTree(mvc.perform(get("/api/worker/assessments").session(worker))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get(0);
    mvc.perform(post("/api/worker/assessments/{id}/submit",assessment.path("skillId").asLong()).session(worker)
        .contentType("application/json").content("[]")).andExpect(status().isBadRequest());

    MockHttpSession employer=login("employer.demo@skilltro.local","employer-demo-123");
    int before=json.readTree(mvc.perform(get("/api/employer/jobs").session(employer))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).size();
    mvc.perform(post("/api/employer/jobs").session(employer).contentType("application/json")
        .content("{\"title\":\"Bad listing\",\"location\":\"Pune\",\"required\":[\"Welding\",\"welding\"],\"preferred\":[]}"))
        .andExpect(status().isBadRequest());
    int after=json.readTree(mvc.perform(get("/api/employer/jobs").session(employer))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).size();
    org.junit.jupiter.api.Assertions.assertEquals(before,after,"invalid job must not be partially persisted");
  }

  private MockHttpSession login(String email,String password) throws Exception {
    MvcResult result=mvc.perform(post("/api/auth/login").contentType("application/json")
        .content(json.writeValueAsString(Map.of("email",email,"password",password))))
        .andExpect(status().isOk()).andReturn();
    return (MockHttpSession)result.getRequest().getSession(false);
  }
  private void addQuestion(String skillName,String prompt,String correct){
    Question q=new Question();q.skill=skills.findByNameIgnoreCase(skillName).orElseThrow();q.prompt=prompt;
    q.optionA=correct;q.optionB="Option B";q.optionC="Option C";q.optionD="Option D";q.correctOption=0;questions.save(q);
  }
  private void addJob(EmployerProfile employer,String title,String location,String desc,String required,String preferred,String safety){
    Job job=new Job();job.employer=employer;job.title=title;job.location=location;job.description=desc;job.safetyRequirements=safety;jobs.save(job);
    addJobSkill(job,required,"REQUIRED");addJobSkill(job,preferred,"PREFERRED");
  }
  private void addJobSkill(Job job,String skillName,String type){JobSkill row=new JobSkill();row.job=job;row.skill=skills.findByNameIgnoreCase(skillName).orElseThrow();row.requirementType=type;jobSkills.save(row);}
}
