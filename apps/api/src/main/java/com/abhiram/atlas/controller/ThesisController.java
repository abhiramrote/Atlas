package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.PublishThesisRequest;
import com.abhiram.atlas.dto.ReviseThesisRequest;
import com.abhiram.atlas.dto.ThesisEventResponse;
import com.abhiram.atlas.dto.ThesisResponse;
import com.abhiram.atlas.dto.ThesisVersionResponse;
import com.abhiram.atlas.dto.TransitionRequest;
import com.abhiram.atlas.service.ThesisService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/theses")
public class ThesisController {

    private final ThesisService service;

    public ThesisController(ThesisService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ThesisResponse> publish(
            @Valid @RequestBody PublishThesisRequest request
    ) {
        ThesisResponse response = service.publish(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<ThesisResponse> list(
            @RequestParam(required = false) UUID companyId
    ) {
        if (companyId != null) {
            return service.getThesesForCompany(companyId);
        }

        return service.getAllTheses();
    }

    @GetMapping("/{thesisId}")
    public ThesisResponse get(
            @PathVariable UUID thesisId
    ) {
        return service.getThesis(thesisId);
    }

    @PostMapping("/{thesisId}/revisions")
    public ThesisResponse revise(
            @PathVariable UUID thesisId,
            @Valid @RequestBody ReviseThesisRequest request
    ) {
        return service.revise(thesisId, request);
    }

    @PostMapping("/{thesisId}/transitions")
    public ThesisResponse transition(
            @PathVariable UUID thesisId,
            @Valid @RequestBody TransitionRequest request
    ) {
        return service.transition(
                thesisId,
                request.targetState(),
                request.reason()
        );
    }

    @GetMapping("/{thesisId}/versions")
    public List<ThesisVersionResponse> versionHistory(
            @PathVariable UUID thesisId
    ) {
        return service.getVersionHistory(thesisId);
    }

    @GetMapping("/{thesisId}/audit")
    public List<ThesisEventResponse> auditTrail(
            @PathVariable UUID thesisId
    ) {
        return service.getAuditTrail(thesisId);
    }
}
