package org.janelia.jacs2.asyncservice.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class FileUtils {

    public static Stream<Path> lookupFiles(Path dir, int maxDepth, String pattern) {
        try {
            PathMatcher inputFileMatcher = FileSystems.getDefault().getPathMatcher(pattern);
            return Files.find(dir, maxDepth, (p, a) -> inputFileMatcher.matches(p));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deletes the given directory even if it's non empty.
     *
     * @param dir
     * @throws IOException
     */
    public static void deletePath(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static Path getFilePath(Path dir, String fileName) {
        return dir.resolve(new File(fileName).getName());
    }

    public static Path replaceFileExt(Path filePath, String fileExt) {
        return getFilePath(filePath.getParent(), filePath.toFile().getName(), fileExt);
    }

    public static Path getFilePath(Path dir, String fileName, String fileExt) {
        return getFilePath(dir, null, fileName, null, fileExt);
    }

    public static Path getFilePath(Path dir, String prefix, String fileName, String suffix, String fileExt) {
        String actualFileName = String.format("%s%s%s.%s",
                StringUtils.defaultIfBlank(prefix, ""),
                com.google.common.io.Files.getNameWithoutExtension(fileName),
                StringUtils.defaultIfBlank(suffix, ""),
                fileExt);
        return dir.resolve(actualFileName);
    }
}
