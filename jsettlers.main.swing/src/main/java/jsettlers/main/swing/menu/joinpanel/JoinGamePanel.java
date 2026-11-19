/*******************************************************************************
 * Copyright (c) 2015 - 2017
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.main.swing.menu.joinpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import jsettlers.common.menu.ENetworkMessage;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IChatMessageListener;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IMultiplayerPlayer;
import jsettlers.common.menu.IMultiplayerSlot;
import jsettlers.common.menu.IStartingGame;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.graphics.localization.Labels;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.MatchRules;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.JSettlersSwingUtil;
import jsettlers.main.swing.lookandfeel.ELFStyle;
import jsettlers.main.swing.lookandfeel.GBC;
import jsettlers.main.swing.lookandfeel.components.BackgroundPanel;
import jsettlers.main.swing.menu.joinpanel.slots.PlayerSlot;
import jsettlers.main.swing.menu.joinpanel.slots.SlotToggleGroup;
import jsettlers.main.swing.menu.joinpanel.slots.factories.ClientOfMultiplayerPlayerSlotFactory;
import jsettlers.main.swing.menu.joinpanel.slots.factories.HostOfMultiplayerPlayerSlotFactory;
import jsettlers.main.swing.menu.joinpanel.slots.factories.IPlayerSlotFactory;
import jsettlers.main.swing.menu.joinpanel.slots.factories.SinglePlayerSlotFactory;

/**
 * Layout:
 * 
 * +---------------------------------------------------------------+
 * |              titleLabel                                       |
 * +------------------------+--------------------------------------+
 * |                        |       playerSlotsPanel               |
 * |                        +--------------------------------------+
 * | westPanel              |       chatPanel                      |
 * |                        +--------------------------------------+
 * |                        |       southPanel                     |
 * +------------------------+--------------------------------------+
 * 
 * @author codingberlin
 */
public class JoinGamePanel extends BackgroundPanel {
	private static final long serialVersionUID = -1186791399814385303L;

	private final JSettlersFrame settlersFrame;
	private final JLabel titleLabel = new JLabel();
	private final JPanel westPanel = new JPanel();
	private final JPanel mapPanel = new JPanel();
	private final JPanel settingsPanel = new JPanel();
	private final JLabel mapNameLabel = new JLabel();
	private final JLabel mapImage = new JLabel();
	private final JLabel numberOfPlayersLabel = new JLabel();
	private final NumberOfPlayersComboBox numberOfPlayersComboBox = new NumberOfPlayersComboBox(this);
	private final JLabel teamPresetLabel = new JLabel();
	private final JComboBox<GameSetupOptions.Teams> teamPresetComboBox = new JComboBox<>(GameSetupOptions.Teams.values());
	private final JLabel amazonsLabel = new JLabel();
	private final JComboBox<AmazonOption> amazonsAllowed = new JComboBox<>(AmazonOption.values());
	private final JLabel gameModeLabel = new JLabel();
	private final JComboBox<GameModeOption> gameModeComboBox = new JComboBox<>();
	private boolean singlePlayer;
	private boolean applyingTeamPreset;
	private final JLabel startResourcesLabel = new JLabel();
	private final JComboBox<MapStartResourcesUIWrapper> startResourcesComboBox = new JComboBox<>();
	private final JPanel playerSlotsPanel = new JPanel();
	private final JButton cancelButton = new JButton();
	private final JButton startGameButton = new JButton();
	private final JLabel slotsHeadlinePlayerNameLabel = new JLabel();
	private final JLabel slotsHeadlineCivilisation = new JLabel();
	private final JLabel slotsHeadlineType = new JLabel();
	private final JLabel slotsHeadlineMapSlot = new JLabel();
	private final JLabel slotsHeadlineTeam = new JLabel();
	private final JTextField chatInputField = new JTextField();
	private final JTextArea chatArea = new JTextArea();
	private final JPanel chatPanel = new JPanel();
	private final JButton sendChatMessageButton = new JButton();
	private MapLoader mapLoader;
	private final List<PlayerSlot> playerSlots = new ArrayList<>();
	private IPlayerSlotFactory playerSlotFactory;

	public JoinGamePanel(JSettlersFrame settlersFrame) {
		this.settlersFrame = settlersFrame;
		createStructure();
		setStyle();
		localize();
	}

