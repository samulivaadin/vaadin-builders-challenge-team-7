package org.vaadin.builderchallenge.data.pseudoentity.townhall;

import com.vaadin.collaborationengine.UserInfo;
import lombok.Setter;
import org.vaadin.builderchallenge.data.entity.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
@Setter
public class Question {
	private static int lastId = 0;
	private final Integer id;
	private final UserInfo authorInfo;
	private final String message;
	private final Map<String, Vote> votes = new HashMap<>();

	// Jackson converter cannot instantiate instance without finding a default constructor. There we go
	public Question() {
		id = null;
		authorInfo = null;
		message = null;
	}
	public Question(UserInfo authorInfo, String message) {
		this.id = ++lastId;
		this.authorInfo = authorInfo;
		this.message = message;
	}

	public void setVote(UserInfo originator, Vote value) {
		if (value != null) {
			votes.put(originator.getId(), value);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Question question = (Question) o;
		return id.equals(question.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public int getScore() {
		return votes.values().stream().map(value -> value.getValue().getIntVal()).reduce(Integer::sum).orElse(0);
	}

	void setScore(int score) {
		// didn't find how to configure jackson's ObjectMapper to ignore 'score'
	}

	public void removeVote(Vote vote) {
		votes.remove(vote.getOriginatorName());
	}
}
