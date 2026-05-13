package com.sanguo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "file.upload-dir=target/test-uploads",
        "file.public-path=/uploads"
})
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void cleanup() {
        Path dir = Paths.get("target/test-uploads");
        FileSystemUtils.deleteRecursively(dir.toFile());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void uploadImage_ok() throws Exception {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
                0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
                0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
                0x0D, 0x0A, 0x2D, (byte) 0xB4,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "a.png",
                "image/png",
                png
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload-image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").exists())
                .andExpect(jsonPath("$.data.path").exists());
    }
}

