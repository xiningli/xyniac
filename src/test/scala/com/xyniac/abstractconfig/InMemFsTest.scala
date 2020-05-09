package com.xyniac.abstractconfig

import java.util.concurrent.atomic.AtomicBoolean
import org.scalatest.FunSuite

object InMemFsTest {
  val fileSystemTestSuccessFlag = new AtomicBoolean(false)
  val fsCloseFlag = new AtomicBoolean(false)

}

class InMemFsTest extends FunSuite {
  test("test correctly reload the file system") {
    val fs = new JavaInMemoryFileSystem
    TestAbstractConfig.getName()
    Thread.sleep(30000)
    assert(InMemFsTest.fileSystemTestSuccessFlag.get())
    assert(InMemFsTest.fsCloseFlag.get())
  }

}
