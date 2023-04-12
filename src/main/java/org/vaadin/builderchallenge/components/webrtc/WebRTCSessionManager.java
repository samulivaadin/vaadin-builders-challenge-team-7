package org.vaadin.builderchallenge.components.webrtc;

import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WebRTCSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebRTCSessionManager.class);
    private final Map<UUID, WebRTCSession> sessions = new HashMap<>();

    public void register(WebRTCSession session) {
        log.debug("Registering session {}", session);
        Set<WebRTCSession> sessionsToInvite;
        synchronized (this) {
            if (sessions.containsKey(session.id())) {
                throw new IllegalArgumentException("Session ID already exists");
            }
            sessionsToInvite = Set.copyOf(sessions.values());
            sessions.put(session.id(), session);
        }
        // TODO This only works for a small number of sessions. For larger numbers, we need to be smarter
        sessionsToInvite.forEach(session::invite);
    }

    public void unregister(WebRTCSession session) {
        log.debug("Unregistering session {}", session); // Client side will take cae of hanging up stuff
        synchronized (this) {
            sessions.remove(session.id());
        }
    }

    public void sendNewICECandidate(WebRTCSession source, UUID recipient, JsonValue message) {
        validateMessageFormat(source, recipient, message);
        getSession(recipient).ifPresent(s -> s.handleNewICECandidate(source, message));
    }

    public void sendVideoAnswer(WebRTCSession source, UUID recipient, JsonValue message) {
        validateMessageFormat(source, recipient, message);
        getSession(recipient).ifPresent(s -> s.handleInvitationAnswer(source, message));
    }

    public void sendVideoOffer(WebRTCSession source, UUID recipient, JsonValue message) {
        validateMessageFormat(source, recipient, message);
        getSession(recipient).ifPresent(s -> s.handleInvitation(source, message));
    }

    private Optional<WebRTCSession> getSession(UUID id) {
        synchronized (this) {
            return Optional.ofNullable(sessions.get(id));
        }
    }

    private void validateMessageFormat(WebRTCSession source, UUID recipient, JsonValue message) {
        if (message instanceof JsonObject msgObj) {
            var sender = msgObj.getString("sender");
            var target = msgObj.getString("target");
            if (!source.id().toString().equals(sender)) {
                throw new IllegalArgumentException("Message sender does not match source");
            }
            if (!recipient.toString().equals(target)) {
                throw new IllegalArgumentException("Message target does not match recipient");
            }
        } else {
            throw new IllegalArgumentException("Message is not a JSON object");
        }
    }
}
