package jsettlers.logic.movable.military;

import java.util.function.IntUnaryOperator;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;

import static jsettlers.common.material.EMaterialType.*;

/** Civilization-specific distribution for one divine gift package. */
public final class GiftDistribution {
    private static final int DRAW_BOUND = 65536;

    private static final Table ROMAN = new Table(
            new EMaterialType[] {GOLD, IRON, SWORD, BOW, SPEAR, SCYTHE, FISHINGROD, STONE},
            new int[] {19656, 36036, 42588, 49140, 55692, 58968, 62244, 65536},
            new int[] {5, 4, 3, 3, 3, 3, 3, 8});
    private static final Table EGYPTIAN = new Table(
            new EMaterialType[] {GEMS, STONE, COAL, SPEAR, MEAT, FISH, SCYTHE, PICK},
            new int[] {3276, 13104, 19656, 26208, 39312, 58968, 62244, 65536},
            new int[] {4, 7, 6, 4, 3, 5, 3, 3});
    private static final Table ASIAN = new Table(
            new EMaterialType[] {IRON, COAL, SWORD, BOW, BREAD, SULFUR, FISHINGROD, SAW},
            new int[] {9828, 29484, 38036, 46588, 53140, 58968, 62244, 65536},
            new int[] {5, 8, 7, 6, 8, 5, 3, 3});
    private static final Table AMAZON = new Table(
            new EMaterialType[] {GEMS, STONE, SULFUR, BOW, SWORD, HONEY, SPEAR, PICK},
            new int[] {3276, 13104, 19656, 26208, 39312, 58968, 62244, 65536},
            new int[] {4, 7, 6, 4, 3, 5, 3, 3});

    private GiftDistribution() {}

    /**
     * Samples a material and quantity using exactly two unsigned 16-bit draws.
     * The supplied random function has the same bounded contract as Random.nextInt.
     */
    public static Gift sample(ECivilisation civilisation, IntUnaryOperator random) {
        Table table;
        switch (civilisation) {
            case ROMAN: table = ROMAN; break;
            case EGYPTIAN: table = EGYPTIAN; break;
            case ASIAN: table = ASIAN; break;
            case AMAZON: table = AMAZON; break;
            default: throw new IllegalArgumentException("Unsupported civilization: " + civilisation);
        }
        int materialDraw = random.applyAsInt(DRAW_BOUND);
        int index = 0;
        while (materialDraw >= table.thresholds[index]) index++;
        int n = table.amounts[index];
        int amount = random.applyAsInt(DRAW_BOUND) % ((n + 1) / 2) + n / 2;
        return new Gift(table.materials[index], amount);
    }

    public static final class Gift {
        private final EMaterialType material;
        private final int amount;

        private Gift(EMaterialType material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public EMaterialType getMaterial() { return material; }
        public int getAmount() { return amount; }
    }

    private static final class Table {
        private final EMaterialType[] materials;
        private final int[] thresholds;
        private final int[] amounts;

        private Table(EMaterialType[] materials, int[] thresholds, int[] amounts) {
            this.materials = materials;
            this.thresholds = thresholds;
            this.amounts = amounts;
        }
    }
}
