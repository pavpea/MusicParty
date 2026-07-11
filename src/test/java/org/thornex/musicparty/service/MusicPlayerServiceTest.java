package org.thornex.musicparty.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.thornex.musicparty.dto.PlayableMusic;
import org.thornex.musicparty.dto.User;
import org.thornex.musicparty.enums.PlayerAction;
import org.thornex.musicparty.event.SystemMessageEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class MusicPlayerServiceTest {

    @Autowired
    private MusicPlayerService musicPlayerService;

    @MockBean
    private UserService userService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private List<SystemMessageEvent> capturedEvents;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public List<SystemMessageEvent> capturedEvents() {
            return new CopyOnWriteArrayList<>();
        }

        @Bean
        public ApplicationListener<SystemMessageEvent> systemMessageListener(List<SystemMessageEvent> capturedEvents) {
            return capturedEvents::add;
        }
    }

    @BeforeEach
    public void setup() {
        capturedEvents.clear();
    }

    @Test
    public void testSeekTo() throws Exception {
        // Arrange
        String sessionId = "test-session-id";
        String token = "test-token";
        User user = new User(token, sessionId, "TestUser");

        when(userService.getUser(sessionId)).thenReturn(Optional.of(user));
        when(userService.getUserByToken(token)).thenReturn(Optional.of(user));

        PlayableMusic dummyMusic = new PlayableMusic(
                "1", "Test Song", List.of("Artist"), 300000L, "netease", "http://song.mp3", "http://cover.jpg", false
        );

        // Inject dummy music using reflection
        java.lang.reflect.Field field = MusicPlayerService.class.getDeclaredField("currentMusic");
        field.setAccessible(true);
        AtomicReference<PlayableMusic> currentMusicRef = (AtomicReference<PlayableMusic>) field.get(musicPlayerService);
        currentMusicRef.set(dummyMusic);

        // Act
        musicPlayerService.seekTo(120000L, sessionId);

        // Assert
        long currentPos = musicPlayerService.getCurrentPlayerState().nowPlaying().currentPosition();
        assertTrue(currentPos >= 120000L && currentPos <= 121000L,
                "Expected currentPosition to be close to 120000, but was " + currentPos);

        // Verify that the seek event was captured
        SystemMessageEvent publishedEvent = capturedEvents.stream()
                .filter(e -> e.getAction() == PlayerAction.SEEK)
                .findFirst()
                .orElse(null);

        assertNotNull(publishedEvent, "SEEK event should have been published");
        assertEquals("120000", publishedEvent.getPayload());
        assertEquals(token, publishedEvent.getUserId());
    }
}
