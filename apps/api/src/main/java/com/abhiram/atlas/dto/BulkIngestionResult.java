package com.abhiram.atlas.dto;

import java.util.List;

/**
 * Summary of a universe ingestion run.
 *
 * skipped is distinct from failed. A skip means the provider
 * responded but returned too few periods to be usable. A failure
 * means the call itself did not succeed. Collapsing the two would
 * hide whether the problem is coverage or connectivity.
 */
public record BulkIngestionResult(
        Integer total,
        Integer succeeded,
        Integer skipped,
        Integer failed,
        Long elapsedSeconds,
        List<BulkIngestionItem> results
) {
}
