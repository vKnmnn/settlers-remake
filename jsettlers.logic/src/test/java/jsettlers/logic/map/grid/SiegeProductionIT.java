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
package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jsettlers.common.CommonConstants;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESiegeWeaponType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.buildings.workers.SiegeWorkshopBuilding;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.military.SiegeWeaponMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.main.replay.PausedGameStartup;
import jsettlers.logic.movable.civilian.BuildingWorkerMovable;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;

/**
 * Exercises worker movement, input handling and production for every civilisation in a running game.
 */
@org.junit.runner.RunWith(org.junit.runners.Parameterized.class)
public class SiegeProductionIT {
	@org.junit.runners.Parameterized.Parameters(name = "{0}")
	public static Object[] weapons() {
		return ESiegeWeaponType.VALUES;
	}

	private final ESiegeWeaponType weaponType;

	public SiegeProductionIT(ESiegeWeaponType weaponType) {
		this.weaponType = weaponType;
	}
	private static final byte PLAYER = 0;
	private static final byte ENEMY = 1;
	private static final int STEP_MS = 200;

	static {
		CommonConstants.ENABLE_CONSOLE_LOGGING = true;
		Constants.FOG_OF_WAR_DEFAULT_ENABLED = false;

		TestUtils.setupTempResourceManager();
	}

	private IStartedGame startedGame;
	private MainGrid grid;
	private Player player;
	private Player enemy;
	private int time;

	@Before
	public void startGame() throws Exception {
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] playerSettings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		playerSettings[PLAYER] = new PlayerSetting(EPlayerType.HUMAN, weaponType.civilisation, PLAYER);
		playerSettings[ENEMY] = new PlayerSetting(EPlayerType.HUMAN, ECivilisation.EGYPTIAN, ENEMY);

		JSettlersGame.GameRunner game = AiTestUtils.createStartingGame(playerSettings, map);
		startedGame = PausedGameStartup.await(game);

		MatchConstants.clock().setPausing(true); // the test thread executes the game
		time = 1000;
		MatchConstants.clock().fastForwardTo(time);

