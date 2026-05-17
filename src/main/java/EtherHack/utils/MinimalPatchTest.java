package EtherHack.utils;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * MinimalPatchTest - Tests Java 25 compatible patching on simple methods
 * Uses Tree API with COMPUTE_FRAMES and proper class loading
 */
public class MinimalPatchTest {
    private static Path gameJarPath;
    private static final Map<String, byte[]> modifiedClasses = new HashMap<>();
    
    public static void setGameJarPath(Path path) {
        gameJarPath = path;
    }
    
    /**
     * Test patching - patches ItemContainer and LuaEventManager
     */
    public static void testPatchItemContainer() {
        Logger.print("Testing minimal patch on ItemContainer...");
        
        try {
            // Patch ItemContainer for unlimited carry
            patchItemContainer();
            
            // Patch LuaEventManager for initialization
            patchLuaEventManager();
            
            // NOTE: GameWindow and TISLogoState patches removed
            // Patching GameWindow causes VerifyError - class too complex
            // Use runtime reflection for title change instead
            
            Logger.print("Test patch successful!");
            
        } catch (Exception e) {
            Logger.print("Test patch failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void patchItemContainer() throws IOException {
        byte[] classBytes = readClassFromJar("zombie/inventory/ItemContainer");
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        
        // Find and modify getCapacityWeight method
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("getCapacityWeight") && method.desc.equals("()F")) {
                Logger.print("Found getCapacityWeight method");
                
                // Create new instruction list
                InsnList newInsn = new InsnList();
                
                // Load EtherMain.getInstance().etherAPI.isUnlimitedCarry
                newInsn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
                    "EtherHack/Ether/EtherMain", "getInstance", 
                    "()LEtherHack/Ether/EtherMain;", false));
                newInsn.add(new FieldInsnNode(Opcodes.GETFIELD, 
                    "EtherHack/Ether/EtherMain", "etherAPI", 
                    "LEtherHack/Ether/EtherAPI;"));
                newInsn.add(new FieldInsnNode(Opcodes.GETFIELD, 
                    "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
                
                // If not unlimited carry, jump to original code
                LabelNode originalCode = new LabelNode();
                newInsn.add(new JumpInsnNode(Opcodes.IFEQ, originalCode));
                
                // Return 0.0f if unlimited carry
                newInsn.add(new InsnNode(Opcodes.FCONST_0));
                newInsn.add(new InsnNode(Opcodes.FRETURN));
                
                // Original code label
                newInsn.add(originalCode);
                
                // Insert at beginning of method
                method.instructions.insert(newInsn);
                
                // Update max stack
                method.maxStack = Math.max(method.maxStack, 3);
                
                Logger.print("Modified getCapacityWeight");
                break;
            }
        }
        
        // Write class with COMPUTE_FRAMES
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Exception e) {
                    return "java/lang/Object";
                }
            }
        };
        classNode.accept(writer);
        modifiedClasses.put("zombie/inventory/ItemContainer", writer.toByteArray());
    }
    
    private static void patchLuaEventManager() throws IOException {
        Logger.print("Patching LuaEventManager.triggerEvent...");
        
        byte[] classBytes = readClassFromJar("zombie/Lua/LuaEventManager");
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        
        // Find triggerEvent method
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("triggerEvent") && method.desc.equals("(Ljava/lang/String;)V")) {
                Logger.print("Found triggerEvent method");
                
                // Create instruction list to call EventSubscriber.invokeSubscriber
                InsnList newInsn = new InsnList();
                
                // Load the event name parameter (first argument)
                newInsn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                
                // Call EventSubscriber.invokeSubscriber(eventName)
                newInsn.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "EtherHack/utils/EventSubscriber", "invokeSubscriber",
                    "(Ljava/lang/String;)V", false));
                
                // Insert at beginning of method
                method.instructions.insert(newInsn);
                
                // Update max stack
                method.maxStack = Math.max(method.maxStack, 1);
                
                Logger.print("Modified triggerEvent");
                break;
            }
        }
        
        // Write class with COMPUTE_FRAMES
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Exception e) {
                    return "java/lang/Object";
                }
            }
        };
        classNode.accept(writer);
        modifiedClasses.put("zombie/Lua/LuaEventManager", writer.toByteArray());
        Logger.print("LuaEventManager patched successfully");
    }
    
    private static byte[] readClassFromJar(String className) throws IOException {
        String entryName = className + ".class";
        try (JarFile jarFile = new JarFile(gameJarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                throw new IOException("Class not found: " + entryName);
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                return is.readAllBytes();
            }
        }
    }
    
    public static void saveModifiedClasses() {
        if (gameJarPath == null || modifiedClasses.isEmpty()) {
            return;
        }
        
        Path tempJarPath = gameJarPath.resolveSibling(gameJarPath.getFileName().toString() + ".tmp");
        
        try (JarFile jarFile = new JarFile(gameJarPath.toFile());
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJarPath.toFile()))) {
            
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - 6);
                    if (modifiedClasses.containsKey(className)) {
                        JarEntry newEntry = new JarEntry(entryName);
                        jos.putNextEntry(newEntry);
                        jos.write(modifiedClasses.get(className));
                        jos.closeEntry();
                        continue;
                    }
                }
                
                jos.putNextEntry(entry);
                try (InputStream is = jarFile.getInputStream(entry)) {
                    is.transferTo(jos);
                }
                jos.closeEntry();
            }
            
        } catch (IOException e) {
            Logger.print("Error saving: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        try {
            Files.deleteIfExists(gameJarPath);
            Files.move(tempJarPath, gameJarPath);
            Logger.print("Saved " + modifiedClasses.size() + " modified classes");
            modifiedClasses.clear();
        } catch (IOException e) {
            Logger.print("Error replacing jar: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
