package org.skilltro;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.skilltro.Domain.*;

/** Idempotent, fictional local presentation data. It never creates applications or hiring outcomes. */
@Configuration
class SeedData {
  private static final String DEMO_PREFIX = "Demo data (fictional): ";
  private static final BCryptPasswordEncoder PASSWORDS = new BCryptPasswordEncoder();
  private static final ObjectMapper JSON = new ObjectMapper();

  private record WorkerSeed(String name,String occupation,int years,String city,String description,
      String skillOne,String evidenceOne,String skillTwo,String evidenceTwo) {}
  private record EmployerSeed(String name,String organization,String city,String description) {}
  private record JobSeed(int employer,String title,String location,String description,List<String> required,
      List<String> preferred,int minimumYears,int payMin,int payMax,String payPeriod,String safety) {}

  private static final List<String> SKILL_NAMES=List.of(
      "Carpentry","Welding","Electrical wiring","Plumbing","Masonry","Painting","Cooking",
      "Driving","Machine operation","First aid","Tailoring","Garment finishing","Mobile phone repair",
      "Customer service","Graphic design","Adobe Photoshop","Housekeeping","Gardening","Tiling",
      "Solar panel installation","Delivery logistics","Sewing machine operation","Food safety");

  private static final List<WorkerSeed> WORKERS=List.of(
      new WorkerSeed("Ravi Kumar","Electrician",4,"Hyderabad","Residential repairs, switchboard replacement, conduit routing, and basic solar support.","Electrical wiring","Assisted with fictional home rewiring and switchboard maintenance; work is scenario data, not independently verified.","Solar panel installation","Practiced fictional rooftop panel mounting and cable routing under a sample supervisor scenario."),
      new WorkerSeed("Saira Begum","Tailor",3,"Vijayawada","Stitches blouse and school uniform orders, takes measurements, and finishes hems.","Tailoring","Completed fictional blouse and uniform stitching orders with measurement and fitting notes.","Sewing machine operation","Used a demo industrial sewing-machine workflow for seams, hems, and fabric changes."),
      new WorkerSeed("Manoj Das","Plumber",5,"Visakhapatnam","Handles tap repairs, leak checks, bathroom fittings, and pipe replacement.","Plumbing","Recorded fictional tap, valve, and pipe repair tasks for the demo profile.","Tiling","Practiced a fictional small bathroom tile repair and grout replacement scenario."),
      new WorkerSeed("Lakshmi Devi","Home catering",6,"Hyderabad","Prepares lunch orders, snacks, and small-event menus from a home kitchen.","Cooking","Prepared fictional batch meals and regional snacks for the sample catering scenario.","Food safety","Used fictional prep, storage, and cleaning checklists in the demo kitchen workflow."),
      new WorkerSeed("Imran Ali","Mobile repair technician",2,"Warangal","Diagnoses charging, screen, speaker, and battery issues on common phones.","Mobile phone repair","Practiced fictional phone intake, charging-port diagnosis, and screen replacement records.","Customer service","Created fictional repair intake notes and customer handoff scenarios."),
      new WorkerSeed("Ramesh Yadav","Carpenter",7,"Hyderabad","Builds shelves, repairs doors, measures timber, and installs simple fixtures.","Carpentry","Built fictional storage shelves and repaired doors in the demo workshop record.","Painting","Prepared and painted fictional furniture surfaces with a sample finish checklist."),
      new WorkerSeed("Asha Rani","Mason",4,"Vijayawada","Supports small wall repairs, plastering, and floor-tile installation.","Masonry","Completed fictional brick patching and plaster preparation tasks for the sample site.","Tiling","Practiced fictional floor layout, tile cutting, and grout finishing scenarios."),
      new WorkerSeed("Farooq Khan","Welder",8,"Visakhapatnam","Fabricates mild-steel frames and repairs gates using workshop tools.","Welding","Logged fictional frame-joint and gate-repair work in the sample fabrication shop.","Machine operation","Used fictional cutting and grinding equipment in a supervised demo workflow."),
      new WorkerSeed("Kavitha Rao","Graphic designer",3,"Remote","Creates social posts, flyers, and simple brand assets for local businesses.","Graphic design","Designed fictional event flyers and social tiles for the presentation portfolio.","Adobe Photoshop","Practiced fictional image cleanup, layer editing, and export tasks."),
      new WorkerSeed("Prakash Naidu","Delivery driver",5,"Secunderabad","Plans local routes and delivers parcels and grocery orders across the city.","Driving","Recorded fictional local delivery and vehicle inspection tasks for the scenario.","Delivery logistics","Practiced fictional route sequencing, pickup notes, and proof-of-delivery workflows."),
      new WorkerSeed("Meena Kumari","Cook",5,"Tirupati","Prepares breakfast and lunch for a small neighborhood meal service.","Cooking","Prepared fictional breakfast batches and lunch menus for the sample service.","Food safety","Used fictional food storage and work-surface cleaning checklists."),
      new WorkerSeed("Sanjay Patel","Electrician",3,"Warangal","Assists with lighting, fan installation, and electrical fault checks.","Electrical wiring","Logged fictional lighting and fan installation scenarios in the demo record.","First aid","Completed a fictional first-aid knowledge-check record; this is not a real certification."),
      new WorkerSeed("Anjali Sharma","Housekeeping worker",2,"Hyderabad","Cleans guest rooms and shared areas and restocks basic supplies.","Housekeeping","Maintained fictional room-turnover and shared-area checklists.","Customer service","Practiced fictional guest requests and service handoff scenarios."),
      new WorkerSeed("Venkatesh Reddy","Gardener",6,"Vijayawada","Maintains home gardens, trims hedges, and prepares seasonal beds.","Gardening","Recorded fictional pruning, planting, and garden-bed maintenance tasks.","Machine operation","Used a fictional safe-start checklist for small garden equipment."),
      new WorkerSeed("Noor Fatima","Tailor",5,"Secunderabad","Alters garments, finishes seams, and completes small production batches.","Tailoring","Logged fictional alteration and garment-fit work for the sample profile.","Garment finishing","Practiced fictional seam finishing, button work, and garment inspection."),
      new WorkerSeed("Ajay Mishra","Plumber",4,"Nizamabad","Installs household fixtures and investigates low-pressure water issues.","Plumbing","Completed fictional fixture replacement and leak-diagnosis tasks.","First aid","Completed a fictional first-aid knowledge-check record; this is not a real certification."),
      new WorkerSeed("Deepa Nair","Home catering",7,"Visakhapatnam","Prepares family meals and snack trays for small community gatherings.","Cooking","Prepared fictional meal trays and event snacks in the sample kitchen scenario.","Food safety","Practiced fictional temperature, ingredient, and sanitation checks."),
      new WorkerSeed("Yusuf Ahmed","Carpenter",3,"Warangal","Repairs window frames and assembles compact furniture for households.","Carpentry","Recorded fictional window-frame repair and furniture assembly tasks.","Tiling","Practiced fictional backsplash tile replacement and grout touch-up."),
      new WorkerSeed("Pooja Soni","Graphic designer",2,"Remote","Makes simple product cards, invitations, and digital banners.","Graphic design","Created fictional product cards and digital invitations for the sample portfolio.","Adobe Photoshop","Practiced fictional crop, color-correction, and layer-export scenarios."),
      new WorkerSeed("Gopal Krishna","Mobile repair technician",6,"Hyderabad","Repairs phones and tablets and explains basic device care to customers.","Mobile phone repair","Recorded fictional battery, speaker, and charging fault repairs.","Customer service","Practiced fictional device intake, repair updates, and handover notes."));

