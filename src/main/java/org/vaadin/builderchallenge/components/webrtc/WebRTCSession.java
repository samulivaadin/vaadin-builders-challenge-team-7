package org.vaadin.builderchallenge.components.webrtc;

import elemental.json.JsonValue;

import java.util.UUID;

public interface WebRTCSession {

    UUID id();

    void invite(WebRTCSession target);

    void handleInvitation(WebRTCSession source, JsonValue message);

    void handleInvitationAnswer(WebRTCSession source, JsonValue message);

    void handleNewICECandidate(WebRTCSession source, JsonValue message);

    void setAttribute(String name, Object value);

    default <T> void setAttribute(Class<T> type, T value) {
        setAttribute(type.getName(), value);
    }

    <T> T getAttribute(String name, T defaultValue);

    default <T> T getAttribute(Class<T> type, T defaultValue) {
        return getAttribute(type.getName(), defaultValue);
    }
}
