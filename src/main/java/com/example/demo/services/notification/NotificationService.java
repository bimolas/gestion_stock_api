package com.example.demo.services.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.Message;
import com.example.demo.repositories.MessageRepository;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void logStockMovement(Long articleId, int quantity, String movementType) {
        try {
            Message message = new Message();
            message.setTitle("Stock Movement");
            message.setContent(
                    "Movement=" + movementType + " | ArticleId=" + articleId + " | Quantity=" + quantity);
            message.setRead(false);
            messageRepository.save(message);
        } catch (Exception ex) {
            logger.warn("Message creation failed for articleId={} movementType={}", articleId, movementType, ex);
        }
    }

    @Transactional
    public void logAction(String title, String content) {
        try {
            Message message = new Message();
            message.setTitle(title);
            message.setContent(content);
            message.setRead(false);
            messageRepository.save(message);
        } catch (Exception ex) {
            logger.warn("Action message creation failed: title={}", title, ex);
        }
    }
}
