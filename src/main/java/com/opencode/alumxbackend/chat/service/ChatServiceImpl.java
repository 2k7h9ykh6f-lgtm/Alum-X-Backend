package com.opencode.alumxbackend.chat.service;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatReadService chatReadService;

    @Transactional
    @Override
    public ChatSendResponse createMessage(Long senderId, Long receiverId, String content) {

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Receiver ID could not be same as Sender ID.");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Receiver not found"));

        Long user1Id, user2Id;
        String user1Username, user2Username;

        // normalize by ID
        if (senderId.compareTo(receiverId) < 0) {
            user1Id = senderId;
            user2Id = receiverId;
            user1Username = sender.getUsername();
            user2Username = receiver.getUsername();
        } else {
            user1Id = receiverId;
            user2Id = senderId;
            user1Username = receiver.getUsername();
            user2Username = sender.getUsername();
        }

        Optional<Chat> chat = chatRepository.findByUser1IdAndUser2Id(user1Id, user2Id);

        Long chatId = chat
                .map(Chat::getChatID)
                .orElseGet(() -> {
                    Chat newChat = Chat.builder()
                            .user1Id(user1Id)
                            .user2Id(user2Id)
                            .user1Username(user1Username)
                            .user2Username(user2Username)
                            .build();

                    return chatRepository.save(newChat).getChatID();
                });

        Chat chatRef = Chat.builder()
                .chatID(chatId)
                .build();

        Message message = messageRepository.save(
            Message.builder()
                .chat(chatRef)
                .senderId(senderId)
                .senderUsername(sender.getUsername())
                .content(content)
                .build()
        );

        ChatSendResponse response = ChatSendResponse.builder()
            .messageId(message.getMessageID())
            .chatId(chatId)
            .senderUsername(sender.getUsername())
            .receiverUsername(receiver.getUsername())
            .content(message.getContent())
            .createdAt(message.getCreatedAt())
            .build();

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatSummaryResponse> listUserChats(Long userId) {
        List<ChatSummaryView> chats = chatRepository.findChatSummariesForUser(userId);

        // Build a map of chatId -> unreadCount from the read-receipt service
        Map<Long, Long> unreadByChat = chatReadService.getAllUnreadCounts(userId).stream()
                .collect(Collectors.toMap(UnreadCountResponse::getChatId, UnreadCountResponse::getUnreadCount));

        return chats.stream()
                .map(view -> {
                    boolean isUser1 = userId.equals(view.getUser1Id());
                    Long peerUserId = isUser1 ? view.getUser2Id() : view.getUser1Id();
                    String peerUsername = isUser1 ? view.getUser2Username() : view.getUser1Username();

                    return ChatSummaryResponse.builder()
                            .chatId(view.getChatId())
                            .peerUserId(peerUserId)
                            .peerUsername(peerUsername)
                            .lastMessage(view.getLastMessageContent())
                            .lastMessageSenderId(view.getLastMessageSenderId())
                            .lastMessageSenderUsername(view.getLastMessageSenderUsername())
                            .lastMessageAt(view.getLastMessageCreatedAt())
                            .unreadCount(unreadByChat.getOrDefault(view.getChatId(), 0L))
                            .build();
                })
                .toList();
    }

}
