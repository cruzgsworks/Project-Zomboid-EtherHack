package EtherHack.utils;

import EtherHack.Ether.EtherAPI;
import EtherHack.Ether.EtherMain;
import zombie.characters.CharacterStat;
import zombie.characters.CheatType;
import zombie.characters.IsoPlayer;
import zombie.characters.PlayerCheats;
import zombie.characters.Stats;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * StatsModifier - Handles player stat and body damage modifications
 * PZ 42.x uses CharacterStat enum with Map-based storage instead of individual fields
 */
public class StatsModifier {
    private static StatsModifier instance;
    private int updateCounter = 0;
    
    // Update frequency - check stats every 10 ticks
    private static final int UPDATE_FREQUENCY = 10;
    
    // PZ 42.x: Stats uses Map<CharacterStat, Float> - no individual fields
    // We use stats.set(CharacterStat.X, value) instead of reflection
    
    // BodyDamage boolean fields (these still exist as direct fields)
    private Field isInfectedField;
    private Field isFakeInfectedField;
    private Field hasAColdField;
    private Field catchAColdField;
    private Field coldStrengthField;
    
    private StatsModifier() {
        initializeReflection();
    }
    
    public static StatsModifier getInstance() {
        if (instance == null) {
            instance = new StatsModifier();
        }
        return instance;
    }
    
    private void initializeReflection() {
        try {
            // PZ 42.x: Only BodyDamage boolean fields need reflection
            // Stats are now accessed via CharacterStat enum + stats.set()/get()
            Class<BodyDamage> bodyDamageClass = BodyDamage.class;
            isInfectedField = bodyDamageClass.getDeclaredField("isInfected");
            isFakeInfectedField = bodyDamageClass.getDeclaredField("isFakeInfected");
            hasAColdField = bodyDamageClass.getDeclaredField("hasACold");
            catchAColdField = bodyDamageClass.getDeclaredField("catchACold");
            coldStrengthField = bodyDamageClass.getDeclaredField("coldStrength");
            
            // Make accessible
            for (Field field : new Field[]{isInfectedField, isFakeInfectedField, hasAColdField,
                    catchAColdField, coldStrengthField}) {
                if (field != null) field.setAccessible(true);
            }
            
        } catch (Exception e) {
            Logger.printLog("Error initializing reflection: " + e.getMessage());
        }
    }
    
    /**
     * Main update method - call this from OnPlayerUpdate event every tick
     * PZ 42.x increases hunger/thirst every tick via stats.add(), so we must
     * reset every tick to prevent stat increases.
     */
    public void update(IsoPlayer player) {
        if (player == null) return;
        modifyStats(player);
        modifyBodyDamage(player);
        modifyDebugBypass(player);
    }
    
    /**
     * Modify player stats based on enabled flags using CharacterStat enum (PZ 42.x)
     */
    private void modifyStats(IsoPlayer player) {
        if (player.getStats() == null) return;
        
        Stats stats = player.getStats();
        EtherAPI api = EtherMain.getInstance().etherAPI;
        
        try {
            // Fatigue management
            if (api.isDisableFatigue) {
                stats.set(CharacterStat.FATIGUE, 0.0f);
            }
            
            // Hunger management
            if (api.isDisableHunger) {
                stats.set(CharacterStat.HUNGER, 0.0f);
            }
            
            // Thirst management
            if (api.isDisableThirst) {
                stats.set(CharacterStat.THIRST, 0.0f);
            }
            
            // Endurance management (unlimited endurance)
            if (api.isUnlimitedEndurance) {
                stats.set(CharacterStat.ENDURANCE, 1.0f);
            }
            
            // Boredom management
            if (api.isDisableBoredomLevel) {
                stats.set(CharacterStat.BOREDOM, 0.0f);
            }
            
            // Stress management
            if (api.isDisableStress) {
                stats.set(CharacterStat.STRESS, 0.0f);
            }
            
            // Panic management
            if (api.isDisablePanic) {
                stats.set(CharacterStat.PANIC, 0.0f);
            }
            
            // Pain management
            if (api.isDisablePain) {
                stats.set(CharacterStat.PAIN, 0.0f);
            }
            
            // Sickness management
            if (api.isDisableSickness) {
                stats.set(CharacterStat.SICKNESS, 0.0f);
            }
            
            // Drunkenness management
            if (api.isDisableDrunkenness) {
                stats.set(CharacterStat.INTOXICATION, 0.0f);
            }
            
            // Anger management
            if (api.isDisableAnger) {
                stats.set(CharacterStat.ANGER, 0.0f);
            }
            
            // Fear management
            if (api.isDisableFear) {
                stats.set(CharacterStat.PANIC, 0.0f);
            }
            
            // Morale management
            if (api.isDisableMorale) {
                stats.set(CharacterStat.MORALE, 1.0f);
            }
            
            // Sanity management
            if (api.isDisableSanity) {
                stats.set(CharacterStat.SANITY, 1.0f);
            }
            
            // Cigarette stress management
            if (api.isDisableStressFromCigarettes) {
                stats.set(CharacterStat.NICOTINE_WITHDRAWAL, 0.0f);
            }
            
            // Wetness management (stored in Stats via CharacterStat in PZ 42.x)
            // Also need to set body part wetness to 0 since game averages body parts
            if (api.isDisableWetness) {
                stats.set(CharacterStat.WETNESS, 0.0f);
            }
            
            // Unhappiness management (stored in Stats via CharacterStat in PZ 42.x)
            if (api.isDisableUnhappynessLevel) {
                stats.set(CharacterStat.UNHAPPINESS, 0.0f);
            }
            
            // Infection level management (stored in Stats via CharacterStat in PZ 42.x)
            if (api.isDisableInfectionLevel) {
                stats.set(CharacterStat.ZOMBIE_INFECTION, 0.0f);
            }
            
        } catch (Exception e) {
            Logger.printLog("Error modifying stats: " + e.getMessage());
        }
    }
    
