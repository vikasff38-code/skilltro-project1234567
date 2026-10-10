package org.skilltro;

import jakarta.validation.constraints.*;
import java.util.List;
import static org.skilltro.Domain.Role;

record RegisterRequest(
    @Email @NotBlank @Size(max=254) String email,
    @NotBlank @Size(min=8,max=72) String password,
    @NotBlank @Size(max=120) String name,
    @NotNull Role role) {}

record LoginRequest(@Email @NotBlank @Size(max=254) String email,@NotBlank String password) {}

record ProfileRequest(
    @Size(max=120) String occupation,
    @Min(0) int experienceYears,
    @Size(max=160) String location,
    @Size(max=4000) String workDescription,
    @Size(max=160) String organization,
    @Size(max=2000) String description) {}

record WorkerSkillRequest(
    Long id,
    @Size(max=100) String name,
    @Pattern(regexp="(?i)CONFIRMED|REJECTED") String state,
    @Size(max=2000) String evidence) {}

record JobRequest(
    @NotBlank @Size(max=160) String title,
    @NotBlank @Size(max=160) String location,
    @Size(max=4000) String description,
    @Size(max=40) List<@NotBlank @Size(max=100) String> required,
    @Size(max=40) List<@NotBlank @Size(max=100) String> preferred,
    @Size(max=1000) String safetyRequirements,
    @Min(0) @Max(80) Integer minimumExperienceYears,
    @Min(0) @Max(10000000) Integer payMin,
    @Min(0) @Max(10000000) Integer payMax,
    @Pattern(regexp="(?i)DAY|MONTH|PROJECT") String payPeriod) {}

record AnswerRequest(@NotNull Long questionId,@Min(0) @Max(3) int option) {}

record ApplicationStatusRequest(
    @NotBlank @Pattern(regexp="REVIEWING|SHORTLISTED|REJECTED|ACCEPTED") String status) {}

record AuthResponse(Long id,String email,String name,Role role) {}
