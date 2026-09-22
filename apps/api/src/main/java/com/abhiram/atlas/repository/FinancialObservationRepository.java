package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.FinancialObservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialObservationRepository
        extends JpaRepository<FinancialObservation, UUID> {

    /**
     * Point-in-time query. The core of honest backtesting.
     *
     * Returns the figures Atlas knew about on the given date, one row
     * per fiscal year, choosing the most recent revision that was
     * knowable by then.
     *
     * The knowledgeDate filter is what prevents lookahead bias. A
     * restatement published in 2026 must not appear in a 2023
     * evaluation even though it describes a 2022 period.
     *
     * The correlated subquery selects the latest knowledge_date per
     * fiscal year rather than simply ordering, because a single year
     * can have several revisions and only the newest knowable one
     * should be returned.
     */
        @Query("""
            SELECT o FROM FinancialObservation o
            WHERE o.company.id = :companyId
              AND o.knowledgeDate <= :asOfDate
              AND o.knowledgeDate = (
                  SELECT MAX(prior.knowledgeDate)
                  FROM FinancialObservation prior
                  WHERE prior.company.id = o.company.id
                    AND prior.fiscalYear = o.fiscalYear
                    AND prior.knowledgeDate <= :asOfDate
              )
            ORDER BY o.fiscalYear DESC
            """)
    List<FinancialObservation> findAsOf(
            @Param("companyId") UUID companyId,
            @Param("asOfDate") LocalDate asOfDate
    );

    /**
     * Current view. Equivalent to findAsOf with today's date, but
     * expressed separately so callers are explicit about intent.
     */
    @Query("""
            SELECT o FROM FinancialObservation o
            WHERE o.company.id = :companyId
              AND o.supersededAt IS NULL
            ORDER BY o.fiscalYear DESC
            """)
    List<FinancialObservation> findCurrent(
            @Param("companyId") UUID companyId
    );

    /**
     * Full revision history for one period, oldest first.
     *
     * Shows how a figure changed as restatements arrived.
     */
    @Query("""
            SELECT o FROM FinancialObservation o
            WHERE o.company.id = :companyId
              AND o.fiscalYear = :fiscalYear
            ORDER BY o.knowledgeDate ASC,
                     o.revisionNumber ASC
            """)
    List<FinancialObservation> findRevisionHistory(
            @Param("companyId") UUID companyId,
            @Param("fiscalYear") Integer fiscalYear
    );

    /**
     * Latest known observation for a period, used to detect whether
     * an incoming figure is a restatement.
     */
    Optional<FinancialObservation>
    findFirstByCompanyIdAndFiscalYearAndSupersededAtIsNullOrderByKnowledgeDateDesc(
            UUID companyId,
            Integer fiscalYear
    );

    /**
     * Earliest knowledge date across all observations for a company.
     *
     * A backtest starting before this date has no data and would
     * silently produce empty results, so callers should check it.
     */
    @Query("""
            SELECT MIN(o.knowledgeDate)
            FROM FinancialObservation o
            WHERE o.company.id = :companyId
            """)
    Optional<LocalDate> findEarliestKnowledgeDate(
            @Param("companyId") UUID companyId
    );

    List<FinancialObservation> findByCompanyId(UUID companyId);
}
