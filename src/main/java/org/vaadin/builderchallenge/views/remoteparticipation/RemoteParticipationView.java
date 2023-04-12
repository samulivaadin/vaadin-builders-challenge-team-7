package org.vaadin.builderchallenge.views.remoteparticipation;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.map.configuration.style.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.vaadin.builderchallenge.data.entity.User;
import org.vaadin.builderchallenge.security.AuthenticatedUser;
import org.vaadin.builderchallenge.views.MainLayout;

import java.io.ByteArrayInputStream;

@PageTitle("Remote Participation")
@Route(value = "remote", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@AnonymousAllowed
public class RemoteParticipationView extends VerticalLayout {

    private final AuthenticatedUser authenticatedUser;

    public RemoteParticipationView(
            AuthenticatedUser authenticatedUser
    ) {
        this.authenticatedUser = authenticatedUser;
        setSpacing(false);

        var auth = this.authenticatedUser.get();
        var localUser = new UserInfo(
                auth.map(User::getUsername).orElse("n/a"),
                auth.map(User::getName).orElse("?")
        );

        var avatarGroup = new CollaborationAvatarGroup(localUser, "map");
        add(avatarGroup);

        var binder = new CollaborationBinder<>(Bean.class, localUser);

        var map = new Map();
        var vaadinHqCoordinates = new Coordinate(22.29985, 60.45234);
        var opts = new Icon.Options();
        if (auth.isPresent()) {
            opts.setImg(new StreamResource("profile-pic",
                    () -> new ByteArrayInputStream(auth
                            .map(User::getProfilePicture)
                            .orElse(new byte[0])
                    )
            ));
        } else {
            opts.setSrc("https://website.vaadin.com/hubfs/1.%20Website%20images/Home/ux-meeting.jpg");
        }

        var marker = new MarkerFeature(vaadinHqCoordinates, new Icon(opts));
        map.getFeatureLayer().addFeature(marker);
        add(map);

        var tfield = new TextField();
        binder.forField(tfield).bind("value");
        binder.setTopic("tfield", Bean::new);
        add(tfield);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");
    }

    public static class Bean {
        String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
