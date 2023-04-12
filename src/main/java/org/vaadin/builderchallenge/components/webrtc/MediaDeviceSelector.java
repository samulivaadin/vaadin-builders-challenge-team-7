package org.vaadin.builderchallenge.components.webrtc;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.shared.Registration;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public abstract class MediaDeviceSelector extends Composite<ComboBox<MediaDevice>> implements HasLabel {

    private final MediaDeviceKind kind;
    private WebRTCSupport webRTCSupport;
    private Registration webRTCSupportListenerRegistration;

    public MediaDeviceSelector(MediaDeviceKind kind) {
        this.kind = Objects.requireNonNull(kind);
        getContent().setItemLabelGenerator(MediaDevice::label);
        getContent().addValueChangeListener(event -> onValueChanged(event.getOldValue(), event.getValue()));
    }

    public void setWebRTCSupport(WebRTCSupport webRTCSupport) {
        if (webRTCSupportListenerRegistration != null) {
            webRTCSupportListenerRegistration.remove();
            webRTCSupportListenerRegistration = null;
        }
        this.webRTCSupport = webRTCSupport;
        updateDevices();
        if (webRTCSupport != null) {
            webRTCSupportListenerRegistration = webRTCSupport.addMediaDeviceListChangeListener(sender -> updateDevices());
        }
    }

    public Optional<WebRTCSupport> webRTCSupport() {
        return Optional.ofNullable(webRTCSupport);
    }

    private void updateDevices() {
        if (webRTCSupport == null) {
            getContent().setItems(Collections.emptyList());
        } else {
            getContent().setItems(webRTCSupport.mediaDevices().stream().filter(md -> md.kind().equals(kind)).toList());
        }
    }

    protected void onValueChanged(MediaDevice old, MediaDevice value) {
        // NOP by default
    }
}
