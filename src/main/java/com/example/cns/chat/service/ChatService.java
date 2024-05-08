package com.example.cns.chat.service;

import com.example.cns.chat.domain.Chat;
import com.example.cns.chat.domain.ChatFile;
import com.example.cns.chat.domain.ChatFileRepository;
import com.example.cns.chat.domain.ChatRoom;
import com.example.cns.chat.domain.repository.ChatRepository;
import com.example.cns.chat.domain.repository.ChatRoomRepository;
import com.example.cns.chat.dto.request.MessageFormat;
import com.example.cns.feed.post.dto.response.PostFileResponse;
import com.example.cns.member.domain.Member;
import com.example.cns.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatFileRepository chatFileRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void saveTextMessage(MessageFormat message) {
        Member sender = memberRepository.findById(message.memberId()).get();
        ChatRoom chatRoom = chatRoomRepository.findById(message.roomId()).get();

        LocalDateTime now = LocalDateTime.now();
        // 채팅방의 마지막 채팅 갱신
        chatRoom.saveLastChat(message.content(), now);

        // 채팅 저장
        Chat chat = message.toChatEntity(chatRoom, sender, now);
        chatRepository.save(chat);
    }

    @Transactional
    public void saveImageMessage(MessageFormat imageMessage, List<PostFileResponse> fileResponses) {
        Member sender = memberRepository.findById(imageMessage.memberId()).get();
        ChatRoom chatRoom = chatRoomRepository.findById(imageMessage.roomId()).get();

        LocalDateTime now = LocalDateTime.now();
        // 채팅방의 마지막 채팅 갱신
        chatRoom.saveLastChat("image", now);

        // 채팅 저장
        Chat save = chatRepository.save(imageMessage.toChatEntityForImageType(chatRoom, sender, now));

        // 채팅 파일 저장
        for (PostFileResponse fileInfo : fileResponses) {
            ChatFile chatFile = ChatFile.builder().fileName(fileInfo.uploadFileName())
                    .url(fileInfo.uploadFileURL())
                    .fileType(fileInfo.fileType())
                    .createdAt(now)
                    .chat(save)
                    .build();
            chatFileRepository.save(chatFile);
        }
    }
}
