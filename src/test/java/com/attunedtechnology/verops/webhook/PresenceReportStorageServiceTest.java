package com.attunedtechnology.verops.webhook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceReportStorageServiceTest {

    @TempDir
    Path tempDir;

    private PresenceReportStorageService service() throws IOException {
        return new PresenceReportStorageService(tempDir.toString());
    }

    @Test
    void savesFileToStorageDirectory() throws IOException {
        byte[] content = "host,presence\nLAPTOP-01,office".getBytes();
        Path saved = service().save("report.csv", content);

        assertThat(saved).exists();
        assertThat(Files.readAllBytes(saved)).isEqualTo(content);
    }

    @Test
    void overwritesExistingFile() throws IOException {
        PresenceReportStorageService svc = service();
        svc.save("report.csv", "old content".getBytes());
        svc.save("report.csv", "new content".getBytes());

        Path file = tempDir.resolve("report.csv");
        assertThat(new String(Files.readAllBytes(file))).isEqualTo("new content");
    }

    @Test
    void sanitizeFilenameStripsPathTraversal() {
        assertThat(PresenceReportStorageService.sanitizeFilename("../../etc/passwd"))
                .isEqualTo("passwd");
    }

    @Test
    void sanitizeFilenameReplacesSpaces() {
        assertThat(PresenceReportStorageService.sanitizeFilename("my report.csv"))
                .isEqualTo("my_report.csv");
    }

    @Test
    void sanitizeFilenameDefaultsWhenBlank() {
        assertThat(PresenceReportStorageService.sanitizeFilename(""))
                .isEqualTo("weekly-presence-report.csv");
        assertThat(PresenceReportStorageService.sanitizeFilename(null))
                .isEqualTo("weekly-presence-report.csv");
    }
}
