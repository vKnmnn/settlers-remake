/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.graphics.map.controls.original.panel.selection;

import java.util.HashMap;
import java.util.Arrays;
import go.graphics.GLDrawContext;
import go.graphics.text.EFontSize;
import jsettlers.common.movable.IGraphicsSiegeWeapon;
import jsettlers.common.movable.ESiegeWeaponType;
import jsettlers.graphics.ui.Label;
import java.util.Iterator;
import java.util.Map;

import jsettlers.common.action.EActionType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.selectable.ISelectionSet;
import jsettlers.common.action.Action;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIPanel;

public class SoldierSelectionContent extends AbstractSelectionContent {

	private static final EMovableType[] soldiertypes = new EMovableType[] {
			EMovableType.SWORDSMAN_L1,
			EMovableType.SWORDSMAN_L2,
			EMovableType.SWORDSMAN_L3,
			EMovableType.PIKEMAN_L1,
			EMovableType.PIKEMAN_L2,
			EMovableType.PIKEMAN_L3,
			EMovableType.BOWMAN_L1,
			EMovableType.BOWMAN_L2,
			EMovableType.BOWMAN_L3,
			EMovableType.CATAPULT,
			EMovableType.BALLISTA,
			EMovableType.CANNON,
			EMovableType.GONG,
	};

	/**
	 * Rows of selectables
	 */
	public static int ROWS = 10;

	private final UIPanel panel;

	public SoldierSelectionContent(ISelectionSet selection) {
		panel = new UIPanel();

		addRowsToPanel(panel, selection, soldiertypes);
		if (selection.getSize() == 1 && selection.get(0) instanceof IGraphicsSiegeWeapon) {
			IGraphicsSiegeWeapon weapon = (IGraphicsSiegeWeapon) selection.get(0);
			ESiegeWeaponType type = ESiegeWeaponType.fromMovableType(weapon.getMovableType());
			if (type != null && type.usesAmmo()) {
				panel.addChild(new Label("", EFontSize.NORMAL) {
					private int previousAmmo = -1;
					@Override public void drawAt(GLDrawContext gl) {
						int ammo = weapon.getAmmo();
						if (ammo != previousAmmo) {
							setText(String.format(Labels.getString("siege_weapon_ammo"), ammo, type.maxAmmo));
							previousAmmo = ammo;
						}
						super.drawAt(gl);
					}
				}, .1f, .65f, .9f, .75f);
			}
		}

		UIPanel kill = new LabeledButton(Labels.getString("kill"), new Action(EActionType.DESTROY));
		UIPanel stop = new LabeledButton(Labels.getString("stop"), new Action(EActionType.STOP_WORKING));

		panel.addChild(kill, .1f, .1f, .5f, .2f);
		panel.addChild(stop, .5f, .1f, .9f, .2f);
	}

	public static void addRowsToPanel(UIPanel panel, ISelectionSet selection, EMovableType[] types) {
		long visibleRows = Arrays.stream(types).filter(type -> selection.getMovableCount(type, new HashMap<>()) > 0).count();
		float rowHeight = Math.min(1f / ROWS, .7f / Math.max(1, visibleRows));

		int rowi = 0; // top down, leaving the action buttons free
		for (int i = 0; i < types.length; i++) {
			EMovableType type = types[i];
			Map<IPlayer, Integer> playerStatistic = new HashMap<>();
			int count = selection.getMovableCount(type, playerStatistic);

			Map.Entry<IPlayer, Integer> bestEntry = null;
			Iterator<Map.Entry<IPlayer, Integer>> iter = playerStatistic.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry<IPlayer, Integer> currEntry = iter.next();

				if(bestEntry == null || bestEntry.getValue() < currEntry.getValue()) bestEntry = currEntry;
			}

			if (count > 0) {
				SelectionRow row = new SelectionRow(bestEntry.getKey(), type, count);
				panel.addChild(row, 0.1f, .95f - rowHeight * (rowi + 1), .9f,
						.95f - rowHeight * rowi);
				rowi++;
			}
		}

	}

	@Override
	public UIPanel getPanel() {
		return panel;
	}

}
