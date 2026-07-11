package org.thornex.musicparty.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.thornex.musicparty.controller.AuthController;
import org.thornex.musicparty.dto.PlayerState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ConfigPersistenceServiceTest {

    @Autowired
    private ConfigPersistenceService configPersistenceService;

    @Autowired
    private AuthController authController;

    @Autowired
    private MusicPlayerService musicPlayerService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private static final String TEST_CONFIG_PATH = "data/config-data.json";

    @BeforeEach
    @AfterEach
    public void cleanUp() {
        File file = new File(TEST_CONFIG_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testSaveAndLoadConfig() {
        // Arrange - set custom states
        authController.forceSetPassword("persist-test-pwd123");
        musicPlayerService.setLock("PAUSE", true);
        musicPlayerService.setLock("SKIP", true);
        musicPlayerService.setLock("SHUFFLE", false);

        // Act - save
        configPersistenceService.saveConfig();

        // Assert - file exists
        File file = new File(TEST_CONFIG_PATH);
        assertTrue(file.exists(), "Configuration file should be created");

        // Act - change states in-memory
        authController.forceSetPassword("temporary-modified-pwd");
        musicPlayerService.setLock("PAUSE", false);
        musicPlayerService.setLock("SKIP", false);

        // Act - reload from file
        configPersistenceService.loadConfig();

        // Assert - states are correctly restored
        assertEquals("persist-test-pwd123", authController.getRawPassword(), "Password should be restored");
        
        PlayerState state = musicPlayerService.getCurrentPlayerState();
        assertTrue(state.isPauseLocked(), "Pause lock should be restored to true");
        assertTrue(state.isSkipLocked(), "Skip lock should be restored to true");
        assertFalse(state.isShuffleLocked(), "Shuffle lock should be restored to false");
    }
}
