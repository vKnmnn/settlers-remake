package jsettlers.logic.movable.military;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;
import org.junit.Test;

import static jsettlers.common.material.EMaterialType.*;
import static org.junit.Assert.*;

public class GiftDistributionTest {
    private static final EMaterialType[][] MATERIALS = {
        {GOLD, IRON, SWORD, BOW, SPEAR, SCYTHE, FISHINGROD, STONE},
        {GEMS, STONE, COAL, SPEAR, MEAT, FISH, SCYTHE, PICK},
        {IRON, COAL, SWORD, BOW, BREAD, SULFUR, FISHINGROD, SAW},
        {GEMS, STONE, SULFUR, BOW, SWORD, HONEY, SPEAR, PICK}
    };
    private static final int[][] THRESHOLDS = {
        {19656, 36036, 42588, 49140, 55692, 58968, 62244, 65536},
        {3276, 13104, 19656, 26208, 39312, 58968, 62244, 65536},
        {9828, 29484, 38036, 46588, 53140, 58968, 62244, 65536},
        {3276, 13104, 19656, 26208, 39312, 58968, 62244, 65536}
    };
    private static final int[][] QUANTITY_PARAMETERS = {
        {5, 4, 3, 3, 3, 3, 3, 8}, {4, 7, 6, 4, 3, 5, 3, 3},
        {5, 8, 7, 6, 8, 5, 3, 3}, {4, 7, 6, 4, 3, 5, 3, 3}
    };

    @Test public void everyThresholdIsExclusiveAndBothEndsAreCovered() {
        for (ECivilisation civilisation : ECivilisation.values()) {
            int c = civilisation.ordinal();
            int lower = 0;
            for (int i = 0; i < 8; i++) {
                assertEquals(MATERIALS[c][i], sample(civilisation, lower, 0).getMaterial());
                assertEquals(MATERIALS[c][i], sample(civilisation, THRESHOLDS[c][i] - 1, 0).getMaterial());
                lower = THRESHOLDS[c][i];
            }
        }
    }

    @Test public void allUnsignedDrawsProduceExactMaterialWeights() {
        for (ECivilisation civilisation : ECivilisation.values()) {
            Map<EMaterialType, Integer> counts = new EnumMap<>(EMaterialType.class);
            for (int draw = 0; draw < 65536; draw++) {
                counts.merge(sample(civilisation, draw, 0).getMaterial(), 1, Integer::sum);
            }
            int c = civilisation.ordinal();
            assertEquals(8, counts.size());
            int lower = 0;
            for (int i = 0; i < 8; i++) {
                assertEquals(Integer.valueOf(THRESHOLDS[c][i] - lower), counts.get(MATERIALS[c][i]));
                lower = THRESHOLDS[c][i];
            }
        }
    }

    @Test public void quantityUsesSecondUnsignedDrawModuloHalfRange() {
        for (ECivilisation civilisation : ECivilisation.values()) {
            int c = civilisation.ordinal();
            for (int i = 0; i < 8; i++) {
                int materialDraw = THRESHOLDS[c][i] - 1;
                int n = QUANTITY_PARAMETERS[c][i];
                int range = (n + 1) / 2;
                assertEquals(n / 2, sample(civilisation, materialDraw, 0).getAmount());
                assertEquals(n - 1, sample(civilisation, materialDraw, range - 1).getAmount());
                assertEquals(n / 2, sample(civilisation, materialDraw, range).getAmount());
                assertEquals(65535 % range + n / 2, sample(civilisation, materialDraw, 65535).getAmount());
            }
        }
    }

    @Test public void fourPackagesConsumeEightDrawsInStableMaterialQuantityOrder() {
        int[] draws = {0, 2, 19656, 1, 36036, 1, 65535, 3};
        AtomicInteger index = new AtomicInteger();
        EMaterialType[] expectedMaterial = {GOLD, IRON, SWORD, STONE};
        int[] expectedAmount = {4, 3, 2, 7};
        for (int i = 0; i < 4; i++) {
            GiftDistribution.Gift gift = GiftDistribution.sample(ECivilisation.ROMAN, bound -> {
                assertEquals(65536, bound);
                return draws[index.getAndIncrement()];
            });
            assertEquals(expectedMaterial[i], gift.getMaterial());
            assertEquals(expectedAmount[i], gift.getAmount());
        }
        assertEquals(8, index.get());
    }

    private GiftDistribution.Gift sample(ECivilisation civilisation, int material, int quantity) {
        AtomicInteger calls = new AtomicInteger();
        GiftDistribution.Gift gift = GiftDistribution.sample(civilisation, bound -> {
            assertEquals(65536, bound);
            return calls.getAndIncrement() == 0 ? material : quantity;
        });
        assertEquals(2, calls.get());
        return gift;
    }
}
