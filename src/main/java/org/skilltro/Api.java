package org.skilltro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.skilltro.Domain.*;

/** HTTP routes and session handling. Business rules live in SkillTroService. */
@RestController
@RequestMapping("/api")
class Api {
  private final SkillTroService service;
  Api(SkillTroService service){this.service=service;}

  @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
  AuthResponse register(@Valid @RequestBody RegisterRequest in,HttpSession session,HttpServletRequest request){User u=service.register(in);request.changeSessionId();session.setAttribute("uid",u.id);return auth(u);}
  @PostMapping("/auth/login") AuthResponse login(@Valid @RequestBody LoginRequest in,HttpSession session,HttpServletRequest request){User u=service.authenticate(in);request.changeSessionId();session.setAttribute("uid",u.id);return auth(u);}
  @PostMapping("/auth/logout") Map<String,String> logout(HttpSession s){s.invalidate();return Map.of("message","Signed out");}
  @GetMapping("/auth/me") AuthResponse me(HttpSession s){return auth(current(s));}
  @GetMapping("/skills") List<Map<String,Object>> allSkills(){return service.allSkills();}

  @GetMapping("/worker/profile") Map<String,Object> workerProfile(HttpSession s){return service.workerProfile(worker(s));}
  @PutMapping("/worker/profile") Map<String,Object> saveWorkerProfile(@Valid @RequestBody ProfileRequest in,HttpSession s){return service.saveWorkerProfile(worker(s),in);}
  @GetMapping("/worker/suggestions") List<Map<String,Object>> suggestions(HttpSession s){return service.suggestions(worker(s));}
  @PostMapping("/worker/skills") Map<String,Object> updateSkill(@Valid @RequestBody WorkerSkillRequest in,HttpSession s){return service.updateSkill(worker(s),in);}
  @GetMapping("/worker/assessments") List<Map<String,Object>> assessmentList(HttpSession s){worker(s);return service.assessmentList();}
  @GetMapping("/worker/assessments/{skillId}") List<Map<String,Object>> assessment(@PathVariable Long skillId,HttpSession s){worker(s);return service.assessment(skillId);}
  @PostMapping("/worker/assessments/{skillId}/submit") Map<String,Object> submitAssessment(@PathVariable Long skillId,@Valid @RequestBody List<@Valid AnswerRequest> answers,HttpSession s){return service.submitAssessment(worker(s),skillId,answers);}
  @GetMapping("/worker/assessments/results") List<Map<String,Object>> results(HttpSession s){return service.results(worker(s));}
  @GetMapping("/worker/jobs") List<Map<String,Object>> discoverJobs(@RequestParam(required=false) String search,@RequestParam(required=false) String location,HttpSession s){return service.discoverJobs(worker(s),search,location);}
  @PostMapping("/worker/jobs/{id}/apply") Map<String,Object> apply(@PathVariable Long id,HttpSession s){return service.apply(worker(s),id);}
  @GetMapping("/worker/applications") List<Map<String,Object>> myApplications(HttpSession s){return service.myApplications(worker(s));}

  @GetMapping("/employer/profile") Map<String,Object> employerProfile(HttpSession s){return service.employerProfile(employer(s));}
  @PutMapping("/employer/profile") Map<String,Object> saveEmployerProfile(@Valid @RequestBody ProfileRequest in,HttpSession s){return service.saveEmployerProfile(employer(s),in);}
  @GetMapping("/employer/jobs") List<Map<String,Object>> employerJobs(HttpSession s){return service.employerJobs(employer(s));}
  @PostMapping("/employer/jobs") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createJob(@Valid @RequestBody JobRequest in,HttpSession s){return service.createJob(employer(s),in);}
  @PutMapping("/employer/jobs/{id}") Map<String,Object> editJob(@PathVariable Long id,@Valid @RequestBody JobRequest in,HttpSession s){return service.editJob(employer(s),id,in);}
  @PostMapping("/employer/jobs/{id}/close") Map<String,Object> closeJob(@PathVariable Long id,HttpSession s){return service.closeJob(employer(s),id);}
  @GetMapping("/employer/workers") List<Map<String,Object>> discoverWorkers(@RequestParam(required=false) String skill,@RequestParam(required=false) String location,HttpSession s){employer(s);return service.discoverWorkers(skill,location);}
  @GetMapping("/employer/applications") List<Map<String,Object>> employerApplications(HttpSession s){return service.employerApplications(employer(s));}
  @PutMapping("/employer/applications/{id}") Map<String,Object> updateApplication(@PathVariable Long id,@Valid @RequestBody ApplicationStatusRequest in,HttpSession s){return service.updateApplication(employer(s),id,in);}

  private User current(HttpSession session){Object id=session.getAttribute("uid");if(!(id instanceof Long userId))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in");return service.userById(userId);}
  private User requireRole(HttpSession session,Role role){User user=current(session);if(user.role!=role)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This action is not available for your account");return user;}
  private WorkerProfile worker(HttpSession s){return service.workerProfileByUserId(requireRole(s,Role.WORKER).id);}
  private EmployerProfile employer(HttpSession s){return service.employerProfileByUserId(requireRole(s,Role.EMPLOYER).id);}
  private static AuthResponse auth(User user){return new AuthResponse(user.id,user.email,user.name,user.role);}
}
