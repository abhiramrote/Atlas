package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.FundamentalsIngestionResult;
import com.abhiram.atlas.provider.FundamentalsData;
import com.abhiram.atlas.provider.IndianApiProvider;
import com.abhiram.atlas.service.FundamentalsIngestionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/fundamentals")
public class FundamentalsController {

    private final FundamentalsIngestionService service;
    private final IndianApiProvider provider;

    public FundamentalsController(
            FundamentalsIngestionService service,
            IndianApiProvider provider
    ) {
        this.service = service;
        this.provider = provider;
    }

    /**
     * Returns the provider payload without writing anything.
     *
     * Use this first. It lets you verify the field mapping against a
     * live response before allowing a destructive ingest.
     */
    @GetMapping("/preview/{symbol}")
    public FundamentalsData preview(
            @PathVariable String symbol
    ) {
        return provider.fetchFundamentals(symbol);
    }

    @PostMapping("/ingest/{symbol}")
    public FundamentalsIngestionResult ingest(
            @PathVariable String symbol
    ) {
        return service.ingest(symbol);
    }
}
