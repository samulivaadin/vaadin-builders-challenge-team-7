package org.vaadin.builderchallenge.components.webrtc;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

import static java.util.Objects.requireNonNull;

@Tag("webrtc-support")
@JsModule("webrtc-support/webrtc-support.js")
public class WebRTCSupport extends Component implements WebRTCSession {

    private static final Logger log = LoggerFactory.getLogger(WebRTCSupport.class.getName());
    private final List<MediaDevice> mediaDevices = new ArrayList<>();
    private final List<MediaDeviceListChangeListener> mediaDeviceListChangeListeners = new ArrayList<>();
    private final UUID selfId = UUID.randomUUID();
    private final WebRTCSessionManager webRTCSessionManager;
    private RemoteStreamAddedHandler remoteStreamAddedHandler;
    private RemoteStreamRemovedHandler remoteStreamRemovedHandler;
    private final Map<UUID, StreamViewer> remoteVideo = new HashMap<>();

    public WebRTCSupport(WebRTCSessionManager webRTCSessionManager) {
        this.webRTCSessionManager = requireNonNull(webRTCSessionManager, "webRTCSessionManager must not be null");
        getElement().setProperty("selfId", selfId.toString());
    }

    public void setSelfVideo(StreamViewer selfVideo) {
        Objects.requireNonNull(selfVideo, "selfVideo must not be null");
        getElement().callJsFunction("showSelfVideo", selfVideo.getElement());
    }

    public void setRemoteStreamAddedHandler(RemoteStreamAddedHandler remoteStreamAddedHandler) {
        this.remoteStreamAddedHandler = remoteStreamAddedHandler;
    }

    public void setRemoteStreamRemovedHandler(RemoteStreamRemovedHandler remoteStreamRemovedHandler) {
        this.remoteStreamRemovedHandler = remoteStreamRemovedHandler;
    }

    public List<MediaDevice> mediaDevices() {
        return Collections.unmodifiableList(mediaDevices);
    }

    public void setVideoInput(MediaDevice mediaDevice) {
        requireNonNull(mediaDevice, "mediaDevice must not be null");
        if (mediaDevice.kind() != MediaDeviceKind.VIDEO_INPUT) {
            throw new IllegalArgumentException("MediaDevice must be a video input");
        }
        getElement().callJsFunction("changeCamera", mediaDevice.deviceId()).then(
                result -> log.debug("{} changed the camera to {}", this, mediaDevice),
                error -> log.error("{} got an error changing the camera to {}: {}", this, mediaDevice, error)
        );
    }

    public Registration addMediaDeviceListChangeListener(MediaDeviceListChangeListener listener) {
        requireNonNull(listener, "listener must not be null");
        mediaDeviceListChangeListeners.add(listener);
        return () -> mediaDeviceListChangeListeners.remove(listener);
    }

    @ClientCallable
    protected void sendToServer(JsonValue message) {
        if (message instanceof JsonObject msgObject) {
            var target = UUID.fromString(msgObject.getString("target"));
            var type = msgObject.getString("type");
            switch (type) {
                case "video-answer" -> webRTCSessionManager.sendVideoAnswer(this, target, msgObject);
                case "new-ice-candidate" -> webRTCSessionManager.sendNewICECandidate(this, target, msgObject);
                case "video-offer" -> webRTCSessionManager.sendVideoOffer(this, target, msgObject);
                default -> throw new IllegalArgumentException("Invalid message type");
            }
        } else {
            throw new IllegalArgumentException("Invalid message format");
        }
    }

    @ClientCallable
    protected void remoteStreamAdded(String sessionId) {
        log.debug("Remote stream {} added to {}", sessionId, this);
        var id = UUID.fromString(sessionId);
        var viewer = remoteVideo.get(id);
        if (viewer == null && remoteStreamAddedHandler != null) {
            viewer = remoteStreamAddedHandler.onRemoteStreamAdded(id);
            remoteVideo.put(id, viewer);
        }
        if (viewer != null) {
            getElement().callJsFunction("showRemoteStream", sessionId, viewer.getElement()).then(
                    result -> log.trace("{} is showing remote stream {}", this, sessionId),
                    error -> log.trace("{} got an error showing remote stream {}: {}", this, sessionId, error)
            );
        }
    }

