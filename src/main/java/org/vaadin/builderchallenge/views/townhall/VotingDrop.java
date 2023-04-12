package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Question;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Vote;

import java.util.Arrays;
import java.util.function.BiConsumer;

public class VotingDrop extends HorizontalLayout {
	private final BiConsumer<Question, Vote.Value> voteConsumer;

	public VotingDrop(BiConsumer<Question, Vote.Value> voteConsumer) {
		this.voteConsumer = voteConsumer;
		Arrays.stream(Vote.Value.values()).forEach(value -> {
			Component component = new VoteComponent(value);
			DropTarget dropTarget = DropTarget.configure(component, true);
			dropTarget.setDropEffect(DropEffect.MOVE);
			dropTarget.addDropListener(this::handleDrop);
			add(component);
		});
	}

	private void handleDrop(ComponentEvent<?> event) {
		if (event instanceof DropEvent<?>) {
			DropEvent dropEvent = (DropEvent) event;

			dropEvent.getDragSourceComponent()
					.filter(component -> component instanceof QuestionComponent)
					.map(component -> ((QuestionComponent) component).getQuestion())
					.ifPresent(o -> voteConsumer.accept((Question)o, ((VoteComponent)event.getSource()).getValue()));
		}
	}
}
