package EtherHack.Ether;

import EtherHack.annotations.SubscribeLuaEvent;
import EtherHack.utils.Logger;
import EtherHack.utils.StatsModifier;
import zombie.characters.IsoPlayer;

/**
 * PlayerUpdateHandler - Handles OnPlayerUpdate event for stat modifications
 */
public class PlayerUpdateHandler {
    private static PlayerUpdateHandler instance;
    private final StatsModifier statsModifier;
    
    private PlayerUpdateHandler() {
        this.statsModifier = StatsModifier.getInstance();
    }
    
    public static PlayerUpdateHandler getInstance() {
        if (instance == null) {
            instance = new PlayerUpdateHandler();
        }
        return instance;
    }
    
    /**
     * Called on every player update tick
     */
    @SubscribeLuaEvent(eventName = "OnPlayerUpdate")
    public void onPlayerUpdate() {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null) {
                statsModifier.update(player);
            }
        } catch (Exception e) {
            Logger.printLog("Error in OnPlayerUpdate: " + e.getMessage());
        }
    }
    
    /**
     * Apply instant modifications (call when enabling a cheat)
     */
    public void applyInstantMods() {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null) {
                statsModifier.applyInstantMods(player);
            }
        } catch (Exception e) {
            Logger.printLog("Error applying instant mods: " + e.getMessage());
        }
    }
}
