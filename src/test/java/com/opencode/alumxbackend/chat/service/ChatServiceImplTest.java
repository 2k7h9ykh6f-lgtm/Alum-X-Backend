package com.opencode.alumxbackend.chat.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
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
import com.opencode.alumxbackend.chatreadreceipt.dto.UnreadCountResponse;
import com.opencode.alumxbackend.chatreadreceipt.service.ChatReadService;
import com.opencode.alumxbackend.common.exception.Errors.BadRequestException;
import com.opencode.alumxbackend.users.model.User;
import com.opencode.alumxbackend.users.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChatReadService chatReadService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final Long USER1_ID = 1L;
    private static final String USER1_NAME = "alice";
    private static final Long USER2_ID = 2L;
    private static final String USER2_NAME = "bob";
    private static final Long CHAT_ID = 100L;

    private User sender;
    private User receiver;
    private Chat existingChat;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(USER1_ID).username(USER1_NAME).name("Alice").build();
        receiver = User.builder().id(USER2_ID).username(USER2_NAME).name("Bob").build();
        existingChat = Chat.builder()
                .chatID(CHAT_ID)
                .user1Id(USER1_ID).user1Username(USER1_NAME)
                .user2Id(USER2_ID).user2Username(USER2_NAME)
                .build();
    }

    // ========== createMessage ==========

    @Nested
    @DisplayName("createMessage")
    class CreateMessageTests {

        // ========== SUCCESS CASES ==========

        @Test
        @DisplayName("should send message and create new chat when none exists")
        void createMessage_NewChat_ReturnsResponse() {
            when(userRepository.findById(USER1_ID)).thenReturn(Optional.of(sender));
            when(userRepository.findById(USER2_ID)).thenReturn(Optional.of(receiver));
            when(chatRepository.findByUser1IdAndUser2Id(USER1_ID, USER2_ID)).thenReturn(Optional.empty());

            Chat savedChat = Chat.builder().chatID(CHAT_ID)
                    .user1Id(USER1_ID).user2Id(USER2_ID)
                    .user1Username(USER1_NAME).user2Username(USER2_NAME)
                    .build();
            when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

            Message savedMessage = Message.builder()
                    .messageID(10L).chat(Chat.builder().chatID(CHAT_ID).build())
                    .senderId(USER1_ID).senderUsername(USER1_NAME)
                    .content("Hello").createdAt(LocalDateTime.now())
                    .build();
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            ChatSendResponse response = chatService.createMessage(USER1_ID, USER2_ID, "Hello");

            assertThat(response).isNotNull();
            assertThat(response.getChatId()).isEqualTo(CHAT_ID);
            assertThat(response.getContent()).isEqualTo("Hello");
            assertThat(response.getSenderUsername()).isEqualTo(USER1_NAME);
            assertThat(response.getReceiverUsername()).isEqualTo(USER2_NAME);

            verify(chatRepository).save(any(Chat.class));
            verify(messageRepository).save(any(Message.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + CHAT_ID), any(ChatSendResponse.class));
        }

        @Test
        @DisplayName("should reuse existing chat when one already exists")
        void createMessage_ExistingChat_ReusesChatId() {
            when(userRepository.findById(USER1_ID)).thenReturn(Optional.of(sender));
            when(userRepository.findById(USER2_ID)).thenReturn(Optional.of(receiver));
            when(chatRepository.findByUser1IdAndUser2Id(USER1_ID, USER2_ID))
                    .thenReturn(Optional.of(existingChat));

            Message savedMessage = Message.builder()
                    .messageID(11L).chat(Chat.builder().chatID(CHAT_ID).build())
                    .senderId(USER1_ID).senderUsername(USER1_NAME)
                    .content("Hi again").createdAt(LocalDateTime.now())
                    .build();
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            ChatSendResponse response = chatService.createMessage(USER1_ID, USER2_ID, "Hi again");

            assertThat(response.getChatId()).isEqualTo(CHAT_ID);
            verify(chatRepository, never()).save(any(Chat.class));
        }

        @Test
        @DisplayName("should normalize user ordering when senderId > receiverId")
        void createMessage_SenderIdGreater_NormalizesOrder() {
            when(userRepository.findById(USER2_ID)).thenReturn(Optional.of(receiver));
            when(userRepository.findById(USER1_ID)).thenReturn(Optional.of(sender));
            // sender=2, receiver=1 => user1=1 (receiver), user2=2 (sender)
            when(chatRepository.findByUser1IdAndUser2Id(USER1_ID, USER2_ID))
                    .thenReturn(Optional.of(existingChat));

            Message savedMessage = Message.builder()
                    .messageID(12L).chat(Chat.builder().chatID(CHAT_ID).build())
                    .senderId(USER2_ID).senderUsername(USER2_NAME)
                    .content("Hey").createdAt(LocalDateTime.now())
                    .build();
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            ChatSendResponse response = chatService.createMessage(USER2_ID, USER1_ID, "Hey");

            assertThat(response.getSenderUsername()).isEqualTo(USER2_NAME);
            assertThat(response.getReceiverUsername()).isEqualTo(USER1_NAME);
        }

        // ========== FAILURE CASES ==========

        @Test
        @DisplayName("should throw BadRequestException when sender equals receiver")
        void createMessage_SameSenderReceiver_ThrowsBadRequest() {
            assertThatThrownBy(() -> chatService.createMessage(USER1_ID, USER1_ID, "oops"))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when sender does not exist")
        void createMessage_SenderNotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createMessage(999L, USER2_ID, "hi"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Sender not found");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when receiver does not exist")
        void createMessage_ReceiverNotFound_ThrowsException() {
            when(userRepository.findById(USER1_ID)).thenReturn(Optional.of(sender));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createMessage(USER1_ID, 999L, "hi"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Receiver not found");
        }
    }

    // ========== listUserChats ==========

    @Nested
    @DisplayName("listUserChats")
    class ListUserChatsTests {

        // ========== SUCCESS CASES ==========

        @Test
        @DisplayName("should return chat summaries with peer info, last message and unread count")
        void listUserChats_WithChats_ReturnsFullSummary() {
            LocalDateTime lastMsgTime = LocalDateTime.of(2026, 6, 14, 10, 30);

            ChatSummaryView view = new ChatSummaryView(
                    CHAT_ID, USER1_ID, USER1_NAME, USER2_ID, USER2_NAME,
                    "Hey there!", USER2_ID, USER2_NAME, lastMsgTime, LocalDateTime.now()
            );

            when(chatRepository.findChatSummariesForUser(USER1_ID)).thenReturn(List.of(view));
            when(chatReadService.getAllUnreadCounts(USER1_ID))
                    .thenReturn(List.of(new UnreadCountResponse(CHAT_ID, USER1_ID, 3L)));

            List<ChatSummaryResponse> result = chatService.listUserChats(USER1_ID);

            assertThat(result).hasSize(1);

            ChatSummaryResponse summary = result.get(0);
            assertThat(summary.getChatId()).isEqualTo(CHAT_ID);
            assertThat(summary.getPeerUserId()).isEqualTo(USER2_ID);
            assertThat(summary.getPeerUsername()).isEqualTo(USER2_NAME);
            assertThat(summary.getLastMessage()).isEqualTo("Hey there!");
            assertThat(summary.getLastMessageSenderId()).isEqualTo(USER2_ID);
            assertThat(summary.getLastMessageSenderUsername()).isEqualTo(USER2_NAME);
            assertThat(summary.getLastMessageAt()).isEqualTo(lastMsgTime);
            assertThat(summary.getUnreadCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should resolve peer correctly when user is user2 in the chat")
        void listUserChats_UserIsUser2_ResolvesPeerAsUser1() {
            ChatSummaryView view = new ChatSummaryView(
                    CHAT_ID, USER1_ID, USER1_NAME, USER2_ID, USER2_NAME,
                    "Hello", USER1_ID, USER1_NAME, LocalDateTime.now(), LocalDateTime.now()
            );

            // query as user2
            when(chatRepository.findChatSummariesForUser(USER2_ID)).thenReturn(List.of(view));
            when(chatReadService.getAllUnreadCounts(USER2_ID))
                    .thenReturn(List.of(new UnreadCountResponse(CHAT_ID, USER2_ID, 1L)));

            List<ChatSummaryResponse> result = chatService.listUserChats(USER2_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPeerUserId()).isEqualTo(USER1_ID);
            assertThat(result.get(0).getPeerUsername()).isEqualTo(USER1_NAME);
        }

        @Test
        @DisplayName("should default unreadCount to 0 when no read state exists for a chat")
        void listUserChats_NoReadState_DefaultsUnreadToZero() {
            ChatSummaryView view = new ChatSummaryView(
                    CHAT_ID, USER1_ID, USER1_NAME, USER2_ID, USER2_NAME,
                    "msg", USER2_ID, USER2_NAME, LocalDateTime.now(), LocalDateTime.now()
            );

            when(chatRepository.findChatSummariesForUser(USER1_ID)).thenReturn(List.of(view));
            // empty unread counts — no read state for this chat
            when(chatReadService.getAllUnreadCounts(USER1_ID)).thenReturn(Collections.emptyList());

            List<ChatSummaryResponse> result = chatService.listUserChats(USER1_ID);

            assertThat(result.get(0).getUnreadCount()).isZero();
        }

        @Test
        @DisplayName("should return empty list when user has no chats")
        void listUserChats_NoChats_ReturnsEmpty() {
            when(chatRepository.findChatSummariesForUser(USER1_ID)).thenReturn(Collections.emptyList());
            when(chatReadService.getAllUnreadCounts(USER1_ID)).thenReturn(Collections.emptyList());

            List<ChatSummaryResponse> result = chatService.listUserChats(USER1_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle multiple chats each with correct unread count")
        void listUserChats_MultipleChats_MapsUnreadCounts() {
            Long chat2Id = 200L;
            LocalDateTime t1 = LocalDateTime.of(2026, 6, 14, 10, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 6, 14, 11, 0);

            ChatSummaryView view1 = new ChatSummaryView(
                    CHAT_ID, USER1_ID, USER1_NAME, USER2_ID, USER2_NAME,
                    "first", USER2_ID, USER2_NAME, t1, t1
            );
            ChatSummaryView view2 = new ChatSummaryView(
                    chat2Id, USER1_ID, USER1_NAME, 3L, "charlie",
                    "second", 3L, "charlie", t2, t2
            );

            when(chatRepository.findChatSummariesForUser(USER1_ID))
                    .thenReturn(List.of(view2, view1)); // repo returns newest-first
            when(chatReadService.getAllUnreadCounts(USER1_ID)).thenReturn(List.of(
                    new UnreadCountResponse(CHAT_ID, USER1_ID, 5L),
                    new UnreadCountResponse(chat2Id, USER1_ID, 0L)
            ));

            List<ChatSummaryResponse> result = chatService.listUserChats(USER1_ID);

            assertThat(result).hasSize(2);

            // ordering is preserved from repository (newest first)
            assertThat(result.get(0).getChatId()).isEqualTo(chat2Id);
            assertThat(result.get(0).getPeerUsername()).isEqualTo("charlie");
            assertThat(result.get(0).getUnreadCount()).isZero();

            assertThat(result.get(1).getChatId()).isEqualTo(CHAT_ID);
            assertThat(result.get(1).getPeerUsername()).isEqualTo(USER2_NAME);
            assertThat(result.get(1).getUnreadCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should handle chat with no messages (null last message fields)")
        void listUserChats_ChatWithNoMessages_HandlesNulls() {
            ChatSummaryView view = new ChatSummaryView(
                    CHAT_ID, USER1_ID, USER1_NAME, USER2_ID, USER2_NAME,
                    null, null, null, null, LocalDateTime.now()
            );

            when(chatRepository.findChatSummariesForUser(USER1_ID)).thenReturn(List.of(view));
            when(chatReadService.getAllUnreadCounts(USER1_ID)).thenReturn(Collections.emptyList());

            List<ChatSummaryResponse> result = chatService.listUserChats(USER1_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLastMessage()).isNull();
            assertThat(result.get(0).getLastMessageSenderId()).isNull();
            assertThat(result.get(0).getLastMessageAt()).isNull();
            assertThat(result.get(0).getUnreadCount()).isZero();
        }
    }
}
