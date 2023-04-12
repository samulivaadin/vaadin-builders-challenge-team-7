package org.vaadin.builderchallenge.data.pseudoentity.townhall;

import lombok.Getter;
import org.vaadin.builderchallenge.data.entity.User;

@Getter
public class Vote {
	private final String originatorName;
	private final Value value;

	public enum Value {
		ONE, TWO, THREE;

		public int getIntVal() {
			return  ordinal() + 1;
		}
	}

	// Jackson converter cannot instantiate instance without finding a default constructor. There we go
	public Vote() {
		this.originatorName = null;
		this.value = null;
	}

	public Vote(String originatorName, Value value) {
		this.originatorName = originatorName;
		this.value = value;
	}
}
