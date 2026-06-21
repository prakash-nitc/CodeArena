package com.codearena.codearena.controller;

import com.codearena.codearena.dto.LoginRequest;
import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.RegisterRequest;
import com.codearena.codearena.dto.SubmissionRequest;
import com.codearena.codearena.dto.SubmissionStatusUpdateRequest;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Language;
import com.codearena.codearena.model.SubmissionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for submissions and the leaderboard, using real JWTs (login
 * for the seeded admin, register fresh users) so the JWT filter, ownership rule
 * and role rules are all exercised. Unique usernames/titles keep tests
 * independent against the shared application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return token(result);
    }

    private String registerToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, "password1"))))
                .andExpect(status().isCreated())
                .andReturn();
        return token(result);
    }

    private String token(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createProblem(String adminToken, String title) throws Exception {
        ProblemRequest request = new ProblemRequest(title, "desc", Difficulty.EASY, List.of());
        MvcResult result = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long submit(String token, long problemId) throws Exception {
        SubmissionRequest request = new SubmissionRequest(Language.JAVA, "class Solution {}");
        MvcResult result = mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void submit_createsPendingSubmission() throws Exception {
        String admin = loginToken("admin", "admin123");
        String alice = registerToken("p7_submit_" + SEQ.incrementAndGet());
        long problemId = createProblem(admin, "P7 submit " + SEQ.incrementAndGet());

        mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionRequest(Language.JAVA, "code"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.problemId").value(problemId))
                .andExpect(jsonPath("$.language").value("JAVA"));
    }

    @Test
    void submit_withoutToken_returns401() throws Exception {
        String admin = loginToken("admin", "admin123");
        long problemId = createProblem(admin, "P7 noauth " + SEQ.incrementAndGet());

        mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionRequest(Language.JAVA, "code"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submit_toMissingProblem_returns404() throws Exception {
        String alice = registerToken("p7_missing_" + SEQ.incrementAndGet());

        mockMvc.perform(post("/api/problems/{id}/submissions", 999_999)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionRequest(Language.JAVA, "code"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubmission_ownerCanRead_otherUserGets403() throws Exception {
        String admin = loginToken("admin", "admin123");
        String alice = registerToken("p7_owner_" + SEQ.incrementAndGet());
        String bob = registerToken("p7_other_" + SEQ.incrementAndGet());
        long problemId = createProblem(admin, "P7 ownership " + SEQ.incrementAndGet());
        long submissionId = submit(alice, problemId);

        mockMvc.perform(get("/api/submissions/{id}", submissionId)
                        .header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId));

        mockMvc.perform(get("/api/submissions/{id}", submissionId)
                        .header("Authorization", "Bearer " + bob))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_adminCanJudge_nonAdminGets403() throws Exception {
        String admin = loginToken("admin", "admin123");
        String alice = registerToken("p7_judge_" + SEQ.incrementAndGet());
        long problemId = createProblem(admin, "P7 judging " + SEQ.incrementAndGet());
        long submissionId = submit(alice, problemId);

        SubmissionStatusUpdateRequest accept = new SubmissionStatusUpdateRequest(SubmissionStatus.ACCEPTED);

        // Non-admin cannot judge.
        mockMvc.perform(put("/api/submissions/{id}/status", submissionId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accept)))
                .andExpect(status().isForbidden());

        // Admin can.
        mockMvc.perform(put("/api/submissions/{id}/status", submissionId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void leaderboard_isPublic_andCountsAcceptedSubmissions() throws Exception {
        String admin = loginToken("admin", "admin123");
        String username = "p7_leader_" + SEQ.incrementAndGet();
        String token = registerToken(username);
        long problemId = createProblem(admin, "P7 leaderboard " + SEQ.incrementAndGet());
        long submissionId = submit(token, problemId);

        // Accept it so it counts toward the leaderboard.
        mockMvc.perform(put("/api/submissions/{id}/status", submissionId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmissionStatusUpdateRequest(SubmissionStatus.ACCEPTED))))
                .andExpect(status().isOk());

        // Public read; our user shows up with one solved problem.
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='" + username + "')].solvedCount", hasItem(1)));
    }
}
