package com.attunedtechnology.verops.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Each test class gets its own temp storage to prevent cross-test pollution.
        "presence.report.storage-dir=${java.io.tmpdir}/verops-webhook-test"
})
class PresenceReportWebhookControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rejectsMultipartWithEmptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv",
                "text/csv", new byte[0]);
        mvc.perform(multipart("/verops/demo/weekly-presence-report").file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void acceptsMultipartUploadAndReturns200() throws Exception {
        byte[] csv = "host,presence\nLAPTOP-01,office\n".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "presence.csv",
                "text/csv", csv);
        mvc.perform(multipart("/verops/demo/weekly-presence-report").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.filename").value("presence.csv"))
                .andExpect(jsonPath("$.bytes").value(csv.length));
    }

    @Test
    void acceptsRawCsvBodyAndReturns200() throws Exception {
        byte[] csv = "host,presence\nDESKTOP-02,remote\n".getBytes();
        mvc.perform(post("/verops/demo/weekly-presence-report")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.bytes").value(csv.length));
    }

    @Test
    void rejectsEmptyRawBody() throws Exception {
        mvc.perform(post("/verops/demo/weekly-presence-report")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(new byte[0]))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipartOverwritesExistingFile() throws Exception {
        byte[] first = "first".getBytes();
        byte[] second = "second content".getBytes();

        MockMultipartFile f1 = new MockMultipartFile("file", "overwrite-test.csv",
                "text/csv", first);
        MockMultipartFile f2 = new MockMultipartFile("file", "overwrite-test.csv",
                "text/csv", second);

        mvc.perform(multipart("/verops/demo/weekly-presence-report").file(f1))
                .andExpect(status().isOk());
        mvc.perform(multipart("/verops/demo/weekly-presence-report").file(f2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bytes").value(second.length));
    }
}