  private static final List<EmployerSeed> EMPLOYERS=List.of(
      new EmployerSeed("SkillTro Demo Works","SkillTro Demo Works","Hyderabad","Fictional local maintenance and workshop scenarios for the SkillTro presentation."),
      new EmployerSeed("Nandini Stitch Collective","Nandini Stitch Collective","Vijayawada","Fictional tailoring and garment-work scenarios; no real hiring is represented."),
      new EmployerSeed("Coastal Craft Studio","Coastal Craft Studio","Visakhapatnam","Synthetic home-service and craft listings for a product demonstration."),
      new EmployerSeed("Warangal Repair Circle","Warangal Repair Circle","Warangal","Fictional repair and trade-work scenarios for the presentation."),
      new EmployerSeed("Deccan Home Services","Deccan Home Services","Hyderabad","Sample household repair listings; this fictional profile does not represent a real employer."),
      new EmployerSeed("Godavari Works Cooperative","Godavari Works Cooperative","Vijayawada","Synthetic maintenance and delivery scenarios for demo data."),
      new EmployerSeed("BrightPath Creative Studio","BrightPath Creative Studio","Remote","Fictional design briefs created only for the SkillTro demonstration."),
      new EmployerSeed("Everyday Kitchen Demo Co.","Everyday Kitchen Demo Co.","Hyderabad","Sample catering and kitchen scenarios. No real vacancies are advertised."),
      new EmployerSeed("Urban Workbench Services","Urban Workbench Services","Secunderabad","Synthetic trade and facilities listings for a demo environment."),
      new EmployerSeed("South Shore Maintenance Lab","South Shore Maintenance Lab","Visakhapatnam","Fictional maintenance tasks and scenario listings for presentation use."));

