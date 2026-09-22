package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ThesisState;
import com.abhiram.atlas.dto.DataQualityReport;
import com.abhiram.atlas.dto.ThesisMonitoringResult;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.entity.Thesis;
import com.abhiram.atlas.entity.ThesisCondition;
import com.abhiram.atlas.entity.ThesisEvent;
import com.abhiram.atlas.entity.ThesisVersion;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.ThesisConditionRepository;
import com.abhiram.atlas.repository.ThesisEventRepository;
import com.abhiram.atlas.repository.ThesisRepository;
import com.abhiram.atlas.repository.ThesisVersionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for automated thesis monitoring.
 *
 * The data quality tests encode the most important guarantee in the
 * system: a corrupted figure must never permanently mark a thesis as
 * disproven. HINDALCO's FY2025 net income produced a 1,856 percent
 * growth figure that never happened. A thesis with a growth condition
 * would have been invalidated on evidence that does not exist.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThesisMonitoringServiceTest {

    private static final UUID THESIS_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private ThesisVersionRepository versionRepository;

    @Mock
    private ThesisConditionRepository conditionRepository;

    @Mock
    private ThesisEventRepository eventRepository;

    @Mock
    private MetricResolver metricResolver;

    @Mock
    private DataQualityService dataQualityService;

    @InjectMocks
    private ThesisMonitoringService service;

    private Thesis thesis;
    private ThesisVersion version;
    private Company company;

    @BeforeEach
    void setUp() throws Exception {

        Instrument instrument = newInstrument();
        company = newCompany(instrument);

        thesis = Thesis.open(
                THESIS_ID,
                company,
                "Margin expansion thesis"
        );

        thesis.recordNewVersion(1);
        thesis.transitionTo(ThesisState.CANDIDATE);

        version = newVersion();

        when(thesisRepository.findById(THESIS_ID))
                .thenReturn(Optional.of(thesis));

        when(versionRepository
                .findFirstByThesisIdOrderByVersionNumberDesc(
                        THESIS_ID))
                .thenReturn(Optional.of(version));

        when(metricResolver.isSupported(any()))
                .thenReturn(true);

        // Default to clean data. Individual tests override.
        when(dataQualityService.check(COMPANY_ID))
                .thenReturn(cleanReport());
    }

    // ----------------------------------------------------------
    // Data quality gate
    // ----------------------------------------------------------

    @Test
    @DisplayName("Does not invalidate on unreliable data")
    void doesNotInvalidateOnUnreliableData() {

        ThesisCondition condition = condition(
                "NET_INCOME_GROWTH",
                "ABOVE",
                "100.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        // The HINDALCO figure. A 1,856 percent growth that never
        // happened.
        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "NET_INCOME_GROWTH", "1856.66"
                ));

        when(dataQualityService.check(COMPANY_ID))
                .thenReturn(unreliableReport());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        // A wrong invalidation destroys the record permanently.
        assertThat(result.stateAfter())
                .isNotEqualTo("INVALIDATED");

        assertThat(condition.getBreached()).isFalse();

        verify(conditionRepository, never())
                .save(any(ThesisCondition.class));
    }

    @Test
    @DisplayName("Moves to weakening instead of invalidated")
    void movesToWeakeningOnSuspectBreach() {

        ThesisCondition condition = condition(
                "NET_INCOME_GROWTH",
                "ABOVE",
                "100.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "NET_INCOME_GROWTH", "1856.66"
                ));

        when(dataQualityService.check(COMPANY_ID))
                .thenReturn(unreliableReport());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        // Weakening is an observation. Invalidated is a verdict.
        // The state machine allows recovery from weakening.
        assertThat(result.stateAfter()).isEqualTo("WEAKENING");

        assertThat(result.note())
                .containsIgnoringCase("unreliable");
    }

    @Test
    @DisplayName("Reports a suspected breach without recording it")
    void reportsSuspectedBreach() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "OPERATING_MARGIN", "15.40"
                ));

        when(dataQualityService.check(COMPANY_ID))
                .thenReturn(unreliableReport());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.breachDetails())
                .isNotEmpty()
                .first()
                .asString()
                .contains("Suspected breach not recorded");

        // newBreaches counts confirmed breaches only.
        assertThat(result.newBreaches()).isZero();
    }

    @Test
    @DisplayName("Invalidates normally when data is reliable")
    void invalidatesOnReliableData() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "OPERATING_MARGIN", "15.40"
                ));

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.stateAfter())
                .isEqualTo("INVALIDATED");

        assertThat(result.newBreaches()).isEqualTo(1);
        assertThat(condition.getBreached()).isTrue();
    }

    @Test
    @DisplayName("Notes quality issues even without a breach")
    void notesQualityIssuesWithoutBreach() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        // Healthy margin. No breach.
        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "OPERATING_MARGIN", "21.18"
                ));

        when(dataQualityService.check(COMPANY_ID))
                .thenReturn(unreliableReport());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.stateAfter()).isEqualTo("CANDIDATE");

        // The absence of a breach is itself unreliable when the
        // data is suspect, so the caller is told.
        assertThat(result.note())
                .containsIgnoringCase("data quality");
    }

    // ----------------------------------------------------------
    // Existing behaviour
    // ----------------------------------------------------------

    @Test
    @DisplayName("Missing data never causes a breach")
    void missingDataDoesNotBreach() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(new HashMap<>());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.newBreaches()).isZero();
        assertThat(result.conditionsEvaluated()).isZero();
        assertThat(condition.getBreached()).isFalse();
    }

    @Test
    @DisplayName("A satisfied condition does not breach")
    void healthyMetricDoesNotBreach() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "OPERATING_MARGIN", "21.18"
                ));

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.conditionsEvaluated()).isEqualTo(1);
        assertThat(result.newBreaches()).isZero();
    }

    @Test
    @DisplayName("Re-running does not re-record an existing breach")
    void rerunIsIdempotent() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        condition.markBreached(new BigDecimal("15.40"));

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(metrics(
                        "OPERATING_MARGIN", "12.00"
                ));

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.newBreaches()).isZero();

        assertThat(condition.getBreachedValue())
                .isEqualByComparingTo("15.40");

        verify(eventRepository, never())
                .save(any(ThesisEvent.class));
    }

    @Test
    @DisplayName("Skips unsupported metrics without breaching")
    void skipsUnsupportedMetrics() {

        ThesisCondition condition = condition(
                "MANAGEMENT_QUALITY",
                "BELOW",
                "5.00"
        );

        when(metricResolver.isSupported("MANAGEMENT_QUALITY"))
                .thenReturn(false);

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(condition));

        when(metricResolver.resolveAll(company))
                .thenReturn(new HashMap<>());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.conditionsEvaluated()).isZero();
        assertThat(condition.getBreached()).isFalse();
    }

    @Test
    @DisplayName("Skips an archived thesis")
    void skipsArchivedThesis() {

        thesis.transitionTo(ThesisState.ARCHIVED);

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.stateAfter()).isEqualTo("ARCHIVED");
        assertThat(result.note())
                .containsIgnoringCase("archived");

        // Quality is not even checked for a closed thesis.
        verify(dataQualityService, never())
                .check(any(UUID.class));
    }

    @Test
    @DisplayName("Handles a thesis with no published version")
    void handlesMissingVersion() {

        when(versionRepository
                .findFirstByThesisIdOrderByVersionNumberDesc(
                        THESIS_ID))
                .thenReturn(Optional.empty());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.note())
                .containsIgnoringCase("no published version");
    }

    @Test
    @DisplayName("Fails when the thesis does not exist")
    void failsWhenThesisMissing() {

        when(thesisRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.monitorThesis(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Thesis not found");
    }

    // ----------------------------------------------------------
    // Fixtures
    // ----------------------------------------------------------

    private DataQualityReport cleanReport() {
        return new DataQualityReport(
                COMPANY_ID,
                "INFY",
                7,
                0,
                0,
                true,
                "No data quality issues detected.",
                List.of()
        );
    }

    private DataQualityReport unreliableReport() {
        return new DataQualityReport(
                COMPANY_ID,
                "HINDALCO",
                8,
                4,
                4,
                false,
                "4 error level issues affect the periods used for "
                        + "scoring. Any score for this company "
                        + "should be treated as unreliable until "
                        + "the source data is verified.",
                List.of()
        );
    }

    private Map<String, BigDecimal> metrics(
            String metric,
            String value
    ) {
        Map<String, BigDecimal> map = new HashMap<>();
        map.put(metric, new BigDecimal(value));
        return map;
    }

    private ThesisCondition condition(
            String metric,
            String comparison,
            String threshold
    ) {
        return ThesisCondition.create(
                UUID.randomUUID(),
                version,
                metric,
                comparison,
                new BigDecimal(threshold),
                "Test condition"
        );
    }

    private ThesisVersion newVersion() {
        return ThesisVersion.publish(
                VERSION_ID,
                thesis,
                1,
                18,
                "MEDIUM",
                "Test rationale for the monitoring fixture.",
                "Test risks",
                83,
                0,
                83,
                "GOOD",
                "v2.1",
                new BigDecimal("1051.40")
        );
    }

    private Instrument newInstrument() throws Exception {

        Instrument instrument = newInstance(Instrument.class);

        setField(instrument, "id", UUID.randomUUID());
        setField(instrument, "symbol", "INFY");
        setField(instrument, "companyName", "Infosys Ltd");
        setField(instrument, "exchange", "NSE");
        setField(instrument, "active", Boolean.TRUE);
        setField(instrument, "createdAt", LocalDateTime.now());
        setField(instrument, "updatedAt", LocalDateTime.now());

        return instrument;
    }

    private Company newCompany(Instrument instrument)
            throws Exception {

        Company newCompany = newInstance(Company.class);

        setField(newCompany, "id", COMPANY_ID);
        setField(newCompany, "instrument", instrument);
        setField(newCompany, "sector", "Information Technology");
        setField(newCompany, "industry", "IT Services");
        setField(newCompany, "website", "https://example.com");
        setField(newCompany, "description", "Fixture");
        setField(
                newCompany,
                "scoringProfile",
                "OPERATING_COMPANY");
        setField(newCompany, "createdAt", LocalDateTime.now());
        setField(newCompany, "updatedAt", LocalDateTime.now());

        return newCompany;
    }

    private <T> T newInstance(Class<T> type) throws Exception {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                target.getClass().getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }
}