	private void createStructure() {
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		westPanel.setLayout(new BorderLayout());
		westPanel.add(mapPanel, BorderLayout.NORTH);
		JPanel settingsPanelWrapper = new JPanel();
		westPanel.add(settingsPanelWrapper, BorderLayout.CENTER);
		settingsPanelWrapper.add(settingsPanel);
		settingsPanel.setLayout(new GridBagLayout());
		mapPanel.setLayout(new BorderLayout());
		JPanel mapNameLabelWrapper = new JPanel();
		mapPanel.add(mapNameLabelWrapper, BorderLayout.NORTH);
		mapNameLabelWrapper.add(mapNameLabel);
		mapNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		mapPanel.add(mapImage, BorderLayout.CENTER);
		mapImage.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		addSettingRow(0, numberOfPlayersLabel, numberOfPlayersComboBox);
		addSettingRow(1, startResourcesLabel, startResourcesComboBox);
		addSettingRow(2, teamPresetLabel, teamPresetComboBox);
		addSettingRow(3, gameModeLabel, gameModeComboBox);
		addSettingRow(4, amazonsLabel, amazonsAllowed);
		teamPresetComboBox.addActionListener(event -> applyTeamPreset());
		amazonsAllowed.setSelectedItem(AmazonOption.YES);
		amazonsAllowed.addActionListener(event -> playerSlots.forEach(slot -> slot.setAmazonsAllowed(amazonsAllowed.getSelectedItem() == AmazonOption.YES)));
		sendChatMessageButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));
		chatPanel.setLayout(new BorderLayout(0, 10));
		JPanel chatInputPanel = new JPanel();
		chatInputPanel.setLayout(new BorderLayout(10, 0));
		chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
		chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
		chatInputPanel.add(chatInputField, BorderLayout.CENTER);
		chatInputPanel.add(sendChatMessageButton, BorderLayout.EAST);
		
		playerSlotsPanel.setLayout(new GridBagLayout());
		playerSlotsPanel.setBorder(new EmptyBorder(20, 25, 20, 20));
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
		cancelButton.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 15));
		southPanel.add(cancelButton);
		startGameButton.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 15));
		southPanel.add(startGameButton);
		
		JPanel content = new JPanel(new GridBagLayout());
		content.add(titleLabel, new GBC().grid(0, 0).size(2, 1).fillx().insets(0, 0, 30, 0));
		content.add(westPanel, new GBC().grid(0, 1).size(1, 3).filly());
		content.add(new JScrollPane(playerSlotsPanel), new GBC().grid(1, 1).fillx().filly());
		content.add(chatPanel, new GBC().grid(1, 2).fillx().filly().insets(30, 0, 0, 0));
		content.add(southPanel, new GBC().grid(1, 3).fillx().insets(30, 0, 0, 0));
		add(content);
	}

	private void addSettingRow(int row, JLabel label, JComponent field) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.insets = new Insets(6, 0, 6, 16);
		settingsPanel.add(label, constraints);
		constraints.gridx = 1;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(6, 0, 6, 0);
		settingsPanel.add(field, constraints);
	}

	private void setStyle() {
		mapNameLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_LONG);
		numberOfPlayersLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_SHORT);
		startResourcesLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_SHORT);
		teamPresetLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_SHORT);
		gameModeLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_SHORT);
		amazonsLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_SHORT);
		titleLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_HEADER);
		cancelButton.putClientProperty(ELFStyle.KEY, ELFStyle.BUTTON_MENU);
		startGameButton.putClientProperty(ELFStyle.KEY, ELFStyle.BUTTON_MENU);
		slotsHeadlinePlayerNameLabel.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_DYNAMIC);
		slotsHeadlineCivilisation.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_DYNAMIC);
		slotsHeadlineType.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_DYNAMIC);
		slotsHeadlineMapSlot.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_DYNAMIC);
		slotsHeadlineTeam.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_DYNAMIC);
		sendChatMessageButton.putClientProperty(ELFStyle.KEY, ELFStyle.BUTTON_MENU);
		chatInputField.putClientProperty(ELFStyle.KEY, ELFStyle.TEXT_DEFAULT);
		chatArea.putClientProperty(ELFStyle.KEY, ELFStyle.PANEL_DARK);
		startResourcesComboBox.putClientProperty(ELFStyle.KEY, ELFStyle.COMBOBOX);
		teamPresetComboBox.putClientProperty(ELFStyle.KEY, ELFStyle.COMBOBOX);
		gameModeComboBox.putClientProperty(ELFStyle.KEY, ELFStyle.COMBOBOX);
		amazonsAllowed.putClientProperty(ELFStyle.KEY, ELFStyle.COMBOBOX);
		chatArea.putClientProperty(ELFStyle.KEY, ELFStyle.TEXT_DEFAULT);
		chatArea.setEditable(false);
		SwingUtilities.updateComponentTreeUI(this);
	}

	private void localize() {
		numberOfPlayersLabel.setText(Labels.getString("join-game-panel-number-of-players"));
		startResourcesLabel.setText(Labels.getString("join-game-panel-start-resources"));
		cancelButton.setText(Labels.getString("join-game-panel-cancel"));
		startGameButton.setText(Labels.getString("join-game-panel-start"));
		teamPresetLabel.setText(Labels.getString("join-game-panel-team-preset"));
		amazonsLabel.setText(Labels.getString("join-game-panel-amazons"));
		gameModeLabel.setText(Labels.getString("join-game-panel-mode"));
		slotsHeadlinePlayerNameLabel.setText(Labels.getString("join-game-panel-player-name"));
		slotsHeadlineCivilisation.setText(Labels.getString("join-game-panel-civilisation"));
		slotsHeadlineType.setText(Labels.getString("join-game-panel-player-type"));
		slotsHeadlineMapSlot.setText(Labels.getString("join-game-panel-map-slot"));
		slotsHeadlineTeam.setText(Labels.getString("join-game-panel-team"));
		sendChatMessageButton.setText(Labels.getString("join-game-panel-send-chat-message"));
	}

	public void setSinglePlayerMap(MapLoader mapLoader) {
		this.playerSlotFactory = new SinglePlayerSlotFactory();
		titleLabel.setText(Labels.getString("join-game-panel-new-single-player-game-title"));
		setLocalOptionsVisible(true);
		startResourcesComboBox.setEnabled(true);
		startGameButton.setVisible(true);
		setChatVisible(false);
		setStartButtonActionListener(e -> {
			applyTeamPreset();
			long randomSeed = System.currentTimeMillis();
			Random civilisationRandom = new Random(randomSeed);
			PlayerSetting[] playerSettings = playerSlots.stream()
					.sorted(Comparator.comparingInt(PlayerSlot::getSlot))
					.map(playerSlot -> {
						if (playerSlot.isAvailable()) {
							return new PlayerSetting(playerSlot.getPlayerType(),
								GameSetupOptions.civilisationFor(playerSlot.getCivilisation(), amazonsAllowed.getSelectedItem() == AmazonOption.YES, civilisationRandom),
									playerSlot.getTeam());
						} else {
							return new PlayerSetting();
						}
					})
					.toArray(PlayerSetting[]::new);

			MapStartResourcesUIWrapper selected = (MapStartResourcesUIWrapper) startResourcesComboBox.getSelectedItem();
			InitialGameState initialGameState = new InitialGameState(playerSlots.get(0).getSlot(), playerSettings, randomSeed, selected.getStartResources(),
					new MatchRules(((GameModeOption) gameModeComboBox.getSelectedItem()).mode));
			JSettlersGame game = new JSettlersGame(mapLoader, initialGameState);
			IStartingGame startingGame = game.start();
			settlersFrame.showStartingGamePanel(startingGame);
		});
		setCancelButtonActionListener(e -> settlersFrame.showMainMenu());

		prepareUiFor(mapLoader);
		numberOfPlayersComboBox.informGame(null);
	}

	public void setNewMultiPlayerMap(MapLoader mapLoader, IMultiplayerConnector connector) {
		this.playerSlotFactory = new HostOfMultiplayerPlayerSlotFactory(connector);
		titleLabel.setText(Labels.getString("join-game-panel-new-multi-player-game-title"));
		setLocalOptionsVisible(false);
		startResourcesComboBox.setEnabled(false);
		startGameButton.setVisible(true);
		setChatVisible(true);
		setStartButtonActionListener(e -> {
		});
		String myId = connector.getPlayerUUID();
		IJoiningGame joiningGame = connector.openNewMultiplayerGame(new OpenMultiPlayerGameInfo(mapLoader));
		joiningGame.setListener(new IJoiningGameListener() {
			@Override
			public void joinProgressChanged(EProgressState state, float progress) {

			}

			@Override
			public void gameJoined(IJoinPhaseMultiplayerGameConnector connector) {
				SwingUtilities.invokeLater(() -> {
					initializeChatFor(connector);
					setStartButtonActionListener(e -> connector.startGame());
					connector.getSlots().setListener(changingSlots -> onSlotsChanged(changingSlots, connector, myId, true));
					connector.setMultiplayerListener(new IMultiplayerListener() {
						@Override
						public void gameIsStarting(IStartingGame game) {
							settlersFrame.showStartingGamePanel(game);
						}

						@Override
						public void gameAborted() {
							settlersFrame.showMainMenu();
						}
					});

					onSlotsChanged(connector.getSlots(), connector, myId, true); // init the UI with the players
					numberOfPlayersComboBox.informGame(connector);
				});
			}
		});

		setCancelButtonActionListener(e -> {
			joiningGame.abort();
			settlersFrame.showMainMenu();
		});

		prepareUiFor(mapLoader);
	}

	public void setJoinMultiPlayerMap(IJoinPhaseMultiplayerGameConnector joinMultiPlayerMap, MapLoader mapLoader, String playerUUID) {
		playerSlotFactory = new ClientOfMultiplayerPlayerSlotFactory();
		titleLabel.setText(Labels.getString("join-game-panel-join-multi-player-game-title"));
		setLocalOptionsVisible(false);
		startResourcesComboBox.setEnabled(false);
		setChatVisible(true);
		startGameButton.setVisible(false);

		prepareUiFor(mapLoader);

		joinMultiPlayerMap.getSlots().setListener(changingSlots -> onSlotsChanged(changingSlots, joinMultiPlayerMap, playerUUID, false));
		joinMultiPlayerMap.setMultiplayerListener(new IMultiplayerListener() {
			@Override
			public void gameIsStarting(IStartingGame game) {
				settlersFrame.showStartingGamePanel(game);
			}

			@Override
			public void gameAborted() {
				settlersFrame.showMainMenu();
			}
		});
		initializeChatFor(joinMultiPlayerMap);

		onSlotsChanged(joinMultiPlayerMap.getSlots(), joinMultiPlayerMap, playerUUID, false); // init the UI with the players
		numberOfPlayersComboBox.informGame(null);
	}

	private void initializeChatFor(IJoinPhaseMultiplayerGameConnector joinMultiPlayerMap) {
		joinMultiPlayerMap.setChatListener(new IChatMessageListener() {
			@Override
			public void chatMessageReceived(String authorId, String message) {
				chatArea.append(authorId + ": " + message + "\n");
			}

			@Override
			public void systemMessageReceived(IMultiplayerPlayer author, ENetworkMessage message) {
				chatArea.append(Labels.getString("network-message-" + message.name()) + "\n");
			}
		});
		ActionListener sendChatMessage = e -> {
			String message = chatInputField.getText();
			if (!message.equals("")) {
				joinMultiPlayerMap.sendChatMessage(message);
				chatInputField.setText("");
			}
		};
		Arrays.stream(sendChatMessageButton.getActionListeners()).forEach(sendChatMessageButton::removeActionListener);
		Arrays.stream(chatInputField.getActionListeners()).forEach(chatInputField::removeActionListener);
		sendChatMessageButton.addActionListener(sendChatMessage);
		chatInputField.addActionListener(sendChatMessage);

	}

	private void setChatVisible(boolean isVisible) {
		chatPanel.setVisible(isVisible);
		chatArea.setVisible(isVisible);
		chatInputField.setVisible(isVisible);
		sendChatMessageButton.setVisible(isVisible);
		chatArea.setText("");
		chatInputField.setText("");
	}

	private void onSlotsChanged(ChangingList<? extends IMultiplayerSlot> changingSlots, IJoinPhaseMultiplayerGameConnector joinMultiPlayerMap, String myId, boolean iAmTheHost) {
		SwingUtilities.invokeLater(() -> {
			Iterator<? extends IMultiplayerSlot> slots = changingSlots.getItems().iterator();
			numberOfPlayersComboBox.setPlayerCount(changingSlots.getItems().size());

			for (int i = 0; slots.hasNext() && i < playerSlots.size(); i++) {
				PlayerSlot playerSlot = playerSlots.get(i);
				IMultiplayerSlot remoteSlot = slots.next();
				playerSlot.setCivilisation(remoteSlot.getCivilisation(), false);
				playerSlot.setTeam(remoteSlot.getTeam(), false);
				playerSlot.setSlot(remoteSlot.getPosition(), false);
				IMultiplayerPlayer player = remoteSlot.getPlayer();
				playerSlot.setPlayerType(remoteSlot.getType(), false);
				if(player != null) {
					playerSlot.setPlayerName(player.getName());

					if (player.getId().equals(myId)) {
						playerSlot.setReadyButtonEnabled(true);
						playerSlot.informGameAboutChanges(joinMultiPlayerMap, true, iAmTheHost);
					} else {
						playerSlot.setReadyButtonEnabled(false);
						if(iAmTheHost) {
							playerSlot.informGameAboutChanges(joinMultiPlayerMap, false, true);
						}
					}
					playerSlot.setReady(player.isReady());
				} else if(iAmTheHost) {
					playerSlot.informGameAboutChanges(joinMultiPlayerMap, false, true);
					playerSlot.setReady(true);
				}
			}
			setCancelButtonActionListener(e -> {
				joinMultiPlayerMap.abort();
				settlersFrame.showMainMenu();
			});
			numberOfPlayersComboBox.setEnabled(iAmTheHost);
		});
	}

	private void prepareUiFor(MapLoader mapLoader) {
		this.mapLoader = mapLoader;
		numberOfPlayersComboBox.informGame(null);
		mapNameLabel.setText(mapLoader.getMapName());
		mapImage.setIcon(new ImageIcon(JSettlersSwingUtil.createBufferedImageFrom(mapLoader)));
		startResourcesComboBox.removeAllItems();
		startResourcesComboBox.addItem(new MapStartResourcesUIWrapper(EMapStartResources.MAP_DEFAULT));
		Arrays.stream(EMapStartResources.values()).filter(resources -> resources != EMapStartResources.MAP_DEFAULT)
				.map(MapStartResourcesUIWrapper::new)
				.forEach(startResourcesComboBox::addItem);
		startResourcesComboBox.setSelectedIndex(0);
		teamPresetComboBox.setEnabled(false);
		teamPresetComboBox.setSelectedItem(GameSetupOptions.Teams.MANUAL);
		amazonsAllowed.setSelectedItem(AmazonOption.YES);
		gameModeComboBox.removeAllItems();
		for (MatchRules.Mode mode : MatchRules.Mode.values()) gameModeComboBox.addItem(new GameModeOption(mode));
		gameModeComboBox.setSelectedIndex(0);
		gameModeComboBox.setEnabled(singlePlayer && mapLoader.supportsCustomMatchRules());
		gameModeComboBox.setToolTipText(mapLoader.supportsCustomMatchRules() ? null : Labels.getString("join-game-panel-mode-locked"));
		buildPlayerSlots();
		numberOfPlayersComboBox.reset(mapLoader.getMaxPlayers());
		boolean lockedTeams = Arrays.stream(mapLoader.getFileHeader().getPlayerSettings()).anyMatch(setting -> setting.getTeamId() != null);
		teamPresetComboBox.setEnabled(singlePlayer && !lockedTeams);
		teamPresetComboBox.setToolTipText(lockedTeams ? Labels.getString("join-game-panel-teams-locked") : null);
		boolean lockedAmazons = Arrays.stream(mapLoader.getFileHeader().getPlayerSettings()).anyMatch(setting -> setting.getCivilisation() == ECivilisation.AMAZON);
		amazonsAllowed.setEnabled(singlePlayer && !lockedAmazons);
		amazonsAllowed.setToolTipText(lockedAmazons ? Labels.getString("join-game-panel-amazons-locked") : null);
		if (!lockedTeams) teamPresetComboBox.setSelectedItem(GameSetupOptions.Teams.FREE_FOR_ALL);
		updateNumberOfPlayerSlots(mapLoader.getMaxPlayers());
	}

	private void buildPlayerSlots() {
		int maximumNumberOfPlayers = this.mapLoader.getMaxPlayers();
		playerSlots.clear();
		for (byte i = 0; i < maximumNumberOfPlayers; i++) {
			PlayerSlot playerSlot = playerSlotFactory.createPlayerSlot(i, this.mapLoader);
			playerSlots.add(playerSlot);
		}

		PlayerSetting[] playerSettings = mapLoader.getFileHeader().getPlayerSettings();
		for (byte i = 0; i < playerSlots.size(); i++) {
			PlayerSlot playerSlot = playerSlots.get(i);
			PlayerSetting playerSetting = playerSettings[i];

			playerSlot.setSlot(i, true);

			if (playerSetting.getTeamId() != null) {
				playerSlot.setTeam(playerSetting.getTeamId(), true);
				playerSlot.disableTeamInput();
			} else {
				playerSlot.setTeam(i, true);
			}

			if (playerSetting.getCivilisation() != null) {
				playerSlot.setCivilisation(playerSetting.getCivilisation(), true);
				playerSlot.disableCivilisationInput();
			}

			if (playerSetting.getPlayerType() != null) {
				playerSlot.setPlayerType(playerSetting.getPlayerType(), true);
				playerSlot.disablePlayerTypeInput();
			}
		}
		for (PlayerSlot slot : playerSlots) {
			slot.addTeamChangeListener(() -> {
				if (singlePlayer && !applyingTeamPreset) teamPresetComboBox.setSelectedItem(GameSetupOptions.Teams.MANUAL);
			});
			slot.addPlayerTypeChangeListener(this::applyTeamPreset);
		}

	}

	private void setLocalOptionsVisible(boolean visible) {
		singlePlayer = visible;
		gameModeLabel.setVisible(visible);
		gameModeComboBox.setVisible(visible);
		teamPresetLabel.setVisible(visible);
		teamPresetComboBox.setVisible(visible);
		amazonsLabel.setVisible(visible);
		amazonsAllowed.setVisible(visible);
		startResourcesLabel.setVisible(visible);
		startResourcesComboBox.setVisible(visible);
	}

	private void applyTeamPreset() {
		if (!singlePlayer || !teamPresetComboBox.isEnabled() || applyingTeamPreset) return;
		GameSetupOptions.Teams preset = (GameSetupOptions.Teams) teamPresetComboBox.getSelectedItem();
		if (preset == null || preset == GameSetupOptions.Teams.MANUAL) return;
		applyingTeamPreset = true;
		try {
			List<PlayerSlot> active = new ArrayList<>();
			for (PlayerSlot slot : playerSlots) if (slot.isAvailable()) active.add(slot);
			for (int i = 0; i < active.size(); i++) {
				PlayerSlot slot = active.get(i);
				slot.setTeam(GameSetupOptions.teamFor(preset, i, active.size(), slot.getPlayerType()), true);
			}
		} finally { applyingTeamPreset = false; }
	}

	private void setStartButtonActionListener(ActionListener actionListener) {
		ActionListener[] actionListeners = startGameButton.getActionListeners();
		Arrays.stream(actionListeners).forEach(startGameButton::removeActionListener);
		startGameButton.addActionListener(actionListener);
	}

	private void setCancelButtonActionListener(ActionListener actionListener) {
		ActionListener[] actionListeners = cancelButton.getActionListeners();
		Arrays.stream(actionListeners).forEach(cancelButton::removeActionListener);
		cancelButton.addActionListener(actionListener);
	}

	void updateNumberOfPlayerSlots(int amount) {
		if (playerSlotFactory == null) {
			return;
		}
		playerSlotsPanel.removeAll();
		addPlayerSlotHeadline();
		for (int i = 0; i < playerSlots.size(); i++) {
			playerSlots.get(i).setAvailable(i < amount);
			if (i < amount) {
				playerSlots.get(i).addTo(playerSlotsPanel, i + 1);
			}
		}
		SwingUtilities.updateComponentTreeUI(playerSlotsPanel);
		SlotToggleGroup slotToggleGroup = new SlotToggleGroup();
		playerSlots.forEach(slotToggleGroup::add);
		applyTeamPreset();
	}

	private void addPlayerSlotHeadline() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridwidth = 4;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		playerSlotsPanel.add(slotsHeadlinePlayerNameLabel, constraints);
		constraints.gridx = 5;
		constraints.gridy = 0;
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		playerSlotsPanel.add(slotsHeadlineCivilisation, constraints);
		constraints = new GridBagConstraints();
		constraints.gridx = 7;
		constraints.gridy = 0;
		constraints.gridwidth = 4;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		playerSlotsPanel.add(slotsHeadlineType, constraints);
		constraints = new GridBagConstraints();
		constraints.gridx = 11;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		playerSlotsPanel.add(slotsHeadlineMapSlot, constraints);
		constraints = new GridBagConstraints();
		constraints.gridx = 12;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		playerSlotsPanel.add(slotsHeadlineTeam, constraints);
	}

	private enum AmazonOption {
		YES, NO;
		@Override public String toString() { return Labels.getString("setup-option-" + name()); }
	}

	private static final class GameModeOption {
		private final MatchRules.Mode mode;
		private GameModeOption(MatchRules.Mode mode) { this.mode = mode; }
		@Override public String toString() { return Labels.getString("game-mode-" + mode.name()); }
	}

}
