package jsettlers.ai.original;

import java.io.*;
import java.util.Arrays;
import jsettlers.ai.army.S3AttackStrategy;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.logic.player.InitialGameState;
import org.junit.Test;
import static org.junit.Assert.*;
import static jsettlers.common.player.ECivilisation.*;

public class S3RulesTest {
    @Test public void originalIndustryDiffersByCivilisationAndStock() {
        assertEquals(65000, S3ConstructionPolicy.lumberjackPriority(ROMAN, 40, 0, 100));
        assertEquals(45000, S3ConstructionPolicy.lumberjackPriority(ROMAN, 100, 2, 100));
        // Asians allow one extra lumberjack for a stock shortage; Romans suppress it at their target.
        assertEquals(65000, S3ConstructionPolicy.lumberjackPriority(ASIAN, 100, 8, 0));
        assertEquals(0, S3ConstructionPolicy.lumberjackPriority(ASIAN, 100, 9, 0));
        assertEquals(0, S3ConstructionPolicy.lumberjackPriority(ROMAN, 100, 6, 0));
        // Egyptian stone demand mirrors the Asian wood exception.
        assertEquals(65000, S3ConstructionPolicy.stonecutterPriority(EGYPTIAN, 100, 5, 0));
        assertEquals(0, S3ConstructionPolicy.stonecutterPriority(EGYPTIAN, 100, 6, 0));
        assertEquals(0, S3ConstructionPolicy.stonecutterPriority(ROMAN, 100, 3, 0));
        assertEquals(65000, S3ConstructionPolicy.stonecutterPriority(ASIAN, 0, 0, 100));
    }

    @Test public void originalProcessingAndReplantingRatiosHaveStrictBoundaries() {
        assertEquals(65000, S3ConstructionPolicy.sawmillPriority(0, 0));
        assertEquals(0, S3ConstructionPolicy.sawmillPriority(3, 1));
        assertEquals(65000, S3ConstructionPolicy.sawmillPriority(4, 1));
        assertEquals(0, S3ConstructionPolicy.foresterPriority(ROMAN, 3, 1, 50));
        assertEquals(65000 / 6, S3ConstructionPolicy.foresterPriority(ROMAN, 3, 1, 49));
        assertEquals(65000 / 2, S3ConstructionPolicy.foresterPriority(ASIAN, 3, 1, 20));
        assertEquals(65000, S3ConstructionPolicy.foresterPriority(ASIAN, 3, 1, 19));
        assertEquals(0, S3ConstructionPolicy.foresterPriority(EGYPTIAN, 3, 1, 0));
    }

    @Test public void interpolationUsesPopulationStagesAndTruncation() {
        int[] curve = {0,4,6,8,8};
        assertEquals(3, S3ConstructionPolicy.target(39, curve));
        assertEquals(4, S3ConstructionPolicy.target(40, curve));
        assertEquals(5, S3ConstructionPolicy.target(99, curve));
        assertEquals(6, S3ConstructionPolicy.target(100, curve));
        assertEquals(7, S3ConstructionPolicy.target(499, curve));
        assertEquals(8, S3ConstructionPolicy.target(500, curve));
        assertEquals(8, S3ConstructionPolicy.target(2000, curve));
    }

    @Test public void easyMiningUsesInclusive16BitThresholdAndOnlyDrawsAfterExtraction() {
        int kept = 0;
        for (int roll = 0; roll < 65536; roll++) {
            final int value = roll;
            if (S3DifficultyRules.retainMinedResource(EPlayerType.AI_S3_EASY, true, () -> value)) kept++;
        }
        assertEquals(22001, kept);
        assertFalse(S3DifficultyRules.retainMinedResource(EPlayerType.AI_S3_EASY, false,
                () -> { throw new AssertionError("Failed extraction must not draw"); }));
        for (EPlayerType type : EPlayerType.VALUES) {
            if (type == EPlayerType.AI_S3_EASY) continue;
            assertTrue(S3DifficultyRules.retainMinedResource(type, true,
                    () -> { throw new AssertionError("Other profiles must not draw"); }));
            assertEquals(10F, S3DifficultyRules.incomingDamage(type, 10F), 0F);
        }
        assertEquals(15F, S3DifficultyRules.incomingDamage(EPlayerType.AI_S3_EASY, 10F), 0F);
    }

    @Test public void originalAttackCountGateUsesFifteenTroopsAndStrictAdvantage() {
        assertFalse(S3AttackStrategy.hasNumericalAdvantage(14, 0, 1));
        assertTrue(S3AttackStrategy.hasNumericalAdvantage(15, 11, 1));
        assertFalse(S3AttackStrategy.hasNumericalAdvantage(20, 15, 1));
        assertTrue(S3AttackStrategy.hasNumericalAdvantage(21, 15, 1));
        assertFalse(S3AttackStrategy.hasNumericalAdvantage(20, 5, 0.2F));
        assertTrue(S3AttackStrategy.hasNumericalAdvantage(21, 5, 0.2F));
    }

    @Test public void clientsAndReplaysKeepHandicapsWithoutIssuingAiCommands() throws Exception {
        for (EPlayerType type : EPlayerType.VALUES) {
            PlayerSetting host = new PlayerSetting(type, ROMAN, (byte)0);
            PlayerSetting client = host.withoutAiExecution();
            PlayerSetting replay = new InitialGameState((byte)0, new PlayerSetting[]{host}, 42L)
                    .clone().getPlayerSettings()[0];
            assertEquals(type.isAi(), host.shouldRunAi());
            assertFalse(client.shouldRunAi());
            assertFalse(replay.shouldRunAi());
            assertSame(type, client.getPlayerType());
            assertSame(type, replay.getPlayerType());
            assertEquals(S3DifficultyRules.incomingDamage(type, 10F),
                    S3DifficultyRules.incomingDamage(client.getPlayerType(), 10F), 0F);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            client.writeTo(new DataOutputStream(bytes));
            PlayerSetting loaded = PlayerSetting.readFromStream(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            assertEquals("Loading a saved client game locally must reactivate the AI", type.isAi(), loaded.shouldRunAi());
        }
    }

    @Test public void oldWireIdsAndExplicitSelectionSurviveSettingsRoundTrip() throws Exception {
        EPlayerType[] legacy = {EPlayerType.AI_VERY_EASY, EPlayerType.AI_EASY, EPlayerType.AI_HARD, EPlayerType.AI_VERY_HARD, EPlayerType.HUMAN};
        for (int id = 0; id < legacy.length; id++) assertSame(legacy[id], EPlayerType.getTypeByIndex(id));
        for (EPlayerType type : EPlayerType.VALUES) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new PlayerSetting(type, ECivilisation.AMAZON, (byte) 2).writeTo(new DataOutputStream(bytes));
            PlayerSetting loaded = PlayerSetting.readFromStream(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            assertSame(type, loaded.getPlayerType());
            assertSame(ECivilisation.AMAZON, loaded.getCivilisation());
            assertEquals(Byte.valueOf((byte)2), loaded.getTeamId());
            if (type.isAi()) assertTrue(Arrays.asList(EPlayerType.VALUES_AI_ONLY).contains(type));
        }
        for (PlayerSetting setting : PlayerSetting.createDefaultSettings((byte)0, (byte)12)) {
            assertFalse("New profiles must be opt-in", setting.getPlayerType().isS3Reconstruction());
        }
    }
}
