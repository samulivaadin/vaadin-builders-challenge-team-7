package org.vaadin.builderchallenge.views.remoteparticipation;

import com.vaadin.flow.component.map.configuration.Coordinate;

public enum Location {

    HQ(new Coordinate(22.29985, 60.45234)),
    GERMANY_OFFICE(new Coordinate(13.45489, 52.51390)),
    US_OFFICE(new Coordinate(-121.92163, 37.36821));

    private final Coordinate coordinate;

    Location(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }
}
