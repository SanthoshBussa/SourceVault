package com.sourcevault.analysis.controller;

import com.sourcevault.analysis.service.WebsiteAnalysisService;
import com.sourcevault.common.dto.AnalysisRequest;
import com.sourcevault.common.dto.AnalysisResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final WebsiteAnalysisService analysisService;

    public AnalysisController(WebsiteAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        AnalysisResponse response = analysisService.analyzeWebsite(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisResponse> getAnalysis(@PathVariable String analysisId) {
        return analysisService.getAnalysis(analysisId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
