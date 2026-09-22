package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ThesisState;
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
 * These encode the behavioural guarantees that make the monitor
 * trustworthy, particularly that missing data never causes a breach.
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
    }

    @Test
    @DisplayName("Breaches a condition and invalidates the thesis")
    void breachesAndInvalidates() {

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

        assertThat(result.newBreaches()).isEqualTo(1);
        assertThat(result.stateBefore()).isEqualTo("CANDIDATE");
        assertThat(result.stateAfter()).isEqualTo("INVALIDATED");

        assertThat(condition.getBreached()).isTrue();
        assertThat(condition.getBreachedValue())
                .isEqualByComparingTo("15.40");

        assertThat(result.breachDetails())
                .hasSize(1)
                .first()
                .asString()
                .contains("fell below")
                .contains("15.40");
    }

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

        // Metric could not be computed.
        when(metricResolver.resolveAll(company))
                .thenReturn(new HashMap<>());

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.newBreaches()).isZero();
        assertThat(result.conditionsEvaluated()).isZero();
        assertThat(result.stateAfter()).isEqualTo("CANDIDATE");

        assertThat(condition.getBreached()).isFalse();

        verify(conditionRepository, never())
                .save(any(ThesisCondition.class));
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
        assertThat(result.stateAfter()).isEqualTo("CANDIDATE");
    }

    @Test
    @DisplayName("Re-running does not re-record an existing breach")
    void rerunIsIdempotent() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        // Already breached at 15.40 on a previous run.
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

        // The original observation survives.
        assertThat(condition.getBreachedValue())
                .isEqualByComparingTo("15.40");

        verify(eventRepository, never())
                .save(any(ThesisEvent.class));
    }

    @Test
    @DisplayName("Evaluates every condition on the version")
    void evaluatesAllConditions() {

        ThesisCondition margin = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "18.00"
        );

        ThesisCondition growth = condition(
                "REVENUE_GROWTH",
                "BELOW",
                "0.00"
        );

        ThesisCondition drawdown = condition(
                "MAX_DRAWDOWN",
                "BELOW",
                "-30.00"
        );

        when(conditionRepository
                .findByThesisVersionId(VERSION_ID))
                .thenReturn(List.of(margin, growth, drawdown));

        Map<String, BigDecimal> values = new HashMap<>();
        values.put("OPERATING_MARGIN", new BigDecimal("21.18"));
        values.put("REVENUE_GROWTH", new BigDecimal("9.68"));
        values.put("MAX_DRAWDOWN", new BigDecimal("-13.08"));

        when(metricResolver.resolveAll(company))
                .thenReturn(values);

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        assertThat(result.conditionsEvaluated()).isEqualTo(3);
        assertThat(result.newBreaches()).isZero();
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
        assertThat(result.newBreaches()).isZero();
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

        verify(conditionRepository, never())
                .findByThesisVersionId(any());
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

        assertThat(result.newBreaches()).isZero();
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

    @Test
    @DisplayName("An invalidated thesis is not re-invalidated")
    void alreadyInvalidatedStaysStable() {

        thesis.transitionTo(ThesisState.INVALIDATED);

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
                        "OPERATING_MARGIN", "10.00"
                ));

        ThesisMonitoringResult result =
                service.monitorThesis(THESIS_ID);

        // The condition still breaches, but the state machine
        // forbids INVALIDATED to INVALIDATED, so the thesis is
        // left alone rather than throwing.
        assertThat(result.stateAfter())
                .isEqualTo("INVALIDATED");

        assertThat(result.note())
                .containsIgnoringCase("could not transition");
    }

    // ----------------------------------------------------------
    // Fixture helpers
    // ----------------------------------------------------------

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

    private ThesisVersion newVersion() throws Exception {

        ThesisVersion newVersion = ThesisVersion.publish(
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

        return newVersion;
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
 