package org.vaadin.builderchallenge.components.webrtc;

public class VideoInputSelector extends MediaDeviceSelector {

    public VideoInputSelector() {
        super(MediaDeviceKind.VIDEO_INPUT);
    }

    @Override
    protected void onValueChanged(MediaDevice old, MediaDevice value) {
        if (value != null) {
            webRTCSupport().ifPresent(s -> s.setVideoInput(value));
        }
    }
}
