package tests.test32_isola18

import dejavu.Verify
import org.junit.Test
import tests.util.testcase.TestCase

class Test32 extends TestCase {
  val TEST = PATH_TO_TESTS + "/test32_isola18"
  val resultfile = s"$TEST/dejavu-results"
  val spec = s"$TEST/spec.qtl"
  val log1 = s"$TEST/log1.csv"
  val log2 = s"$TEST/log2.csv"

  @Test def test1(): Unit = {
    Verify("--specfile", spec, "--logfile", log1, "--resultfile", resultfile, "--bits", "2")
    checkResults(resultfile, 8 -- "f2",8 -- "f3")
  }
}

