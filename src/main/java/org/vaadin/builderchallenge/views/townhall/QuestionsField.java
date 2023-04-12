package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Question;

import java.util.List;

public class QuestionsField extends CustomField<List<Question>> {
	private Board board;

	@Override
	protected List<Question> generateModelValue() {
		return null;
	}

	@Override
	protected void setPresentationValue(List<Question> questions) {
		if (board != null) {
			remove(board);
		}
		board = new Board();
		questions.forEach(question -> board.addRow(createCell(question)));
		addClassName("board-column-wrapping");
		add(board);
	}

	private static Div createCell(Question question) {
		QuestionComponent questionComponent = new QuestionComponent(question);
		questionComponent.addClassNames("cell", "color");

		DragSource dragSource = DragSource.create(questionComponent);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);

		return questionComponent;
	}
}
