package com.attunedtechnology.verops.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Persists incoming presence reports to the configured storage directory.
 *
 * <p>Files are always written with {@link StandardOpenOption#TRUNCATE_EXISTING}
 * so a report with the same name is silently overwritten — the filesystem is
 * the source of truth and the latest report from VerOps wins.
 */
@Service
public class PresenceReportStorageService {

    private static final Logger log = LoggerFactory.getLogger(PresenceReportStorageService.class);

    private final Path storageDir;

    public PresenceReportStorageService(
            @Value("${presence.report.storage-dir:/home/ifaru02/attunedtechnology.com/verops/demo/presence-report}")
            String storageDirPath) throws IOException {
        this.storageDir = Paths.get(storageDirPath);
        Files.createDirectories(this.storageDir);
        log.info("Presence report storage directory: {}", this.storageDir.toAbsolutePath());
    }

    /**
     * Write {@code content} to {@code filename} under the storage directory,
     * overwriting any existing file with the same name.
     *
     * @param filename the target filename (must not contain path separators)
     * @param content  the raw bytes to persist
     * @return the absolute path of the written file
     */
    public Path save(String filename, byte[] content) throws IOException {
        String safeName = sanitizeFilename(filename);
        Path target = storageDir.resolve(safeName);
        Files.write(target, content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        log.info("Saved presence report: {} ({} bytes)", target.toAbsolutePath(), content.length);
        return target;
    }

    /**
     * Strip any path traversal characters from the filename so a crafted
     * {@code Content-Disposition} cannot escape the storage directory.
     */
    static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "weekly-presence-report.csv";
        }
        // Keep only the last path segment, then strip dangerous characters.
        String name = Paths.get(raw).getFileName().toString();
        name = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return name.isBlank() ? "weekly-presence-report.csv" : name;
    }
}
