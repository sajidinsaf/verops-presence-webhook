package com.attunedtechnology.verops.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the filename resolution logic.
 *
 * The key question is: does VerOps send a timestamped filename already
 * (e.g. hosts-inventory-20261005-0600.csv) or a fixed generic name?
 * These tests document both paths so the behaviour is clear once we
 * see the actual log output from the first real delivery.
 */
class ResolveFilenameTest {

    // ── Generic / fixed names — our timestamp is appended ────────────────────

    @Test
    void genericCsvNameGetsTimestampSuffix() {
        String result = PresenceReportWebhookController.resolveFilename("weekly-presence-report.csv");
        // e.g. weekly-presence-report_10-06-2026_14-30.csv
        assertThat(result).startsWith("weekly-presence-report_");
        assertThat(result).endsWith(".csv");
        assertThat(result).matches("weekly-presence-report_\\d{2}-\\d{2}-\\d{4}_\\d{2}-\\d{2}\\.csv");
    }

    @Test
    void genericPdfNameGetsTimestampSuffix() {
        String result = PresenceReportWebhookController.resolveFilename("weekly-presence-report.pdf");
        assertThat(result).startsWith("weekly-presence-report_");
        assertThat(result).endsWith(".pdf");
    }

    @Test
    void blankNameDefaultsAndGetsTimestamp() {
        String result = PresenceReportWebhookController.resolveFilename("");
        assertThat(result).startsWith("weekly-presence-report_");
        assertThat(result).endsWith(".csv");
    }

    @Test
    void nullNameDefaultsAndGetsTimestamp() {
        String result = PresenceReportWebhookController.resolveFilename(null);
        assertThat(result).startsWith("weekly-presence-report_");
    }

    // ── Already-timestamped names (VerOps style) — left unchanged ─────────────

    @Test
    void veropsTimestampedNameIsLeftUnchanged() {
        // This is what VerOps generates internally: {slug}-{yyyyMMdd-HHmm}.csv
        String name = "hosts-inventory-20261005-0600.csv";
        assertThat(PresenceReportWebhookController.resolveFilename(name)).isEqualTo(name);
    }

    @Test
    void anyOtherDistinctNameIsLeftUnchanged() {
        String name = "presence-report-Q4-2026.csv";
        assertThat(PresenceReportWebhookController.resolveFilename(name)).isEqualTo(name);
    }
}