  private static final List<JobSeed> JOBS=List.of(
      job(0,"Electrician — residential wiring helper","Hyderabad","Assist with household lighting, switchboard, and conduit tasks under appropriate supervision.","Electrical wiring","First aid",2,800,1200,"DAY","Electrical work must follow local licensing and safety requirements; supervision is required."),
      job(1,"Tailor — school uniform orders","Vijayawada","Measure, stitch, and finish small batches of school uniforms and alterations.","Tailoring","Sewing machine operation",2,15000,22000,"MONTH","Follow safe machine operation and fabric-handling practices."),
      job(0,"Carpenter — furniture and repair work","Hyderabad","Build simple furniture, repair doors, and install shelves for a fictional local workshop.","Carpentry","Painting",3,1000,1000,"DAY","Use appropriate eye and hearing protection with cutting tools."),
      job(3,"Mobile repair technician","Warangal","Diagnose common charging, screen, and battery issues and record repairs clearly.","Mobile phone repair","Customer service",1,12000,20000,"MONTH","Handle batteries and electronic waste using safe procedures."),
      job(6,"Junior graphic designer","Remote","Prepare social media graphics, event flyers, and basic digital layouts from sample briefs.","Graphic design","Adobe Photoshop",0,18000,28000,"MONTH","All briefs and organizations shown are fictional demo data."),
      job(2,"Plumber — apartment maintenance","Visakhapatnam","Inspect taps and valves and help with small pipe and fixture repairs.","Plumbing","Tiling",2,900,1400,"DAY","Shut off water and confirm the work area is safe before repairs."),
      job(8,"Welder — fabrication assistant","Secunderabad","Support a workshop with mild-steel frames, gates, and basic fabrication tasks.","Welding","Machine operation",3,1100,1600,"DAY","Use welding PPE and follow workshop fire-safety procedures."),
      job(7,"Catering cook — neighborhood meal service","Hyderabad","Prepare daily meal batches and maintain a clean, organized work area.","Cooking","Food safety",2,16000,24000,"MONTH","Follow food hygiene and allergen-handling practices."),
      job(5,"Mason — small repair crew","Vijayawada","Assist with plaster patching, brick repairs, and surface preparation.","Masonry","Tiling",2,850,1300,"DAY","Use appropriate PPE and safe lifting practices."),
      job(4,"Housekeeping attendant","Hyderabad","Prepare guest rooms and common areas and maintain simple restocking records.","Housekeeping","Customer service",1,11000,16000,"MONTH","Use cleaning products according to their labels."),
      job(5,"Local delivery driver","Vijayawada","Complete local pickup and delivery routes with clear delivery records.","Driving","Delivery logistics",2,14000,21000,"MONTH","Valid local driving documents are required for real work; this listing is fictional."),
      job(0,"Solar panel installation assistant","Hyderabad","Support a trained team with panel handling, mounting, and cable routing tasks.","Solar panel installation","Electrical wiring",1,900,1400,"DAY","Electrical tasks require qualified supervision and applicable licenses."),
      job(1,"Garment finishing operator","Vijayawada","Inspect seams, finish hems, and prepare completed garments for packing.","Garment finishing","Tailoring",1,13000,19000,"MONTH","Follow safe machine operation and ergonomic practices."),
      job(7,"Kitchen hygiene steward","Hyderabad","Support meal preparation with ingredient handling, cleaning, and storage routines.","Food safety","Cooking",1,12000,17500,"MONTH","Follow food hygiene and allergen-handling practices."),
      job(8,"Furniture painting assistant","Secunderabad","Prepare wooden surfaces and apply finishes to sample furniture orders.","Painting","Carpentry",1,800,1200,"DAY","Use ventilation and protective equipment when handling coatings."),
      job(2,"Tile installer — home bathrooms","Visakhapatnam","Help lay floor and wall tiles and complete grout and cleanup work.","Tiling","Masonry",2,950,1450,"DAY","Use eye protection when cutting tile."),
      job(9,"Workshop machine operator","Visakhapatnam","Operate basic workshop equipment after site-specific safety orientation.","Machine operation","First aid",2,15000,23000,"MONTH","Site training and machine guarding are required before operation."),
      job(3,"Workshop first-aid support assistant","Warangal","Help maintain first-aid supplies and follow the site emergency plan.","First aid","Customer service",1,12000,18000,"MONTH","This demo listing does not replace recognized first-aid training or certification."),
      job(4,"Garden maintenance worker","Hyderabad","Maintain residential garden beds, prune shrubs, and keep paths tidy.","Gardening","Machine operation",1,800,1200,"DAY","Use garden equipment only after appropriate instruction."),
      job(3,"Repair desk customer assistant","Warangal","Record device drop-offs, describe repair updates, and organize customer handovers.","Customer service","Mobile phone repair",1,11000,17000,"MONTH","All customers and service records in the demo environment are fictional."),
      job(6,"Graphic production artist","Remote","Resize and prepare campaign assets from fictional briefs and approved source files.","Adobe Photoshop","Graphic design",1,20000,30000,"MONTH","All briefs and organizations shown are fictional demo data."),
      job(7,"Home kitchen cook — evening shift","Hyderabad","Prepare evening dishes and package sample orders for a local meal service.","Cooking","Food safety",2,15000,22000,"MONTH","Follow food hygiene and allergen-handling practices."),
      job(9,"Solar panel mounting technician","Visakhapatnam","Assist with panel mounting, site preparation, and cable organization.","Solar panel installation","Electrical wiring",2,1000,1500,"DAY","Electrical tasks require qualified supervision and applicable licenses."),
      job(5,"Last-mile delivery associate","Vijayawada","Plan short delivery routes and confirm handoffs for sample orders.","Delivery logistics","Driving",1,13000,19000,"MONTH","Valid local driving documents are required for real work; this listing is fictional."),
      job(2,"Water pipe repair technician","Visakhapatnam","Support leak checks, pipe fitting, and small wall-repair tasks.","Plumbing","Masonry",3,1000,1500,"DAY","Isolate water supply before repair work."),
      job(0,"Furniture assembly carpenter","Hyderabad","Measure, assemble, and install compact household furniture.","Carpentry","Machine operation",2,900,1350,"DAY","Use appropriate eye and hearing protection with cutting tools."),
      job(1,"Seamstress — alteration counter","Vijayawada","Complete clothing alterations, hems, and customer measurements.","Tailoring","Sewing machine operation",2,14000,20500,"MONTH","Follow safe machine operation and fabric-handling practices."),
      job(8,"Metal fabrication assistant","Secunderabad","Help prepare metal pieces and complete basic workshop welds.","Welding","Machine operation",2,1000,1500,"DAY","Use welding PPE and follow workshop fire-safety procedures."),
      job(4,"Housekeeping team lead","Hyderabad","Coordinate room-turnover checklists and support routine guest requests.","Housekeeping","First aid",3,16000,23000,"MONTH","Use cleaning products according to their labels."),
      job(9,"Mural and surface painter","Visakhapatnam","Prepare walls and apply finishes to sample community-space projects.","Painting","Masonry",2,900,1400,"DAY","Use ventilation and protective equipment when handling coatings."));

