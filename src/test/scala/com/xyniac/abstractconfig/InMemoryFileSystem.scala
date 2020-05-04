package com.xyniac.abstractconfig

import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import java.nio.file.{FileStore, FileSystem, Path, PathMatcher, WatchService}
import java.{lang, util}

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder

class InMemoryFileSystem extends FileSystem {
  val inMemFileStore: FileSystem = MemoryFileSystemBuilder.newLinux().build()
  InMemFsTest.fileSystemTestSuccessFlag.set(true)
  override def provider(): FileSystemProvider = inMemFileStore.provider()

  override def close(): Unit = inMemFileStore.close()

  override def isOpen: Boolean = inMemFileStore.isOpen()

  override def isReadOnly: Boolean = inMemFileStore.isReadOnly()

  override def getSeparator: String = inMemFileStore.getSeparator()

  override def getRootDirectories: lang.Iterable[Path] = inMemFileStore.getRootDirectories()

  override def getFileStores: lang.Iterable[FileStore] = inMemFileStore.getFileStores()

  override def supportedFileAttributeViews(): util.Set[String] = inMemFileStore.supportedFileAttributeViews()

  override def getPath(first: String, more: String*): Path = inMemFileStore.getPath(first, more:_*)

  override def getPathMatcher(syntaxAndPattern: String): PathMatcher = inMemFileStore.getPathMatcher(syntaxAndPattern)

  override def getUserPrincipalLookupService: UserPrincipalLookupService = inMemFileStore.getUserPrincipalLookupService()

  override def newWatchService(): WatchService = inMemFileStore.newWatchService()

  override def toString: String = "scala file system for testing purpose"
}
