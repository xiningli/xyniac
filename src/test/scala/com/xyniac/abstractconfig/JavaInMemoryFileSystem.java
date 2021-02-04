package com.xyniac.abstractconfig;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public class JavaInMemoryFileSystem extends FileSystem {
    private FileSystem inMemFileStore;


    public JavaInMemoryFileSystem()  {
        try {
            inMemFileStore = MemoryFileSystemBuilder.newEmpty().build();
            Files.createDirectory(inMemFileStore.getPath("/conf"));
            String target = "{\n" +
                    "  \"fileSystemFullyQualifiedName\": \"com.xyniac.abstractconfig.InMemoryFileSystem\",\n" +
                    "  \"initialDelay\": 3000,\n" +
                    "  \"delay\": 10000\n" +
                    "}\n" +
                    "\n";
            Files.write(inMemFileStore.getPath("/conf", "com.xyniac.abstractconfig.RemoteConfig$"), target.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public FileSystemProvider provider() {
        return inMemFileStore.provider();
    }

    @Override
    public void close() throws IOException {
//        InMemFsTest.fsCloseFlag().set(true);
        inMemFileStore.close();
    }

    @Override
    public boolean isOpen() {
        return inMemFileStore.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return inMemFileStore.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return inMemFileStore.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return inMemFileStore.getRootDirectories();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return inMemFileStore.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return inMemFileStore.supportedFileAttributeViews();
    }

    @Override
    public Path getPath(String first, String... more) {
        return inMemFileStore.getPath(first, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return inMemFileStore.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return inMemFileStore.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return inMemFileStore.newWatchService();
    }

    @Override
    public String toString() {
        return "Java in memory file system used for testing";
    }
}
