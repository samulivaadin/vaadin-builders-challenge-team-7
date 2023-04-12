package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Question;
import lombok.Getter;

@Getter
public class QuestionComponent extends Div {
	private final Question question;

	public QuestionComponent(Question question) {
		this.question = question;
		add(new Label(question.getMessage()));
		if (question.getScore() > 0) {
			add(new Label(String.valueOf(question.getScore())));
		}
	}
}
