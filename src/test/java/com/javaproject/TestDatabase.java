package com.javaproject;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;

@SpringBootTest
@AutoConfigureMockMvc
class TestDatabase {

    private DatabaseAccess da;

    @Autowired
    public void setDatabase(DatabaseAccess da) {
        this.da = da;
    }

    // -------------------------------------------------------
    // BoardGame tests
    // -------------------------------------------------------

    @Test
    public void testGetBoardGames() {
        List<BoardGame> games = da.getBoardGames();
        assertNotNull(games);
        assertFalse(games.isEmpty());
    }

    @Test
    public void testGetBoardGame() {
        List<BoardGame> games = da.getBoardGames();
        Long id = games.get(0).getId();

        BoardGame game = da.getBoardGame(id);
        assertNotNull(game);
        assertEquals(id, game.getId());
    }

    @Test
    public void testGetBoardGameNotFound() {
        BoardGame game = da.getBoardGame(99999L);
        assertNull(game);
    }

    @Test
    public void testDatabaseAddBoardGame() {
        BoardGame boardGame = new BoardGame();
        boardGame.setName("onecard");
        boardGame.setLevel(1);
        boardGame.setMinPlayers(2);
        boardGame.setMaxPlayers("+");
        boardGame.setGameType("Party Game");

        int originalSize = da.getBoardGames().size();
        da.addBoardGame(boardGame);
        int newSize = da.getBoardGames().size();

        assertEquals(newSize, originalSize + 1);
    }

    // -------------------------------------------------------
    // Review tests
    // -------------------------------------------------------

    @Test
    public void testGetReviews() {
        List<BoardGame> games = da.getBoardGames();
        Long gameId = games.get(0).getId();

        List<Review> reviews = da.getReviews(gameId);
        assertNotNull(reviews);
        assertFalse(reviews.isEmpty());
    }

    @Test
    public void testGetReviewsEmptyForGameWithNoReviews() {
        // Linkee has no reviews in seed data
        // use stream filter to find by name — order from DB is not guaranteed
        List<BoardGame> games = da.getBoardGames();
        Long gameId = games.stream()
                .filter(g -> g.getName().equals("Linkee"))
                .findFirst()
                .orElseThrow()
                .getId();

        List<Review> reviews = da.getReviews(gameId);
        assertNull(reviews);
    }

    @Test
    public void testGetReview() {
        List<BoardGame> games = da.getBoardGames();
        Long gameId = games.get(0).getId();

        List<Review> reviews = da.getReviews(gameId);
        assertNotNull(reviews);

        Long reviewId = reviews.get(0).getId();
        Review review = da.getReview(reviewId);

        assertNotNull(review);
        assertEquals(reviewId, review.getId());
    }

    @Test
    public void testGetReviewNotFound() {
        Review review = da.getReview(99999L);
        assertNull(review);
    }

    @Test
    public void testAddAndDeleteReview() {
        List<BoardGame> games = da.getBoardGames();
        Long gameId = games.get(0).getId();

        // get size before adding
        List<Review> before = da.getReviews(gameId);
        int beforeSize = before == null ? 0 : before.size();

        // add a unique review
        Review review = new Review();
        review.setGameId(gameId);
        review.setText("Unique review text " + System.currentTimeMillis());
        da.addReview(review);

        // verify size increased
        List<Review> after = da.getReviews(gameId);
        assertNotNull(after);
        assertEquals(beforeSize + 1, after.size());

        // delete the new review
        Long newReviewId = after.get(after.size() - 1).getId();
        da.deleteReview(newReviewId);

        // verify size restored
        List<Review> finalList = da.getReviews(gameId);
        int finalSize = finalList == null ? 0 : finalList.size();
        assertEquals(beforeSize, finalSize);
    }

    @Test
    public void testEditReview() {
        List<BoardGame> games = da.getBoardGames();
        Long gameId = games.get(0).getId();

        List<Review> reviews = da.getReviews(gameId);
        assertNotNull(reviews);

        Review review = reviews.get(0);
        // use timestamp to avoid conflict with TestController.testEditReview
        String updatedText = "Updated text " + System.currentTimeMillis();
        review.setText(updatedText);

        int result = da.editReview(review);
        assertEquals(1, result);

        // verify the update was persisted
        Review updated = da.getReview(review.getId());
        assertEquals(updatedText, updated.getText());
    }

    // -------------------------------------------------------
    // Authority tests
    // -------------------------------------------------------

    @Test
    public void testGetAuthorities() {
        List<String> authorities = da.getAuthorities();
        assertNotNull(authorities);
        assertFalse(authorities.isEmpty());
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("ROLE_MANAGER"));
    }
}