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
package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESiegeWeaponType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.SiegeWorkshopBuilding;
import jsettlers.logic.buildings.workers.SiegeWorkshopBuilding.ESiegeWorkshopJob;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

/**
 * The worker of the siege workshops. He assembles ordered siege weapons at the anvil and produces ammunition otherwise.
 */
public class SiegeEngineerMovable extends BuildingWorkerMovable {
	private static final long serialVersionUID = -2270986125497352411L;


	private ESiegeWorkshopJob job = ESiegeWorkshopJob.NONE;
	private EMaterialType fetchMaterial = null;

	public SiegeEngineerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.SIEGE_ENGINEER, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.SIEGE_ENGINEER, new Root<>(createSiegeEngineerBehaviour()));
	}

	private static Node<SiegeEngineerMovable> createSiegeEngineerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					waitFor(
						sequence(
							isAllowedToWork(),
							condition(SiegeEngineerMovable::chooseJob)
						)
					),
					show(),
					ignoreFailure(
						selector(
							sequence(
								condition(mov -> mov.job == ESiegeWorkshopJob.ASSEMBLE_WEAPON),
								goToAnvil(),
								action(mov -> mov.getWorkshop().workOnWeapon())
							),
							sequence(
								condition(mov -> mov.job == ESiegeWorkshopJob.FETCH_WEAPON_MATERIAL),
								goToInputStack(mov -> mov.fetchMaterial),
								setDirectionNode(EDirection.NORTH_WEST),
								take(mov -> mov.fetchMaterial, true),
								goToAnvil(),
								crouchDown(action(mov -> {
									mov.getWorkshop().weaponMaterialTaken(mov.getMaterial());
									mov.setMaterial(EMaterialType.NO_MATERIAL);
								})),
								playAction(EMovableAction.ACTION1, ESiegeWeaponType.MATERIAL_WORK_DURATION_MS)
							),
							sequence(
								condition(mov -> mov.job == ESiegeWorkshopJob.FETCH_AMMO_MATERIAL),
								goToInputStack(mov -> mov.fetchMaterial),
								setDirectionNode(EDirection.NORTH_WEST),
								take(mov -> mov.fetchMaterial, true),
								enterHome(),
								action(mov -> {
									mov.getWorkshop().ammoMaterialTaken(mov.getMaterial());
									mov.setMaterial(EMaterialType.NO_MATERIAL);
								}),
								sleep(ESiegeWeaponType.MATERIAL_WORK_DURATION_MS)
							),
							sequence(
								condition(mov -> mov.job == ESiegeWorkshopJob.PRODUCE_AMMO),
								show(),
								goToPos(mov -> mov.getWorkshop().getAmmoStackPosition()),
								setDirectionNode(EDirection.NORTH_EAST),
								crouchDown(action(SiegeEngineerMovable::storeAmmo))
							)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SiegeEngineerMovable> goToAnvil() {
		return sequence(
				goToPos(mov -> mov.building.getBuildingVariant().getAnvilPosition().calculatePoint(mov.building.getPosition())),
				setDirectionNode(mov -> mov.building.getBuildingVariant().getAnvilPosition().getDirection())
		);
	}

	private boolean chooseJob() {
		SiegeWorkshopBuilding workshop = getWorkshop();
		// A migrated old coroutine may have already picked up an item. Never overwrite it with the next pickup.
		EMaterialType carried = getMaterial();
		if (carried != EMaterialType.NO_MATERIAL) {
			if (job == ESiegeWorkshopJob.FETCH_WEAPON_MATERIAL && workshop.getOrderedSiegeWeapons() > 0) {
				workshop.weaponMaterialTaken(carried);
			} else if (getWeaponType().isAmmoInput(carried)) {
				workshop.ammoMaterialTaken(carried);
			} else if (!grid.dropMaterial(position, carried, true, false)) {
				return false;
			}
			setMaterial(EMaterialType.NO_MATERIAL);
		}
		job = workshop.getNextJob();
		fetchMaterial = job == ESiegeWorkshopJob.FETCH_WEAPON_MATERIAL ? workshop.findAvailableWeaponMaterial()
				: job == ESiegeWorkshopJob.FETCH_AMMO_MATERIAL ? workshop.findAvailableAmmoMaterial() : null;
		return job != ESiegeWorkshopJob.NONE;
	}

	private void storeAmmo() {
		int produced = getWorkshop().storeAmmoBatch();
		for (int i = 0; i < produced; i++) {
			produce(getWeaponType().getAmmo());
		}
	}

	private SiegeWorkshopBuilding getWorkshop() {
		return (SiegeWorkshopBuilding) building;
	}

	private ESiegeWeaponType getWeaponType() {
		return getWorkshop().getSiegeWeaponType();
	}
}
