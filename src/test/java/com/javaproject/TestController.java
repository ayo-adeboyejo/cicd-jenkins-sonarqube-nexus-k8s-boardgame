package com.javaproject;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;

@SpringBootTest
@AutoConfigureMockMvc
class TestController {

    private DatabaseAccess da;
    private MockMvc mockMvc;

    @Autowired
    public void setDatabase(DatabaseAccess da) {
        this.da = da;
    }

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // -------------------------------------------------------
    // Public page tests — no authentication required
    // -------------------------------------------------------

    @Test
    public void testRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    public void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void testNewUserPage() throws Exception {
        mockMvc.perform(get("/newUser"))
                .andExpect(status().isOk())
                .andExpect(view().name("new-user"));
    }

    @Test
    public void testPermissionDeniedPage() throws Exception {
        mockMvc.perform(get("/permission-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/permission-denied"));
    }

    @Test
    public void testGetBoardgameDetail() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long id = boardGames.get(0).getId();

        mockMvc.perform(get("/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("boardgame"));
    }

    @Test
    public void testGetReviews() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long id = boardGames.get(0).getId();

        mockMvc.perform(get("/{id}/reviews", id))
                .andExpect(status().isOk())
                .andExpect(view().name("review"));
    }

    // -------------------------------------------------------
    // Secured page tests — authentication required
    // -------------------------------------------------------

    @Test
    @WithMockUser(roles = "USER")
    public void testSecuredGatewayWithUser() throws Exception {
        mockMvc.perform(get("/secured"))
                .andExpect(status().isOk())
                .andExpect(view().name("secured/gateway"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void testManagerPage() throws Exception {
        mockMvc.perform(get("/manager"))
                .andExpect(status().isOk())
                .andExpect(view().name("secured/manager/index"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAddReviewPage() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long id = boardGames.get(0).getId();

        mockMvc.perform(get("/secured/addReview/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("secured/addReview"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAddBoardGamePage() throws Exception {
        mockMvc.perform(get("/secured/addBoardGame"))
                .andExpect(status().isOk())
                .andExpect(view().name("secured/addBoardGame"));
    }

    // -------------------------------------------------------
    // Form submission tests — POST requests require CSRF
    // -------------------------------------------------------

    @Test
    public void testAddBoardGame() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        // unique name — boardgames.name now has UNIQUE constraint
        requestParams.add("name", "onecard" + System.currentTimeMillis());
        requestParams.add("level", "1");
        requestParams.add("minPlayers", "2");
        requestParams.add("maxPlayers", "+");
        requestParams.add("gameType", "Party Game");

        int origSize = da.getBoardGames().size();

        mockMvc.perform(post("/boardgameAdded")
                .params(requestParams)
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andDo(print());

        int newSize = da.getBoardGames().size();
        assertEquals(newSize, origSize + 1);
    }

    @Test
    public void testAddUserSuccess() throws Exception {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        String testCredential = "T3st" + System.currentTimeMillis();
        params.add("userName", "testuser" + System.currentTimeMillis());
        params.add("password", testCredential);
        params.add("authorities", "ROLE_USER");

        mockMvc.perform(post("/addUser")
                .params(params)
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testAddUserDuplicate() throws Exception {
        // bugs is a seeded user — adding again should return new-user view
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("userName", "bugs");
        params.add("password", "bunny");
        params.add("authorities", "ROLE_USER");

        mockMvc.perform(post("/addUser")
                .params(params)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("new-user"));
    }

    @Test
    public void testEditReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        Review review = reviews.get(0);
        Long reviewId = review.getId();

        // unique text — reviews.text has UNIQUE constraint
        // timestamp also avoids conflict with TestDatabase.testEditReview
        String editedText = "Edited text " + System.currentTimeMillis();
        review.setText(editedText);

        mockMvc.perform(post("/reviewAdded")
                .flashAttr("review", review)
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/" + review.getGameId() + "/reviews"));

        review = da.getReview(reviewId);
        assertEquals(review.getText(), editedText);
    }

    @Test
    public void testDeleteReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        Long reviewId = reviews.get(0).getId();

        int origSize = reviews.size();

        mockMvc.perform(get("/deleteReview/{id}", reviewId))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/" + boardgameId + "/reviews"));

        int newSize = da.getReviews(boardgameId).size();
        assertEquals(newSize, origSize - 1);
    }

    // -------------------------------------------------------
    // BoardGameController REST API tests
    // /boardgames/** has CSRF disabled in SecurityConfig
    // so no .with(csrf()) needed for these endpoints
    // -------------------------------------------------------

    @Test
    public void testGetBoardGamesRest() throws Exception {
        mockMvc.perform(get("/boardgames")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetBoardGameRestFound() throws Exception {
        List<BoardGame> games = da.getBoardGames();
        Long id = games.get(0).getId();

        mockMvc.perform(get("/boardgames/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetBoardGameRestNotFound() throws Exception {
        mockMvc.perform(get("/boardgames/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPostBoardGameRestSuccess() throws Exception {
        // unique name — boardgames.name now has UNIQUE constraint
        String json = "{"
                + "\"name\": \"RestTestGame" + System.currentTimeMillis() + "\","
                + "\"level\": 2,"
                + "\"minPlayers\": 2,"
                + "\"maxPlayers\": \"4\","
                + "\"gameType\": \"Strategy Game\""
                + "}";

        mockMvc.perform(post("/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void testPostBoardGameRestConflict() throws Exception {
        // Splendor already exists in seed data
        // with UNIQUE constraint on name this now correctly triggers 409
        String json = "{"
                + "\"name\": \"Splendor\","
                + "\"level\": 3,"
                + "\"minPlayers\": 2,"
                + "\"maxPlayers\": \"4\","
                + "\"gameType\": \"Strategy Game\""
                + "}";

        mockMvc.perform(post("/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict());
    }
}