package com.example.cns.chat.service;

import com.example.cns.chat.dto.MessageFormat;

public interface MessagePublisher {
    void publish(Long roomId, MessageFormat messageFormat);
}