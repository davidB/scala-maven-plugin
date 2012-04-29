package sbt_inc;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sbt.inc.Locate;
import xsbti.compile.DefinesClass;

public class SbtLocate {

    public static DefinesClass NeverDefinesClass = new FalseDefinesClass();

    public static DefinesClass definesClass(File file) {
        if (file.isDirectory()) {
            return new DirectoryDefinesClass(file);
        } else if (file.exists() && sbt.classpath.ClasspathUtilities.isArchive(file)) {
            return new JarDefinesClass(file);
        } else {
            return NeverDefinesClass;
        }
    }

    public static class JarDefinesClass implements DefinesClass {

        private File file;
        private HashSet entries = new HashSet<String>();

        public JarDefinesClass(File file) {
            this.file = file;
            try {
                ZipFile jar = new ZipFile(file, ZipFile.OPEN_READ);
                try {
                    for (Enumeration<? extends ZipEntry> e = jar.entries(); e.hasMoreElements();) {
                        entries.add(Locate.toClassName(e.nextElement().getName()));
                    }
                } finally {
                    jar.close();
                }
            } catch (java.io.IOException e) {
                // currently ignored
            }
        }

        public boolean apply(String className) {
            return entries.contains(className);
        }
    }

    public static class DirectoryDefinesClass implements DefinesClass {

        private File file;

        public DirectoryDefinesClass(File file) {
            this.file = file;
        }

        public boolean apply(String className) {
            return Locate.classFile(file, className).isFile();
        }
    }

    public static class FalseDefinesClass implements DefinesClass {

        public boolean apply(String className) {
            return false;
        }
    }
}
