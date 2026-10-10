package org.skilltro;

import jakarta.persistence.*;
import java.time.Instant;

public final class Domain {
  private Domain() {}
  @MappedSuperclass public abstract static class Timed {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Instant createdAt=Instant.now();
    @Column(nullable=false) public Instant updatedAt=Instant.now();
    @PreUpdate public void touch(){updatedAt=Instant.now();}
  }
  public enum Role { WORKER, EMPLOYER }
  @Entity @Table(name="users") public static class User extends Timed {
    @Column(nullable=false,unique=true) public String email;
    @Column(nullable=false) public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role;
    @Column(nullable=false) public String name;
  }
  @Entity @Table(name="worker_profiles") public static class WorkerProfile extends Timed {
    @OneToOne(optional=false) @JoinColumn(nullable=false,unique=true) public User user;
    public String occupation; public int experienceYears; public String location;
    @Column(length=4000) public String workDescription;
    @Column(nullable=false) public boolean demo;
  }
  @Entity @Table(name="employer_profiles") public static class EmployerProfile extends Timed {
    @OneToOne(optional=false) @JoinColumn(nullable=false,unique=true) public User user;
    public String organization; public String location; @Column(length=2000) public String description;
    @Column(nullable=false) public boolean demo;
  }
  @Entity @Table(name="skills") public static class Skill extends Timed {
    @Column(nullable=false,unique=true) public String name;
    public String category;
  }
  @Entity @Table(name="worker_skills", uniqueConstraints=@UniqueConstraint(columnNames={"worker_profile_id","skill_id"}))
  public static class WorkerSkill extends Timed {
    @ManyToOne(optional=false) public WorkerProfile workerProfile;
    @ManyToOne(optional=false) public Skill skill;
    @Column(nullable=false) public String state="CONFIRMED";
    @Column(length=2000) public String evidence;
  }
  @Entity @Table(name="assessment_questions") public static class Question extends Timed {
    @ManyToOne(optional=false) public Skill skill;
    @Column(nullable=false,length=1000) public String prompt;
    @Column(name="option_a",nullable=false) public String optionA; @Column(name="option_b",nullable=false) public String optionB;
    @Column(name="option_c",nullable=false) public String optionC; @Column(name="option_d",nullable=false) public String optionD;
    @Column(nullable=false) public int correctOption;
  }
  @Entity @Table(name="assessment_attempts") public static class Attempt extends Timed {
    @ManyToOne(optional=false) public WorkerProfile workerProfile;
    @ManyToOne(optional=false) public Skill skill;
    @Column(nullable=false) public int score; @Column(nullable=false) public int total;
    @Column(nullable=false,columnDefinition="text") public String answersJson;
    @Column(nullable=false) public boolean demo;
  }
  @Entity @Table(name="jobs") public static class Job extends Timed {
    @ManyToOne(optional=false) public EmployerProfile employer;
    @Column(nullable=false) public String title; @Column(nullable=false) public String location;
    @Column(length=4000) public String description;
    @Column(nullable=false) public boolean demo=true; @Column(nullable=false) public boolean open=true;
    @Column(length=1000) public String safetyRequirements="";
    @Column(nullable=false) public int minimumExperienceYears;
    public Integer payMin; public Integer payMax;
    @Column(nullable=false,length=32) public String payPeriod="PROJECT";
  }
  @Entity @Table(name="job_skills",uniqueConstraints=@UniqueConstraint(columnNames={"job_id","skill_id","requirement_type"}))
  public static class JobSkill extends Timed {
    @ManyToOne(optional=false) public Job job; @ManyToOne(optional=false) public Skill skill;
    @Column(name="requirement_type",nullable=false) public String requirementType="REQUIRED";
  }
  @Entity @Table(name="applications",uniqueConstraints=@UniqueConstraint(columnNames={"job_id","worker_profile_id"}))
  public static class Application extends Timed {
    @ManyToOne(optional=false) public Job job; @ManyToOne(optional=false) public WorkerProfile workerProfile;
    @Column(nullable=false) public String status="SUBMITTED";
  }
  @Entity @Table(name="matching_results",uniqueConstraints=@UniqueConstraint(name="uk_match_worker_job",columnNames={"worker_profile_id","job_id"}))
  public static class MatchResult extends Timed {
    @ManyToOne(optional=false) @JoinColumn(name="worker_profile_id",nullable=false) public WorkerProfile workerProfile;
    @ManyToOne(optional=false) @JoinColumn(name="job_id",nullable=false) public Job job;
    @Column(nullable=false) public int matchPercentage;
    @Column(nullable=false) public int matchedRequiredCount;
    @Column(nullable=false) public int totalRequiredCount;
    @Column(nullable=false,columnDefinition="text") public String matchedSkillsJson="[]";
    @Column(nullable=false,columnDefinition="text") public String missingSkillsJson="[]";
    @Column(nullable=false,columnDefinition="text") public String preferredSkillsJson="[]";
    @Column(nullable=false) public boolean experienceRequirementMet;
    @Column(nullable=false) public boolean locationMatched;
    @Column(length=1000) public String explanation;
    @Column(nullable=false,columnDefinition="text") public String relevantEvidenceJson="{}";
  }
}
