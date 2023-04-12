package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Question;
import lombok.Getter;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Vote;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class QuestionComponent extends HorizontalLayout {
	private final Question question;
	private final Component dragComponent;

	public QuestionComponent(Question question, UserInfo authUser, BiConsumer<Question, Vote> voteRemover) {
		this.question = question;
		dragComponent = new DragComponent(question);
		add(dragComponent);
		dragComponent.addClassName("rcorners");

		if (question.getScore() > 0) {
			question.getVotes().values().stream()
					.filter(vote -> Objects.equals(vote.getOriginatorName(), authUser.getId()))
					.findFirst().ifPresent(vote -> {
						Component trash = LineAwesomeIcon.TRASH_ALT.create();
						Button delete = new Button(trash, event -> voteRemover.accept(question, vote));
						delete.setTooltipText("Delete my vote");
						add(delete);
					});

			List<AvatarGroup.AvatarGroupItem> avatars = question.getVotes().keySet().stream()
					.map(entry -> new AvatarGroup.AvatarGroupItem(entry)).collect(Collectors.toList());
			AvatarGroup avatarGroup = new AvatarGroup(avatars);
			add(avatarGroup);

		}

	}

	@Getter
	public static class DragComponent extends Label implements HasQuestion {
		private final Question question;

		DragComponent(Question question) {
			super(question.getMessage());
			this.question = question;
		}
	}
}