    @ClientCallable
    protected void remoteStreamRemoved(String sessionId) {
        log.debug("Remote stream {} removed from {}", sessionId, this);
        var id = UUID.fromString(sessionId);
        var viewer = remoteVideo.remove(id);
        if (viewer != null) {
            if (remoteStreamRemovedHandler != null) {
                remoteStreamRemovedHandler.onRemoteStreamRemoved(id, viewer);
            }
        }
        getElement().callJsFunction("hideRemoteStream", sessionId).then(
                result -> log.trace("{} hid remote stream {}", this, sessionId),
                error -> log.trace("{} got an error hiding remote stream {}: {}", this, sessionId, error)
        );
    }

    @ClientCallable
    protected void updateMediaDevices(JsonValue devices) {
        mediaDevices.clear();
        if (devices instanceof JsonArray deviceArray) {
            for (var i = 0; i < deviceArray.length(); ++i) {
                var device = deviceArray.getObject(i);
                var deviceId = device.getString("deviceId");
                var kind = device.getString("kind");
                var label = device.getString("label");

                if ("audioinput".equals(kind)) {
                    mediaDevices.add(new MediaDevice(deviceId, MediaDeviceKind.AUDIO_INPUT, label));
                } else if ("audiooutput".equals(kind)) {
                    mediaDevices.add(new MediaDevice(deviceId, MediaDeviceKind.AUDIO_OUTPUT, label));
                } else if ("videoinput".equals(kind)) {
                    mediaDevices.add(new MediaDevice(deviceId, MediaDeviceKind.VIDEO_INPUT, label));
                } else {
                    log.warn("Unknown device kind: {}", kind);
                }
            }
        }
        log.debug("Devices updated: {}", mediaDevices);
        List.copyOf(mediaDeviceListChangeListeners).forEach(listener -> listener.onMediaDevicesChanged(this));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        webRTCSessionManager.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        webRTCSessionManager.unregister(this);
    }

    @Override
    public UUID id() {
        return selfId;
    }

    @Override
    public void invite(WebRTCSession target) {
        getUI().ifPresent(ui -> ui.access(() -> {
            log.trace("{} invites {}", this, target);
            getElement().callJsFunction("invite", target.id().toString()).then(
                    result -> log.trace("{} sent invitation to {}", this, target),
                    error -> log.trace("{} got an error sending invitation to {}: {}", this, target, error)
            );
        }));
    }

    @Override
    public void handleInvitation(WebRTCSession source, JsonValue message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            log.trace("{} handles invitation from {}", this, source);
            getElement().callJsFunction("handleInvitation", message).then(
                    result -> log.trace("{} handled invitation from {}", this, source),
                    error -> log.trace("{} got an error handling invitation from {}: {}", this, source, error)
            );
        }));
    }

    @Override
    public void handleInvitationAnswer(WebRTCSession source, JsonValue message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            log.trace("{} handles invitation answer from {}", this, source);
            getElement().callJsFunction("handleInvitationAnswer", message).then(
                    result -> log.trace("{} handled invitation answer from {}", this, source),
                    error -> log.trace("{} got an error handling invitation answer from {}: {}", this, source, error)
            );
        }));
    }

    @Override
    public void handleNewICECandidate(WebRTCSession source, JsonValue message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            log.trace("{} handles new ICE candidate from {}", this, source);
            getElement().callJsFunction("handleNewICECandidate", message).then(
                    result -> log.trace("{} handled new ICE candidate from {}", this, source),
                    error -> log.trace("{} got an error handling new ICE candidate from {}: {}", this, source, error)
            );
        }));
    }

    @FunctionalInterface
    public interface MediaDeviceListChangeListener extends Serializable {
        void onMediaDevicesChanged(WebRTCSupport sender);
    }

    @FunctionalInterface
    public interface RemoteStreamAddedHandler extends Serializable {
        StreamViewer onRemoteStreamAdded(UUID sessionId);
    }

    @FunctionalInterface
    public interface RemoteStreamRemovedHandler extends Serializable {
        void onRemoteStreamRemoved(UUID sessionId, StreamViewer streamViewer);
    }

    @Override
    public String toString() {
        return "%s{%s}".formatted(getClass().getSimpleName(), selfId);
    }
}
