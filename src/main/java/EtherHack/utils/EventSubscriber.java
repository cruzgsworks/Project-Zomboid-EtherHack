package EtherHack.utils;

import EtherHack.Ether.EtherMain;
import EtherHack.annotations.SubscribeLuaEvent;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EventSubscriber {
   private static Map<String, List<AbstractMap.SimpleEntry<Object, Method>>> subscribers = new HashMap<>();
   private static boolean initialized = false;

    private static void changeWindowTitle() {
       try {
          // Try multiple classloaders to find LWJGL Display class
          Class<?> displayClass = null;
          
          // PZ 42.x uses LWJGLX: org.lwjglx.opengl.Display
          String displayClassName = "org.lwjglx.opengl.Display";
          
          // Try 1: System classloader
          try {
             displayClass = Class.forName(displayClassName);
          } catch (ClassNotFoundException e) {
             // Try 2: Context classloader
             ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
             if (contextLoader != null) {
                displayClass = contextLoader.loadClass(displayClassName);
             }
          }
          
          if (displayClass == null) {
             Logger.printLog("Could not find org.lwjgl.opengl.Display class");
             return;
          }
          
          Method setTitleMethod = displayClass.getMethod("setTitle", String.class);
          setTitleMethod.invoke(null, "Project Zomboid | EtherHack");
          Logger.printLog("Window title changed to: Project Zomboid | EtherHack");
       } catch (Exception e) {
          Logger.printLog("Failed to change window title: " + e.getClass().getSimpleName() + " - " + e.getMessage());
       }
    }

    private static void injectLogoState() {
       try {
          // Access GameWindow.states via reflection
          Class<?> gameWindowClass = Class.forName("zombie.GameWindow");
          Object states = gameWindowClass.getField("states").get(null);
          
          if (states == null) {
             Logger.printLog("GameWindow.states is null, skipping logo injection");
             return;
          }
          
          // Get the states ArrayList (raw type for reflection)
          @SuppressWarnings("unchecked")
          ArrayList<Object> stateList = (ArrayList<Object>) states.getClass().getField("states").get(states);
          
          if (stateList == null || stateList.isEmpty()) {
             Logger.printLog("State list is empty, skipping logo injection");
             return;
          }
          
          // Check if first state is TISLogoState
          Object firstState = stateList.get(0);
          if (firstState == null || !firstState.getClass().getName().equals("zombie.gameStates.TISLogoState")) {
             Logger.printLog("First state is not TISLogoState (got: " + (firstState != null ? firstState.getClass().getName() : "null") + "), skipping logo injection");
             return;
          }
          
          // Create EtherLogoState and insert at index 0
          Class<?> logoStateClass = Class.forName("EtherHack.states.EtherLogoState");
          Object etherLogo = logoStateClass.getDeclaredConstructor().newInstance();
          
          // Insert before TISLogoState
          stateList.add(0, etherLogo);
          
          // Set loopToState to 1 to show our logo first
          states.getClass().getField("loopToState").set(states, 1);
          
          Logger.printLog("EtherHack logo state injected successfully");
       } catch (Exception e) {
          Logger.printLog("Failed to inject logo state: " + e.getClass().getSimpleName() + " - " + e.getMessage());
          e.printStackTrace();
       }
    }

    public static void register(Object handler) {
      Logger.printLog("Registering a class object and subscribing to Lua events: " + handler);

      for (Method method : handler.getClass().getMethods()) {
         for (SubscribeLuaEvent annotation : method.getAnnotationsByType(SubscribeLuaEvent.class)) {
            String eventName = annotation.eventName();
            subscribers.computeIfAbsent(eventName, k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(handler, method));
         }
      }
   }

    public static void invokeSubscriber(String eventName) {
       // Initialize EtherHack on first event
       if (!initialized) {
          initialized = true;
          try {
             Logger.printLog("Initializing EtherHack from LuaEventManager...");
             EtherMain.getInstance().init();
             Logger.printLog("EtherHack initialized successfully!");
             
              // Change window title via reflection
              changeWindowTitle();
              
              // Inject logo state via reflection
              injectLogoState();
          } catch (Exception e) {
             Logger.printLog("Failed to initialize EtherHack: " + e.getMessage());
          }
       }
      
      List<AbstractMap.SimpleEntry<Object, Method>> handlers = subscribers.get(eventName);
      if (handlers != null) {
         for (AbstractMap.SimpleEntry<Object, Method> entry : handlers) {
            try {
               entry.getValue().invoke(entry.getKey());
            } catch (Exception e) {
               Logger.printLog(String.format("Exception when calling method '%s' for event '%s': %s",
                       entry.getValue(), eventName, e));
            }
         }
      }
   }
}