  @Bean CommandLineRunner seed(UserRepo users,WorkerRepo workers,EmployerRepo employers,SkillRepo skills,
      WorkerSkillRepo workerSkills,QuestionRepo questions,AttemptRepo attempts,JobRepo jobs,
      JobSkillRepo jobSkills,SkillTroService service) {
    return args -> {
      seedSkills(skills);
      seedAssessments(skills,questions);
      List<WorkerProfile> demoWorkers=new ArrayList<>();
      for(int i=0;i<WORKERS.size();i++){WorkerProfile worker=ensureWorker(i,WORKERS.get(i),users,workers,skills,workerSkills);if(worker!=null&&worker.demo)demoWorkers.add(worker);}
      List<EmployerProfile> demoEmployers=new ArrayList<>();
      for(int i=0;i<EMPLOYERS.size();i++)demoEmployers.add(ensureEmployer(i,EMPLOYERS.get(i),users,employers));
      seedAssessmentResults(demoWorkers,questions,attempts,skills);
      List<Job> demoJobs=new ArrayList<>();
      for(JobSeed row:JOBS){Job job=ensureJob(row,demoEmployers.get(row.employer()),jobs,jobSkills,skills);if(job!=null)demoJobs.add(job);}
      service.refreshDemoMatchSnapshots(demoWorkers,demoJobs);
    };
  }

