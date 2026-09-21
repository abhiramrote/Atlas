package com.abhiram.atlas.dto;

import jakarta.validation.constraints.NotBlank;

public record TransitionRequest(

        @NotBlank(message = "Target state is required")
        String targetState,

        @NotBlank(message = "A reason is required")
        String reason
) {
}
