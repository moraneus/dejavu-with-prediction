package tests.test60_prediction

import dejavu.Verify
import org.junit.Test
import tests.util.testcase.TestCase

/**
  * This test should not run the prediction since some of
  * the predicates in the specification get more than one value.
  */
class Test60 extends TestCase {
  val TEST: String = PATH_TO_TESTS + "/test60_prediction"
  val resultfile = s"$TEST/dejavu-results"
  val spec1 = s"$TEST/spec1.qtl"
  val spec2 = s"$TEST/spec2.qtl"
  val spec3 = s"$TEST/spec3.qtl"
  val spec4 = s"$TEST/spec4.qtl"
  val log1 = s"$TEST/log1.csv"
  val log2 = s"$TEST/log2.csv"
  val log3 = s"$TEST/log3.csv"


  @Test def test1_1(): Unit = {
    Verify("--execution", "0", "--specfile", spec1, "--logfile", log1, "--resultfile",
           resultfile, "--bits", "20", "--prediction", "1", "--prediction_type", "smart")
    val expected = List[String](
      "1",
      "open(f11)=1",
      "close(f11)=1",
      "write(f11,a1)=0",
      "write(f11,a)=0",
      "open(f1)=1",
      "close(f1)=1",
      "write(f1,a1)=0",
      "write(f1,a)=0"
    )
    checkResults(resultfile, expected: _*)
  }

  @Test def test1_2(): Unit = {
    Verify("--execution", "0", "--specfile", spec1, "--logfile", log1, "--resultfile", resultfile, "--bits", "7", "--prediction", "2", "--prediction_type", "smart")
    val expected = List[String](
      "1",
      "open(f11)=1;open(f1)=1",
      "open(f11)=1;close(f1)=1",
      "open(f11)=1;write(f1,a)=0",
      "open(f11)=1;open(f11)=1",
      "open(f11)=1;close(f11)=1",
      "open(f11)=1;write(f11,a)=1",
      "close(f11)=1;open(f1)=1",
      "close(f11)=1;close(f1)=1",
      "close(f11)=1;write(f1,a)=0",
      "close(f11)=1;open(f11)=1",
      "close(f11)=1;close(f11)=1",
      "close(f11)=1;write(f11,a)=0",
      "write(f11,a1)=0;open(f1)=1",
      "write(f11,a1)=0;close(f1)=1",
      "write(f11,a1)=0;write(f1,a)=0",
      "write(f11,a1)=0;write(f1,a1)=0",
      "write(f11,a1)=0;open(f11)=1",
      "write(f11,a1)=0;close(f11)=1",
      "write(f11,a1)=0;write(f11,a)=0",
      "write(f11,a1)=0;write(f11,a1)=0",
      "write(f11,a)=0;open(f1)=1",
      "write(f11,a)=0;close(f1)=1",
      "write(f11,a)=0;write(f1,a1)=0",
      "write(f11,a)=0;write(f1,a)=0",
      "write(f11,a)=0;open(f11)=1",
      "write(f11,a)=0;close(f11)=1",
      "write(f11,a)=0;write(f11,a1)=0",
      "write(f11,a)=0;write(f11,a)=0",
      "open(f1)=1;open(f11)=1",
      "open(f1)=1;close(f11)=1",
      "open(f1)=1;write(f11,a)=0",
      "open(f1)=1;open(f1)=1",
      "open(f1)=1;close(f1)=1",
      "open(f1)=1;write(f1,a)=1",
      "close(f1)=1;open(f11)=1",
      "close(f1)=1;close(f11)=1",
      "close(f1)=1;write(f11,a)=0",
      "close(f1)=1;open(f1)=1",
      "close(f1)=1;close(f1)=1",
      "close(f1)=1;write(f1,a)=0",
      "write(f1,a1)=0;open(f11)=1",
      "write(f1,a1)=0;close(f11)=1",
      "write(f1,a1)=0;write(f11,a)=0",
      "write(f1,a1)=0;write(f11,a1)=0",
      "write(f1,a1)=0;open(f1)=1",
      "write(f1,a1)=0;close(f1)=1",
      "write(f1,a1)=0;write(f1,a)=0",
      "write(f1,a1)=0;write(f1,a1)=0",
      "write(f1,a)=0;open(f11)=1",
      "write(f1,a)=0;close(f11)=1",
      "write(f1,a)=0;write(f11,a1)=0",
      "write(f1,a)=0;write(f11,a)=0",
      "write(f1,a)=0;open(f1)=1",
      "write(f1,a)=0;close(f1)=1",
      "write(f1,a)=0;write(f1,a1)=0",
      "write(f1,a)=0;write(f1,a)=0"
    )
    checkResults(resultfile, expected: _*)
  }

