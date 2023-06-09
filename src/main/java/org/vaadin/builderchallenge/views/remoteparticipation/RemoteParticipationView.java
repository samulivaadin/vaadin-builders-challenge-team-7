package org.vaadin.builderchallenge.views.remoteparticipation;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.Feature;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.map.configuration.geometry.Point;
import com.vaadin.flow.component.map.configuration.style.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.builderchallenge.components.webrtc.*;
import org.vaadin.builderchallenge.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

@PageTitle("Remote Participation")
@Route(value = "remote", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@AnonymousAllowed
public class RemoteParticipationView extends HorizontalLayout {

    public static final String TESTING_STREAM1 = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";
    public static final String TESTING_STREAM2 = "https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4";

    private static final Logger log = Logger.getLogger(RemoteParticipationView.class.getName());

    private final Map map;
    private final WebRTCSupport webRTCSupport;
    private final StreamViewer selfCamera;

    //Mapping of Coordinates and stream addresses
    private final HashMap<Coordinate, String> coordinateStreams = new HashMap<>();

    public RemoteParticipationView(WebRTCSessionManager webRTCSessionManager) {
        setPadding(true);
        setSpacing(true);

        webRTCSupport = new WebRTCSupport(webRTCSessionManager);

        selfCamera = new StreamViewer();
        selfCamera.setWidthFull();
        selfCamera.addClassName(LumoUtility.Border.ALL);
        selfCamera.addClassName(LumoUtility.BorderColor.CONTRAST_5);
        selfCamera.addClassName(LumoUtility.BorderRadius.SMALL);

        webRTCSupport.setSelfVideo(selfCamera);

        var sideBar = new VerticalLayout();
        sideBar.setWidth("400px");
        sideBar.setHeightFull();
        sideBar.setSpacing(false);
        sideBar.setPadding(false);
        add(sideBar);

        sideBar.add(webRTCSupport);

        var location = new ComboBox<Location>();
        location.setItems(Location.values());
        location.setWidthFull();
        location.addValueChangeListener(event -> webRTCSupport.setAttribute(Location.class, event.getValue()));
        sideBar.add(location);

        sideBar.add(selfCamera);

        webRTCSupport.setRemoteStreamAddedHandler(sessionId -> {
            var viewer = new StreamViewer();
            viewer.addClassName(LumoUtility.Border.ALL);
            viewer.addClassName(LumoUtility.BorderColor.CONTRAST_5);
            viewer.addClassName(LumoUtility.BorderRadius.SMALL);
            return viewer;
        });
        webRTCSupport.setRemoteStreamRemovedHandler((sessionId, streamViewer) -> streamViewer.removeFromParent());

        var cameraSelector = new VideoInputSelector();
        cameraSelector.setWebRTCSupport(webRTCSupport);
        cameraSelector.setTooltipText("Camera");
        cameraSelector.setWidthFull();
        sideBar.add(cameraSelector);

        map = new Map();
        var vaadinHqCoordinates = new Coordinate(22.29985, 60.45234);
        var germanOfficeCoordinates = new Coordinate(13.45489, 52.51390);
        var usOfficeCoordinates = new Coordinate(-121.92163, 37.36821);
        coordinateStreams.put(vaadinHqCoordinates, TESTING_STREAM1);
        coordinateStreams.put(germanOfficeCoordinates, TESTING_STREAM1);
        coordinateStreams.put(usOfficeCoordinates, TESTING_STREAM1);

        Stream.of(Location.values()).forEach(l -> {
            map.getFeatureLayer().addFeature(addVideoMarkerForLocation(l));
        });
        map.addFeatureClickListener(ev -> {
            var clickedLocation = featureLocationMap.get(ev.getFeature());
            if (clickedLocation != null) {
                webRTCSessionManager
                        .findSession(session -> clickedLocation.equals(session.getAttribute(Location.class, null)))
                        .map(WebRTCSession::id)
                        .flatMap(webRTCSupport::getRemoteVideo)
                        .ifPresent(viewer -> {
                            var dialog = new Dialog();
                            dialog.addThemeName(DialogVariant.LUMO_NO_PADDING.getVariantName());

                            //Still not possible to do this in a nice way https://github.com/vaadin/flow-components/issues/1173
                            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
                            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
                            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", ev.getMouseDetails().getAbsoluteX() - 200 + "px");
                            dialog.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", ev.getMouseDetails().getAbsoluteY() - 115 + "px");

                            dialog.add(viewer);
                            viewer.setWidthFull();
                            dialog.setWidth("500px");
                            dialog.open();
                        });
            }
        });
        map.setSizeFull();
        add(map);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultVerticalComponentAlignment(Alignment.CENTER);
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

    private final java.util.Map<Feature, Location> featureLocationMap = new HashMap<>();

    private MarkerFeature addVideoMarkerForLocation(Location location) {
        var opts = new Icon.Options();
        opts.setImg(getVideoStream(coordinateStreams.get(location.getCoordinate())));
        var marker = new MarkerFeature(location.getCoordinate(), new Icon(opts));
        featureLocationMap.put(marker, location);
        return marker;
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