  private static void seedSkills(SkillRepo skills){
    for(String name:SKILL_NAMES)if(skills.findByNameIgnoreCase(name).isEmpty()){
      Skill skill=new Skill();skill.name=name;skill.category="Practical";skills.save(skill);
    }
  }

  private static WorkerProfile ensureWorker(int index,WorkerSeed seed,UserRepo users,WorkerRepo workers,
      SkillRepo skills,WorkerSkillRepo workerSkills){
    String email=index==0?"worker.demo@skilltro.local":String.format(Locale.ROOT,"demo.worker%02d@skilltro.local",index+1);
    User user=users.findByEmailIgnoreCase(email).orElseGet(()->{
      User created=new User();created.email=email;created.name=seed.name();created.passwordHash=PASSWORDS.encode("worker-demo-123");created.role=Role.WORKER;return users.save(created);
    });
    if(user.role!=Role.WORKER)return null;
    WorkerProfile profile=workers.findByUserId(user.id).orElseGet(()->{
      WorkerProfile created=new WorkerProfile();created.user=user;created.occupation=seed.occupation();created.experienceYears=seed.years();created.location=seed.city();created.workDescription=DEMO_PREFIX+seed.description();created.demo=true;return workers.save(created);
    });
    if(profile.demo){ensureEvidence(profile,seed.skillOne(),seed.evidenceOne(),skills,workerSkills);ensureEvidence(profile,seed.skillTwo(),seed.evidenceTwo(),skills,workerSkills);}
    return profile;
  }

  private static void ensureEvidence(WorkerProfile worker,String skillName,String description,SkillRepo skills,WorkerSkillRepo workerSkills){
    Skill skill=skills.findByNameIgnoreCase(skillName).orElseThrow();
    boolean exists=workerSkills.findByWorkerProfileId(worker.id).stream().anyMatch(row->row.skill.id.equals(skill.id));
    if(!exists){WorkerSkill row=new WorkerSkill();row.workerProfile=worker;row.skill=skill;row.state="CONFIRMED";row.evidence=DEMO_PREFIX+description;workerSkills.save(row);}
  }

  private static EmployerProfile ensureEmployer(int index,EmployerSeed seed,UserRepo users,EmployerRepo employers){
    String email=index==0?"employer.demo@skilltro.local":String.format(Locale.ROOT,"demo.employer%02d@skilltro.local",index+1);
    User user=users.findByEmailIgnoreCase(email).orElseGet(()->{
      User created=new User();created.email=email;created.name=seed.name();created.passwordHash=PASSWORDS.encode("employer-demo-123");created.role=Role.EMPLOYER;return users.save(created);
    });
    if(user.role!=Role.EMPLOYER)return null;
    return employers.findByUserId(user.id).orElseGet(()->{
      EmployerProfile created=new EmployerProfile();created.user=user;created.organization=seed.organization();created.location=seed.city();created.description=seed.description();created.demo=true;return employers.save(created);
    });
  }

  private static Job ensureJob(JobSeed seed,EmployerProfile employer,JobRepo jobs,JobSkillRepo jobSkills,SkillRepo skills){
    if(employer==null)return null;
    Job existing=jobs.findByEmployerId(employer.id).stream().filter(row->row.title.equalsIgnoreCase(seed.title())).findFirst().orElse(null);
    if(existing!=null&&!existing.demo)return null;
    Job job=existing==null?new Job():existing;
    if(existing==null){job.employer=employer;job.title=seed.title();job.location=seed.location();job.description=DEMO_PREFIX+seed.description();job.safetyRequirements=seed.safety();job.minimumExperienceYears=seed.minimumYears();job.payMin=seed.payMin();job.payMax=seed.payMax();job.payPeriod=seed.payPeriod();job.demo=true;job.open=true;job=jobs.save(job);}
    List<JobSkill> existingSkills=jobSkills.findByJobId(job.id);
    for(String name:seed.required())ensureJobSkill(job,name,"REQUIRED",existingSkills,jobSkills,skills);
    for(String name:seed.preferred())ensureJobSkill(job,name,"PREFERRED",existingSkills,jobSkills,skills);
    return job;
  }

