/*******************************************************************************
 * Copyright (c) 2026
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
package jsettlers.graphics.map.controls.original.panel.content.settlers.production;

import go.graphics.text.EFontSize;
import go.graphics.GLDrawContext;
import java.util.Optional;
import java.text.NumberFormat;
import jsettlers.common.action.Action;
import jsettlers.common.player.ProductionHistory;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IProductionStatistic;
import jsettlers.graphics.action.ActionFireable;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.controls.original.panel.content.AbstractContentProvider;
import jsettlers.graphics.map.controls.original.panel.content.ESecondaryTabType;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiContentUpdater.IUiContentReceiver;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiPlayerDependingContentUpdater;
import jsettlers.graphics.ui.Button;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.graphics.ui.layout.ProductionStatisticLayout;

/**
 * Production totals with a locally selected material and simulation-time history.
 */
public class ProductionPanel extends AbstractContentProvider {

	/**
	 * This label displays the produced amount of a material.
	 */
	public static class ProducedCount extends Label implements IUiContentReceiver<IProductionStatistic> {

		private final EMaterialType material;
		private boolean plural;

		public ProducedCount(EMaterialType material) {
			super("", EFontSize.NORMAL);
			this.material = material;
		}

		@Override
		public String getDescription(float relativeX, float relativeY) {
			return Labels.getName(material, plural);
		}

		@Override
		public void update(IProductionStatistic productionStatistic) {
			int amount = productionStatistic != null ? productionStatistic.getAmountProduced(material) : 0;
			plural = amount != 1;
			setText(amount + "");
		}
	}

	/**
	 * This is a button that displays the icon of a material.
	 */
	public static class ProducedMaterialButton extends Button implements IUiContentReceiver<IProductionStatistic> {

		private final EMaterialType material;
		private boolean plural;

		public ProducedMaterialButton(EMaterialType material) {
			super(material.getIcon());
			this.material = material;
		}

		@Override
		public String getDescription(float relativeX, float relativeY) {
			return Labels.getName(material, plural);
		}

		@Override
		public void update(IProductionStatistic productionStatistic) {
			int amount = productionStatistic != null ? productionStatistic.getAmountProduced(material) : 0;
			plural = amount != 1;
		}
	}

	private static final int[] WINDOWS_MINUTES = {15, 30, 60};
	private final UIPanel overview;
	private final UIPanel detail = new UIPanel();
	private volatile boolean showingDetail;
	private volatile EMaterialType selectedMaterial = EMaterialType.PLANK;
	private volatile int windowMinutes = WINDOWS_MINUTES[0];
	private volatile IProductionStatistic statistic;
	private final ProductionGraph graph = new ProductionGraph();
	private final Label materialName = new Label("", EFontSize.NORMAL);
	private final Label total = new Label("", EFontSize.SMALL);
	private final Label interval = new Label("", EFontSize.SMALL);
	private final Label notice = new Label("", EFontSize.SMALL);
	private final UIPanel panel = new UIPanel() {
		private UIPanel current() { return showingDetail ? detail : overview; }
		@Override public void drawAt(GLDrawContext gl) {
			UIPanel current = current();
			current.setPosition(getPosition());
			current.drawAt(gl);
		}
		@Override public Optional<Action> getAction(float x, float y) { return current().getAction(x, y); }
		@Override public String getDescription(float x, float y) { return current().getDescription(x, y); }
		@Override public void onAttach() { overview.onAttach(); detail.onAttach(); }
		@Override public void onDetach() { overview.onDetach(); detail.onDetach(); }
	};
	private final UiPlayerDependingContentUpdater<IProductionStatistic> uiContentUpdater = new UiPlayerDependingContentUpdater<>(
			IInGamePlayer::getProductionStatistic);

	public ProductionPanel() {
		overview = new ProductionStatisticLayout()._root;

		// noinspection unchecked
		overview.getChildren().stream()
				.filter(c -> c instanceof IUiContentReceiver)
				.map(c -> (IUiContentReceiver<IProductionStatistic>) c)
				.forEach(uiContentUpdater::addListener);
		for (var child : overview.getChildren()) {
			if (child instanceof ProducedMaterialButton) {
				ProducedMaterialButton button = (ProducedMaterialButton) child;
				button.setAction(localAction(() -> {
					selectedMaterial = button.material;
					showingDetail = true;
					refreshDetail();
				}));
			}
		}
		detail.addChild(materialName, 0, .90f, 1, 1);
		detail.addChild(total, 0, .83f, 1, .90f);
		detail.addChild(interval, 0, .72f, 1, .83f);
		detail.addChild(graph, 0, .29f, 1, .71f);
		detail.addChild(notice, 0, .16f, 1, .28f);
		for (int i = 0; i < WINDOWS_MINUTES.length; i++) {
			int minutes = WINDOWS_MINUTES[i];
			LabeledButton button = new LabeledButton(minutes + " min", localAction(() -> {
				windowMinutes = minutes;
				refreshDetail();
			}), EFontSize.SMALL) {
				@Override public void drawAt(GLDrawContext gl) {
					setActive(windowMinutes == minutes);
					super.drawAt(gl);
				}
			};
			detail.addChild(button, i / 3f, .08f, (i + 1) / 3f, .16f);
		}
		detail.addChild(new LabeledButton(Labels.getString("production_history_overview"),
				localAction(() -> showingDetail = false), EFontSize.SMALL), 0, 0, 1, .08f);
		uiContentUpdater.addListener(value -> { statistic = value; refreshDetail(); });
	}

	public void setPlayer(IInGamePlayer player) {
		statistic = player == null ? null : player.getProductionStatistic();
		uiContentUpdater.updatePlayer(player);
		refreshDetail();
	}

	private static ExecutableAction localAction(Runnable action) {
		return new ExecutableAction() { @Override public void execute() { action.run(); } };
	}

	private synchronized void refreshDetail() {
		EMaterialType material = selectedMaterial;
		IProductionStatistic current = statistic;
		materialName.setText(Labels.getName(material, true));
		total.setText(Labels.getString("production_history_total") + ": " +
				(current == null ? 0 : current.getAmountProduced(material)));
		ProductionHistory history = current == null ? null : current.getHistory(material, windowMinutes);
		graph.setHistory(history, windowMinutes);
		interval.setText(history == null ? "" : Labels.getString("production_history_interval",
				NumberFormat.getNumberInstance().format(history.getBucketMillis() / 1000d)));
		notice.setText(Labels.getString("production_history_partial"));
	}

	@Override
	public ESecondaryTabType getTabs() {
		return ESecondaryTabType.SETTLERS;
	}

	@Override
	public UIPanel getPanel() {
		return panel;
	}

	@Override
	public void contentShowing(ActionFireable actionFireable) {
		uiContentUpdater.start();
	}

	@Override
	public void contentHiding(ActionFireable actionFireable, AbstractContentProvider nextContent) {
		uiContentUpdater.stop();
	}
}
