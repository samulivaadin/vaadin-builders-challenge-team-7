package org.vaadin.builderchallenge.views.remoteparticipation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
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
import org.vaadin.builderchallenge.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Logger;

@PageTitle("Remote Participation")
@Route(value = "remote", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@AnonymousAllowed
public class RemoteParticipationView extends VerticalLayout {

    public static final String TESTING_STREAM1 = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";
    public static final String TESTING_STREAM2 = "https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4";

    private static final Logger log = Logger.getLogger(RemoteParticipationView.class.getName());

    private final Map map;

    //Mapping of Coordinates and stream addresses
    private final HashMap<Coordinate, String> coordinateStreams = new HashMap<>();

    public RemoteParticipationView() {
        setSpacing(false);

        map = new Map();
        var vaadinHqCoordinates = new Coordinate(22.29985, 60.45234);
        var germanOfficeCoordinates = new Coordinate(13.45489, 52.51390);
        var usOfficeCoordinates = new Coordinate(-121.92163, 37.36821);
        coordinateStreams.put(vaadinHqCoordinates, TESTING_STREAM1);
        coordinateStreams.put(germanOfficeCoordinates, TESTING_STREAM1);
        coordinateStreams.put(usOfficeCoordinates, TESTING_STREAM1);

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
            var video = new AutoplayVideo(coordinateStreams.get(coord.getCoordinates()));
            dialog.addThemeName(DialogVariant.LUMO_NO_PADDING.getVariantName());

            //Still not possible to do this in a nice way https://github.com/vaadin/flow-components/issues/1173
            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", ev.getMouseDetails().getAbsoluteX() - 200 + "px");
            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", ev.getMouseDetails().getAbsoluteY() - 115 + "px");

            dialog.add(video);
            dialog.open();
        });
        add(map);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getElement().executeJs("""               
                if (navigator.geolocation) {
                    that = this;
                    navigator.geolocation.getCurrentPosition(pos => {
                        that.$server.setCurrentClientCoordinates(pos.coords.latitude, pos.coords.longitude);
                    });
                }
                """);
    }

    @ClientCallable
    public void setCurrentClientCoordinates(String latitude, String longitude) {
        log.info("Got coordinates %s %s".formatted(latitude, longitude));
        Coordinate userCoordinates = new Coordinate(Double.parseDouble(longitude), Double.parseDouble(latitude));
        coordinateStreams.put(userCoordinates, TESTING_STREAM2);
        map.getFeatureLayer().addFeature(getVideoMarkerForCoordinate(userCoordinates));
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

    private MarkerFeature getVideoMarkerForCoordinate(Coordinate coordinate) {
        var opts = new Icon.Options();
        opts.setImg(getVideoStream(coordinateStreams.get(coordinate)));
        return new MarkerFeature(coordinate, new Icon(opts));
    }

    private static StreamResource getVideoStream(String streamAddress) {
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
                                src="%s"
                                type="video/mp4">
                            </video>
                            </body>
                            </foreignObject>
                            </svg>
                             """.formatted(streamAddress);
                    return new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));
                });
    }

}
