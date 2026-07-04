package info.mengnan.dialogerai.common.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Classpath 扫描工具：支持按包名扫描类、按路径前缀扫描资源
 */
public final class ClasspathScanner {

    private ClasspathScanner() {
    }

    public static ClassLoader contextClassLoader(Class<?> anchor) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader != null ? classLoader : anchor.getClassLoader();
    }

    /**
     * 扫描包下所有顶层类（跳过内部类），并对每个类执行 action
     */
    public static void forEachClass(String packageName, Consumer<Class<?>> action) {
        forEachClass(packageName, contextClassLoader(ClasspathScanner.class), action);
    }

    public static void forEachClass(String packageName, ClassLoader classLoader, Consumer<Class<?>> action) {
        try {
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    File directory = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8));
                    scanClassDirectory(directory, packageName, classLoader, action);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanClassJar(resource, path, packageName, classLoader, action);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to scan package: " + packageName, ex);
        }
    }

    /**
     * 扫描 classpath 下指定前缀的资源，并对匹配后缀的条目执行 action
     */
    public static void forEachResource(String locationPrefix, String suffix,
                                         BiConsumer<String, InputStream> action) {
        forEachResource(locationPrefix, suffix, contextClassLoader(ClasspathScanner.class), action);
    }

    public static void forEachResource(String locationPrefix, String suffix, ClassLoader classLoader,
                                         BiConsumer<String, InputStream> action) {
        try {
            Enumeration<URL> resources = classLoader.getResources(locationPrefix);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    scanResourceDirectory(resource, locationPrefix, suffix, action);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanResourceJar(resource, locationPrefix, suffix, classLoader, action);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to scan resources: " + locationPrefix, ex);
        }
    }

    public static JarFile openJar(URL jarUrl) throws IOException {
        JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
        connection.setUseCaches(false);
        return connection.getJarFile();
    }

    private static void scanClassDirectory(File directory, String packageName, ClassLoader classLoader,
                                           Consumer<Class<?>> action) {
        if (!directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanClassDirectory(file, packageName + "." + file.getName(), classLoader, action);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                loadClass(className, classLoader, action);
            }
        }
    }

    private static void scanClassJar(URL resource, String path, String packageName, ClassLoader classLoader,
                                     Consumer<Class<?>> action) throws IOException {
        try (JarFile jarFile = openJar(resource)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith(path) || !entryName.endsWith(".class") || entryName.contains("$")) {
                    continue;
                }
                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                if (!className.startsWith(packageName)) {
                    continue;
                }
                loadClass(className, classLoader, action);
            }
        }
    }

    private static void loadClass(String className, ClassLoader classLoader, Consumer<Class<?>> action) {
        try {
            action.accept(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to load class: " + className, ex);
        }
    }

    private static void scanResourceDirectory(URL dirUrl, String locationPrefix, String suffix,
                                              BiConsumer<String, InputStream> action) throws IOException {
        File dir = new File(dirUrl.getPath());
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(suffix));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String entryName = locationPrefix + file.getName();
            try (InputStream inputStream = file.toURI().toURL().openStream()) {
                action.accept(entryName, inputStream);
            }
        }
    }

    private static void scanResourceJar(URL dirUrl, String locationPrefix, String suffix, ClassLoader classLoader,
                                        BiConsumer<String, InputStream> action) throws IOException {
        try (JarFile jar = openJar(dirUrl)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(locationPrefix) || !name.endsWith(suffix) || entry.isDirectory()) {
                    continue;
                }
                try (InputStream inputStream = classLoader.getResourceAsStream(name)) {
                    if (inputStream != null) {
                        action.accept(name, inputStream);
                    }
                }
            }
        }
    }
}
