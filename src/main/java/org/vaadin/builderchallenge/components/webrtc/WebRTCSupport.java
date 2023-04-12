package org.vaadin.builderchallenge.components.webrtc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonArray;
import elemental.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

@Tag("webrtc-support")
@JsModule("webrtc-support/webrtc-support.js")
public class WebRTCSupport extends Component {

    private static final Logger log = LoggerFactory.getLogger(WebRTCSupport.class.getName());
    private final List<MediaDevice> mediaDevices = new ArrayList<>();
    private final List<MediaDeviceListChangeListener> mediaDeviceListChangeListeners = new ArrayList<>();
    private StreamViewer remoteVideo;
    private StreamViewer selfVideo;

    public StreamViewer remoteVideo() {
        return remoteVideo;
    }

    public void setRemoteVideo(StreamViewer streamViewer) {
        this.remoteVideo = streamViewer;
    }

    public StreamViewer selfVideo() {
        return selfVideo;
    }

    public void setSelfVideo(StreamViewer selfVideo) {
        this.selfVideo = selfVideo;
    }

    public List<MediaDevice> mediaDevices() {
        return Collections.unmodifiableList(mediaDevices);
    }

    public void start() {
        var remoteVideoElement = Optional.ofNullable(remoteVideo).map(HasElement::getElement).orElse(null);
        var selfVideoElement = Optional.ofNullable(selfVideo).map(HasElement::getElement).orElse(null);
        getElement().callJsFunction("start",
                remoteVideoElement,
                selfVideoElement).then(
                result -> {
                    log.debug("WebRTC started");
                    updateMediaDevices(result);
                },
                error -> {
                    log.error("Error starting WebRTC: {}", error);
                }
        );
    }

    private void updateMediaDevices(JsonValue devices) {
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

    public void stop() {
        getElement().callJsFunction("stop"); // TODO What to do with the result?
    }

    public void setAudioInput(MediaDevice mediaDevice) {
        // TODO Implement me!
    }

    public void setAudioOutput(MediaDevice mediaDevice) {
        // TODO Implement me!
    }

    public void setVideoInput(MediaDevice mediaDevice) {
        Objects.requireNonNull(mediaDevice, "mediaDevice must not be null");
        if (mediaDevice.kind() != MediaDeviceKind.VIDEO_INPUT) {
            throw new IllegalArgumentException("MediaDevice must be a video input");
        }
        getElement().callJsFunction("changeCamera", mediaDevice.deviceId()).then(
                result -> log.debug("Camera changed"),
                error -> log.error("Error changing camera: {}", error)
        );
    }

    public Registration addMediaDeviceListChangeListener(MediaDeviceListChangeListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        mediaDeviceListChangeListeners.add(listener);
        return () -> mediaDeviceListChangeListeners.remove(listener);
    }

    @FunctionalInterface
    public interface MediaDeviceListChangeListener extends Serializable {
        void onMediaDevicesChanged(WebRTCSupport sender);
    }
}
