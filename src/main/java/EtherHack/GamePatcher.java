package EtherHack;

import EtherHack.utils.Info;
import EtherHack.utils.Logger;
import EtherHack.utils.MinimalPatchTest;
import EtherHack.utils.Patch;
import EtherHack.utils.PatchUsingVisitor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Class responsible for installing and removing the cheat from game code
 */
public class GamePatcher {

    /**
     * List of all files subject to injection (jar entry paths)
     * Note: zombie/Lua/LuaManager.class temporarily removed due to bytecode issues
     */
    private final String[] patchFiles = new String[]{
            "zombie/GameWindow.class", "zombie/inventory/ItemContainer.class", "zombie/Lua/LuaEventManager.class"
    };

    /**
     * Name of the game jar file
     */
    private final String gameJarName = "projectzomboid.jar";

    /**
     * Folders and files to export to the game root directory
     */
    private final String whiteListPathEtherFiles = "EtherHack";

    /**
     * Exporting EtherHack files to the game root directory
     */
    public void extractEtherHack() {
        Logger.print("Extracting EtherHack files...");
        try {
            String jarFilePath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            // Fix Windows path (remove leading slash if present)
            if (jarFilePath.startsWith("/") && jarFilePath.charAt(2) == ':') {
                jarFilePath = jarFilePath.substring(1);
            }
            Path currentDirectory = Paths.get(System.getProperty("user.dir"));
            
            Logger.print("JAR path: " + jarFilePath);
            Logger.print("Current directory: " + currentDirectory);

            File jarFile = new File(jarFilePath);
            if (!jarFile.exists()) {
                Logger.print("ERROR: JAR file not found at: " + jarFilePath);
                return;
            }
            
            int extractedCount = 0;
            try (JarFile jar = new JarFile(jarFile)) {
                List<JarEntry> entries = jar.stream()
                        .filter((entry) -> entry.getName().startsWith(whiteListPathEtherFiles))
                        .collect(Collectors.toList());
                
                Logger.print("Found " + entries.size() + " entries to extract");
                
                for (JarEntry entry : entries) {
                    try {
                        Path extractPath = currentDirectory.resolve(entry.getName());

                        if (entry.isDirectory()) {
                            Files.createDirectories(extractPath);
                        } else {
                            Files.createDirectories(extractPath.getParent());

                            try (InputStream inputStream = jar.getInputStream(entry)) {
                                Files.copy(inputStream, extractPath, StandardCopyOption.REPLACE_EXISTING);
                                extractedCount++;
                                Logger.print("Extracted: " + entry.getName());
                            }
                        }
                    } catch (IOException e) {
                        Logger.print("ERROR extracting " + entry.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                Logger.print("Extraction completed successfully. Extracted " + extractedCount + " files.");
            }
        } catch (URISyntaxException | IOException e) {
            Logger.print("ERROR during extraction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Delete all exported EtherHack files from the game directory
     */
    public void uninstallEtherHackFiles() {
        Logger.print("Deleting all EtherHack files...");

        try {
            Path currentDirectory = Paths.get(System.getProperty("user.dir"));
            Path targetPath = currentDirectory.resolve(whiteListPathEtherFiles);
            if (Files.exists(targetPath)) {
                Files.walk(targetPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            Logger.print("Deletion EtherHack files completed successfully");
        } catch (IOException except) {
            except.printStackTrace();
        }
    }

    /**
     * Creates backup copies of game files if they don't already exist.
     * Files will be saved with .bkup extension in the same folder as originals.
     */
    public void backupGameFiles() {
        Path currentPath = Paths.get("").toAbsolutePath();
        Path gameJarPath = currentPath.resolve(gameJarName);

        Logger.print("Creating a backup of the game jar...");

        if (Files.exists(gameJarPath)) {
            try {
                Path backupFilePath = Paths.get(currentPath.toString(), gameJarName + ".bkup");

                if (Files.exists(backupFilePath)) {
                    Logger.print("Backup of the game jar already exists. Skipping backup.");
                } else {
                    Files.copy(gameJarPath, backupFilePath);
                    Logger.print("Backup created successfully");
                }
            } catch (IOException e) {
                Logger.print("Error while creating backup file: " + e.getMessage());
            }
        } else {
            Logger.print(gameJarName + " file not found.");
        }

        Logger.print("Backups of game files have been completed!");
    }

    /**
     * Injection into the game window file
     */
    public void patchGameWindow() {
        Patch.injectIntoClass("zombie/GameWindow", "InitDisplay", true, (method) -> {
            String oldTitle = "Project Zomboid";
            String newTitle = "Project Zomboid" + Info.CHEAT_WINDOW_TITLE_SUFFIX;
            AbstractInsnNode[] nodes = method.instructions.toArray();

            for (AbstractInsnNode insn : nodes) {
                if (insn instanceof LdcInsnNode ldcInsnNode) {
                    if (ldcInsnNode.cst.equals(oldTitle)) {
                        ldcInsnNode.cst = newTitle;
                    }
                }
            }
        });

        Patch.injectIntoClass("zombie/GameWindow", "init", true, (method) -> {
            AbstractInsnNode insertionPoint = null;

            // Find the point of injection
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC
                            && methodInsn.owner.equals("zombie/Lua/LuaManager")
                            && methodInsn.name.equals("init")) {
                        insertionPoint = insn;
                        break;
                    }
                }
            }

            if (insertionPoint != null) {
                InsnList initEtherLuaInstructions = new InsnList();
                initEtherLuaInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherLuaCompiler", "getInstance", "()LEtherHack/Ether/EtherLuaCompiler;", false));
                initEtherLuaInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherHack/Ether/EtherLuaCompiler", "init", "()V", false));
                method.instructions.insert(insertionPoint, initEtherLuaInstructions);
            } else {
                throw new IllegalStateException("Cannot find LuaManager.init() invocation in the method when patching the Game window");
            }

            AbstractInsnNode lastInsn = method.instructions.getLast();
            if (lastInsn != null) {
                InsnList initLogoInstructions = new InsnList();
                initLogoInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherLogo", "getInstance", "()LEtherHack/Ether/EtherLogo;", false));
                initLogoInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherHack/Ether/EtherLogo", "init", "()V", false));
                method.instructions.insertBefore(lastInsn, initLogoInstructions);

                InsnList initEtherInstructions = new InsnList();
                initEtherInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                initEtherInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherHack/Ether/EtherMain", "init", "()V", false));
                method.instructions.insertBefore(lastInsn, initEtherInstructions);
            } else {
                throw new IllegalStateException("Could not find the end of the method when patching the Game window");
            }
        });
    }

    /**
     * Injection into game item files
     */
    public void patchItemContainer() {
        // Note: getWeight() no longer exists in PZ 42.x. Removed this patch.
        // The unlimited carry functionality relies on getCapacityWeight and getContentsWeight patches.

        Patch.injectIntoClass("zombie/inventory/ItemContainer", "getCapacityWeight", false, (method) -> {
            InsnList newInstructions = new InsnList();
            LabelNode carryOnLabel = new LabelNode();
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
            newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, carryOnLabel));
            newInstructions.add(new InsnNode(Opcodes.FCONST_0));
            newInstructions.add(new InsnNode(Opcodes.FRETURN));
            newInstructions.add(carryOnLabel);
            method.instructions.insert(newInstructions);
        });

        Patch.injectIntoClass("zombie/inventory/ItemContainer", "getContentsWeight", false, (method) -> {
            InsnList newInstructions = new InsnList();
            LabelNode carryOnLabel = new LabelNode();
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
            newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, carryOnLabel));
            newInstructions.add(new InsnNode(Opcodes.FCONST_0));
            newInstructions.add(new InsnNode(Opcodes.FRETURN));
            newInstructions.add(carryOnLabel);
            method.instructions.insert(newInstructions);
        });
    }

    /**
     * Injection into LuaEventManager file
     */
    public void patchLuaEventManager() {
        Patch.injectIntoClass("zombie/Lua/LuaEventManager", "triggerEvent", true, (method) -> {
            InsnList toInject = new InsnList();
            toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
            toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/utils/EventSubscriber", "invokeSubscriber", "(Ljava/lang/String;)V", false));
            method.instructions.insertBefore(method.instructions.get(0), toInject);
        });
    }

    /**
     * Injection into LuaManager file
     * TEMPORARILY DISABLED - causing bytecode verification errors
     * TODO: Fix StackMapTable handling for this injection
     */
    public void patchLuaManager() {
        // Disabled due to VerifyError - Lua compiler bypass not critical for stats restoration
        Logger.print("Skipping LuaManager injection (disabled due to bytecode issues)");
        /*
        Patch.injectIntoClass("zombie/Lua/LuaManager", "RunLua", true, (method) -> {
            if (!method.desc.equals("(Ljava/lang/String;Z)Ljava/lang/Object;")) {
                return;
            }

            InsnList newInstructions = new InsnList();
            LabelNode endOfMethodLabel = new LabelNode();

            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherLuaCompiler", "getInstance", "()LEtherHack/Ether/EtherLuaCompiler;", false));
            newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            newInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherHack/Ether/EtherLuaCompiler", "isShouldLuaCompile", "(Ljava/lang/String;)Z", false));

            newInstructions.add(new JumpInsnNode(Opcodes.IFNE, endOfMethodLabel));

            newInstructions.add(new InsnNode(Opcodes.ACONST_NULL));
            newInstructions.add(new InsnNode(Opcodes.ARETURN));

            newInstructions.add(endOfMethodLabel);

            method.instructions.insert(newInstructions);
        });
        */
    }

    /**
     * Checks if at least one of the specified files contains the @Injected annotation.
     * @return true if the @Injected annotation is found in at least one file, false otherwise.
     */
    public boolean checkInjectedAnnotations() {
        return Arrays.stream(patchFiles)
                .anyMatch(filePath -> Patch.isInjectedAnnotationPresent(filePath, ""));
    }

    /**
     * Checks for the presence of the game jar.
     * @return true if the game jar is present, false otherwise.
     */
    public boolean isGameFolder() {
        Path gameJarPath = Paths.get(gameJarName);
        return Files.exists(gameJarPath) && !Files.isDirectory(gameJarPath);
    }

    /**
     * Patching game bytecode files to implement custom functionality
     * Uses MethodVisitor approach for Java 25 compatibility
     */
    public void patchGame() {
        Logger.printCredits();

        Logger.print("Preparing to install the EtherHack...");

        if (!isGameFolder()) {
            Logger.print("No game files were found in this directory. Place the cheat in the root folder of the game");
            return;
        }

        Logger.print("Checking for injections in game files");
        if (checkInjectedAnnotations()) {
            Logger.print("The game files have already been modified. Uninstall the previous version of the cheat first");
            return;
        }

        Logger.print("Preparation for injection into game file...");
        
        // Set up the patcher
        PatchUsingVisitor.setGameJarPath(Paths.get(gameJarName).toAbsolutePath());
        
        // Use minimal test patching approach
        MinimalPatchTest.setGameJarPath(Paths.get(gameJarName).toAbsolutePath());
        MinimalPatchTest.testPatchItemContainer();
        MinimalPatchTest.saveModifiedClasses();
        // patchStatsUsingVisitor() - Disabled: needs careful stack manipulation
        // patchLuaEventManager() - Skip for now, needs special handling
        // patchLuaManager() - Already disabled
        
        PatchUsingVisitor.saveModifiedClasses();

        Logger.print("Modified classes saved to game jar successfully");
        Logger.print("The injections were completed!");

        Logger.print("Extracting EtherHack files to the current directory...");
        extractEtherHack();
        Logger.print("Extraction completed successfully");

        Logger.print("The cheat installation is complete, you can enter the game!");
    }
    
    /**
     * Patch GameWindow using MethodVisitor (Java 25 compatible)
     */
    private void patchGameWindowUsingVisitor() {
        Logger.print("Patching GameWindow using MethodVisitor...");
        
        // Modify window title
        PatchUsingVisitor.modifyStringConstant(
            "zombie/GameWindow", 
            "Project Zomboid", 
            "Project Zomboid" + Info.CHEAT_WINDOW_TITLE_SUFFIX
        );
        
        // Inject EtherHack init at the end of init() method
        PatchUsingVisitor.injectBeforeReturn("zombie/GameWindow", "init", "()V", (mv, label) -> {
            // Call EtherMain.getInstance().init()
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance", 
                "()LEtherHack/Ether/EtherMain;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "EtherHack/Ether/EtherMain", "init", "()V", false);
        });
    }
    
    /**
     * Patch ItemContainer using MethodVisitor (Java 25 compatible)
     */
    private void patchItemContainerUsingVisitor() {
        Logger.print("Patching ItemContainer using MethodVisitor...");
        
        // Inject unlimited carry check at start of getCapacityWeight
        PatchUsingVisitor.injectAtMethodEntry("zombie/inventory/ItemContainer", "getCapacityWeight", "()F", (mv, access) -> {
            // Check if unlimited carry is enabled
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance",
                "()LEtherHack/Ether/EtherMain;", false);
            mv.visitFieldInsn(Opcodes.GETFIELD, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;");
            mv.visitFieldInsn(Opcodes.GETFIELD, "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z");
            
            Label continueLabel = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, continueLabel);
            
            // Return 0.0f if unlimited carry enabled
            mv.visitInsn(Opcodes.FCONST_0);
            mv.visitInsn(Opcodes.FRETURN);
            
            mv.visitLabel(continueLabel);
        });
        
        // Same for getContentsWeight
        PatchUsingVisitor.injectAtMethodEntry("zombie/inventory/ItemContainer", "getContentsWeight", "()F", (mv, access) -> {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EtherHack/Ether/EtherMain", "getInstance",
                "()LEtherHack/Ether/EtherMain;", false);
            mv.visitFieldInsn(Opcodes.GETFIELD, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;");
            mv.visitFieldInsn(Opcodes.GETFIELD, "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z");
            
            Label continueLabel = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, continueLabel);
            
            mv.visitInsn(Opcodes.FCONST_0);
            mv.visitInsn(Opcodes.FRETURN);
            
            mv.visitLabel(continueLabel);
        });
    }
    
    /**
     * Patch Stats class using hook injection (Java 25 compatible)
     * Hooks Stats.set() and Stats.add() to intercept stat modifications
     */
    private void patchStatsUsingVisitor() {
        Logger.print("Patching Stats class using hook injection...");
        
        // Hook Stats.set(CharacterStat, float) method
        // Inject: value = StatsHook.interceptStatSet(stat, value);
        PatchUsingVisitor.injectAtMethodEntry("zombie/characters/Stats", "set", 
            "(Lzombie/characters/CharacterStat;F)Z", (mv, access) -> {
            // Stack: [stat, value]
            // Dup the arguments for the hook call
            mv.visitInsn(Opcodes.DUP2); // [stat, value, stat, value]
            
            // Call StatsHook.interceptStatSet(stat, value)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EtherHack/Ether/StatsHook", "interceptStatSet",
                "(Lzombie/characters/CharacterStat;F)F", false);
            
            // Stack: [stat, value, newValue]
            // We need to replace 'value' with 'newValue' on the stack
            // Swap to get: [stat, newValue, value]
            mv.visitInsn(Opcodes.SWAP);
            // Pop the old value
            mv.visitInsn(Opcodes.POP);
            // Stack: [stat, newValue]
            // Now the method continues with the modified value
        });
        
        // Hook Stats.add(CharacterStat, float) method  
        PatchUsingVisitor.injectAtMethodEntry("zombie/characters/Stats", "add",
            "(Lzombie/characters/CharacterStat;F)Z", (mv, access) -> {
            // Similar hook for add method
            mv.visitInsn(Opcodes.DUP2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EtherHack/Ether/StatsHook", "interceptStatAdd",
                "(Lzombie/characters/CharacterStat;F)F", false);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitInsn(Opcodes.POP);
        });
    }

    /**
     * Restoring original game files
     */
    public void restoreFiles() {
        Logger.printCredits();
        Logger.print("Restoring files...");
        Path currentPath = Paths.get("").toAbsolutePath();

        Path originalFilePath = currentPath.resolve(gameJarName);
        Path backupFilePath = Paths.get(originalFilePath.toString() + ".bkup");

        if (Files.exists(backupFilePath)) {
            try {
                if (Files.exists(originalFilePath)) {
                    Files.delete(originalFilePath);
                }
                Files.move(backupFilePath, originalFilePath);
                Logger.print("Game jar restored successfully");
            } catch (IOException e) {
                Logger.print("Error when restoring the game jar: " + e.getMessage());
            }
        } else {
            Logger.print("Backup file '" + gameJarName + ".bkup' not found. Skipping restore");
        }

        Logger.print("Files restoration completed!");
        uninstallEtherHackFiles();
    }
}
