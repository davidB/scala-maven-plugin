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
 * Grossly hacky utility class which provides the fully unpacked OSGi classpath
 * (ie. including nested .jars) when used in the context of Tycho and Maven 3.
 * The use of reflection is required to avoid wiring in a dependency on either
 * Tycho or Maven 3.
 *
 * @author miles.sabin
 */
public class TychoUtilities {
    private static final String TychoConstants_CTX_ECLIPSE_PLUGIN_CLASSPATH = "org.codehaus.tycho.TychoConstants/eclipsePluginClasspath";
    private static final Method getContextValueMethod;
    private static final Method getLocationsMethod;

    static {
        Method getContextValueMethod0 = null;
        Method getLocationsMethod0 = null;
        try {
            final Class<?> mpClazz = MavenProject.class;
            getContextValueMethod0 = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                @Override
                public Method run() throws Exception {
                    Method m = mpClazz.getDeclaredMethod("getContextValue", String.class);
                    m.setAccessible(true);
                    return m;
                }
            });

            final Class<?> cpeClazz = Class.forName("org.codehaus.tycho.ClasspathEntry");
            getLocationsMethod0 = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                @Override
                public Method run() throws Exception {
                    Method m = cpeClazz.getDeclaredMethod("getLocations");
                    m.setAccessible(true);
                    return m;
                }
            });
        } catch (ClassNotFoundException ex) {
        } catch (PrivilegedActionException ex) {
        }
        getContextValueMethod = getContextValueMethod0;
        getLocationsMethod = getLocationsMethod0;
    }

    @SuppressWarnings("unchecked")
    public static List<String> addOsgiClasspathElements(MavenProject project, List<String> defaultClasspathElements) {
        if (getLocationsMethod == null) {
            return defaultClasspathElements;
        }

        List<Object> classpath = (List<Object>) getContextValue(project, TychoConstants_CTX_ECLIPSE_PLUGIN_CLASSPATH);
        if (classpath == null || classpath.isEmpty())
            return defaultClasspathElements;

        List<String> osgiClasspath = new ArrayList<String>();
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
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        } catch (IllegalAccessException e) {
            return Collections.emptyList();
        } catch (InvocationTargetException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<File> getLocations(Object classpathEntry) {
        try {
            return (List<File>) getLocationsMethod.invoke(classpathEntry);
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        } catch (IllegalAccessException e) {
            return Collections.emptyList();
        } catch (InvocationTargetException e) {
            return Collections.emptyList();
        }
    }
}
