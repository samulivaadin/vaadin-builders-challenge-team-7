package org.vaadin.builderchallenge.views.townhall;

import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Question;
import org.vaadin.builderchallenge.data.pseudoentity.townhall.Vote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class QuestionsField extends CustomField<List<Question>> {
	private final UserInfo authUser;
	private final BiConsumer<Question, Vote> voteRemover;
	private Board board;
	private Map<Integer, Row> rowMap = new HashMap<>();

	public QuestionsField(UserInfo authUser, BiConsumer<Question, Vote> voteRemover) {
		this.authUser = authUser;
		this.voteRemover = voteRemover;
	}

	@Override
	protected List<Question> generateModelValue() {
		return null;
	}

	@Override
	protected void setPresentationValue(List<Question> questions) {
		if (board != null) {
			remove(board);
		}
		rowMap = new HashMap<>();
		board = new Board();
		questions.forEach(question -> {
			assignToRow(question);
		});
		addClassName("board-column-wrapping");
		add(board);
	}

	private QuestionComponent createCell(Question question) {
		QuestionComponent questionComponent = new QuestionComponent(question, authUser, voteRemover);
		questionComponent.addClassNames("cell", "color");

		DragSource dragSource = DragSource.create(questionComponent.getDragComponent());
		dragSource.setEffectAllowed(EffectAllowed.MOVE);

		return questionComponent;
	}

	private void assignToRow(Question question) {
		int score = question.getScore();

		QuestionComponent cell = createCell(question);
		Row row = rowMap.get(score);
		if (row == null) {
			Row newRow = board.addRow(cell);
			if (score > 0) {
				rowMap.put(score, newRow);
			}
		} else {
			row.add(cell);
		}
		Component forBorder = cell.getDragComponent();
		String className = null;

		if (score > 0) {
			switch (rowMap.size()) {
				case 1:
					className = "gold-voted";
					break;
				case 2:
					className = "silver-voted";
					break;
				case 3:
					className = "bronce-voted";
					break;
				default:
					return;
			}
			if (StringUtils.isNotEmpty(className)) {
				forBorder.getClassNames().remove("rcorners");
				forBorder.addClassName(className);
			}
		}
	}

}
