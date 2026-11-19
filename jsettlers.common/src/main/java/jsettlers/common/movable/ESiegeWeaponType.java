/*
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
 */
package jsettlers.common.movable;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;

/**
 * The siege weapons of the civilisations with the workshop that builds them, their ammunition and their combat values.
 * <p>
 * A workshop builds a weapon when the player orders one. Otherwise it produces ammunition from {@link #getAmmoInputs()}. Ammunition can't be
 * carried by bearers: the weapons reload themselves from ammunition stacks next to them.
 */
public enum ESiegeWeaponType {
	CATAPULT(ECivilisation.ROMAN, EBuildingType.CATAPULT_WORKSHOP, EMovableType.CATAPULT, EMaterialType.CATAPULT_AMMO,
			new EMaterialType[] { EMaterialType.STONE },
			new EMaterialType[] { EMaterialType.PLANK, EMaterialType.IRON, EMaterialType.STONE }, new short[] { 6, 3, 6 },
			10, 27, 60f, 1536, 6, 0.045f, 1536),

	BALLISTA(ECivilisation.EGYPTIAN, EBuildingType.BALLISTA_WORKSHOP, EMovableType.BALLISTA, EMaterialType.BALLISTA_AMMO,
			new EMaterialType[] { EMaterialType.TRUNK, EMaterialType.IRON },
			new EMaterialType[] { EMaterialType.PLANK, EMaterialType.IRON, EMaterialType.TRUNK }, new short[] { 5, 6, 6 },
			10, 31, 80f, 960, 6, 0.03f, 960),

	CANNON(ECivilisation.ASIAN, EBuildingType.CANNON_WORKSHOP, EMovableType.CANNON, EMaterialType.CANNON_AMMO,
			new EMaterialType[] { EMaterialType.IRON, EMaterialType.GUN_POWDER },
			new EMaterialType[] { EMaterialType.PLANK, EMaterialType.IRON, EMaterialType.GUN_POWDER }, new short[] { 3, 8, 6 },
			10, 35, 100f, 1152, 6, 0.035f, 1152),

	/**
	 * The gong needs no ammunition. It attacks a single target and can destroy civilian buildings as well.
	 */
	GONG(ECivilisation.AMAZON, EBuildingType.GONG_WORKSHOP, EMovableType.GONG, null,
			new EMaterialType[0],
			new EMaterialType[] { EMaterialType.PLANK, EMaterialType.IRON, EMaterialType.GOLD }, new short[] { 8, 4, 6 },
			3, 16, 100f, 3072, 0, 0f, 3072);

	public static final ESiegeWeaponType[] VALUES = values();

	/**
	 * Maximum number of ammunition items a workshop puts on its output stack. The ammunition stack images have only 6 frames.
	 */
	public static final int AMMO_STACK_CAPACITY = 6;

	/**
	 * Finalization attempts needed after every material has received its work animation.
	 */
	public static final int ASSEMBLY_STEPS = 1;

	/** Original workshop animation 0x113: 80 frames at 64 ms, for every consumed input. */
	public static final short MATERIAL_WORK_DURATION_MS = 5120;

	public final ECivilisation civilisation;
	public final EBuildingType workshop;
	public final EMovableType movableType;
	private final EMaterialType ammo;
	private final EMaterialType[] ammoInputs;
	private final EMaterialType[] weaponCostMaterials;
	private final short[] weaponCostAmounts;
	public final short minRange;
	public final short maxRange;
	public final float damage;
	public final int cooldownMs;
	public final int maxAmmo;
	public final float projectileSecondsPerTile;
	public final short fireDurationMs;

	ESiegeWeaponType(ECivilisation civilisation, EBuildingType workshop, EMovableType movableType, EMaterialType ammo, EMaterialType[] ammoInputs,
			EMaterialType[] weaponCostMaterials, short[] weaponCostAmounts, int minRange, int maxRange, float damage, int cooldownMs,
			int maxAmmo, float projectileSecondsPerTile, int fireDurationMs) {
		this.civilisation = civilisation;
		this.workshop = workshop;
		this.movableType = movableType;
		this.ammo = ammo;
		this.ammoInputs = ammoInputs;
		this.weaponCostMaterials = weaponCostMaterials;
		this.weaponCostAmounts = weaponCostAmounts;
		this.minRange = (short) minRange;
		this.maxRange = (short) maxRange;
		this.damage = damage;
		this.cooldownMs = cooldownMs;
		this.maxAmmo = maxAmmo;
		this.projectileSecondsPerTile = projectileSecondsPerTile;
		this.fireDurationMs = (short) fireDurationMs;
	}

	/**
	 * @return true if this weapon shoots projectiles that need ammunition. The gong does not consume ammunition.
	 */
	public boolean usesAmmo() {
		return ammo != null;
	}

	/**
	 * @return The ammunition material or null if this weapon needs no ammunition.
	 */
	public EMaterialType getAmmo() {
		return ammo;
	}

	/**
	 * @return Distinct input materials of a six-shot ammunition batch; quantities are given by getAmmoBatchInputCount.
	 */
	public EMaterialType[] getAmmoInputs() {
		return ammoInputs.clone();
	}

	/** Original recipes produce six shots from six primary inputs and, where needed, three iron bars. */
	public int getAmmoBatchInputCount(EMaterialType material) {
		return !isAmmoInput(material) ? 0 : material == EMaterialType.IRON ? 3 : 6;
	}

	public boolean isAmmoInput(EMaterialType material) {
		for (EMaterialType input : ammoInputs) {
			if (input == material) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return The materials needed to build one weapon. The amounts are given by {@link #getWeaponCost(EMaterialType)}.
	 */
	public EMaterialType[] getWeaponCostMaterials() {
		return weaponCostMaterials.clone();
	}

	/**
	 * @return The number of items of the given material that are needed to build one weapon.
	 */
	public short getWeaponCost(EMaterialType material) {
		for (int i = 0; i < weaponCostMaterials.length; i++) {
			if (weaponCostMaterials[i] == material) {
				return weaponCostAmounts[i];
			}
		}
		return 0;
	}

	/** Original 16-bit hit roll, recovered from S3.EXE FUN_00500860. */
	public boolean isHit(int roll) {
		int missThreshold = this == CATAPULT ? 32000 : this == CANNON ? 15000 : 20000;
		return roll > missThreshold;
	}

	/** Original successful projectile damage; the gong instead destroys its building target. */
	public int getHitDamage(int roll) {
		int base = (int) damage;
		return base + ((roll * base) >>> 16);
	}

	public boolean canAttackBuilding(EBuildingType building) {
		return this == GONG || building == EBuildingType.TOWER || building == EBuildingType.BIG_TOWER || building == EBuildingType.CASTLE;
	}

	public static ESiegeWeaponType fromMovableType(EMovableType movableType) {
		for (ESiegeWeaponType type : VALUES) {
			if (type.movableType == movableType) {
				return type;
			}
		}
		return null;
	}

	public static ESiegeWeaponType fromWorkshop(EBuildingType buildingType) {
		for (ESiegeWeaponType type : VALUES) {
			if (type.workshop == buildingType) {
				return type;
			}
		}
		return null;
	}

	public static ESiegeWeaponType forCivilisation(ECivilisation civilisation) {
		for (ESiegeWeaponType type : VALUES) {
			if (type.civilisation == civilisation) {
				return type;
			}
		}
		return null;
	}
}
