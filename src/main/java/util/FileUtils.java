package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class FileUtils {

    private FileUtils() {
    }

    public static List<File> listDirectoryContent(Path directory, Function<File, Boolean> filter) throws IOException {
        List<File> files = new ArrayList<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                File f = file.toFile();
                if (filter.apply(f)) {
                    files.add(f);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                File f = dir.toFile();
                if (!dir.equals(directory) && filter.apply(f)) {
                    files.add(f);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    public static void deleteDirectory(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            // life...
        }
    }
}