		grid = game.getMainGrid();
		player = grid.partitionsGrid.getPlayer(PLAYER);
		enemy = grid.partitionsGrid.getPlayer(ENEMY);
	}

	@After
	public void stopGame() {
		ReplayUtils.awaitShutdown(startedGame);
	}

	@Test
	public void workshopWorkerProducesAmmunitionAndTheOrderedWeapon() {
		ShortPoint2D position = findConstructionPosition(weaponType.workshop, PLAYER);
		SiegeWorkshopBuilding workshop = (SiegeWorkshopBuilding) grid.constructBuildingAt(position, weaponType.workshop, player, true);
		Movable.createMovable(EMovableType.SIEGE_ENGINEER, player, findFreePosition(workshop.getDoor(), 1), grid.movablePathfinderGrid);

		if (weaponType.usesAmmo()) {
			for (EMaterialType material : weaponType.getAmmoInputs()) {
				supply(workshop, material, weaponType.getAmmoBatchInputCount(material));
			}
			assertTrue(weaponType + " worker failed to produce ammunition",
					runUntil(() -> workshop.getAmmoStackSize() > 0, 3 * 60 * 1000));
			assertTrue(workshop.getAmmoStackSize() <= ESiegeWeaponType.AMMO_STACK_CAPACITY);
		}
		assertTrue("Weapons must only be built on order", getWeapons(weaponType.movableType).isEmpty());

		workshop.orderSiegeWeapon();
		assertEquals(1, workshop.getOrderedSiegeWeapons());
		for (EMaterialType material : weaponType.getWeaponCostMaterials()) {
			// Allow for ammunition already using the same stacks before the order was placed.
			supply(workshop, material, Math.max(0, weaponType.getWeaponCost(material) - inputAmount(workshop, material)));
		}
		assertTrue(weaponType + " worker failed to assemble the ordered weapon",
				runUntil(() -> !getWeapons(weaponType.movableType).isEmpty(), 5 * 60 * 1000));
		assertEquals(0, workshop.getOrderedSiegeWeapons());
		assertEquals(1, getWeapons(weaponType.movableType).size());
		SiegeWeaponMovable weapon = getWeapons(weaponType.movableType).get(0);
		assertEquals(player, weapon.getPlayer());
		assertEquals(weapon, grid.movableGrid.getMovableAt(weapon.getPosition().x, weapon.getPosition().y));
		if (weaponType.usesAmmo()) {
			assertTrue(weaponType + " failed to load its workshop ammunition", runUntil(() -> weapon.getAmmo() > 0, 60 * 1000));
		}
		Movable worker = MovableManager.getAllMovables().stream()
				.filter(m -> m instanceof BuildingWorkerMovable
						&& ((BuildingWorkerMovable) m).getGarrisonedBuilding() == workshop)
				.map(m -> (Movable) m).findFirst().orElseThrow(() -> new AssertionError("No assigned engineer"));
		assertTrue("Engineer must survive assembling the weapon", worker.isAlive());
		runFor(60 * 1000);
		assertTrue("Engineer must survive the following idle/work cycle", worker.isAlive());
		assertEquals("Finishing one order must not create another weapon", 1, getWeapons(weaponType.movableType).size());
	}

	@Test
	public void powderWorkerConsumesCoalAndSulfurToProduceGunPowder() {
		org.junit.Assume.assumeTrue(weaponType.civilisation == ECivilisation.ASIAN);
		Building powderHouse = grid.constructBuildingAt(findConstructionPosition(EBuildingType.POWDER_MAKER, PLAYER),
				EBuildingType.POWDER_MAKER, player, true);
		Movable.createMovable(EMovableType.POWDER_MAKER, player, findFreePosition(powderHouse.getDoor(), 1), grid.movablePathfinderGrid);
		int before = player.getProductionStatistic().getAmountProduced(EMaterialType.GUN_POWDER);
		supply(powderHouse, EMaterialType.COAL, 2);
		supply(powderHouse, EMaterialType.SULFUR, 2);
		assertTrue("Powder maker did not finish two real worker cycles", runUntil(
				() -> player.getProductionStatistic().getAmountProduced(EMaterialType.GUN_POWDER) >= before + 2, 5 * 60 * 1000));
		assertEquals("Sulfur must be consumed by production", 0, inputAmount(powderHouse, EMaterialType.SULFUR));
		assertTrue("The output must be materialized on a stack or carried by a settler", hasPowderOnGrid());
	}

	private boolean hasPowderOnGrid() {
		for (short y = 0; y < grid.height; y++) {
			for (short x = 0; x < grid.width; x++) {
				if (grid.mapObjectsManager.getStackSize(x, y, EMaterialType.GUN_POWDER) > 0) return true;
			}
		}
		return MovableManager.getAllMovables().stream().anyMatch(m -> m.getPlayer() == player && m.getMaterial() == EMaterialType.GUN_POWDER);
	}

	private int inputAmount(Building building, EMaterialType material) {
		return java.util.Arrays.stream(building.getBuildingVariant().getRequestStacks())
				.filter(stack -> stack.getMaterialType() == material)
				.mapToInt(stack -> {
					ShortPoint2D pos = stack.calculatePoint(building.getPosition());
					return grid.mapObjectsManager.getStackSize(pos.x, pos.y, material);
				}).sum();
	}

	private List<SiegeWeaponMovable> getWeapons(EMovableType type) {
		return MovableManager.getAllMovables().stream()
				.filter(movable -> movable.isAlive() && movable.getPlayer() == player && movable.getMovableType() == type)
				.map(movable -> (SiegeWeaponMovable) movable)
				.collect(Collectors.toList());
	}

	private void supply(Building workshop, EMaterialType material, int count) {
		for (RelativeStack stack : workshop.getBuildingVariant().getRequestStacks()) {
			if (stack.getMaterialType() == material) {
				for (int i = 0; i < count; i++) {
					assertTrue("Cannot supply " + material, grid.movablePathfinderGrid.dropMaterial(
							stack.calculatePoint(workshop.getPosition()), material, false, false));
				}
				return;
			}
		}
		throw new AssertionError("no stack for " + material);
	}

	private ShortPoint2D findConstructionPosition(EBuildingType type, byte playerId) {
		ShortPoint2D center = Building.getAllBuildings().stream()
				.filter(building -> building.getPlayer().getPlayerId() == playerId)
				.findFirst().get().getPosition();

		Optional<ShortPoint2D> position = HexGridArea.stream(center.x, center.y, 8, 40)
				.filterBounds(grid.width, grid.height)
				.filter((x, y) -> grid.constructionMarksGrid.canConstructAt(x, y, type, playerId))
				.getFirst();
		assertTrue("no construction position for " + type, position.isPresent());
		return position.get();
	}

	private ShortPoint2D findFreePosition(ShortPoint2D center, int minRadius) {
		Optional<ShortPoint2D> position = HexGridArea.stream(center.x, center.y, minRadius, minRadius + 10)
				.filterBounds(grid.width, grid.height)
				.filter((x, y) -> grid.movablePathfinderGrid.isFreePosition(x, y) && !grid.movablePathfinderGrid.isWater(x, y)
						&& !grid.flagsGrid.isProtected(x, y))
				.getFirst();
		assertNotNull(position.orElse(null));
		return position.get();
	}

	private boolean runUntil(BooleanSupplier condition, int timeoutMs) {
		for (int passed = 0; passed < timeoutMs; passed += STEP_MS) {
			runFor(STEP_MS);
			if (condition.getAsBoolean()) {
				return true;
			}
		}
		return false;
	}

	private void runFor(int ms) {
		time += ms;
		MatchConstants.clock().fastForwardTo(time);
	}
}
