package org.vaadin.builderchallenge.components.webrtc;

import elemental.json.JsonValue;

import java.util.UUID;

public interface WebRTCSession {

    UUID id();

    void invite(WebRTCSession target);

    void handleInvitation(WebRTCSession source, JsonValue message);

    void handleInvitationAnswer(WebRTCSession source, JsonValue message);

    void handleNewICECandidate(WebRTCSession source, JsonValue message);
}
