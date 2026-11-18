/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.logic.player;

import jsettlers.common.player.ECivilisation;
import jsettlers.common.player.IEndgameStatistic;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * @author codingberlin
 */
public class EndgameStatistic implements IEndgameStatistic, Serializable {
	private static final long serialVersionUID = -1352905249487671843L;

	private Player player;
	private boolean complete = true;
	private boolean extendedCounters = true;
	private volatile long settlersCreated, buildingMaterialsUsed, foodConsumed, minedMaterials;
	private volatile long goldWeight, soldiersRecruited, battlePoints;

	@Override public long getSettlersCreated() { return settlersCreated; }
	@Override public long getBuildingMaterialsUsed() { return buildingMaterialsUsed; }
	@Override public long getFoodConsumed() { return foodConsumed; }
	@Override public long getMinedMaterials() { return minedMaterials; }
	@Override public long getGoldValue() { return goldWeight / 2; }
	@Override public long getManaProduced() { return player.getMannaInformation().getLifetimeMannaProduced(); }
	@Override public long getSoldiersRecruited() { return soldiersRecruited; }
	@Override public long getBattlePoints() { return battlePoints; }
	@Override public boolean isComplete() { return complete; }
	@Override public long getScore() {
		return 5 * battlePoints + 2 * (soldiersRecruited + settlersCreated)
				+ getManaProduced() + minedMaterials + foodConsumed + buildingMaterialsUsed + goldWeight;
	}

	public void recordSettlerCreated() { settlersCreated++; }
	public void recordBuildingMaterialUsed() { buildingMaterialsUsed++; }
	public void recordFoodConsumed() { foodConsumed++; }
	public void recordMinedMaterial() { minedMaterials++; }
	public void recordGemProduced() { goldWeight += 4; }
	public void recordBattleWon() { battlePoints++; }

	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		if (!extendedCounters) {
			soldiersRecruited = Short.toUnsignedInt(amountOfProducedSoldiers);
			goldWeight = 2L * Short.toUnsignedInt(amountOfProducedGold);
			complete = false;
			extendedCounters = true;
		}
	}

	private short amountOfProducedSoldiers = 0;
	private short amountOfProducedGold = 0;

	public EndgameStatistic(Player player) {
		this.player = player;
	}

	@Override
	public short getAmountOfProducedSoldiers() {
		return amountOfProducedSoldiers;
	}

	@Override
	public short getAmountOfProducedMana() {
		return player.getMannaInformation().getAmountOfProducedManna();
	}

	@Override
	public short getAmountOfProducedGold() {
		return amountOfProducedGold;
	}

	@Override
	public String getName() {
		return player.getPlayerType().toString();
	}

	@Override
	public byte getTeam() {
		return player.getTeamId();
	}

	public void incrementAmountOfProducedSoldiers() {
		amountOfProducedSoldiers++;
		soldiersRecruited++;
	}

	public void incrementAmountOfProducedGold() {
		amountOfProducedGold++;
		goldWeight += player.getCivilisation() == ECivilisation.EGYPTIAN ? 1 : 2;
	}

	@Override
	public String toString() {

		return "amountOfProducedSoldiers: " + amountOfProducedSoldiers +
				", amountOfProducedGold: " + amountOfProducedGold +
				", amountOfProducedMana: " + getAmountOfProducedMana();
	}
}
