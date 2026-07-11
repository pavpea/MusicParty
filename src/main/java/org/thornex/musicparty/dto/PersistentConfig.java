package org.thornex.musicparty.dto;

import lombok.Data;

@Data
public class PersistentConfig {
    private String roomPassword;
    private String neteaseCookie;
    private String bilibiliSessdata;
    private boolean pauseLocked;
    private boolean skipLocked;
    private boolean shuffleLocked;
    private boolean streamEnabled;
    
    // System Configs
    private Integer maxQueueSize;
    private Integer maxHistorySize;
    private Integer maxUserSongs;
    private Integer maxPlaylistImportSize;
    private Integer maxChatHistorySize;
    private Long minChatIntervalMs;
    private Integer maxChatMessageLength;
    private Boolean neteaseEnabled;
    private Boolean bilibiliEnabled;
    private Boolean voteSkipEnabled;
    private Double voteSkipThreshold;
    private Integer voteSkipWaitTime;
}
