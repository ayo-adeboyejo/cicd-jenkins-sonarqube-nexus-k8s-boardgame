package com.javaproject.controllers;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.ErrorMessage;
import com.javaproject.database.DatabaseAccess;

/**
 * REST controller for BoardGame resource.
 */
@RestController
@RequestMapping("/boardgames")
public class BoardGameController {

    private static final Logger logger =
            LoggerFactory.getLogger(BoardGameController.class);

    private static final String NO_RECORD_FOUND = "No such record";
    private static final String NAME_EXISTS     = "Name already exists.";
    private static final String PATH_ID         = "/{id}";

    private final DatabaseAccess da;

    public BoardGameController(DatabaseAccess da) {
        this.da = da;
    }

    @GetMapping
    public List<BoardGame> getBoardGames() {
        return da.getBoardGames();
    }

    @GetMapping(PATH_ID)
    public ResponseEntity<Object> getBoardGame(@PathVariable Long id) {
        BoardGame boardGame = da.getBoardGame(id);
        if (boardGame != null) {
            return ResponseEntity.ok(boardGame);
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage(NO_RECORD_FOUND));  // ✅ ErrorMessage(String) constructor
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Object> postBoardGame(@RequestBody BoardGame boardGame) {
        try {
            Long id = da.addBoardGame(boardGame);
            boardGame.setId(id);                           // ✅ setId() exists on BoardGame
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path(PATH_ID)
                    .buildAndExpand(id)
                    .toUri();
            return ResponseEntity.created(location).body(boardGame);
        } catch (Exception e) {
            logger.error("Failed to create board game: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorMessage(NAME_EXISTS));   // ✅ ErrorMessage(String) constructor
        }
    }
}
