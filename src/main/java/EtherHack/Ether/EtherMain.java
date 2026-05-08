package EtherHack.Ether;

import EtherHack.utils.EventSubscriber;
import EtherHack.utils.Logger;

public class EtherMain {
   private static EtherMain instance;
   public EtherTranslator etherTranslator;
   public EtherCredits etherCredits;
   public EtherLuaManager etherLuaManager;
   public EtherAPI etherAPI;
   private PlayerUpdateHandler playerUpdateHandler;

   private EtherMain() {
   }

   public void init() {
      Logger.printLog("Initializing EtherHack...");
      this.etherTranslator = new EtherTranslator();
      this.etherTranslator.loadTranslations();
      this.etherCredits = new EtherCredits();
      this.etherAPI = new EtherAPI();
      this.etherAPI.loadAPI();
      this.etherLuaManager = new EtherLuaManager();
      this.etherLuaManager.loadLua();
      
      // Register event handlers
      this.playerUpdateHandler = PlayerUpdateHandler.getInstance();
      EventSubscriber.register(this.playerUpdateHandler);
      Logger.printLog("Registered PlayerUpdateHandler for stat modifications");
      
      Logger.printLog("Initialization EtherHack was completed!");
   }

   public static EtherMain getInstance() {
      if (instance == null) {
         instance = new EtherMain();
      }

      return instance;
   }
}
