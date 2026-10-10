package org.skilltro;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.skilltro.Domain.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:skilltro-api-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc
class JpaApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JpaUserRepo users;
  @Autowired JpaWorkerRepo workers;
  @Autowired JpaWorkerSkillRepo workerSkills;
  @Autowired JpaSkillRepo skills;
  @Autowired JpaQuestionRepo questions;
  @Autowired JpaAttemptRepo attempts;
  @Autowired JpaApplicationRepo applications;
  @Autowired JpaJobRepo jobs;
  @Autowired JpaJobSkillRepo jobSkills;
  @Autowired JpaMatchResultRepo matches;

  @Test void authenticatedWorkerLoadsAndPersistsRealJpaMatches() throws Exception {
    MvcResult login=mvc.perform(post("/api/auth/login").contentType("application/json")
        .content(json.writeValueAsString(Map.of("email","worker.demo@skilltro.local","password","worker-demo-123"))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("WORKER")).andReturn();
    MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
    assertNotNull(session);

    mvc.perform(get("/api/worker/jobs").session(session)).andExpect(status().isOk())
        .andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(30)))
        .andExpect(jsonPath("$[0].explanation").isNotEmpty())
        .andExpect(jsonPath("$[0].matchPercentage").isNumber());
    User user=users.findByEmailIgnoreCase("worker.demo@skilltro.local").orElseThrow();
    WorkerProfile worker=workers.findByUserId(user.id).orElseThrow();
    Job job=jobs.findByOpenTrueOrderByCreatedAtDesc().get(0);
    MatchResult match=matches.findByWorkerProfileIdAndJobId(worker.id,job.id).orElseThrow();
    assertNotNull(match.explanation);
    assertNotNull(match.relevantEvidenceJson);
  }

  @Test void startupSeedsFictionalRecordsIdempotentlyWithoutApplications() throws Exception {
    long demoWorkerCount=workers.findAll().stream().filter(w->w.demo).count();
    assertEquals(20,demoWorkerCount);
    assertEquals(30,jobs.findByOpenTrueOrderByCreatedAtDesc().stream().filter(j->j.demo&&j.description!=null&&j.description.startsWith("Demo data (fictional):")).count());
    assertEquals(23,skills.findAll().size());
    long evidenceCount=workers.findAll().stream().filter(w->w.demo)
        .mapToLong(w->workerSkills.findByWorkerProfileId(w.id).stream().filter(row->row.evidence!=null&&row.evidence.startsWith("Demo data (fictional):")).count()).sum();
    assertEquals(40,evidenceCount);
    long employerCount=users.findAll().stream().filter(u->u.role==Role.EMPLOYER&&u.email.endsWith("@skilltro.local")).count();
    assertEquals(10,employerCount);
    assertEquals(30,questions.findAll().size());
    assertEquals(60,jobSkills.findAll().stream().filter(row->row.job.demo&&row.job.description!=null&&row.job.description.startsWith("Demo data (fictional):")).count());
    long demoAttempts=workers.findAll().stream().filter(w->w.demo)
        .mapToLong(w->attempts.findByWorkerProfileIdOrderByCreatedAtDesc(w.id).stream().filter(a->a.demo).count()).sum();
    assertEquals(10,demoAttempts);
    assertEquals(0,applications.count());
    long snapshotCount=0;
    for(WorkerProfile worker:workers.findAll())if(worker.demo)for(Job job:jobs.findByOpenTrueOrderByCreatedAtDesc())if(job.demo&&job.description!=null&&job.description.startsWith("Demo data (fictional):")&&matches.findByWorkerProfileIdAndJobId(worker.id,job.id).isPresent())snapshotCount++;
    assertEquals(600,snapshotCount);
    MockHttpSession employerSession=(MockHttpSession)mvc.perform(post("/api/auth/login").contentType("application/json")
        .content(json.writeValueAsString(Map.of("email","employer.demo@skilltro.local","password","employer-demo-123"))))
        .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    mvc.perform(get("/api/employer/workers").session(employerSession)).andExpect(status().isOk())
        .andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(20)))
        .andExpect(jsonPath("$[0].demo").value(true));
  }

  @Test void employerCanCreateAndEditAnOpportunityThatPersistsInH2() throws Exception {
    MockHttpSession session=(MockHttpSession)mvc.perform(post("/api/auth/login").contentType("application/json")
        .content(json.writeValueAsString(Map.of("email","employer.demo@skilltro.local","password","employer-demo-123"))))
        .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    String create="{\"title\":\"H2 persistence test opportunity\",\"location\":\"Hyderabad\",\"required\":[\"Electrical wiring\"],\"preferred\":[],\"minimumExperienceYears\":2,\"payMin\":900,\"payMax\":1300,\"payPeriod\":\"DAY\"}";
    MvcResult created=mvc.perform(post("/api/employer/jobs").session(session).contentType("application/json").content(create))
        .andExpect(status().isCreated()).andReturn();
    long id=json.readTree(created.getResponse().getContentAsString()).path("id").asLong();
    Job persisted=((JobRepo)jobs).findById(id).orElseThrow();
    assertEquals(900,persisted.payMin);
    assertEquals(2,persisted.minimumExperienceYears);

    String edit="{\"title\":\"Edited H2 persistence test opportunity\",\"location\":\"Secunderabad\",\"required\":[\"Electrical wiring\"],\"preferred\":[],\"minimumExperienceYears\":3,\"payMin\":1100,\"payMax\":1500,\"payPeriod\":\"DAY\"}";
    mvc.perform(put("/api/employer/jobs/{id}",id).session(session).contentType("application/json").content(edit))
        .andExpect(status().isOk());
    Job edited=((JobRepo)jobs).findById(id).orElseThrow();
    assertEquals("Edited H2 persistence test opportunity",edited.title);
    assertEquals("Secunderabad",edited.location);
    assertEquals(1100,edited.payMin);
    assertEquals(3,edited.minimumExperienceYears);
  }

  @Test void corsPreflightAndValidationErrorsAreHandled() throws Exception {
    mvc.perform(options("/api/auth/login").header("Origin","http://localhost:5173")
        .header("Access-Control-Request-Method","POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"))
        .andExpect(header().string("Access-Control-Allow-Credentials","true"));

    mvc.perform(post("/api/auth/register").contentType("application/json")
        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"name\":\"\",\"role\":\"WORKER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Please check the submitted fields."))
        .andExpect(jsonPath("$.errors.email").exists());
  }
}
