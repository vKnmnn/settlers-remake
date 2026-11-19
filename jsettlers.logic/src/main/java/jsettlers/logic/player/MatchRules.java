package jsettlers.logic.player;

import java.io.Serializable;
import java.util.Objects;

/** Immutable rules selected before a skirmish; saved with the running simulation. */
public final class MatchRules implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum Mode { MILITARY, FREEPLAY }

	public static final MatchRules DEFAULT = new MatchRules(Mode.MILITARY);

	private final Mode mode;

	public MatchRules(Mode mode) {
		this.mode = Objects.requireNonNull(mode, "mode");
	}

	public Mode getMode() { return mode; }

	@Override public boolean equals(Object other) {
		if (!(other instanceof MatchRules)) return false;
		MatchRules rules = (MatchRules) other;
		return mode == rules.mode;
	}

	@Override public int hashCode() { return mode.hashCode(); }
	@Override public String toString() { return mode.name(); }
}