  @Test def test2_1(): Unit = {
    Verify("--execution", "0", "--specfile", spec2, "--logfile", log2, "--resultfile",
      resultfile, "--bits", "8", "--prediction", "2", "--prediction_type", "smart")
    val expected = List[String](
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "open(f91)=1;open(f911)=1",
      "open(f91)=1;write(f911,1)=0",
      "open(f91)=1;open(f7)=1",
      "open(f91)=1;write(f7,1)=1",
      "open(f91)=1;open(f91)=1",
      "open(f91)=1;write(f91,1)=1",
      "write(f91,11)=1;open(f911)=1",
      "write(f91,11)=1;write(f911,1)=1",
      "write(f91,11)=1;write(f911,11)=1",
      "write(f91,11)=1;open(f7)=1",
      "write(f91,11)=1;write(f7,1)=1",
      "write(f91,11)=1;write(f7,11)=1",
      "write(f91,11)=1;open(f91)=1",
      "write(f91,11)=1;write(f91,1)=1",
      "write(f91,11)=1;write(f91,11)=1",
      "write(f91,1)=0;open(f911)=1",
      "write(f91,1)=0;write(f911,11)=1",
      "write(f91,1)=0;write(f911,1)=0",
      "write(f91,1)=0;open(f7)=1",
      "write(f91,1)=0;write(f7,11)=1",
      "write(f91,1)=0;write(f7,1)=1",
      "write(f91,1)=0;open(f91)=1",
      "write(f91,1)=0;write(f91,11)=1",
      "write(f91,1)=0;write(f91,1)=0",
      "open(f7)=1;open(f91)=1",
      "open(f7)=1;write(f91,1)=0",
      "open(f7)=1;open(f7)=1",
      "open(f7)=1;write(f7,1)=1",
      "open(f7)=1;open(f6)=1",
      "open(f7)=1;write(f6,1)=1",
      "write(f7,11)=1;open(f91)=1",
      "write(f7,11)=1;write(f91,1)=1",
      "write(f7,11)=1;write(f91,11)=1",
      "write(f7,11)=1;open(f7)=1",
      "write(f7,11)=1;write(f7,1)=1",
      "write(f7,11)=1;write(f7,11)=1",
      "write(f7,11)=1;open(f6)=1",
      "write(f7,11)=1;write(f6,1)=1",
      "write(f7,11)=1;write(f6,11)=1",
      "write(f7,1)=1;open(f91)=1",
      "write(f7,1)=1;write(f91,11)=1",
      "write(f7,1)=1;write(f91,1)=0",
      "write(f7,1)=1;open(f7)=1",
      "write(f7,1)=1;write(f7,11)=1",
      "write(f7,1)=1;write(f7,1)=1",
      "write(f7,1)=1;open(f6)=1",
      "write(f7,1)=1;write(f6,11)=1",
      "write(f7,1)=1;write(f6,1)=1",
      "open(f1)=1;open(f91)=1",
      "open(f1)=1;write(f91,1)=0",
      "open(f1)=1;open(f7)=1",
      "open(f1)=1;write(f7,1)=1",
      "open(f1)=1;open(f1)=1",
      "open(f1)=1;write(f1,1)=1",
      "write(f1,11)=1;open(f91)=1",
      "write(f1,11)=1;write(f91,1)=1",
      "write(f1,11)=1;write(f91,11)=1",
      "write(f1,11)=1;open(f7)=1",
      "write(f1,11)=1;write(f7,1)=1",
      "write(f1,11)=1;write(f7,11)=1",
      "write(f1,11)=1;open(f1)=1",
      "write(f1,11)=1;write(f1,1)=1",
      "write(f1,11)=1;write(f1,11)=1",
      "write(f1,1)=1;open(f91)=1",
      "write(f1,1)=1;write(f91,11)=1",
      "write(f1,1)=1;write(f91,1)=0",
      "write(f1,1)=1;open(f7)=1",
      "write(f1,1)=1;write(f7,11)=1",
      "write(f1,1)=1;write(f7,1)=1",
      "write(f1,1)=1;open(f1)=1",
      "write(f1,1)=1;write(f1,11)=1",
      "write(f1,1)=1;write(f1,1)=1"
    )
    checkResults(resultfile, expected: _*)
  }

  @Test def test2_2(): Unit = {
    Verify("--execution", "0", "--specfile", spec2, "--logfile", log1, "--resultfile", resultfile, "--bits", "7", "--prediction", "1", "--prediction_type", "brute")
    val expected = List[String](
      "1",
      "open(f1)=1",
      "write(f1,a)=0",
      "write(f1,a1)=1",
      "open(f11)=1",
      "write(f11,a)=0",
      "write(f11,a1)=1"
    )
    checkResults(resultfile, expected: _*)
  }


