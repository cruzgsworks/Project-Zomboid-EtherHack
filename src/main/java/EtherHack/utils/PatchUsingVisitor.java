package EtherHack.utils;

import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.function.BiConsumer;

/**
 * PatchUsingVisitor - Uses ASM MethodVisitor approach which preserves StackMapTable
 * This is more compatible with Java 25 than the Tree API approach
 */
public class PatchUsingVisitor {
    private static Path gameJarPath;
    private static final Map<String, byte[]> modifiedClasses = new HashMap<>();
    
    public static void setGameJarPath(Path path) {
        gameJarPath = path;
    }
    
    /**
     * Inject method entry code using MethodVisitor (preserves frames)
     */
    public static void injectAtMethodEntry(String className, String methodName, String methodDesc, 
                                           BiConsumer<MethodVisitor, Integer> injector) {
        Logger.print("Injecting into " + className + "." + methodName + " using MethodVisitor");
        
        try {
            byte[] classBytes = readClassFromJar(className);
            ClassReader reader = new ClassReader(classBytes);
            // Use ClassWriter(0) to preserve existing frames without computing new ones
            ClassWriter writer = new ClassWriter(0);
            
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                                String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    
                    if (name.equals(methodName) && (methodDesc == null || descriptor.equals(methodDesc))) {
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            private boolean inserted = false;
                            
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                if (!inserted) {
                                    inserted = true;
                                    injector.accept(this, access);
                                }
                            }
                        };
                    }
                    return mv;
                }
            };
            
            reader.accept(cv, 0);
            modifiedClasses.put(className, writer.toByteArray());
            
        } catch (IOException e) {
            Logger.print("Failed to inject: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Inject before RETURN instructions using MethodVisitor
     */
    public static void injectBeforeReturn(String className, String methodName, String methodDesc,
                                          BiConsumer<MethodVisitor, Label> injector) {
        Logger.print("Injecting before return in " + className + "." + methodName);
        
        try {
            byte[] classBytes = readClassFromJar(className);
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(0);
            
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    
                    if (name.equals(methodName) && (methodDesc == null || descriptor.equals(methodDesc))) {
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                                    // Insert before return
                                    Label label = new Label();
                                    injector.accept(this, label);
                                }
                                super.visitInsn(opcode);
                            }
                        };
                    }
                    return mv;
                }
            };
            
            reader.accept(cv, 0);
            modifiedClasses.put(className, writer.toByteArray());
            
        } catch (IOException e) {
            Logger.print("Failed to inject: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Simple method to modify string constants
     */
    public static void modifyStringConstant(String className, String oldValue, String newValue) {
        Logger.print("Modifying string constant in " + className);
        
        try {
            byte[] classBytes = readClassFromJar(className);
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(0);
            
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value.equals(oldValue)) {
                                super.visitLdcInsn(newValue);
                            } else {
                                super.visitLdcInsn(value);
                            }
                        }
                    };
                }
            };
            
            reader.accept(cv, 0);
            modifiedClasses.put(className, writer.toByteArray());
            
        } catch (IOException e) {
            Logger.print("Failed to modify: " + e.getMessage());
            throw new RuntimeException(e);
        }
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
                        // Write modified class
                        JarEntry newEntry = new JarEntry(entryName);
                        jos.putNextEntry(newEntry);
                        jos.write(modifiedClasses.get(className));
                        jos.closeEntry();
                        continue;
                    }
                }
                
                // Copy unmodified entry
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
    
    public static void clear() {
        modifiedClasses.clear();
    }
}
