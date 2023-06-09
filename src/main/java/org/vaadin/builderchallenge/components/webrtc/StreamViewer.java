package org.vaadin.builderchallenge.components.webrtc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;

@Tag("video")
public class StreamViewer extends Component implements HasStyle, HasSize {

    public StreamViewer() {
        getElement().setAttribute("autoplay", true);
    }
}
