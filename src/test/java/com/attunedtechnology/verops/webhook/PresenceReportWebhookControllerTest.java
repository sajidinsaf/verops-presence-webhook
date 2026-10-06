package com.attunedtechnology.verops.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "presence.report.storage-dir=${java.io.tmpdir}/verops-webhook-test",
        "webhook.auth.token=test-secret-token"
})
class PresenceReportWebhookControllerTest {

    private static final String TOKEN = "test-secret-token";
    private static final String URL   = "/verops/demo/weekly-presence-report";

    @Autowired
    private MockMvc mvc;

    // ── Auth checks ──────────────────────────────────────────────────────────

    @Test
    void rejectsMultipartWithoutToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "r.csv",
                "text/csv", "host,presence".getBytes());
        mvc.perform(multipart(URL).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRawBodyWithWrongToken() throws Exception {
        mvc.perform(post(URL)
                        .header(PresenceReportWebhookController.AUTH_HEADER, "wrong")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("data"))
                .andExpect(status().isUnauthorized());
    }

    // ── Multipart ────────────────────────────────────────────────────────────

    @Test
    void rejectsMultipartWithEmptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv",
                "text/csv", new byte[0]);
        mvc.perform(multipart(URL)
                        .file(empty)
                        .header(PresenceReportWebhookController.AUTH_HEADER, TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void acceptsMultipartUploadAndReturns200() throws Exception {
        byte[] csv = "host,presence\nLAPTOP-01,office\n".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "presence.csv",
                "text/csv", csv);
        mvc.perform(multipart(URL)
                        .file(file)
                        .header(PresenceReportWebhookController.AUTH_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.filename").value("presence.csv"))
                .andExpect(jsonPath("$.bytes").value(csv.length));
    }

    @Test
    void multipartOverwritesExistingFile() throws Exception {
        byte[] first  = "first".getBytes();
        byte[] second = "second content".getBytes();
        MockMultipartFile f1 = new MockMultipartFile("file", "overwrite-test.csv", "text/csv", first);
        MockMultipartFile f2 = new MockMultipartFile("file", "overwrite-test.csv", "text/csv", second);

        mvc.perform(multipart(URL).file(f1).header(PresenceReportWebhookController.AUTH_HEADER, TOKEN))
                .andExpect(status().isOk());
        mvc.perform(multipart(URL).file(f2).header(PresenceReportWebhookController.AUTH_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bytes").value(second.length));
    }

    // ── Raw body ─────────────────────────────────────────────────────────────

    @Test
    void acceptsRawCsvBodyAndReturns200() throws Exception {
        byte[] csv = "host,presence\nDESKTOP-02,remote\n".getBytes();
        mvc.perform(post(URL)
                        .header(PresenceReportWebhookController.AUTH_HEADER, TOKEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.bytes").value(csv.length));
    }

    @Test
    void rejectsEmptyRawBody() throws Exception {
        mvc.perform(post(URL)
                        .header(PresenceReportWebhookController.AUTH_HEADER, TOKEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(new byte[0]))
                .andExpect(status().isBadRequest());
    }
}
