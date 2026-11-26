package jsettlers.ai.army;

import java.util.Set;
import jsettlers.common.player.IPlayer;

/**
 * Reconstructed count-based attack gates from 00594080. Target placement and movement are
 * JSettlers adapters; original region/group management and weighted-strength branch remain open.
 */
public final class S3AttackStrategy extends SimpleStrategy {
    public S3AttackStrategy(ArmyFramework parent) { super(parent); }

    public static boolean hasNumericalAdvantage(int attackers, int defenders, float combatStrength) {
        return attackers >= 15 && (defenders < attackers * combatStrength * 0.75F || attackers > defenders * 4L);
    }

    @Override
    public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
        if (!parent.existsAliveEnemy()) return;
        IPlayer enemy = parent.getWeakestEnemy();
        SoldierPositions own = new SoldierPositions(parent.getPlayerId(), soldiersWithOrders);
        SoldierPositions opposing = new SoldierPositions(enemy.getPlayerId(), Set.of());
        if (hasNumericalAdvantage(own.getSoldiersCount(), opposing.getSoldiersCount(),
                parent.getPlayer().getCombatStrengthInformation().getCombatStrength(false))) {
            attack(own, false, soldiersWithOrders);
        }
    }

    @Override public void applyLightRules(Set<Integer> soldiersWithOrders) { }
}