  private static void ensureJobSkill(Job job,String name,String type,List<JobSkill> existing,JobSkillRepo jobSkills,SkillRepo skills){
    Skill skill=skills.findByNameIgnoreCase(name).orElseThrow();
    if(existing.stream().noneMatch(row->row.skill.id.equals(skill.id)&&row.requirementType.equals(type))){JobSkill row=new JobSkill();row.job=job;row.skill=skill;row.requirementType=type;jobSkills.save(row);}
  }

  private static void seedAssessments(SkillRepo skills,QuestionRepo questions){
    List<String> quizSkills=List.of("Electrical wiring","Tailoring","Plumbing","Cooking","Mobile phone repair","Carpentry","Masonry","Welding","Graphic design","Driving");
    for(String name:quizSkills){
      Skill skill=skills.findByNameIgnoreCase(name).orElseThrow();
      for(int i=0;i<3;i++){
        String prompt=switch(i){case 0->"What should happen before starting "+name+" work?";case 1->"How can the quality of "+name+" work be checked?";default->"What is the safest response to an unclear instruction during "+name+" work?";};
        if(questions.findBySkillId(skill.id).stream().anyMatch(q->q.prompt.equals(prompt)))continue;
        Question q=new Question();q.skill=skill;q.prompt=prompt;
        if(i==0){q.optionA="Check the work area, tools, and task requirements";q.optionB="Begin immediately to save time";q.optionC="Skip the work-area check for a small task";q.optionD="Ask another person to guess the requirements";}
        else if(i==1){q.optionA="Compare the completed work with the stated requirements";q.optionB="Assume the work is correct if it looks finished";q.optionC="Leave checks until a complaint is received";q.optionD="Change the instructions without asking";}
        else{q.optionA="Pause and clarify before continuing";q.optionB="Continue and hope the instruction was understood";q.optionC="Ignore the instruction";q.optionD="Ask an unrelated person to decide";}
        q.correctOption=0;questions.save(q);
      }
    }
  }

  private static void seedAssessmentResults(List<WorkerProfile> workers,QuestionRepo questions,AttemptRepo attempts,SkillRepo skills){
    for(WorkerProfile worker:workers){
      if(worker==null||!worker.demo)continue;
      int i=workerSeedIndex(worker.user.email);if(i<0||i>=10)continue;
      String skillName=WORKERS.get(i).skillOne();Skill skill=skills.findByNameIgnoreCase(skillName).orElse(null);
      if(skill==null)continue;
      List<Question> qs=questions.findBySkillId(skill.id);if(qs.size()<3)continue;
      boolean exists=attempts.findByWorkerProfileIdOrderByCreatedAtDesc(worker.id).stream().anyMatch(a->a.demo&&a.skill.id.equals(skill.id));
      if(exists)continue;
      int wrongIndex=i%3;List<Map<String,Object>> answers=new ArrayList<>();
      for(int q=0;q<3;q++)answers.add(Map.of("questionId",qs.get(q).id,"option",q==wrongIndex?1:0));
      Attempt result=new Attempt();result.workerProfile=worker;result.skill=skill;result.score=2;result.total=3;result.demo=true;
      try{result.answersJson=JSON.writeValueAsString(answers);}catch(Exception e){throw new IllegalStateException("Could not prepare synthetic assessment data",e);}
      attempts.save(result);
    }
  }

  private static int workerSeedIndex(String email){
    if("worker.demo@skilltro.local".equalsIgnoreCase(email))return 0;
    if(email==null||!email.startsWith("demo.worker")||!email.endsWith("@skilltro.local"))return -1;
    try{return Integer.parseInt(email.substring("demo.worker".length(),email.indexOf('@')))-1;}catch(NumberFormatException e){return -1;}
  }

  private static JobSeed job(int employer,String title,String location,String description,String required,String preferred,
      int years,int min,int max,String period,String safety){
    return new JobSeed(employer,title,location,description,List.of(required),preferred.isBlank()?List.of():List.of(preferred),years,min,max,period,safety);
  }
}
