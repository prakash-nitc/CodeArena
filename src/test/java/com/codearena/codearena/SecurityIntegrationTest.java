package com.codearena.codearena;

import com.codearena.codearena.dto.LoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authentication & authorization tests that exercise the real JWT
 * filter and role rules — no {@code @WithMockUser} shortcut here. Tokens are
 * obtained by logging in as the seeded {@code admin}/{@code user} accounts and
 * sent in the {@code Authorization: Bearer} header, just like a real client.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenFor(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createProblem(String token, String title) throws Exception {
        ProblemRequest request = new ProblemRequest(title, "desc", Difficulty.EASY, List.of());
        MvcResult result = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void listingProblems_isPublic() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk());
    }

    @Test
    void creatingProblem_withoutToken_returns401() throws Exception {
        ProblemRequest request = new ProblemRequest("No Auth", "desc", Difficulty.EASY, List.of());
        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingProblem_withUserToken_succeeds() throws Exception {
        String userToken = tokenFor("user", "user123");
        createProblem(userToken, "SecIT user can create");
    }

    @Test
    void deletingProblem_asUser_returns403() throws Exception {
        String adminToken = tokenFor("admin", "admin123");
        long id = createProblem(adminToken, "SecIT delete forbidden for user");

        String userToken = tokenFor("user", "user123");
        mockMvc.perform(delete("/api/problems/{id}", id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingProblem_asAdmin_returns204() throws Exception {
        String adminToken = tokenFor("admin", "admin123");
        long id = createProblem(adminToken, "SecIT delete allowed for admin");

        mockMvc.perform(delete("/api/problems/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
