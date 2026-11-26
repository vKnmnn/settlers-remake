package jsettlers.ai.original;

import java.util.function.IntSupplier;
import jsettlers.common.ai.EPlayerType;

/** Campaign easy-mode handicaps; explicitly opting into S3 easy also enables them in free games. */
public final class S3DifficultyRules {
    private S3DifficultyRules() { }

    /** Called AFTER depletion. Failed extraction and other AI types consume no additional random value. */
    public static boolean retainMinedResource(EPlayerType type, boolean extracted, IntSupplier random16) {
        return extracted && (type != EPlayerType.AI_S3_EASY || random16.getAsInt() <= 22000);
    }

    /** JSettlers health/damage are floats; preserve the original +50% ratio without integer quantisation. */
    public static float incomingDamage(EPlayerType targetType, float damage) {
        return targetType == EPlayerType.AI_S3_EASY ? damage * 1.5F : damage;
    }
}
