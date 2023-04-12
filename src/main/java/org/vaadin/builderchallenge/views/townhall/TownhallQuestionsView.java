package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.collaborationengine.*;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.builderchallenge.data.entity.User;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Question;
import org.vaadin.builderchallenge.security.AuthenticatedUser;
import org.vaadin.builderchallenge.views.MainLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@PageTitle("Townhall Questions")
@Route(value = "townhall", layout = MainLayout.class)
//@RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
public class TownhallQuestionsView extends VerticalLayout {

	private final AuthenticatedUser authenticatedUser;
	private CollaborationList questions;
	private static Map<Question, ListKey> questionMap = new ConcurrentHashMap<>();

	public TownhallQuestionsView(AuthenticatedUser authenticatedUser) {
		this.authenticatedUser = authenticatedUser;

		var auth = this.authenticatedUser.get();
		var localUser = new UserInfo(
				auth.map(User::getUsername).orElse("n/a"),
				auth.map(User::getName).orElse("?")
		);

		var avatarGroup = new CollaborationAvatarGroup(localUser, "map");
		add(avatarGroup);

		QuestionsField questionsField = new QuestionsField(localUser, (question, vote) -> {
			question.removeVote(vote);
			reorderList(question);
		});
		questionsField.setSizeFull();

		CollaborationEngine.getInstance().openTopicConnection(this, "townhall",
				localUser, connection -> {
					questions = connection.getNamedList("questions");
					questions.subscribe(event ->
							questionsField.setPresentationValue(questions.getItems(Question.class)));
					return null;
				});

		add(new Label("Vote by dropping a question:"));
		VotingDrop votingDrop = new VotingDrop(localUser, (question, value) -> {
			question.setVote(localUser, value);
			reorderList(question);
		});

		var textInput = new TextArea("Enter Your Question:");
		textInput.setWidthFull();
		textInput.addValueChangeListener(event -> {
			String message = textInput.getValue();
			if (StringUtils.isNotEmpty(message)) {
				Question newQuestion = new Question(localUser, textInput.getValue());
				ListOperationResult<Void> result = questions.insertLast(newQuestion);
				questionMap.put(newQuestion, result.getKey());
				result.getCompletableFuture().thenAccept(v ->
						questionsField.setPresentationValue(questions.getItems(Question.class))
				);
				textInput.setValue("");
			}
		});

		Button submit = new Button("Submit", event -> {
			textInput.blur();
		});
		submit.addClickShortcut(Key.ENTER);

		add(votingDrop, questionsField, textInput, submit);

		setSizeFull();
		setJustifyContentMode(JustifyContentMode.CENTER);
		setDefaultHorizontalComponentAlignment(Alignment.START);
	}

	private void reorderList(Question trigger) {
		int newScore = trigger.getScore();

		questions.remove(questionMap.get(trigger)).thenAccept(aBoolean -> {
			List<Question> list = questions.getItems(Question.class);
			Optional<ListKey> nextKey = list.stream().filter(question -> question.getScore() < newScore)
					.map(question -> questionMap.get(question))
					.findFirst();
			final ListKey newKey;
			if (nextKey.isPresent()) {
				ListOperationResult<Boolean> result = questions.insertBefore(nextKey.get(), trigger);
				newKey = result.getKey();
			} else {
				ListOperationResult<Void> result = questions.insertLast(trigger);
				newKey = result.getKey();
			}
			questionMap.put(trigger, newKey);
		});

	}

	@Data
	public static class Questions {
		private List<Question> list = new ArrayList<>();
	}

}