  @Test def test3_1(): Unit = {
    Verify("--execution", "0", "--specfile", spec3, "--logfile", log3, "--resultfile",
      resultfile, "--bits", "8", "--prediction", "3", "--prediction_type", "smart")
    val expected = List[String](
      "open(f91)=1;open(f911)=1;open(f9111)=1",
      "open(f91)=1;open(f911)=1;close(f9111)=0",
      "open(f91)=1;open(f911)=1;open(f7)=1",
      "open(f91)=1;open(f911)=1;close(f7)=1",
      "open(f91)=1;open(f911)=1;open(f911)=1",
      "open(f91)=1;open(f911)=1;close(f911)=1",
      "open(f91)=1;close(f911)=0;open(f9111)=1",
      "open(f91)=1;close(f911)=0;close(f9111)=0",
      "open(f91)=1;close(f911)=0;open(f7)=1",
      "open(f91)=1;close(f911)=0;close(f7)=1",
      "open(f91)=1;close(f911)=0;open(f911)=1",
      "open(f91)=1;close(f911)=0;close(f911)=0",
      "open(f91)=1;open(f7)=1;open(f911)=1",
      "open(f91)=1;open(f7)=1;close(f911)=0",
      "open(f91)=1;open(f7)=1;open(f7)=1",
      "open(f91)=1;open(f7)=1;close(f7)=1",
      "open(f91)=1;open(f7)=1;open(f6)=1",
      "open(f91)=1;open(f7)=1;close(f6)=1",
      "open(f91)=1;close(f7)=1;open(f911)=1",
      "open(f91)=1;close(f7)=1;close(f911)=0",
      "open(f91)=1;close(f7)=1;open(f7)=1",
      "open(f91)=1;close(f7)=1;close(f7)=1",
      "open(f91)=1;close(f7)=1;open(f6)=1",
      "open(f91)=1;close(f7)=1;close(f6)=1",
      "open(f91)=1;open(f91)=1;open(f911)=1",
      "open(f91)=1;open(f91)=1;close(f911)=0",
      "open(f91)=1;open(f91)=1;open(f7)=1",
      "open(f91)=1;open(f91)=1;close(f7)=1",
      "open(f91)=1;open(f91)=1;open(f91)=1",
      "open(f91)=1;open(f91)=1;close(f91)=1",
      "open(f91)=1;close(f91)=1;open(f911)=1",
      "open(f91)=1;close(f91)=1;close(f911)=0",
      "open(f91)=1;close(f91)=1;open(f7)=1",
      "open(f91)=1;close(f91)=1;close(f7)=1",
      "open(f91)=1;close(f91)=1;open(f91)=1",
      "open(f91)=1;close(f91)=1;close(f91)=1",
      "close(f91)=0;open(f911)=1;open(f91)=1",
      "close(f91)=0;open(f911)=1;close(f91)=0",
      "close(f91)=0;open(f911)=1;open(f7)=1",
      "close(f91)=0;open(f911)=1;close(f7)=1",
      "close(f91)=0;open(f911)=1;open(f911)=1",
      "close(f91)=0;open(f911)=1;close(f911)=1",
      "close(f91)=0;close(f911)=0;open(f91)=1",
      "close(f91)=0;close(f911)=0;close(f91)=0",
      "close(f91)=0;close(f911)=0;open(f7)=1",
      "close(f91)=0;close(f911)=0;close(f7)=1",
      "close(f91)=0;close(f911)=0;open(f911)=1",
      "close(f91)=0;close(f911)=0;close(f911)=0",
      "close(f91)=0;open(f7)=1;open(f91)=1",
      "close(f91)=0;open(f7)=1;close(f91)=0",
      "close(f91)=0;open(f7)=1;open(f7)=1",
      "close(f91)=0;open(f7)=1;close(f7)=1",
      "close(f91)=0;open(f7)=1;open(f6)=1",
      "close(f91)=0;open(f7)=1;close(f6)=1",
      "close(f91)=0;close(f7)=1;open(f91)=1",
      "close(f91)=0;close(f7)=1;close(f91)=0",
      "close(f91)=0;close(f7)=1;open(f7)=1",
      "close(f91)=0;close(f7)=1;close(f7)=1",
      "close(f91)=0;close(f7)=1;open(f6)=1",
      "close(f91)=0;close(f7)=1;close(f6)=1",
      "close(f91)=0;open(f91)=1;open(f911)=1",
      "close(f91)=0;open(f91)=1;close(f911)=0",
      "close(f91)=0;open(f91)=1;open(f7)=1",
      "close(f91)=0;open(f91)=1;close(f7)=1",
      "close(f91)=0;open(f91)=1;open(f91)=1",
      "close(f91)=0;open(f91)=1;close(f91)=1",
      "close(f91)=0;close(f91)=0;open(f911)=1",
      "close(f91)=0;close(f91)=0;close(f911)=0",
      "close(f91)=0;close(f91)=0;open(f7)=1",
      "close(f91)=0;close(f91)=0;close(f7)=1",
      "close(f91)=0;close(f91)=0;open(f91)=1",
      "close(f91)=0;close(f91)=0;close(f91)=0",
      "open(f7)=1;open(f91)=1;open(f911)=1",
      "open(f7)=1;open(f91)=1;close(f911)=0",
      "open(f7)=1;open(f91)=1;open(f7)=1",
      "open(f7)=1;open(f91)=1;close(f7)=1",
      "open(f7)=1;open(f91)=1;open(f91)=1",
      "open(f7)=1;open(f91)=1;close(f91)=1",
      "open(f7)=1;close(f91)=0;open(f911)=1",
      "open(f7)=1;close(f91)=0;close(f911)=0",
      "open(f7)=1;close(f91)=0;open(f7)=1",
      "open(f7)=1;close(f91)=0;close(f7)=1",
      "open(f7)=1;close(f91)=0;open(f91)=1",
      "open(f7)=1;close(f91)=0;close(f91)=0",
      "open(f7)=1;open(f7)=1;open(f91)=1",
      "open(f7)=1;open(f7)=1;close(f91)=0",
      "open(f7)=1;open(f7)=1;open(f7)=1",
      "open(f7)=1;open(f7)=1;close(f7)=1",
      "open(f7)=1;open(f7)=1;open(f6)=1",
      "open(f7)=1;open(f7)=1;close(f6)=1",
      "open(f7)=1;close(f7)=1;open(f91)=1",
      "open(f7)=1;close(f7)=1;close(f91)=0",
      "open(f7)=1;close(f7)=1;open(f7)=1",
      "open(f7)=1;close(f7)=1;close(f7)=1",
      "open(f7)=1;close(f7)=1;open(f6)=1",
      "open(f7)=1;close(f7)=1;close(f6)=1",
      "open(f7)=1;open(f6)=1;open(f91)=1",
      "open(f7)=1;open(f6)=1;close(f91)=0",
      "open(f7)=1;open(f6)=1;open(f7)=1",
      "open(f7)=1;open(f6)=1;close(f7)=1",
      "open(f7)=1;open(f6)=1;open(f6)=1",
      "open(f7)=1;open(f6)=1;close(f6)=1",
      "open(f7)=1;close(f6)=1;open(f91)=1",
      "open(f7)=1;close(f6)=1;close(f91)=0",
      "open(f7)=1;close(f6)=1;open(f7)=1",
      "open(f7)=1;close(f6)=1;close(f7)=1",
      "open(f7)=1;close(f6)=1;open(f6)=1",
      "open(f7)=1;close(f6)=1;close(f6)=1",
      "close(f7)=1;open(f91)=1;open(f911)=1",
      "close(f7)=1;open(f91)=1;close(f911)=0",
      "close(f7)=1;open(f91)=1;open(f7)=1",
      "close(f7)=1;open(f91)=1;close(f7)=1",
      "close(f7)=1;open(f91)=1;open(f91)=1",
      "close(f7)=1;open(f91)=1;close(f91)=1",
      "close(f7)=1;close(f91)=0;open(f911)=1",
      "close(f7)=1;close(f91)=0;close(f911)=0",
      "close(f7)=1;close(f91)=0;open(f7)=1",
      "close(f7)=1;close(f91)=0;close(f7)=1",
      "close(f7)=1;close(f91)=0;open(f91)=1",
      "close(f7)=1;close(f91)=0;close(f91)=0",
      "close(f7)=1;open(f7)=1;open(f91)=1",
      "close(f7)=1;open(f7)=1;close(f91)=0",
      "close(f7)=1;open(f7)=1;open(f7)=1",
      "close(f7)=1;open(f7)=1;close(f7)=1",
      "close(f7)=1;open(f7)=1;open(f6)=1",
      "close(f7)=1;open(f7)=1;close(f6)=1",
      "close(f7)=1;close(f7)=1;open(f91)=1",
      "close(f7)=1;close(f7)=1;close(f91)=0",
      "close(f7)=1;close(f7)=1;open(f7)=1",
      "close(f7)=1;close(f7)=1;close(f7)=1",
      "close(f7)=1;close(f7)=1;open(f6)=1",
      "close(f7)=1;close(f7)=1;close(f6)=1",
      "close(f7)=1;open(f6)=1;open(f91)=1",
      "close(f7)=1;open(f6)=1;close(f91)=0",
      "close(f7)=1;open(f6)=1;open(f7)=1",
      "close(f7)=1;open(f6)=1;close(f7)=1",
      "close(f7)=1;open(f6)=1;open(f6)=1",
      "close(f7)=1;open(f6)=1;close(f6)=1",
      "close(f7)=1;close(f6)=1;open(f91)=1",
      "close(f7)=1;close(f6)=1;close(f91)=0",
      "close(f7)=1;close(f6)=1;open(f7)=1",
      "close(f7)=1;close(f6)=1;close(f7)=1",
      "close(f7)=1;close(f6)=1;open(f6)=1",
      "close(f7)=1;close(f6)=1;close(f6)=1",
      "open(f6)=1;open(f91)=1;open(f911)=1",
      "open(f6)=1;open(f91)=1;close(f911)=0",
      "open(f6)=1;open(f91)=1;open(f7)=1",
      "open(f6)=1;open(f91)=1;close(f7)=1",
      "open(f6)=1;open(f91)=1;open(f91)=1",
      "open(f6)=1;open(f91)=1;close(f91)=1",
      "open(f6)=1;close(f91)=0;open(f911)=1",
      "open(f6)=1;close(f91)=0;close(f911)=0",
      "open(f6)=1;close(f91)=0;open(f7)=1",
      "open(f6)=1;close(f91)=0;close(f7)=1",
      "open(f6)=1;close(f91)=0;open(f91)=1",
      "open(f6)=1;close(f91)=0;close(f91)=0",
      "open(f6)=1;open(f7)=1;open(f91)=1",
      "open(f6)=1;open(f7)=1;close(f91)=0",
      "open(f6)=1;open(f7)=1;open(f7)=1",
      "open(f6)=1;open(f7)=1;close(f7)=1",
      "open(f6)=1;open(f7)=1;open(f6)=1",
      "open(f6)=1;open(f7)=1;close(f6)=1",
      "open(f6)=1;close(f7)=1;open(f91)=1",
      "open(f6)=1;close(f7)=1;close(f91)=0",
      "open(f6)=1;close(f7)=1;open(f7)=1",
      "open(f6)=1;close(f7)=1;close(f7)=1",
      "open(f6)=1;close(f7)=1;open(f6)=1",
      "open(f6)=1;close(f7)=1;close(f6)=1",
      "open(f6)=1;open(f6)=1;open(f91)=1",
      "open(f6)=1;open(f6)=1;close(f91)=0",
      "open(f6)=1;open(f6)=1;open(f7)=1",
      "open(f6)=1;open(f6)=1;close(f7)=1",
      "open(f6)=1;open(f6)=1;open(f6)=1",
      "open(f6)=1;open(f6)=1;close(f6)=1",
      "open(f6)=1;close(f6)=1;open(f91)=1",
      "open(f6)=1;close(f6)=1;close(f91)=0",
      "open(f6)=1;close(f6)=1;open(f7)=1",
      "open(f6)=1;close(f6)=1;close(f7)=1",
      "open(f6)=1;close(f6)=1;open(f6)=1",
      "open(f6)=1;close(f6)=1;close(f6)=1",
      "close(f6)=1;open(f91)=1;open(f911)=1",
      "close(f6)=1;open(f91)=1;close(f911)=0",
      "close(f6)=1;open(f91)=1;open(f7)=1",
      "close(f6)=1;open(f91)=1;close(f7)=1",
      "close(f6)=1;open(f91)=1;open(f91)=1",
      "close(f6)=1;open(f91)=1;close(f91)=1",
      "close(f6)=1;close(f91)=0;open(f911)=1",
      "close(f6)=1;close(f91)=0;close(f911)=0",
      "close(f6)=1;close(f91)=0;open(f7)=1",
      "close(f6)=1;close(f91)=0;close(f7)=1",
      "close(f6)=1;close(f91)=0;open(f91)=1",
      "close(f6)=1;close(f91)=0;close(f91)=0",
      "close(f6)=1;open(f7)=1;open(f91)=1",
      "close(f6)=1;open(f7)=1;close(f91)=0",
      "close(f6)=1;open(f7)=1;open(f7)=1",
      "close(f6)=1;open(f7)=1;close(f7)=1",
      "close(f6)=1;open(f7)=1;open(f6)=1",
      "close(f6)=1;open(f7)=1;close(f6)=1",
      "close(f6)=1;close(f7)=1;open(f91)=1",
      "close(f6)=1;close(f7)=1;close(f91)=0",
      "close(f6)=1;close(f7)=1;open(f7)=1",
      "close(f6)=1;close(f7)=1;close(f7)=1",
      "close(f6)=1;close(f7)=1;open(f6)=1",
      "close(f6)=1;close(f7)=1;close(f6)=1",
      "close(f6)=1;open(f6)=1;open(f91)=1",
      "close(f6)=1;open(f6)=1;close(f91)=0",
      "close(f6)=1;open(f6)=1;open(f7)=1",
      "close(f6)=1;open(f6)=1;close(f7)=1",
      "close(f6)=1;open(f6)=1;open(f6)=1",
      "close(f6)=1;open(f6)=1;close(f6)=1",
      "close(f6)=1;close(f6)=1;open(f91)=1",
      "close(f6)=1;close(f6)=1;close(f91)=0",
      "close(f6)=1;close(f6)=1;open(f7)=1",
      "close(f6)=1;close(f6)=1;close(f7)=1",
      "close(f6)=1;close(f6)=1;open(f6)=1",
      "close(f6)=1;close(f6)=1;close(f6)=1"
    )
    checkResults(resultfile, expected: _*)
  }

