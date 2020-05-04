package com.xyniac.abstractconfig;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class InMemoryFileSystemTest {
    @Test
    public void test() throws IOException {
        JavaInMemoryFileSystem fs = new JavaInMemoryFileSystem();
        Path p = fs.getPath("p");
        Files.createDirectory(fs.getPath("test0"));
        Files.createDirectory(fs.getPath("conf"));

        Stream<Path> ls = Files.list(fs.getPath(""));
        ls.forEach(System.out::println);
//        System.out.println();

    }
}
