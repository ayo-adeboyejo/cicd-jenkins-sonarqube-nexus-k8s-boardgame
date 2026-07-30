package com.javaproject.database;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;

/**
 * Data access layer for BoardGame and Review entities.
 */
@Repository
public class DatabaseAccess {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseAccess.class);

    // SQL constants
    private static final String GET_AUTHORITIES =
            "SELECT DISTINCT authority FROM authorities";

    private static final String GET_ALL_BOARDGAMES =
            "SELECT * FROM boardgames";

    private static final String GET_BOARDGAME_BY_ID =
            "SELECT * FROM boardgames WHERE id = :id";

    private static final String GET_REVIEWS_BY_GAME_ID =
            "SELECT * FROM reviews WHERE gameId = :gameId";

    private static final String GET_REVIEW_BY_ID =
            "SELECT * FROM reviews WHERE id = :id";

    private static final String INSERT_BOARDGAME =
            "INSERT INTO boardgames (name, level, minPlayers, maxPlayers, gameType) " +
            "VALUES (:name, :level, :minPlayers, :maxPlayers, :gameType)";

    // Review table column is "text", Java field is "text" — getText() correct
    private static final String INSERT_REVIEW =
            "INSERT INTO reviews (gameId, text) VALUES (:gameId, :text)";

    private static final String DELETE_REVIEW =
            "DELETE FROM reviews WHERE id = :id";

    private static final String UPDATE_REVIEW =
            "UPDATE reviews SET text = :text WHERE id = :id";

    // Parameter name constants
    private static final String PARAM_ID      = "id";
    private static final String PARAM_GAME_ID = "gameId";
    private static final String PARAM_NAME    = "name";
    private static final String PARAM_TEXT    = "text";
    private static final String PARAM_LEVEL   = "level";
    private static final String PARAM_MIN     = "minPlayers";
    private static final String PARAM_MAX     = "maxPlayers";
    private static final String PARAM_TYPE    = "gameType";

    private final NamedParameterJdbcTemplate jdbc;

    public DatabaseAccess(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> getAuthorities() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        return jdbc.queryForList(GET_AUTHORITIES, params, String.class);
    }

    public List<BoardGame> getBoardGames() {
        BeanPropertyRowMapper<BoardGame> mapper =
                new BeanPropertyRowMapper<>(BoardGame.class);
        return jdbc.query(GET_ALL_BOARDGAMES, mapper);
    }

    public BoardGame getBoardGame(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_ID, id);

        BeanPropertyRowMapper<BoardGame> mapper =
                new BeanPropertyRowMapper<>(BoardGame.class);

        List<BoardGame> results = jdbc.query(GET_BOARDGAME_BY_ID, params, mapper);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Review> getReviews(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_GAME_ID, id);

        BeanPropertyRowMapper<Review> mapper =
                new BeanPropertyRowMapper<>(Review.class);

        List<Review> results = jdbc.query(GET_REVIEWS_BY_GAME_ID, params, mapper);
        return results.isEmpty() ? null : results;
    }

    public Long addBoardGame(BoardGame boardgame) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_NAME,  boardgame.getName())
              .addValue(PARAM_LEVEL, boardgame.getLevel())
              .addValue(PARAM_MIN,   boardgame.getMinPlayers())
              .addValue(PARAM_MAX,   boardgame.getMaxPlayers())
              .addValue(PARAM_TYPE,  boardgame.getGameType());

        KeyHolder key = new GeneratedKeyHolder();
        int rows = jdbc.update(INSERT_BOARDGAME, params, key);

        Number generatedKey = key.getKey();
        if (rows > 0 && generatedKey != null) {
            return generatedKey.longValue();
        }

        logger.warn("addBoardGame returned no key for: {}", boardgame.getName());
        return 0L;
    }

    public int addReview(Review review) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_GAME_ID, review.getGameId())  // ✅ getGameId() on Review
              .addValue(PARAM_TEXT,    review.getText());    // ✅ getText() on Review

        return jdbc.update(INSERT_REVIEW, params);
    }

    public int deleteReview(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_ID, id);
        return jdbc.update(DELETE_REVIEW, params);
    }

    public Review getReview(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_ID, id);

        BeanPropertyRowMapper<Review> mapper =
                new BeanPropertyRowMapper<>(Review.class);

        List<Review> results = jdbc.query(GET_REVIEW_BY_ID, params, mapper);
        return results.isEmpty() ? null : results.get(0);
    }

    public int editReview(Review review) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(PARAM_TEXT, review.getText())   // ✅ getText() on Review
              .addValue(PARAM_ID,   review.getId());    // ✅ getId() on Review

        return jdbc.update(UPDATE_REVIEW, params);
    }
}
