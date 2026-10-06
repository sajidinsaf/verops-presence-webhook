package com.attunedtechnology.verops.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

/**
 * Webhook endpoint that receives the VerOps weekly presence report and
 * persists it to the configured storage directory.
 *
 * <p>The endpoint is mounted at {@code /verops/demo/weekly-presence-report}.
 * The WAR is named {@code ROOT.war} so Tomcat serves it at the root context
 * path ({@code /}) — no renaming needed on MochaHost.  The full public URL is:
 * <pre>https://o11y.attunedtechnology.com/verops/demo/weekly-presence-report</pre>
 *
 * <p><b>Accepted request formats</b>
 * <ol>
 *   <li><b>Multipart file upload</b> — {@code multipart/form-data} with a
 *       field named {@code file}.  VerOps can POST the generated CSV/PDF
 *       directly as a form attachment.  The original filename from the
 *       {@code Content-Disposition} header is used as the stored filename.</li>
 *   <li><b>Raw body</b> — any {@code Content-Type} other than multipart (e.g.
 *       {@code text/csv}, {@code application/octet-stream}, {@code application/json}).
 *       The filename defaults to {@code weekly-presence-report.csv}; pass an
 *       optional {@code ?filename=} query parameter to override it.</li>
 * </ol>
 *
 * <p>Either way, if a file with the same name already exists it is
 * silently overwritten.
 */
@RestController
@RequestMapping("/verops/demo/weekly-presence-report")
public class PresenceReportWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PresenceReportWebhookController.class);

    private final PresenceReportStorageService storage;

    public PresenceReportWebhookController(PresenceReportStorageService storage) {
        this.storage = storage;
    }

    // ── Multipart file upload ────────────────────────────────────────────────

    /**
     * Accept a multipart/form-data POST.  The file field must be named
     * {@code file}.  VerOps attaches the report under its generated filename
     * (e.g. {@code hosts-inventory-20261005-0600.csv}).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> receiveMultipart(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "uploaded file is empty"));
        }
        try {
            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "weekly-presence-report.csv";
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

    /**
     * Accept any raw-body POST (CSV text, binary PDF, JSON, etc.).
     *
     * @param filename optional query param to control the stored filename;
     *                 defaults to {@code weekly-presence-report.csv}
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveRaw(
            @RequestBody(required = false) byte[] body,
            @RequestParam(name = "filename", required = false,
                          defaultValue = "weekly-presence-report.csv") String filename) {

        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "request body is empty"));
        }
        try {
            Path saved = storage.save(filename, body);
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
}
