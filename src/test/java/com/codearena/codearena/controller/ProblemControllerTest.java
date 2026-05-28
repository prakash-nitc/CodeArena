package com.codearena.codearena.controller;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.model.Difficulty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ProblemController}.
 *
 * <p>{@code @SpringBootTest} boots the full application context (including the
 * permissive Phase 2 {@code SecurityConfig}), and {@code @AutoConfigureMockMvc}
 * provides a {@link MockMvc} instance that drives the controllers without
 * starting a real HTTP server. Each test sends a request and asserts on the
 * status code and JSON body.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ping_returnsUp() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("codearena"));
    }

    @Test
    void getAllProblems_returnsSeededProblems() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // Two problems are seeded on startup.
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void createProblem_returns201_withLocationHeader_andIsRetrievable() throws Exception {
        ProblemRequest request = new ProblemRequest(
                "Valid Parentheses",
                "Determine if the input string of brackets is valid.",
                Difficulty.EASY,
                List.of("stack", "string"));

        MvcResult result = mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Valid Parentheses"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/problems/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Valid Parentheses"));
    }

    @Test
    void getProblemById_returns404_whenMissing() throws Exception {
        mockMvc.perform(get("/api/problems/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProblem_replacesFields() throws Exception {
        // Create first.
        ProblemRequest create = new ProblemRequest(
                "Old Title", "Old description", Difficulty.MEDIUM, List.of("graph"));
        MvcResult created = mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Then update.
        ProblemRequest update = new ProblemRequest(
                "New Title", "New description", Difficulty.HARD, List.of("dp"));
        mockMvc.perform(put("/api/problems/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.difficulty").value("HARD"));
    }

    @Test
    void updateProblem_returns404_whenMissing() throws Exception {
        ProblemRequest update = new ProblemRequest(
                "Nope", "Nope", Difficulty.EASY, List.of());
        mockMvc.perform(put("/api/problems/{id}", 888888)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProblem_returns204_thenDeletingAgainReturns404() throws Exception {
        ProblemRequest create = new ProblemRequest(
                "To Be Deleted", "temp", Difficulty.EASY, List.of());
        MvcResult created = mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/problems/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/problems/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsOnlyMatchingProblems() throws Exception {
        // A token unique to this test makes the assertion order-independent.
        String token = "zphase3searchtoken";
        ProblemRequest request = new ProblemRequest(
                "Find " + token + " Here", "desc", Difficulty.MEDIUM, List.of("misc"));
        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/problems").param("search", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Find " + token + " Here"));
    }

    @Test
    void filterByDifficultyAndSearch_combinesBothFilters() throws Exception {
        String tag = "zphase3hardtag";
        ProblemRequest request = new ProblemRequest(
                "Hard Combo", "desc", Difficulty.HARD, List.of(tag));
        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/problems")
                        .param("difficulty", "HARD")
                        .param("search", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].difficulty").value("HARD"));
    }

    @Test
    void stats_returnsTotalAndAllDifficultyBuckets() throws Exception {
        mockMvc.perform(get("/api/problems/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.countByDifficulty.EASY").exists())
                .andExpect(jsonPath("$.countByDifficulty.MEDIUM").exists())
                .andExpect(jsonPath("$.countByDifficulty.HARD").exists());
    }

    @Test
    void createProblem_returns400_withFieldErrors_whenTitleBlank() throws Exception {
        ProblemRequest request = new ProblemRequest(
                "   ", "a description", Difficulty.EASY, List.of());

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createProblem_returns409_whenTitleDuplicate() throws Exception {
        String title = "Duplicate Title Phase5";
        ProblemRequest request = new ProblemRequest(
                title, "a description", Difficulty.EASY, List.of());

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Same title again (different case) -> 409 Conflict.
        ProblemRequest dup = new ProblemRequest(
                title.toLowerCase(), "another", Difficulty.HARD, List.of());
        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createProblem_returns400_whenDifficultyInvalid() throws Exception {
        // Raw JSON with an enum value that doesn't exist -> unreadable body -> 400.
        String body = "{\"title\":\"X\",\"description\":\"d\",\"difficulty\":\"SUPERHARD\"}";

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProblemById_returns400_whenIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/problems/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
