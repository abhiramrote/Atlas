package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.CurrentUserResponse;
import com.abhiram.atlas.entity.AppUser;
import com.abhiram.atlas.security.AtlasUserPrincipal;
import com.abhiram.atlas.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the signed in user, or 204 when anonymous.
     *
     * No content rather than 401 because the frontend calls this on
     * every page load to decide what to render. A 401 would be an
     * error in the console for the ordinary case of not being logged
     * in yet.
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(
            @AuthenticationPrincipal AtlasUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.noContent().build();
        }

        AppUser user = userService.getById(principal.userId());

        return ResponseEntity.ok(new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.isAdmin(),
                user.getLastLoginAt()
        ));
    }
}
