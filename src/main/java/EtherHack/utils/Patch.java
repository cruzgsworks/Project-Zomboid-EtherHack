package EtherHack.utils;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class Patch {
    private static Path gameJarPath;
    private static final Map<String, ClassNode> classNodeMap = new HashMap<>();

    public static void setGameJarPath(Path path) {
        gameJarPath = path;
    }

    public static Path getGameJarPath() {
        return gameJarPath;
    }

    public static void injectIntoClass(String className, String methodName, boolean isStatic, Consumer<MethodNode> injector) {
        Logger.print("Injection into a game file '" + className + "' in method: '" + methodName + "'");

        ClassNode classNode = classNodeMap.computeIfAbsent(className, key -> {
            try {
                byte[] classBytes = readClassFromJar(key);
                ClassReader reader = new ClassReader(classBytes);
                ClassNode node = new ClassNode();
                // Use EXPAND_FRAMES to expand the frame structure
                // This makes it easier to modify methods without corrupting frames
                reader.accept(node, ClassReader.EXPAND_FRAMES);
                return node;
            } catch (IOException e) {
                Logger.print("Failed to read class: " + e.getMessage());
                return null;
            }
        });

        if (classNode == null) {
            throw new RuntimeException("Failed to load class " + className);
        }

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(methodName) && Modifier.isStatic(methodNode.access) == isStatic) {
                if (!hasInjectedAnnotation(methodNode)) {
                    addInjectAnnotation(classNode, methodName);
                }
                injector.accept(methodNode);
            }
        }

        classNodeMap.put(className, classNode);
    }

    private static byte[] readClassFromJar(String className) throws IOException {
        if (gameJarPath == null) {
            throw new IllegalStateException("Game jar path not set");
        }
        String entryName = className + ".class";
        try (JarFile jarFile = new JarFile(gameJarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                throw new IOException("Class not found in jar: " + entryName);
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                return is.readAllBytes();
            }
        }
    }

    public static boolean isInjectedAnnotationPresent(String classFilePath, String baseDir) {
        if (gameJarPath == null) {
            return false;
        }
        // Normalize to jar entry path
        String entryName = classFilePath.replace('\\', '/');
        if (!entryName.endsWith(".class")) {
            entryName = entryName + ".class";
        }

        try (JarFile jarFile = new JarFile(gameJarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                return false;
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                ClassReader reader = new ClassReader(is);
                boolean[] found = new boolean[]{false};
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                if (descriptor.equals("LEtherHack/annotations/Injected;")) {
                                    found[0] = true;
                                }
                                return super.visitAnnotation(descriptor, visible);
                            }
                        };
                    }
                }, 0);
                return found[0];
            }
        } catch (IOException e) {
            Logger.print("Error checking for injected annotations: " + e.getMessage());
            return false;
        }
    }

    private static void addInjectAnnotation(ClassNode classNode, String methodName) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodName)) {
                if (method.visibleAnnotations == null) {
                    method.visibleAnnotations = new LinkedList<>();
                }
                boolean hasAnnotation = method.visibleAnnotations.stream()
                        .anyMatch(anno -> anno.desc.equals("LEtherHack/annotations/Injected;"));
                if (!hasAnnotation) {
                    method.visibleAnnotations.add(new AnnotationNode("LEtherHack/annotations/Injected;"));
                }
                return;
            }
        }
    }

    private static boolean hasInjectedAnnotation(MethodNode method) {
        if (method.visibleAnnotations == null) {
            return false;
        }
        return method.visibleAnnotations.stream()
                .anyMatch(anno -> anno.desc.equals("LEtherHack/annotations/Injected;"));
    }

    public static void saveModifiedClasses() {
        if (gameJarPath == null) {
            throw new IllegalStateException("Game jar path not set");
        }

        Path tempJarPath = gameJarPath.resolveSibling(gameJarPath.getFileName().toString() + ".tmp");

        try (JarFile jarFile = new JarFile(gameJarPath.toFile());
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJarPath.toFile()))) {

            Enumeration<JarEntry> entries = jarFile.entries();
            Set<String> modifiedEntries = new HashSet<>(classNodeMap.keySet());

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                String className = entryName.endsWith(".class") ? entryName.substring(0, entryName.length() - 6) : null;

                if (className != null && modifiedEntries.contains(className)) {
                    ClassNode classNode = classNodeMap.get(className);
                    // Use COMPUTE_MAXS to recalculate stack sizes but preserve frame structure
                    // This is safer than COMPUTE_FRAMES which can corrupt complex bytecode
                    ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    byte[] bytes = writer.toByteArray();

                    JarEntry newEntry = new JarEntry(entryName);
                    jos.putNextEntry(newEntry);
                    jos.write(bytes);
                    jos.closeEntry();
                    modifiedEntries.remove(className);
                } else {
                    jos.putNextEntry(entry);
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        is.transferTo(jos);
                    }
                    jos.closeEntry();
                }
            }

            // Add any new classes not originally in jar
            for (String className : modifiedEntries) {
                ClassNode classNode = classNodeMap.get(className);
                ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(writer);
                byte[] bytes = writer.toByteArray();

                JarEntry newEntry = new JarEntry(className + ".class");
                jos.putNextEntry(newEntry);
                jos.write(bytes);
                jos.closeEntry();
            }

        } catch (IOException e) {
            Logger.print("Error saving modified classes: " + e.getMessage());
            throw new RuntimeException(e);
        }

        try {
            Files.deleteIfExists(gameJarPath);
            Files.move(tempJarPath, gameJarPath);
            Logger.print("Modified classes saved to game jar successfully");
        } catch (IOException e) {
            Logger.print("Error replacing game jar: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Safe ClassWriter that doesn't try to load classes for frame computation.
     * Falls back to Object when class hierarchy cannot be determined.
     */
    private static class SafeClassWriter extends ClassWriter {
        public SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            } catch (Exception e) {
                // If we can't determine common superclass, return Object
                return "java/lang/Object";
            }
        }
    }
}
