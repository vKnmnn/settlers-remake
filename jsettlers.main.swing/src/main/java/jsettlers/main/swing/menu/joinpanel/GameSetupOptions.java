package jsettlers.main.swing.menu.joinpanel;

import java.util.Random;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.graphics.localization.Labels;

/** Local game setup choices; no network protocol options are implied. */
final class GameSetupOptions {
	enum Teams {
		FREE_FOR_ALL, HUMANS_VS_AI, TWO_TEAMS, MANUAL;
		@Override public String toString() { return Labels.getString("team-preset-" + name()); }
	}

	static byte teamFor(Teams preset, int activeIndex, int activeCount, EPlayerType type) {
		switch (preset) {
		case FREE_FOR_ALL: return (byte) activeIndex;
		case HUMANS_VS_AI: return (byte) (type == EPlayerType.HUMAN ? 0 : 1);
		case TWO_TEAMS: return (byte) (activeIndex < (activeCount + 1) / 2 ? 0 : 1);
		default: throw new IllegalArgumentException("Manual teams must retain their slot values");
		}
	}

	static ECivilisation civilisationFor(ECivilisation selected, boolean amazonsAllowed, Random random) {
		if (amazonsAllowed || selected != null && selected != ECivilisation.AMAZON) return selected;
		if (selected == ECivilisation.AMAZON) throw new IllegalArgumentException("Amazon civilisation is disabled");
		ECivilisation[] allowed = java.util.Arrays.stream(ECivilisation.values())
				.filter(civilisation -> civilisation != ECivilisation.AMAZON).toArray(ECivilisation[]::new);
		return allowed[random.nextInt(allowed.length)];
	}
}