  @Test def test4_1(): Unit = {
    Verify("--execution", "0", "--specfile", spec4, "--logfile", log3, "--resultfile",
      resultfile, "--bits", "8", "--prediction", "3", "--prediction_type", "smart")
    val expected = List[String](
      "open(f1)=1;open(f6)=1;open(f5)=1",
      "open(f1)=1;open(f6)=1;close(f5)=0",
      "open(f1)=1;open(f6)=1;open(f7)=1",
      "open(f1)=1;open(f6)=1;close(f7)=1",
      "open(f1)=1;open(f6)=1;open(f6)=1",
      "open(f1)=1;open(f6)=1;close(f6)=1",
      "open(f1)=1;close(f6)=0;open(f5)=1",
      "open(f1)=1;close(f6)=0;close(f5)=0",
      "open(f1)=1;close(f6)=0;open(f7)=1",
      "open(f1)=1;close(f6)=0;close(f7)=1",
      "open(f1)=1;close(f6)=0;open(f6)=1",
      "open(f1)=1;close(f6)=0;close(f6)=0",
      "open(f1)=1;open(f7)=1;open(f6)=1",
      "open(f1)=1;open(f7)=1;close(f6)=0",
      "open(f1)=1;open(f7)=1;open(f7)=1",
      "open(f1)=1;open(f7)=1;close(f7)=1",
      "open(f1)=1;open(f7)=1;open(f1)=1",
      "open(f1)=1;open(f7)=1;close(f1)=1",
      "open(f1)=1;close(f7)=1;open(f6)=1",
      "open(f1)=1;close(f7)=1;close(f6)=0",
      "open(f1)=1;close(f7)=1;open(f7)=1",
      "open(f1)=1;close(f7)=1;close(f7)=0",
      "open(f1)=1;close(f7)=1;open(f1)=1",
      "open(f1)=1;close(f7)=1;close(f1)=1",
      "open(f1)=1;open(f1)=1;open(f6)=1",
      "open(f1)=1;open(f1)=1;close(f6)=0",
      "open(f1)=1;open(f1)=1;open(f7)=1",
      "open(f1)=1;open(f1)=1;close(f7)=1",
      "open(f1)=1;open(f1)=1;open(f1)=1",
      "open(f1)=1;open(f1)=1;close(f1)=1",
      "open(f1)=1;close(f1)=1;open(f6)=1",
      "open(f1)=1;close(f1)=1;close(f6)=0",
      "open(f1)=1;close(f1)=1;open(f7)=1",
      "open(f1)=1;close(f1)=1;close(f7)=1",
      "open(f1)=1;close(f1)=1;open(f1)=1",
      "open(f1)=1;close(f1)=1;close(f1)=0",
      "close(f1)=0;open(f6)=1;open(f1)=1",
      "close(f1)=0;open(f6)=1;close(f1)=0",
      "close(f1)=0;open(f6)=1;open(f7)=1",
      "close(f1)=0;open(f6)=1;close(f7)=1",
      "close(f1)=0;open(f6)=1;open(f6)=1",
      "close(f1)=0;open(f6)=1;close(f6)=1",
      "close(f1)=0;close(f6)=0;open(f1)=1",
      "close(f1)=0;close(f6)=0;close(f1)=0",
      "close(f1)=0;close(f6)=0;open(f7)=1",
      "close(f1)=0;close(f6)=0;close(f7)=1",
      "close(f1)=0;close(f6)=0;open(f6)=1",
      "close(f1)=0;close(f6)=0;close(f6)=0",
      "close(f1)=0;open(f7)=1;open(f6)=1",
      "close(f1)=0;open(f7)=1;close(f6)=0",
      "close(f1)=0;open(f7)=1;open(f7)=1",
      "close(f1)=0;open(f7)=1;close(f7)=1",
      "close(f1)=0;open(f7)=1;open(f8)=1",
      "close(f1)=0;open(f7)=1;close(f8)=1",
      "close(f1)=0;close(f7)=1;open(f6)=1",
      "close(f1)=0;close(f7)=1;close(f6)=0",
      "close(f1)=0;close(f7)=1;open(f7)=1",
      "close(f1)=0;close(f7)=1;close(f7)=0",
      "close(f1)=0;close(f7)=1;open(f8)=1",
      "close(f1)=0;close(f7)=1;close(f8)=1",
      "close(f1)=0;open(f1)=1;open(f6)=1",
      "close(f1)=0;open(f1)=1;close(f6)=0",
      "close(f1)=0;open(f1)=1;open(f7)=1",
      "close(f1)=0;open(f1)=1;close(f7)=1",
      "close(f1)=0;open(f1)=1;open(f1)=1",
      "close(f1)=0;open(f1)=1;close(f1)=1",
      "close(f1)=0;close(f1)=0;open(f6)=1",
      "close(f1)=0;close(f1)=0;close(f6)=0",
      "close(f1)=0;close(f1)=0;open(f7)=1",
      "close(f1)=0;close(f1)=0;close(f7)=1",
      "close(f1)=0;close(f1)=0;open(f1)=1",
      "close(f1)=0;close(f1)=0;close(f1)=0",
      "open(f7)=1;open(f6)=1;open(f1)=1",
      "open(f7)=1;open(f6)=1;close(f1)=0",
      "open(f7)=1;open(f6)=1;open(f7)=1",
      "open(f7)=1;open(f6)=1;close(f7)=1",
      "open(f7)=1;open(f6)=1;open(f6)=1",
      "open(f7)=1;open(f6)=1;close(f6)=1",
      "open(f7)=1;close(f6)=0;open(f1)=1",
      "open(f7)=1;close(f6)=0;close(f1)=0",
      "open(f7)=1;close(f6)=0;open(f7)=1",
      "open(f7)=1;close(f6)=0;close(f7)=1",
      "open(f7)=1;close(f6)=0;open(f6)=1",
      "open(f7)=1;close(f6)=0;close(f6)=0",
      "open(f7)=1;open(f7)=1;open(f6)=1",
      "open(f7)=1;open(f7)=1;close(f6)=0",
      "open(f7)=1;open(f7)=1;open(f7)=1",
      "open(f7)=1;open(f7)=1;close(f7)=1",
      "open(f7)=1;open(f7)=1;open(f8)=1",
      "open(f7)=1;open(f7)=1;close(f8)=1",
      "open(f7)=1;close(f7)=1;open(f6)=1",
      "open(f7)=1;close(f7)=1;close(f6)=0",
      "open(f7)=1;close(f7)=1;open(f7)=1",
      "open(f7)=1;close(f7)=1;close(f7)=0",
      "open(f7)=1;close(f7)=1;open(f8)=1",
      "open(f7)=1;close(f7)=1;close(f8)=1",
      "open(f7)=1;open(f8)=1;open(f6)=1",
      "open(f7)=1;open(f8)=1;close(f6)=0",
      "open(f7)=1;open(f8)=1;open(f7)=1",
      "open(f7)=1;open(f8)=1;close(f7)=1",
      "open(f7)=1;open(f8)=1;open(f8)=1",
      "open(f7)=1;open(f8)=1;close(f8)=1",
      "open(f7)=1;close(f8)=1;open(f6)=1",
      "open(f7)=1;close(f8)=1;close(f6)=0",
      "open(f7)=1;close(f8)=1;open(f7)=1",
      "open(f7)=1;close(f8)=1;close(f7)=1",
      "open(f7)=1;close(f8)=1;open(f8)=1",
      "open(f7)=1;close(f8)=1;close(f8)=0",
      "close(f7)=1;open(f6)=1;open(f7)=1",
      "close(f7)=1;open(f6)=1;close(f7)=0",
      "close(f7)=1;open(f6)=1;open(f8)=1",
      "close(f7)=1;open(f6)=1;close(f8)=1",
      "close(f7)=1;open(f6)=1;open(f6)=1",
      "close(f7)=1;open(f6)=1;close(f6)=1",
      "close(f7)=1;close(f6)=0;open(f7)=1",
      "close(f7)=1;close(f6)=0;close(f7)=0",
      "close(f7)=1;close(f6)=0;open(f8)=1",
      "close(f7)=1;close(f6)=0;close(f8)=1",
      "close(f7)=1;close(f6)=0;open(f6)=1",
      "close(f7)=1;close(f6)=0;close(f6)=0",
      "close(f7)=1;open(f7)=1;open(f6)=1",
      "close(f7)=1;open(f7)=1;close(f6)=0",
      "close(f7)=1;open(f7)=1;open(f7)=1",
      "close(f7)=1;open(f7)=1;close(f7)=1",
      "close(f7)=1;open(f7)=1;open(f8)=1",
      "close(f7)=1;open(f7)=1;close(f8)=1",
      "close(f7)=1;close(f7)=0;open(f6)=1",
      "close(f7)=1;close(f7)=0;close(f6)=0",
      "close(f7)=1;close(f7)=0;open(f7)=1",
      "close(f7)=1;close(f7)=0;close(f7)=0",
      "close(f7)=1;close(f7)=0;open(f8)=1",
      "close(f7)=1;close(f7)=0;close(f8)=1",
      "close(f7)=1;open(f8)=1;open(f7)=1",
      "close(f7)=1;open(f8)=1;close(f7)=0",
      "close(f7)=1;open(f8)=1;open(f9)=1",
      "close(f7)=1;open(f8)=1;close(f9)=1",
      "close(f7)=1;open(f8)=1;open(f8)=1",
      "close(f7)=1;open(f8)=1;close(f8)=1",
      "close(f7)=1;close(f8)=1;open(f7)=1",
      "close(f7)=1;close(f8)=1;close(f7)=0",
      "close(f7)=1;close(f8)=1;open(f9)=1",
      "close(f7)=1;close(f8)=1;close(f9)=1",
      "close(f7)=1;close(f8)=1;open(f8)=1",
      "close(f7)=1;close(f8)=1;close(f8)=0",
      "open(f6)=1;open(f1)=1;open(f5)=1",
      "open(f6)=1;open(f1)=1;close(f5)=0",
      "open(f6)=1;open(f1)=1;open(f7)=1",
      "open(f6)=1;open(f1)=1;close(f7)=1",
      "open(f6)=1;open(f1)=1;open(f1)=1",
      "open(f6)=1;open(f1)=1;close(f1)=1",
      "open(f6)=1;close(f1)=0;open(f5)=1",
      "open(f6)=1;close(f1)=0;close(f5)=0",
      "open(f6)=1;close(f1)=0;open(f7)=1",
      "open(f6)=1;close(f1)=0;close(f7)=1",
      "open(f6)=1;close(f1)=0;open(f1)=1",
      "open(f6)=1;close(f1)=0;close(f1)=0",
      "open(f6)=1;open(f7)=1;open(f1)=1",
      "open(f6)=1;open(f7)=1;close(f1)=0",
      "open(f6)=1;open(f7)=1;open(f7)=1",
      "open(f6)=1;open(f7)=1;close(f7)=1",
      "open(f6)=1;open(f7)=1;open(f6)=1",
      "open(f6)=1;open(f7)=1;close(f6)=1",
      "open(f6)=1;close(f7)=1;open(f1)=1",
      "open(f6)=1;close(f7)=1;close(f1)=0",
      "open(f6)=1;close(f7)=1;open(f7)=1",
      "open(f6)=1;close(f7)=1;close(f7)=0",
      "open(f6)=1;close(f7)=1;open(f6)=1",
      "open(f6)=1;close(f7)=1;close(f6)=1",
      "open(f6)=1;open(f6)=1;open(f1)=1",
      "open(f6)=1;open(f6)=1;close(f1)=0",
      "open(f6)=1;open(f6)=1;open(f7)=1",
      "open(f6)=1;open(f6)=1;close(f7)=1",
      "open(f6)=1;open(f6)=1;open(f6)=1",
      "open(f6)=1;open(f6)=1;close(f6)=1",
      "open(f6)=1;close(f6)=1;open(f1)=1",
      "open(f6)=1;close(f6)=1;close(f1)=0",
      "open(f6)=1;close(f6)=1;open(f7)=1",
      "open(f6)=1;close(f6)=1;close(f7)=1",
      "open(f6)=1;close(f6)=1;open(f6)=1",
      "open(f6)=1;close(f6)=1;close(f6)=0",
      "close(f6)=0;open(f1)=1;open(f6)=1",
      "close(f6)=0;open(f1)=1;close(f6)=0",
      "close(f6)=0;open(f1)=1;open(f7)=1",
      "close(f6)=0;open(f1)=1;close(f7)=1",
      "close(f6)=0;open(f1)=1;open(f1)=1",
      "close(f6)=0;open(f1)=1;close(f1)=1",
      "close(f6)=0;close(f1)=0;open(f6)=1",
      "close(f6)=0;close(f1)=0;close(f6)=0",
      "close(f6)=0;close(f1)=0;open(f7)=1",
      "close(f6)=0;close(f1)=0;close(f7)=1",
      "close(f6)=0;close(f1)=0;open(f1)=1",
      "close(f6)=0;close(f1)=0;close(f1)=0",
      "close(f6)=0;open(f7)=1;open(f6)=1",
      "close(f6)=0;open(f7)=1;close(f6)=0",
      "close(f6)=0;open(f7)=1;open(f7)=1",
      "close(f6)=0;open(f7)=1;close(f7)=1",
      "close(f6)=0;open(f7)=1;open(f8)=1",
      "close(f6)=0;open(f7)=1;close(f8)=1",
      "close(f6)=0;close(f7)=1;open(f6)=1",
      "close(f6)=0;close(f7)=1;close(f6)=0",
      "close(f6)=0;close(f7)=1;open(f7)=1",
      "close(f6)=0;close(f7)=1;close(f7)=0",
      "close(f6)=0;close(f7)=1;open(f8)=1",
      "close(f6)=0;close(f7)=1;close(f8)=1",
      "close(f6)=0;open(f6)=1;open(f1)=1",
      "close(f6)=0;open(f6)=1;close(f1)=0",
      "close(f6)=0;open(f6)=1;open(f7)=1",
      "close(f6)=0;open(f6)=1;close(f7)=1",
      "close(f6)=0;open(f6)=1;open(f6)=1",
      "close(f6)=0;open(f6)=1;close(f6)=1",
      "close(f6)=0;close(f6)=0;open(f1)=1",
      "close(f6)=0;close(f6)=0;close(f1)=0",
      "close(f6)=0;close(f6)=0;open(f7)=1",
      "close(f6)=0;close(f6)=0;close(f7)=1",
      "close(f6)=0;close(f6)=0;open(f6)=1",
      "close(f6)=0;close(f6)=0;close(f6)=0"
    )
    checkResults(resultfile, expected: _*)
  }
}

