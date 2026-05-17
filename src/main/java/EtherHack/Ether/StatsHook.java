package EtherHack.Ether;

import zombie.characters.CharacterStat;
import zombie.characters.Stats;

/**
 * StatsHook - Intercepts stat modifications via bytecode hooks
 * This is the Java 25 compatible approach used by EtherMenu
 */
public class StatsHook {
    
    /**
     * Called from bytecode-injected hook in Stats.set(CharacterStat, float)
     * Intercepts ALL stat changes and returns the enforced override value
     */
    public static float interceptStatSet(CharacterStat stat, float value) {
        try {
            EtherMain main = EtherMain.getInstance();
            if (main == null || main.etherAPI == null) return value;
            EtherAPI api = main.etherAPI;
            
            // Map CharacterStat to our cheat flags
            if (stat == CharacterStat.ENDURANCE && api.isUnlimitedEndurance) return 1.0f;
            if (stat == CharacterStat.FATIGUE && api.isDisableFatigue) return 0.0f;
            if (stat == CharacterStat.HUNGER && api.isDisableHunger) return 0.0f;
            if (stat == CharacterStat.THIRST && api.isDisableThirst) return 0.0f;
            if (stat == CharacterStat.INTOXICATION && api.isDisableDrunkenness) return 0.0f;
            if (stat == CharacterStat.ANGER && api.isDisableAnger) return 0.0f;
            if (stat == CharacterStat.PANIC && (api.isDisableFear || api.isDisablePanic)) return 0.0f;
            if (stat == CharacterStat.PAIN && api.isDisablePain) return 0.0f;
            if (stat == CharacterStat.MORALE && api.isDisableMorale) return 1.0f;
            if (stat == CharacterStat.STRESS && api.isDisableStress) return 0.0f;
            if (stat == CharacterStat.SICKNESS && api.isDisableSickness) return 0.0f;
            if (stat == CharacterStat.NICOTINE_WITHDRAWAL && api.isDisableStressFromCigarettes) return 0.0f;
            if (stat == CharacterStat.SANITY && api.isDisableSanity) return 1.0f;
            if (stat == CharacterStat.BOREDOM && api.isDisableBoredomLevel) return 0.0f;
            if (stat == CharacterStat.UNHAPPINESS && api.isDisableUnhappynessLevel) return 0.0f;
            if (stat == CharacterStat.WETNESS && api.isDisableWetness) return 0.0f;
            if (stat == CharacterStat.ZOMBIE_INFECTION && api.isDisableInfectionLevel) return 0.0f;
            
        } catch (Throwable ignored) {
            // Silently fail - don't break the game
        }
        return value;
    }
    
    /**
     * Called from bytecode-injected hook in Stats.add(CharacterStat, float)
     * Prevents stat increases when cheats are enabled
     */
    public static float interceptStatAdd(CharacterStat stat, float amount) {
        try {
            EtherMain main = EtherMain.getInstance();
            if (main == null || main.etherAPI == null) return amount;
            EtherAPI api = main.etherAPI;
            
            // If we're disabling this stat, prevent any additions
            if (stat == CharacterStat.FATIGUE && api.isDisableFatigue) return 0.0f;
            if (stat == CharacterStat.HUNGER && api.isDisableHunger) return 0.0f;
            if (stat == CharacterStat.THIRST && api.isDisableThirst) return 0.0f;
            if (stat == CharacterStat.INTOXICATION && api.isDisableDrunkenness) return 0.0f;
            if (stat == CharacterStat.PANIC && (api.isDisableFear || api.isDisablePanic)) return 0.0f;
            if (stat == CharacterStat.PAIN && api.isDisablePain) return 0.0f;
            if (stat == CharacterStat.STRESS && api.isDisableStress) return 0.0f;
            if (stat == CharacterStat.SICKNESS && api.isDisableSickness) return 0.0f;
            if (stat == CharacterStat.BOREDOM && api.isDisableBoredomLevel) return 0.0f;
            if (stat == CharacterStat.UNHAPPINESS && api.isDisableUnhappynessLevel) return 0.0f;
            if (stat == CharacterStat.WETNESS && api.isDisableWetness) return 0.0f;
            if (stat == CharacterStat.ZOMBIE_INFECTION && api.isDisableInfectionLevel) return 0.0f;
            
        } catch (Throwable ignored) {
        }
        return amount;
    }
}
