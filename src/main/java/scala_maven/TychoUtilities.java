package scala_maven;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;

/**
 * Grossly hacky utility class which provides the fully unpacked OSGI classpath
 * (ie. including nested .jars) when used in the context of Tycho and Maven 3.
 * The use of reflection is required to avoid wiring in a dependency on either
 * Tycho or Maven 3.
 *
 * @author miles.sabin
 */
class TychoUtilities {
    private static final String TychoConstants_CTX_ECLIPSE_PLUGIN_CLASSPATH = "org.eclipse.tycho.core.TychoConstants/eclipsePluginClasspath";
    private static final Method getContextValueMethod;
    private static final Method getLocationsMethod;

    static {
        Method getContextValueMethod0 = null;
        Method getLocationsMethod0 = null;
        try {
            final Class<?> mpClazz = MavenProject.class;
            getContextValueMethod0 = AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> {
                Method m = mpClazz.getDeclaredMethod("getContextValue", String.class);
                m.setAccessible(true);
                return m;
            });

            final Class<?> cpeClazz = Class.forName("org.codehaus.tycho.ClasspathEntry");
            getLocationsMethod0 = AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> {
                Method m = cpeClazz.getDeclaredMethod("getLocations");
                m.setAccessible(true);
                return m;
            });
        } catch (ClassNotFoundException | PrivilegedActionException ex) {
        }
        getContextValueMethod = getContextValueMethod0;
        getLocationsMethod = getLocationsMethod0;
    }

    @SuppressWarnings("unchecked")
    static List<String> addOsgiClasspathElements(MavenProject project, List<String> defaultClasspathElements) {
        if (getLocationsMethod == null) {
            return defaultClasspathElements;
        }

        List<Object> classpath = (List<Object>) getContextValue(project, TychoConstants_CTX_ECLIPSE_PLUGIN_CLASSPATH);
        if (classpath == null || classpath.isEmpty())
            return defaultClasspathElements;

        List<String> osgiClasspath = new ArrayList<>();
        for (Object classpathEntry : classpath) {
            for (File file : getLocations(classpathEntry))
                osgiClasspath.add(file.getAbsolutePath());
        }
        osgiClasspath.addAll(defaultClasspathElements);
        return osgiClasspath;
    }

    private static Object getContextValue(MavenProject project, String key) {
        try {
            return getContextValueMethod.invoke(project, key);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<File> getLocations(Object classpathEntry) {
        try {
            return (List<File>) getLocationsMethod.invoke(classpathEntry);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            return Collections.emptyList();
        }
    }
}
