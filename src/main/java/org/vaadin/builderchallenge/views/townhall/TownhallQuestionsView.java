package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.collaborationengine.*;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;
import lombok.Data;
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
//		setSpacing(false);

		var auth = this.authenticatedUser.get();
		var localUser = new UserInfo(
				auth.map(User::getUsername).orElse("n/a"),
				auth.map(User::getName).orElse("?")
		);

		var avatarGroup = new CollaborationAvatarGroup(localUser, "map");
		add(avatarGroup);

		QuestionsField questionsField = new QuestionsField();
		questionsField.setSizeFull();

		CollaborationEngine.getInstance().openTopicConnection(this, "townhall",
				localUser, connection -> {
					questions = connection.getNamedList("questions");
					questions.subscribe(event ->
							questionsField.setPresentationValue(questions.getItems(Question.class)));
					return null;
				});

		add(new Label("Vote by dropping a question:"));
		VotingDrop votingDrop = new VotingDrop((question, value) -> {
			question.setVote(auth.get().getName(), value);
			reorderList(question);
		});

		var tfield = new TextField();
		tfield.addValueChangeListener(event -> {
			Question newQuestion = new Question(auth.get().getName(), event.getValue());
			ListOperationResult<Void> result = questions.insertLast(newQuestion);
			questionMap.put(newQuestion, result.getKey());
			result.getCompletableFuture().thenAccept(v ->
					questionsField.setPresentationValue(questions.getItems(Question.class))
			);
		});

		add(votingDrop, questionsField, tfield);

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

//		List<Question> list = questions.getItems(Question.class);
//		list.stream().filter(question -> question.getScore() < newScore)
//				.map(question -> questionMap.get(question))
//				.findFirst().ifPresent(listKey -> {
//					questions.insertBefore(listKey, trigger).getCompletableFuture().thenAccept(aBoolean -> questions.remove(questionMap.get(trigger)));
////					questions.remove(questionMap.get(trigger));
////					CompletableFuture<Boolean> result = questions.remove(questionMap.get(trigger));
////					result.thenAccept(aBoolean -> questions.insertBefore(listKey, trigger));
//				});

	}
	@Data
	public static class Questions {
		private List<Question> list = new ArrayList<>();
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
