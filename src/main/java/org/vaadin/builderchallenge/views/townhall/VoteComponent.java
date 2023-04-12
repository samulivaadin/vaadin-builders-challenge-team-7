package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Vote;
import org.vaadin.lineawesome.LineAwesomeIcon;

@Getter
public class VoteComponent extends HorizontalLayout {
	private final Vote value;

	public VoteComponent(Vote value) {
		this.value = value;
		for (int i = 1; i <= value.getValue().getIntVal(); i++) {
			Component star = LineAwesomeIcon.STAR.create();
			add(star);
		}
		setSizeFull();
		addClassName("droppable-border");
	}
}
