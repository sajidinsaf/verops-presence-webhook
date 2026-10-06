package com.attunedtechnology.verops.webhook;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Webhook endpoint that receives the VerOps weekly presence report and
 * persists it to the configured storage directory.
 *
 * <p>The endpoint is mounted at {@code /verops/demo/weekly-presence-report}.
 * The WAR is named {@code ROOT.war} so Tomcat serves it at the root context
 * path ({@code /}) — no renaming needed on MochaHost.  The full public URL is:
 * <pre>https://o11y.attunedtechnology.com/verops/demo/weekly-presence-report</pre>
 *
 * <p><b>Filename strategy</b> — we do not yet know whether VerOps sends a
 * timestamped filename (e.g. {@code hosts-inventory-20261005-0600.csv}) or a
 * fixed name.  Every incoming request is therefore logged in full (all headers
 * + multipart filename) so we can inspect what VerOps actually sends on the
 * first real delivery.  If the filename looks generic (matches
 * {@code weekly-presence-report.*}) a {@code MM-dd-yyyy_HH-mm} timestamp is
 * appended automatically so files are never silently overwritten.
 *
 * <p><b>Authentication</b> — every request must carry the header
 * {@code X-Webhook-Token} with the value configured in
 * {@code webhook.auth.token}.  Requests without a matching token are
 * rejected with HTTP 401.
 */
@RestController
@RequestMapping("/verops/demo/weekly-presence-report")
public class PresenceReportWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PresenceReportWebhookController.class);

    /** Header name VerOps is told to send — matches the "Auth header name" field. */
    static final String AUTH_HEADER = "X-Webhook-Token";

    /**
     * Filenames that look generic / fixed rather than already timestamped.
     * When matched, we append our own timestamp before saving.
     */
    private static final Pattern GENERIC_NAME =
            Pattern.compile("^weekly-presence-report\\.[a-z]+$", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("MM-dd-yyyy_HH-mm").withZone(ZoneOffset.UTC);

    private final PresenceReportStorageService storage;
    private final String expectedToken;

    public PresenceReportWebhookController(
            PresenceReportStorageService storage,
            @Value("${webhook.auth.token}") String expectedToken) {
        this.storage = storage;
        this.expectedToken = expectedToken;
    }

    // ── Multipart file upload ────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> receiveMultipart(
            @RequestHeader(value = AUTH_HEADER, required = false) String token,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        if (!expectedToken.equals(token)) return unauthorized();
        logRequest(request, "multipart", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "uploaded file is empty"));
        }
        try {
            String rawName = file.getOriginalFilename();
            String filename = resolveFilename(rawName != null ? rawName : "");
            byte[] bytes = file.getBytes();
            Path saved = storage.save(filename, bytes);
            return ResponseEntity.ok(Map.of(
                    "status", "saved",
                    "filename", saved.getFileName().toString(),
                    "bytes", bytes.length));
        } catch (Exception e) {
            log.error("Failed to save multipart presence report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "could not save report: " + e.getMessage()));
        }
    }

    // ── Raw body (text/csv, application/octet-stream, application/json …) ───

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveRaw(
            @RequestHeader(value = AUTH_HEADER, required = false) String token,
            @RequestBody(required = false) byte[] body,
            @RequestParam(name = "filename", required = false,
                          defaultValue = "weekly-presence-report.csv") String filename,
            HttpServletRequest request) {

        if (!expectedToken.equals(token)) return unauthorized();
        logRequest(request, "raw-body", filename, body == null ? 0 : body.length);

        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "request body is empty"));
        }
        try {
            String resolvedName = resolveFilename(filename);
            Path saved = storage.save(resolvedName, body);
            return ResponseEntity.ok(Map.of(
                    "status", "saved",
                    "filename", saved.getFileName().toString(),
                    "bytes", body.length));
        } catch (Exception e) {
            log.error("Failed to save raw presence report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "could not save report: " + e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> unauthorized() {
        log.warn("Rejected webhook request: missing or invalid {}", AUTH_HEADER);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid or missing " + AUTH_HEADER + " header"));
    }

    /**
     * If the filename looks generic (no timestamp already baked in by VerOps),
     * insert a {@code MM-dd-yyyy_HH-mm} stamp before the extension so each
     * delivery produces a distinct, non-overwriting file.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code weekly-presence-report.csv}  →  {@code weekly-presence-report_10-05-2026_06-00.csv}</li>
     *   <li>{@code hosts-inventory-20261005-0600.csv}  →  unchanged (VerOps already stamped it)</li>
     * </ul>
     */
    static String resolveFilename(String raw) {
        String name = (raw == null || raw.isBlank()) ? "weekly-presence-report.csv" : raw.trim();
        if (!GENERIC_NAME.matcher(name).matches()) {
            // VerOps sent its own timestamped name — trust it.
            return name;
        }
        // Generic name — append our timestamp.
        String stamp = STAMP.format(ZonedDateTime.now(ZoneOffset.UTC));
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name.substring(0, dot) + "_" + stamp + name.substring(dot);
        }
        return name + "_" + stamp;
    }

    /**
     * Log every header and the key file metadata so we can see exactly what
     * VerOps sends on first delivery.  Logged at INFO so it appears in the
     * default Tomcat catalina.out without any log-level changes.
     */
    private static void logRequest(HttpServletRequest request, String mode,
                                   String filename, long bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n── Incoming VerOps webhook ──────────────────────────\n");
        sb.append("  mode        : ").append(mode).append('\n');
        sb.append("  filename    : ").append(filename).append('\n');
        sb.append("  body bytes  : ").append(bytes).append('\n');
        sb.append("  content-type: ").append(request.getContentType()).append('\n');
        sb.append("  headers:\n");
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String h = names.nextElement();
            // Redact the auth token value in the log.
            String v = AUTH_HEADER.equalsIgnoreCase(h) ? "***redacted***" : request.getHeader(h);
            sb.append("    ").append(h).append(": ").append(v).append('\n');
        }
        sb.append("─────────────────────────────────────────────────────");
        log.info(sb.toString());
    }
}
