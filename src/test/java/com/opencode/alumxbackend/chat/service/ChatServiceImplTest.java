package com.opencode.alumxbackend.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.opencode.alumxbackend.chat.dto.ChatSendResponse;
import com.opencode.alumxbackend.chat.dto.ChatSummaryResponse;
import com.opencode.alumxbackend.chat.dto.ChatSummaryView;
import com.opencode.alumxbackend.chat.model.Chat;
import com.opencode.alumxbackend.chat.model.Message;
import com.opencode.alumxbackend.chat.repository.ChatRepository;
import com.opencode.alumxbackend.chat.repository.MessageRepository;
import com.opencode.alumxbackend.chatreadreceipt.model.ChatReadState;
import com.opencode.alumxbackend.chatreadreceipt.repository.ChatReadStateRepository;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatReadStateRepository chatReadStateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    // ========== listUserChats ==========

    @Test
    @DisplayName("listUserChats - resolves peer, keeps repository order, and fills unread counts")
    void listUserChats_resolvesPeerAndUnreadCounts_inRepositoryOrder() {
        LocalDateTime tA = LocalDateTime.of(2026, 6, 14, 10, 31);
        LocalDateTime tB = LocalDateTime.of(2026, 6, 14, 9, 15);

        // Viewer (id=1) is user1 in chat 100 and user2 in chat 200.
        ChatSummaryView chatA = new ChatSummaryView(
                100L, 1L, "me", 2L, "alice",
                "see you then", 2L, "alice", tA, tA.minusDays(1));
        ChatSummaryView chatB = new ChatSummaryView(
                200L, 3L, "bob", 1L, "me",
                "thanks!", 1L, "me", tB, tB.minusDays(1));

        when(chatRepository.findChatSummariesForUser(userId)).thenReturn(List.of(chatA, chatB));
        when(chatReadStateRepository.findByUserId(userId)).thenReturn(List.of(
                ChatReadState.builder().chatId(100L).userId(userId).lastReadMessageId(50L).build(),
                ChatReadState.builder().chatId(200L).userId(userId).lastReadMessageId(10L).build()));
        when(chatReadStateRepository.countUnreadMessages(100L, userId, 50L)).thenReturn(2L);
        when(chatReadStateRepository.countUnreadMessages(200L, userId, 10L)).thenReturn(5L);

        List<ChatSummaryResponse> result = chatService.listUserChats(userId);

        assertThat(result).extracting(ChatSummaryResponse::getChatId).containsExactly(100L, 200L);

        ChatSummaryResponse first = result.get(0);
        assertThat(first.getOtherUserId()).isEqualTo(2L);
        assertThat(first.getOtherUsername()).isEqualTo("alice");
        assertThat(first.getLastMessageContent()).isEqualTo("see you then");
        assertThat(first.getLastMessageSenderId()).isEqualTo(2L);
        assertThat(first.getLastMessageSenderUsername()).isEqualTo("alice");
        assertThat(first.getLastMessageAt()).isEqualTo(tA);
        assertThat(first.getUnreadCount()).isEqualTo(2L);

        ChatSummaryResponse second = result.get(1);
        assertThat(second.getOtherUserId()).isEqualTo(3L);
        assertThat(second.getOtherUsername()).isEqualTo("bob");
        assertThat(second.getUnreadCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("listUserChats - counts all messages from the peer when no read state exists")
    void listUserChats_noReadState_countsAllMessagesFromOther() {
        ChatSummaryView chat = new ChatSummaryView(
                100L, 1L, "me", 2L, "alice",
                "hi", 2L, "alice", LocalDateTime.now(), LocalDateTime.now());

        when(chatRepository.findChatSummariesForUser(userId)).thenReturn(List.of(chat));
        when(chatReadStateRepository.findByUserId(userId)).thenReturn(List.of());
        when(chatReadStateRepository.countAllMessagesFromOther(100L, userId)).thenReturn(4L);

        List<ChatSummaryResponse> result = chatService.listUserChats(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOtherUserId()).isEqualTo(2L);
        assertThat(result.get(0).getUnreadCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("listUserChats - treats a null last-read marker as nothing read")
    void listUserChats_nullReadMarker_countsAllMessagesFromOther() {
        ChatSummaryView chat = new ChatSummaryView(
                100L, 1L, "me", 2L, "alice",
                "hi", 2L, "alice", LocalDateTime.now(), LocalDateTime.now());

        when(chatRepository.findChatSummariesForUser(userId)).thenReturn(List.of(chat));
        when(chatReadStateRepository.findByUserId(userId)).thenReturn(List.of(
                ChatReadState.builder().chatId(100L).userId(userId).lastReadMessageId(null).build()));
        when(chatReadStateRepository.countAllMessagesFromOther(100L, userId)).thenReturn(7L);

        List<ChatSummaryResponse> result = chatService.listUserChats(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUnreadCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("listUserChats - returns empty list when the user has no chats")
    void listUserChats_noChats_returnsEmptyList() {
        when(chatRepository.findChatSummariesForUser(userId)).thenReturn(List.of());
        when(chatReadStateRepository.findByUserId(userId)).thenReturn(List.of());

        List<ChatSummaryResponse> result = chatService.listUserChats(userId);

        assertThat(result).isEmpty();
    }

    // ========== createMessage (behavior must remain unchanged) ==========

    @Test
    @DisplayName("createMessage - persists the message, creates the chat, and broadcasts the response")
    void createMessage_happyPath_persistsAndBroadcasts() {
        Long senderId = 1L;
        Long receiverId = 2L;

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(User.builder().id(senderId).username("me").build()));
        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(User.builder().id(receiverId).username("alice").build()));
        when(chatRepository.findByUser1IdAndUser2Id(senderId, receiverId)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenReturn(
                Chat.builder().chatID(100L).user1Id(senderId).user2Id(receiverId)
                        .user1Username("me").user2Username("alice").build());
        when(messageRepository.save(any(Message.class))).thenReturn(
                Message.builder().messageID(500L).senderId(senderId).senderUsername("me")
                        .content("hello").createdAt(LocalDateTime.now()).build());

        ChatSendResponse response = chatService.createMessage(senderId, receiverId, "hello");

        assertThat(response.getMessageId()).isEqualTo(500L);
        assertThat(response.getChatId()).isEqualTo(100L);
        assertThat(response.getSenderUsername()).isEqualTo("me");
        assertThat(response.getReceiverUsername()).isEqualTo("alice");
        assertThat(response.getContent()).isEqualTo("hello");

        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/100"), eq(response));
    }
}
