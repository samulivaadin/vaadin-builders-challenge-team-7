package org.vaadin.builderchallenge.views.remoteparticipation;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.map.configuration.geometry.Point;
import com.vaadin.flow.component.map.configuration.style.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.vaadin.builderchallenge.security.AuthenticatedUser;
import org.vaadin.builderchallenge.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@PageTitle("Remote Participation")
@Route(value = "remote", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@AnonymousAllowed
public class RemoteParticipationView extends VerticalLayout {

    private final AuthenticatedUser authenticatedUser;
    private static final Logger log = Logger.getLogger(RemoteParticipationView.class.getName());

    public RemoteParticipationView(
            AuthenticatedUser authenticatedUser
    ) {
        this.authenticatedUser = authenticatedUser;
        setSpacing(false);

        var map = new Map();
        var vaadinHqCoordinates = new Coordinate(22.29985, 60.45234);
        var germanOfficeCoordinates = new Coordinate(13.45489, 52.51390);
        var usOfficeCoordinates = new Coordinate(-121.92163, 37.36821);

        map.getFeatureLayer().addFeature(getVideoMarkerForCoordinate(vaadinHqCoordinates));
        map.getFeatureLayer().addFeature(getVideoMarkerForCoordinate(germanOfficeCoordinates));
        map.getFeatureLayer().addFeature(getVideoMarkerForCoordinate(usOfficeCoordinates));
        map.addFeatureClickListener(ev -> {
            var coord = (Point) ev.getFeature().getGeometry();
            log.info(String.format("Coordinates of clicked marker %s and x,y position of mouse %d, %d",
                    coord.getCoordinates().toString(),
                    ev.getMouseDetails().getAbsoluteX(),
                    ev.getMouseDetails().getAbsoluteY())
            );
            var dialog = new Dialog();
            var video = new AutoplayVideo("https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4");
            dialog.addThemeName(DialogVariant.LUMO_NO_PADDING.getVariantName());
            dialog.add(video);
            dialog.open();
        });
        add(map);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");
    }

    @Tag("video")
    public static class AutoplayVideo extends HtmlComponent {
        public AutoplayVideo(String src) {
            getElement().setAttribute("src", src);
            getElement().setAttribute("autoplay", true);
            getElement().setAttribute("loop", true);
            getElement().setAttribute("width", "400px");
            getElement().setAttribute("height", "230px");
        }
    }

    private static MarkerFeature getVideoMarkerForCoordinate(Coordinate vaadinHqCoordinates) {
        var opts = new Icon.Options();
        opts.setImg(getVideoStream());
        return new MarkerFeature(vaadinHqCoordinates, new Icon(opts));
    }

    private static StreamResource getVideoStream() {
        return new StreamResource("image.svg",
                () -> {
                    String svg = """
                            <?xml version='1.0' encoding='UTF-8' standalone='no'?>
                            <svg 
                            width="200"
                            height="150" 
                            xmlns='http://www.w3.org/2000/svg' 
                            xmlns:xlink='http://www.w3.org/1999/xlink'>
                            <foreignObject width="200" height="150" x="0" y="0">
                            <body width="200" height="150" xmlns="http://www.w3.org/1999/xhtml">
                            <!-- Looks like the content is rendered only once and thus the video player wont autoplay. -->
                            <video
                                width="200"
                                height="100"
                                autoplay="true"
                                controls="0"
                                muted="true"
                                loop="loop"
                                src="https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
                                type="video/mp4">
                            </video>
                            </body>
                            </foreignObject>
                            </svg>
                             """;
                    return new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));
                });
    }

}
