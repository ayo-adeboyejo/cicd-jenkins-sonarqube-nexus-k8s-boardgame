package com.javaproject.controllers;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;

@Controller
public class HomeController {

    private static final Logger logger =
            LoggerFactory.getLogger(HomeController.class);

    private static final String REDIRECT_ROOT    = "redirect:/";
    private static final String ATTR_BOARDGAME   = "boardgame";
    private static final String ATTR_REVIEW      = "review";
    private static final String RETURN_VALUE_LOG = "return value is: {}";
    private static final String VIEW_ADD_REVIEW  = "secured/addReview";

    private final DatabaseAccess da;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcUserDetailsManager jdbcUserDetailsManager;

    public HomeController(DatabaseAccess da,
                          @Lazy BCryptPasswordEncoder passwordEncoder,
                          JdbcUserDetailsManager jdbcUserDetailsManager) {
        this.da = da;
        this.passwordEncoder = passwordEncoder;
        this.jdbcUserDetailsManager = jdbcUserDetailsManager;
    }

    @GetMapping("/newUser")
    public String newUser(Model model) {
        List<String> authorities = da.getAuthorities();
        model.addAttribute("authorities", authorities);
        return "new-user";
    }

    @PostMapping("/addUser")
    public String addUser(@RequestParam String userName,
            @RequestParam String password,
            @RequestParam String[] authorities,
            Model model,
            RedirectAttributes redirectAttrs) {

        List<GrantedAuthority> authorityList = new ArrayList<>();
        for (String authority : authorities) {
            authorityList.add(new SimpleGrantedAuthority(authority));
        }

        String encodedPassword = passwordEncoder.encode(password);

        if (jdbcUserDetailsManager.userExists(userName)) {
            model.addAttribute("errorMsg",
                    "User name already Exists. Try a different user name.");
            model.addAttribute("authorities", authorityList);
            return "new-user";
        } else {
            User user = new User(userName, encodedPassword, authorityList);
            jdbcUserDetailsManager.createUser(user);
            redirectAttrs.addFlashAttribute("userAddedMsg", "User succesfully added!");
            return REDIRECT_ROOT;
        }
    }

    @GetMapping("/")
    public String goHome(Model model) {
        List<BoardGame> boardgames = da.getBoardGames();
        model.addAttribute("boardgames", boardgames);
        return "index";
    }

    @GetMapping("/{id}")
    public String getBoardgameDetail(@PathVariable Long id, Model model) {
        model.addAttribute(ATTR_BOARDGAME, da.getBoardGame(id));
        return ATTR_BOARDGAME;
    }

    @GetMapping("/{id}/reviews")
    public String getReviews(@PathVariable Long id, Model model) {
        model.addAttribute(ATTR_BOARDGAME, da.getBoardGame(id));
        model.addAttribute("reviews", da.getReviews(id));
        return "review";
    }

    @GetMapping("/secured/addReview/{id}")
    public String addReview(@PathVariable Long id, Model model) {
        model.addAttribute(ATTR_BOARDGAME, da.getBoardGame(id));
        model.addAttribute(ATTR_REVIEW, new Review());
        return VIEW_ADD_REVIEW;
    }

    @GetMapping("/{gameId}/reviews/{id}")
    public String editReview(@PathVariable Long gameId,
            @PathVariable Long id, Model model) {
        Review review = da.getReview(id);
        model.addAttribute(ATTR_REVIEW, review);
        model.addAttribute(ATTR_BOARDGAME, da.getBoardGame(gameId));
        return VIEW_ADD_REVIEW;
    }

    @GetMapping("/secured/addBoardGame")
    public String addBoardGame(Model model) {
        model.addAttribute(ATTR_BOARDGAME, new BoardGame());
        return "secured/addBoardGame";
    }

    @PostMapping("/boardgameAdded")
    public String boardgameAdded(@ModelAttribute BoardGame boardgame) {
        Long returnValue = da.addBoardGame(boardgame);
        logger.info(RETURN_VALUE_LOG, returnValue);
        return REDIRECT_ROOT;
    }

    @PostMapping("/reviewAdded")
    public String reviewAdded(@ModelAttribute Review review) {
        int returnValue;
        if (review.getId() != null) {
            returnValue = da.editReview(review);
        } else {
            returnValue = da.addReview(review);
        }
        logger.info(RETURN_VALUE_LOG, returnValue);

        Long gameId = review.getGameId();
        if (gameId == null) {
            return REDIRECT_ROOT;
        }
        return REDIRECT_ROOT + gameId + "/reviews";
    }

    @GetMapping("/deleteReview/{id}")
    public String deleteReview(@PathVariable Long id) {
        Review review = da.getReview(id);
        if (review == null) {
            return REDIRECT_ROOT;
        }
        Long gameId = review.getGameId();
        int returnValue = da.deleteReview(id);
        logger.info(RETURN_VALUE_LOG, returnValue);
        return REDIRECT_ROOT + gameId + "/reviews";
    }

    @GetMapping("/user")
    public String goToUserSecured() {
        return "secured/user/index";
    }

    @GetMapping("/manager")
    public String goToManagerSecured() {
        return "secured/manager/index";
    }

    @GetMapping("/secured")
    public String goToSecured() {
        return "secured/gateway";
    }

    @GetMapping("/login")
    public String goToLogin() {
        return "login";
    }

    @GetMapping("/permission-denied")
    public String goToDenied() {
        return "error/permission-denied";
    }
}