    /**
     * Modify player body damage based on enabled flags
     * PZ 42.x: BodyDamage stores wetness/boredom/unhappiness/infection in Stats (CharacterStat)
     * Boolean fields (isInfected, isFakeInfected, hasACold) still exist in BodyDamage
     */
    private void modifyBodyDamage(IsoPlayer player) {
        if (player.getBodyDamage() == null) return;
        
        BodyDamage bodyDamage = player.getBodyDamage();
        EtherAPI api = EtherMain.getInstance().etherAPI;
        
        try {
            // Wetness management - need to set body part wetness to 0
            // Game calculates Stats.WETNESS from average of body part wetness
            if (api.isDisableWetness) {
                ArrayList<BodyPart> bodyParts = bodyDamage.getBodyParts();
                if (bodyParts != null) {
                    for (BodyPart part : bodyParts) {
                        if (part != null) {
                            part.setWetness(0.0f);
                        }
                    }
                }
            }
            
            // Infection status (boolean fields in BodyDamage)
            if (api.isDisableInfectionLevel) {
                if (isInfectedField != null) isInfectedField.setBoolean(bodyDamage, false);
            }
            
            // Fake infection status (boolean field in BodyDamage + per body part)
            // BodyDamage.setIsFakeInfected() only sets body part 0, we need all parts
            if (api.isDisableFakeInfectionLevel) {
                if (isFakeInfectedField != null) isFakeInfectedField.setBoolean(bodyDamage, false);
                // Also clear fake infection from all body parts
                ArrayList<BodyPart> bodyParts = bodyDamage.getBodyParts();
                if (bodyParts != null) {
                    for (BodyPart part : bodyParts) {
                        if (part != null) {
                            part.SetFakeInfected(false);
                        }
                    }
                }
            }
            
            // Cold management
            if (api.isDisableHasACold) {
                if (hasAColdField != null) hasAColdField.setBoolean(bodyDamage, false);
                if (catchAColdField != null) catchAColdField.setFloat(bodyDamage, 0.0f);
                if (coldStrengthField != null) coldStrengthField.setFloat(bodyDamage, 0.0f);
            }
            
        } catch (Exception e) {
            Logger.printLog("Error modifying body damage: " + e.getMessage());
        }
    }
    
    /**
     * Instant stat restoration - call when enabling a cheat
     */
    public void applyInstantMods(IsoPlayer player) {
        if (player == null) return;
        modifyStats(player);
        modifyBodyDamage(player);
    }
    
    /**
     * Modify debug bypass - enables debug context menu in multiplayer
     * PZ 42.x: setCanUseDebugContextMenu() checks role capability which fails for regular players
     * We bypass this by directly setting the cheat flag via reflection
     */
    private void modifyDebugBypass(IsoPlayer player) {
        EtherAPI api = EtherMain.getInstance().etherAPI;
        
        if (!api.isBypassDebugMode) return;
        
        try {
            // Check if already enabled
            if (player.canUseDebugContextMenu()) return;
            
            // Get PlayerCheats object
            PlayerCheats cheats = player.getCheats();
            if (cheats == null) return;
            
            // Use reflection to call cheats.set(CheatType.DEBUG_CONTEXT_MENU, true)
            // This bypasses the capability check in setCanUseDebugContextMenu()
            Method setMethod = PlayerCheats.class.getDeclaredMethod("set", CheatType.class, boolean.class);
            setMethod.setAccessible(true);
            setMethod.invoke(cheats, CheatType.DEBUG_CONTEXT_MENU, true);
            
        } catch (Exception e) {
            Logger.printLog("Error enabling debug bypass: " + e.getMessage());
        }
    }
}
