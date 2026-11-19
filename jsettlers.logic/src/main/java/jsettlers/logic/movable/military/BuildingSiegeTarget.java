package jsettlers.logic.movable.military;

import java.io.Serializable;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.interfaces.ISiegeAttackable;

/** A structural target, separate from a tower's infantry entrance and defenders. */
public final class BuildingSiegeTarget implements ISiegeAttackable, Serializable {
    private static final long serialVersionUID = 1L;
    private final Building building;

    public BuildingSiegeTarget(Building building) { this.building = building; }
    public float getHealth() { return building.getSiegeHealth(); }
    @Override public ShortPoint2D getPosition() { return building.getPosition(); }
    @Override public IPlayer getPlayer() { return building.getPlayer(); }
    @Override public boolean isAlive() { return !building.isDestroyed(); }
    @Override public boolean isAttackable() { return canReceiveSiegeDamage(); }
    @Override public boolean canReceiveSiegeDamage() { return isAlive() && building.isConstructionFinished(); }
    @Override public boolean isTower() { return building.getBuildingVariant().getType().isMilitaryBuilding(); }
    @Override public EMovableType getMovableType() { return null; }
    @Override public void informAboutAttackable(IAttackable other) { }
    @Override public void receiveHit(float strength, ShortPoint2D source, IPlayer player) { receiveSiegeHit(strength, source, player); }
    @Override public void receiveSiegeHit(float strength, ShortPoint2D source, IPlayer player) { building.receiveSiegeHit(strength, source, player); }
}
