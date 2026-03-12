package com.trivine.llc.api.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class WsDomainEventPublisher {

    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent e) {
        messaging.convertAndSend(
                WsTopics.title(e.getTitleId()),
                new WsEvent<>("postCreated", e.getPostDto(), e.getTitleId())
        );
        messaging.convertAndSend(
                WsTopics.post(e.getPostId()),
                new WsEvent<>("postCreated", e.getPostDto(), e.getPostId())
        );
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onReplyAdded(ReplyAddedEvent e) {
        messaging.convertAndSend(
                WsTopics.post(e.getPostId()),
                new WsEvent<>("replyAdded", e.getReplyDto(), e.getPostId())
        );
        messaging.convertAndSend(
                WsTopics.title(e.getTitleId()),
                new WsEvent<>("replyAdded", e.getReplyDto(), e.getTitleId())
        );
    }
}
