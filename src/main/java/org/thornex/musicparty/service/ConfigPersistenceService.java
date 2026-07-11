package org.thornex.musicparty.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thornex.musicparty.config.AppProperties;
import org.thornex.musicparty.controller.AuthController;
import org.thornex.musicparty.dto.AdminConfigUpdateRequest;
import org.thornex.musicparty.dto.PersistentConfig;
import org.thornex.musicparty.dto.PlayerState;
import org.thornex.musicparty.service.api.BilibiliMusicApiService;
import org.thornex.musicparty.service.api.NeteaseMusicApiService;
import org.thornex.musicparty.service.stream.LiveStreamService;

import java.io.File;

@Service
@Slf4j
public class ConfigPersistenceService implements SmartInitializingSingleton {

    private final AppProperties appProperties;
    private final AuthController authController;
    private final MusicPlayerService musicPlayerService;
    private final NeteaseMusicApiService neteaseMusicApiService;
    private final BilibiliMusicApiService bilibiliMusicApiService;
    private final LiveStreamService liveStreamService;
    private final ObjectMapper objectMapper;

    private static final String CONFIG_FILE_PATH = "data/config-data.json";

    public ConfigPersistenceService(
            AppProperties appProperties,
            @Lazy AuthController authController,
            @Lazy MusicPlayerService musicPlayerService,
            @Lazy NeteaseMusicApiService neteaseMusicApiService,
            @Lazy BilibiliMusicApiService bilibiliMusicApiService,
            @Lazy LiveStreamService liveStreamService,
            ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.authController = authController;
        this.musicPlayerService = musicPlayerService;
        this.neteaseMusicApiService = neteaseMusicApiService;
        this.bilibiliMusicApiService = bilibiliMusicApiService;
        this.liveStreamService = liveStreamService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterSingletonsInstantiated() {
        loadConfig();
    }

    public synchronized void saveConfig() {
        try {
            File file = getConfigFile();
            PersistentConfig config = new PersistentConfig();
            config.setRoomPassword(authController.getRawPassword());
            config.setNeteaseCookie(neteaseMusicApiService.getCookie());
            config.setBilibiliSessdata(bilibiliMusicApiService.getSessdata());

            PlayerState state = musicPlayerService.getCurrentPlayerState();
            config.setPauseLocked(state.isPauseLocked());
            config.setSkipLocked(state.isSkipLocked());
            config.setShuffleLocked(state.isShuffleLocked());
            config.setStreamEnabled(liveStreamService.isEnabled());

            PlayerState.AppConfigSummary summary = state.config();
            config.setMaxQueueSize(summary.maxQueueSize());
            config.setMaxHistorySize(summary.maxHistorySize());
            config.setMaxUserSongs(summary.maxUserSongs());
            config.setMaxPlaylistImportSize(summary.maxPlaylistImportSize());
            config.setMaxChatHistorySize(summary.maxChatHistorySize());
            config.setMinChatIntervalMs(summary.minChatIntervalMs());
            config.setMaxChatMessageLength(summary.maxChatMessageLength());
            config.setNeteaseEnabled(summary.neteaseEnabled());
            config.setBilibiliEnabled(summary.bilibiliEnabled());
            config.setVoteSkipEnabled(state.isVoteSkipEnabled());
            config.setVoteSkipThreshold(state.voteSkipThreshold());
            config.setVoteSkipWaitTime(state.voteSkipWaitTime());

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
            log.info("System configuration successfully saved to {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to save system configuration", e);
        }
    }

    public synchronized void loadConfig() {
        File file = getConfigFile();
        if (!file.exists()) {
            log.info("No system configuration persistence file found at {}, starting with defaults.", file.getAbsolutePath());
            return;
        }

        try {
            PersistentConfig config = objectMapper.readValue(file, PersistentConfig.class);
            log.info("Loading persisted system configuration from {}", file.getAbsolutePath());

            // 1. Restore room password
            if (config.getRoomPassword() != null) {
                authController.forceSetPassword(config.getRoomPassword());
            }

            // 2. Restore platform credentials (only if they are non-empty, to prevent overwriting valid env configs)
            if (config.getNeteaseCookie() != null && !config.getNeteaseCookie().trim().isEmpty()) {
                neteaseMusicApiService.updateCookie(config.getNeteaseCookie());
            }
            if (config.getBilibiliSessdata() != null && !config.getBilibiliSessdata().trim().isEmpty()) {
                bilibiliMusicApiService.updateSessdata(config.getBilibiliSessdata());
            }

            // 3. Restore locks
            musicPlayerService.setLock("PAUSE", config.isPauseLocked());
            musicPlayerService.setLock("SKIP", config.isSkipLocked());
            musicPlayerService.setLock("SHUFFLE", config.isShuffleLocked());

            // 4. Restore stream enabled
            liveStreamService.setEnabled(config.isStreamEnabled());

            // 5. Restore system configs
            AdminConfigUpdateRequest request = new AdminConfigUpdateRequest(
                    config.getMaxQueueSize(),
                    config.getMaxHistorySize(),
                    config.getMaxUserSongs(),
                    config.getMaxPlaylistImportSize(),
                    config.getMaxChatHistorySize(),
                    config.getMinChatIntervalMs(),
                    config.getMaxChatMessageLength(),
                    config.getNeteaseEnabled(),
                    config.getBilibiliEnabled(),
                    config.getVoteSkipEnabled(),
                    config.getVoteSkipThreshold(),
                    config.getVoteSkipWaitTime()
            );
            musicPlayerService.updateConfig(request);

            log.info("System configuration successfully loaded and applied.");
        } catch (Exception e) {
            log.error("Failed to load system configuration from {}", file.getAbsolutePath(), e);
        }
    }

    private File getConfigFile() {
        File file = new File(CONFIG_FILE_PATH);
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }
}
