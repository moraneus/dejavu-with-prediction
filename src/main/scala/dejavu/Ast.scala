
package dejavu

import java.io._
import java.nio.file.Paths
import scala.io.Source
import scala.util.Random
import scala.util.control.Breaks._

// ### Prediction Extension ###
import scala.collection.mutable.{Set => mSet}

object AstUtil {
  val DEBUG_BRACKETS = false
  val DEBUG_AST = true

  // --- Debugging: ----

  /**
    * Used for debugging a problem: insert and then remove when bug is found.
    *
    * @param items any items to be printed.
    */

  def debug(items: Any*): Unit = {
    println("==========")
    items foreach println
    println("----------")
  }

  // --- Quoting: ---

  def quote(s: Any): String = "\"" + s + "\""

  def iquote(s: Any): String = "\\\"" + s + "\\\""

  def requote(s: Any): String = {
    val unQuoted = s.asInstanceOf[String].replace("\"", "")
    s"''$unQuoted''"
  }

  def requoteIfString(s: Any): String =
    if (s.isInstanceOf[String]) requote(s) else s.toString

  def quoteIfNotQuoted(s: Any): String = {
    val str = s.asInstanceOf[String]
    if (str.startsWith("\"")) str else quote(str)
  }

  def bracket(s: Any): String = if (DEBUG_BRACKETS) s"[$s]" else s.toString

  // --- File writing begin: ---

  var pw: PrintWriter = null

  def openFile(name: String): Unit = {
    pw = new PrintWriter(new File(name))
  }

  def closeFile(): Unit = {
    pw.close()
  }

  def write(x: Any) = {
    // Predef.print(x)
    pw.write(x.toString)
  }

  def writeln() = {
    // Predef.println()
    pw.write("\n")
  }

  def writeln(x: Any) = {
    // Predef.println(x)
    pw.write(x + "\n")
  }

  // --- File writing end ---

  /**
    * This method copies the code in Monitor.scala to Monitor.txt (except for the initial package
    * declaration). Monitor.txt becomes part of the generated monitoring code for a set of
    * properties. This approach is necessary since Monitor.scala will not be available in
    * the generated jar file. The method only has  a side effect in development mode where
    * Monitor.scala is available, and is called each time the system (Verify.main) is run.
    */

  def refreshMonitorTextIfDevelopment(): Unit = {
    val dejavuDir = s"${Paths.get(".").toAbsolutePath}src/main/scala/dejavu/"
    val monitorFreshFileIn = dejavuDir + "Monitor.scala"
    if (new java.io.File(monitorFreshFileIn).exists) { // true in development environment
      val monitorFreshText = Source.fromFile(monitorFreshFileIn).getLines.drop(1).mkString("\n") // drop package name
      val monitorFreshFileOut = dejavuDir + "Monitor.txt"
      openFile(monitorFreshFileOut)
      writeln(monitorFreshText)
      closeFile()
    }
  }

  // --- Errors: ---

  def error(msg: String, args: Any*): Nothing = {
    var message = msg
    for (arg <- args) {
      message += s"\n- $arg"
    }
    assert(false, msg).asInstanceOf[Nothing]
  }

  // --- Type definitions: ---

  type MacroMap = Map[String, Macro]
  type Substitution = Map[String, ConstOrVar]

  // --- Datatype auxiliary methods: ---

  def log2(x: Int):Int = {
    (scala.math.log(x)/scala.math.log(2.0)).ceil.toInt
  }

  def convertQuantifierInRuleBody(z: String, subst: Substitution): String = {
    subst.toList.collectFirst { case (y, VPat(`z`)) => y } match {
      case None => z
      case Some(y) => y
    }
  }
}

import AstUtil._

case class Spec(properties: List[Property]) {

  var generatedMonitorsPath: String = ""

  private def getRandomFolderName: String = {
    val x = Random.alphanumeric
    s"test_${(x take 10).mkString}"
  }


  private def buildEventString(eventsMapping: Map[String, mSet[String]]): String = {
    var eventsString: String = """Map( """
    for (event <- eventsMapping) {
      eventsString += s""""${event._1}" -> List("""
      for (e <- event._2) {
        eventsString += s""""$e","""
      }
      eventsString = eventsString.dropRight(1)
      eventsString += "),"
    }
    eventsString = eventsString.dropRight(1) + " )"
    eventsString
  }

  private def buildMultipleEventsString(eventsMapping: Map[String, mSet[String]]): String = {
    var eventsString: String = """Map( """
    for (event <- eventsMapping) {
      eventsString += "Array( "
      for (value <- event._1.split(",")) {
        eventsString += s""""${value}","""
      }
      eventsString = eventsString.dropRight(1)
      eventsString += " ) -> List("
//      eventsString += s""""${event._1.split(",")}" ) -> List("""
      for (e <- event._2) {
        eventsString += s""""$e","""
      }
      eventsString = eventsString.dropRight(1)
      eventsString += "),"
    }
    eventsString = eventsString.dropRight(1) + " )"
    eventsString
  }

  def translate(executionMode: Boolean): Unit = {
    refreshMonitorTextIfDevelopment()

    if (executionMode) generatedMonitorsPath = s"${Paths.get(".").toAbsolutePath}/output"
    else generatedMonitorsPath = s"${Paths.get(".").toAbsolutePath}/src/test/scala/sandbox/generated_monitors/${getRandomFolderName}"
    val dir = new File(generatedMonitorsPath)
    if (!dir.exists) dir.mkdirs
    openFile(s"$generatedMonitorsPath/TraceMonitor.scala")
    println(s"Generated TraceMonitor.scala file in: $generatedMonitorsPath")

    writeln(ResourceReader.read("Monitor.txt"))
    writeln()
    for (property <- properties) {
      val name = property.name
      writeln(
        s"""
           |/*
           |  $property
           |*/
           |
           |class Formula_$name(monitor: Monitor) extends Formula(monitor) {
          """
          .stripMargin)

      writeln("  override def evaluate(): Boolean = {")

      LTL.translate(property)

      writeln(
        s"""
           |    debugMonitorState()
           |
           |    val error = now(0).isZero
           |    if (error) monitor.recordResult()
           |    tmp = now
           |    now = pre
           |    pre = tmp
           |    touchedByLastEvent = emptyTouchedSet
           |    !error
           |  }""".stripMargin)

      writeln()

      val bitsPerTimeVar : Int = if (LTL.timeLimits.isEmpty) 0 else {
        val maxTimeLimit = LTL.timeLimits.max
        val maxTimeValue = 2 * maxTimeLimit + 1
        log2(maxTimeValue)
      }

      println(s"$bitsPerTimeVar time bits allocated!")

      val variables = property.getQuantifiedVariables
      if (variables == Nil) {
        writeln(s"  declareVariables()($bitsPerTimeVar)")
      } else {
        val lhs = variables.map { case (n, b) => s"var_$n" }.mkString(" :: ") + " :: Nil"
        val rhs = variables.map { case (n, b) => (quote(n), b) }.mkString(", ")
        writeln(s"  val $lhs = declareVariables($rhs)($bitsPerTimeVar)")
      }

      if (!LTL.timeLimits.isEmpty) {

        writeln()
        writeln("// Declarations related to timed properties:")
        writeln(
          s"""
             |  val startTimeVar : Int = ${variables.length} * Options.BITS
             |  val offsetTimeVar : Int = $bitsPerTimeVar
             |
             |  val (sBegin,sEnd) = (startTimeVar,startTimeVar + offsetTimeVar - 1)
             |  val (uBegin,uEnd) = (sEnd + 1, sEnd + offsetTimeVar)
             |  val (dBegin,dEnd) = (uEnd + 1, uEnd + offsetTimeVar)
             |  val (cBegin,cEnd) = (dEnd + 1, dEnd + offsetTimeVar)
             |  val (lBegin,lEnd) = (cEnd + 1, cEnd + offsetTimeVar)
             |
             |  val tPosArray = (sBegin to sEnd).toArray
             |  val uPosArray = (uBegin to uEnd).toArray
             |  val dPosArray = (dBegin to dEnd).toArray
             |  val cPosArray = (cBegin to cEnd).toArray
             |  val lPosArray = (lBegin to lEnd).toArray
             |
             |  val tBDDList = generateBDDList(tPosArray)
             |  val uBDDList = generateBDDList(uPosArray)
             |  val dBDDList = generateBDDList(dPosArray)
             |  val cBDDList = generateBDDList(cPosArray)
             |  val lBDDList = generateBDDList(lPosArray)
             |
             |  val tBDDListHighToLow = tBDDList.reverse
             |  val uBDDListHighToLow = uBDDList.reverse
             |  val lBDDListHighToLow = lBDDList.reverse
             |
             |  val tPosArrayHighToLow = tPosArray.reverse
             |  val uPosArrayHighToLow = uPosArray.reverse
             |  val dPosArrayHighToLow = dPosArray.reverse
             |  val lPosArrayHighToLow = lPosArray.reverse
             |
             |  val var_t_quantvar : BDD = bddGenerator.getQuantVars(tPosArray)
             |  val var_d_quantvar : BDD = bddGenerator.getQuantVars(dPosArray)
             |  val var_c_quantvar : BDD = bddGenerator.getQuantVars(cPosArray)
             |  val var_l_quantvar : BDD = bddGenerator.getQuantVars(lPosArray)
             |
             |  val u_to_t_map = bddGenerator.B.makePair()
             |  for ((u,t) <- uPosArray.zip(tPosArray)) {
             |    u_to_t_map.set(u,t)
             |  }
             |
             |  val zeroTime : BDD = bddGenerator.B.buildCube(0,tPosArrayHighToLow)
             |""".stripMargin)

        val timeLimits = LTL.timeLimits.toList.map { case limit =>
          s"$limit -> bddGenerator.B.buildCube($limit,lPosArrayHighToLow)"
        }

        val timeLimitsPlusOne = LTL.timeLimits.toList.map { case limit =>
          s"$limit -> bddGenerator.B.buildCube($limit + 1,uPosArrayHighToLow)"
        }

        val maxTimeLimit = LTL.timeLimits.max

        writeln(
          s"""  val limitMap : Map[Int,BDD] =
             |    Map(
             |      ${timeLimits.mkString("    ",",\n    ","")}
             |    )
             |
             |  val uMap: Map[Int, BDD] =
             |    Map(
             |      ${timeLimitsPlusOne.mkString("    ",",\n    ","")}
             |    )
             |
             |  val maxTimeLimit = $maxTimeLimit + 1
             |  var DeltaBDD : BDD = null
             |
             |  override def setTime(actualDelta: Int) {
             |    val reducedDelta = scala.math.min(actualDelta,maxTimeLimit)
             |    DeltaBDD = bddGenerator.B.buildCube(reducedDelta,dPosArrayHighToLow)
             |  }""".stripMargin)

        writeln()
        writeln("// End of declarations related to timed properties")
      }

      val size = LTL.next
      val subFormulas: String = LTL.subExpressions.map(e => quote(e.toString)).mkString(",\n      ")
      val indices: String = LTL.indicesOfPastTimeFormulas.mkString(",")

      writeln(s"""
                 |  varsInRelations = Set(${LTL.varsInRelations.map(quote).mkString(",")})
                 |  val indices: List[Int] = List($indices)
                 |
                 |  pre = Array.fill($size)(bddGenerator.False)
                 |  now = Array.fill($size)(bddGenerator.False)
                 |
                 |  txt = Array(
                 |    $subFormulas
                 |  )
                 |
                 |  debugMonitorState()
                 |}
        """.stripMargin)
    }

    val constructors = properties.map(_.name).map(nm => s"new Formula_$nm(this)").mkString(",")
    val eventsInSpec = SymbolTable.referredEvents.map(quote(_)).mkString(",")

    // ### Prediction Extension ###
    val property = properties.head
    val listOfMaps: Array[Map[String, mSet[String]]] = Array.fill(3)(Map.empty[String, mSet[String]])

//    var eventsInVarsMapping = Map[String, mSet[String]]()
//    var eventsInConstMapping = Map[String, mSet[String]]()
//    var multipleValuesPredicateMapping = Map[String, mSet[String]]()

//    breakable {
//      for (a <- property.getPredicateTerms) {
//        if (a.values.nonEmpty) {
//          val values = a.values
//          var currVar = values.mkString.replace("List(","").dropRight(0)
//          if (values.size == 1) {
//            if (values.head.getClass.getName == "dejavu.CPat")
//            {
//              currVar = currVar.replace("'", "")
//              if (eventsInConstMapping.contains(currVar)) {
//                eventsInConstMapping(currVar) += a.name
//              } else {
//                eventsInConstMapping += currVar -> mSet(a.name)
//              }
//            } else {
//              if (eventsInVarsMapping.contains(currVar)) {
//                eventsInVarsMapping(currVar) += a.name
//              } else {
//                eventsInVarsMapping += currVar -> mSet(a.name)
//              }
//            }
//          } else {
//            var multipleValues = ""
//            for (value <- values) {
//              if (values.head.getClass.getName == "dejavu.CPat") {
//                multipleValues += s"${value.toString},"
//              } else {
//                multipleValues += s"${value.toString},"
//              }
//            }
//            multipleValues = multipleValues.dropRight(1)
//            if (multipleValuesPredicateMapping.contains(multipleValues)) {
//              multipleValuesPredicateMapping(multipleValues) += a.name
//            } else {
//              multipleValuesPredicateMapping += multipleValues -> mSet(a.name)
//            }
//          }
//        } else {
//          eventsInVarsMapping = Map[String, mSet[String]]()
//          break
//        }
//      }
//    }

    def addEventToMapping(mappingIndex: Int, key: String, eventName: String): Unit = {
      if (listOfMaps(mappingIndex).contains(key)) {
        listOfMaps(mappingIndex)(key) += eventName
      } else {
        listOfMaps(mappingIndex) += (key -> mSet(eventName))
      }
    }

    def processSingleValue(value: Any, eventName: String): Unit = {
      var currVar = value.toString
      if (value.getClass.getName == "dejavu.CPat") {
        currVar = currVar.replace("'", "")
        addEventToMapping(1, currVar, eventName)
      } else {
        addEventToMapping(0, currVar, eventName)
      }
    }

    def processMultipleValues(values: List[Any], eventName: String): Unit = {
      val multipleValues = values.map(_.toString).mkString(",")
      addEventToMapping(2, multipleValues, eventName)
    }

    breakable {
      for (a <- property.getPredicateTerms) {
        if (a.values.nonEmpty) {
          val values = a.values
          if (values.size == 1) {
            processSingleValue(values.head, a.name)
          } else {
            processMultipleValues(values, a.name)
          }
        } else {
//          listOfMaps(0) = Map[String, mSet[String]]()
          break
        }
      }
    }

    val eventsInVarsString: String = buildEventString(listOfMaps(0))
    val eventsInConstString: String = buildEventString(listOfMaps(1))
    val multipleValuesPredicateString: String = buildMultipleEventsString(listOfMaps(2))

    writeln(
      s"""/* The specialized Monitor for the provided properties. */
         |
         |class PropertyMonitor extends Monitor {
         |  def eventsInSpec: Set[String] = Set(${eventsInSpec})
         |
         |  // ### Prediction Extension ###
         |  val eventsInVars: Map[String, List[String]] = $eventsInVarsString
         |  val eventsInConstants: Map[String, List[String]] = $eventsInConstString
         |  val multipleValuesPredicates: Map[Array[String], List[String]] = $multipleValuesPredicateString
         |
         |  formulae ++= List($constructors)
         |}
      """.stripMargin)

    writeln(
      """
        |// ### Prediction Extension ###
        |
        |// Define a simple mutable wrapper for a boolean value
        |class MutableBoolean(var value: Boolean)
        |
        |abstract class Prediction(monitor: Monitor) {
        |  val G: BDDGenerator = monitor.formulae.head.bddGenerator
        |  val formula: Formula = monitor.formulae.head
        |  val vars: Map[String, Variable] = G.varMap
        |  Options.PREDICTION_RUNNING_STATE = true
        |  var counter = 0
        |  val wrappedPattern: Regex = { "''([^*]+)''".r }
        |
        |  /**
        |    *
        |    * The main prediction function, which is works by recursion.
        |    *
        |    * @param k             the k step for the prediction
        |    * @param predictedEvents the predicts event seen so far
        |    */
        |
        |  def prediction(k: Int, predictedEvents: ListBuffer[(String, String, Int)]): Unit
        |
        |  /**
        |    * Store the current state of the monitoring process.
        |    * This is necessary before taking any recursion step in the prediction process.
        |    *
        |    * @param M               the monitor object
        |    * @param V               a variable object which is the one we wants to store his dynamically states.
        |    * @param nowTmp          a tmp array which save the F.now BDDs array.
        |    * @param preTmp          a tmp array which save the F.pre BDDs array.
        |    * @param statTmp         a tmp array which save the M.error and M.lineNr values.
        |    * @param variableBddsTmp a tmp array which save the V.free, V.seen, V.inRelation BDDs.
        |    */
        |
        |  def storePreviousState(
        |                          M: Monitor,
        |                          V: Array[Variable],
        |                          nowTmp: Array[BDD],
        |                          preTmp: Array[BDD],
        |                          statTmp: Array[Int],
        |                          variableBddsTmp: Array[BDD]): Unit = {
        |
        |    nowTmp.indices.foreach(i => nowTmp(i) = formula.now(i))
        |    preTmp.indices.foreach(i => preTmp(i) = formula.pre(i))
        |
        |    statTmp(0) = M.lineNr
        |    statTmp(1) = M.errors
        |    M.lineNr += 1
        |
        |    var base = 0
        |    for (index <- V.indices) {
        |      variableBddsTmp(base + 0) = V(index).free
        |      variableBddsTmp(base + 1) = V(index).seen
        |      variableBddsTmp(base + 2) = V(index).inRelation
        |      base += 3
        |    }
        |  }
        |
        |  /**
        |    * Restore the previous state of the monitoring process.
        |    * This is necessary right after any recursion step is done in the prediction process.
        |    *
        |    * @param M               the monitor object
        |    * @param V               a variable object which is the one we wants to store his dynamically states.
        |    * @param nowTmp          a tmp array which hold the previous F.now BDDs array.
        |    * @param preTmp          a tmp array which hold the previous the F.pre BDDs array.
        |    * @param statTmp         a tmp array which hold the previous the M.error and M.lineNr values.
        |    * @param variableBddsTmp a tmp array which hold the previous the V.free, V.seen, V.inRelation BDDs.
        |    */
        |
        |  def restorePreviousState(
        |                            M: Monitor,
        |                            V: Array[Variable],
        |                            nowTmp: Array[BDD],
        |                            preTmp: Array[BDD],
        |                            statTmp: Array[Int],
        |                            variableBddsTmp: Array[BDD]): Unit = {
        |
        |    nowTmp.indices.foreach(i => formula.now(i) = nowTmp(i))
        |    preTmp.indices.foreach(i => formula.pre(i) = preTmp(i))
        |
        |    M.lineNr = statTmp(0)
        |    M.errors = statTmp(1)
        |
        |    var base = 0
        |    for (index <- V.indices) {
        |      V(index).free = variableBddsTmp(base + 0)
        |      V(index).seen = variableBddsTmp(base + 1)
        |      V(index).inRelation = variableBddsTmp(base + 2)
        |      base += 3
        |    }
        |  }
        |
        |  def printPredictionSummary(predictedEvents: ListBuffer[(String, String, Int)]): Unit = {
        |    // Prints the selected path
        |    println("\n\n######### SUMMARY OF PREDICTION #########\n")
        |
        |    var predictionAsString = ""
        |    for (event <- predictedEvents) {
        |      val currEventState = s"${event._1}(${event._2})=${event._3}"
        |      predictionAsString += currEventState + ";"
        |      print(s"$currEventState -> ")
        |    }
        |    println("DONE")
        |    writelnResult(s"${predictionAsString.dropRight(1)}")
        |
        |    // Print the summary of trace (including extension)
        |    println(s"Processed ${monitor.lineNr} events")
        |  }
        |
        |  def handleBaseCase(predictedEvents: ListBuffer[(String, String, Int)]): Unit = {
        |    if (predictedEvents.last._3 == Options.EXPECTED_VERDICT) {
        |      Options.FOUND_VERDICT = true
        |    }
        |    counter += 1
        |    printPredictionSummary(predictedEvents)
        |    monitor.end()
        |  }
        """.stripMargin)
    writeln(
      s"""
        | /**
        |   * Initializes the necessary variables for the prediction function.
        |   *
        |   * @param predictedEvents  The ListBuffer of predicted events as tuples (String, String, Int).
        |   * @return                 A tuple containing the following values:
        |   *                           - tmpPredictedEvents: ListBuffer[(String, String, Int)]
        |   *                           - tmpEventTable: Map[String, Long]
        |   *                           - tmpVarBdds: Array[Map[Any, BDD]]
        |   *                           - tmpStat: Array[Int]
        |   *                           - tmpVariableBdds: Array[BDD]
        |   *                           - tmpPre: Array[BDD]
        |   *                           - tmpNow: Array[BDD]
        |   *                           - singleConstValuedPredicateFlag: Boolean
        |   *                           - multipleConstValuedPredicatesFlag: Boolean
        |   *                           - multipleValuesPredicatesFlag: Map[Array[String], MutableBoolean]
        |   */
        |   def initializeVariables(predictedEvents: ListBuffer[(String, String, Int)]):
        |   (
        |     ListBuffer[(String, String, Int)],
        |     Map[String, Long],
        |     Array[Map[Any, BDD]],
        |     Array[Int],
        |     Array[BDD],
        |     Array[BDD],
        |     Array[BDD],
        |     Boolean,
        |     Boolean,
        |     Map[Array[String], MutableBoolean]) = {
        |
        |      // Defines all the tmp objects in order to have the ability to restore previous
        |      // states while going back and forth in recursion.
        |      val tmpEventTable: Map[String, Long] = null
        |      val tmpVarBdds: Array[Map[Any, BDD]] = Array.fill(${property.getQuantifiedVariables.size})(null)
        |      val tmpStat: Array[Int] = Array.fill(2)(0)
        |      val tmpVariableBdds = Array.fill(3 * ${property.getQuantifiedVariables.size})(G.False)
        |      val tmpPre: Array[BDD] = Array.fill(${LTL.next})(G.False)
        |      val tmpNow: Array[BDD] = Array.fill(${LTL.next})(G.False)
        |      val tmpPredictedEvents: ListBuffer[(String, String, Int)] = predictedEvents
        |
        |      // Used to avoid duplicates when spec have constants or predicate with multiple values.
        |      // If true, we need to handle constants and predicate with multiple values.
        |      // All are initialize to true.
        |      val singleConstValuedPredicateFlag = true
        |      val multipleConstValuedPredicatesFlag = true
        |      val multipleValuesPredicatesFlag = monitor.multipleValuesPredicates.map { case (key, _) => (key, new MutableBoolean(true))}
        |
        |     // The return tuple
        |     (tmpPredictedEvents,
        |      tmpEventTable,
        |      tmpVarBdds,
        |      tmpStat,
        |      tmpVariableBdds,
        |      tmpPre,
        |      tmpNow,
        |      singleConstValuedPredicateFlag,
        |      multipleConstValuedPredicatesFlag,
        |      multipleValuesPredicatesFlag)
        |   }
        | }
        |
        |// ### Prediction Extension ###
        |class BruteForcePrediction(monitor: Monitor) extends Prediction(monitor) {
        |
        |  /**
        |   * The main prediction function, which is works by recursion.
        |   *
        |   * @param k             the k step for the prediction
        |   * @param predictedEvents the predicts event seen so far
        |   */
        |  override def prediction(k: Int, predictedEvents: ListBuffer[(String, String, Int)]): Unit = {
        |
        |       // Recursion Base case
        |       if (k == 0) {
        |         handleBaseCase(predictedEvents)
        |         return
        |       }
        """.stripMargin)

    writeln(
      s"""
         |    // Initialize necessary variables
         |    var
         |    (tmpPredictedEvents,
         |    tmpEventTable,
         |    tmpVarBdds,
         |    tmpStat,
         |    tmpVariableBdds,
         |    tmpPre,
         |    tmpNow,
         |    singleConstValuedPredicatesFlag,
         |    multipleConstValuedPredicatesFlag,
         |    multipleValuesPredicatesFlag) = initializeVariables(predictedEvents)
         |
         """.stripMargin)

    writeln(
      """
        |    // Loop through all property variables
        |    for ((varName, varObject) <- vars) {
        |      var equivClassesNum = 0
        |
        |      // Filter relevant multiple values predicates
        |      var relevantMultipleValuesPredicates =
        |        monitor.multipleValuesPredicates.filterKeys {
        |          key =>
        |            val a = key.contains(varName)
        |            val b = key.forall(item => wrappedPattern.findFirstIn(item).isDefined)
        |            a || b && multipleConstValuedPredicatesFlag
        |        }
        |
        |      // If false, we are now going to handle multiple values predicates,
        |      // so in the next iteration the handled multiple values predicates will be ignored.
        |      if (relevantMultipleValuesPredicates.nonEmpty) {
        |        for (key <- relevantMultipleValuesPredicates.keys) {
        |          if (multipleValuesPredicatesFlag(key).value) {
        |            multipleValuesPredicatesFlag(key).value = false
        |          } else {
        |            relevantMultipleValuesPredicates = relevantMultipleValuesPredicates.filterKeys(k => !(k sameElements key))
        |          }
        |        }
        |      }
        |
        |      var varAssignments = varObject.bdds
        |      val unseenValue = generateValue(varAssignments)
        |      varAssignments += (unseenValue -> null)
        |
        |      for (assignment <- varAssignments) {
        |        equivClassesNum += 1
        |
        |        // Fetch one value of corresponds isomorphic assignment.
        |        val fetchedValue = assignment._1.toString
        |
        |        // Filter relevant event types
        |        val filteredEvents = monitor.eventsInVars.filterKeys(_ == varName)
        |        // Include eventsInConstants if singleConstValuedPredicatesFlag is true
        |        val allRelevantEvents = if (singleConstValuedPredicatesFlag) filteredEvents ++ monitor.eventsInConstants else filteredEvents
        |
        |        // If false, we are now going to handle constants,
        |        // so in the next iteration the handled constants will be ignored.
        |        singleConstValuedPredicatesFlag = false
        |
        |        // Process Events
        |        if (allRelevantEvents.nonEmpty) {
        |          processRelevantEvents()
        |        }
        |        if (relevantMultipleValuesPredicates.nonEmpty) {
        |          processMultipleValuesPredicates()
        |        }
        |        debug(s"##### Total Equivalence classes for variable=$varName and for k=$k is $equivClassesNum #####")
        |
        |        // Function to process an event
        |        def processEvent(eventList: List[String], currentValues: List[String], relevantVars: List[Variable]): Unit = {
        |          eventList.foreach { event =>
        |            // Store the current state before taking the recursion step
        |            storePreviousState(monitor, relevantVars.toArray, tmpNow, tmpPre, tmpStat, tmpVariableBdds)
        |            tmpEventTable = monitor.statistics.eventTable
        |
        |            for (index <- relevantVars.indices) {
        |                tmpVarBdds(index) = relevantVars(index).bdds
        |             }
        |
        |            // Submit the event with the current values
        |            monitor.submit(event, currentValues)
        |
        |            // Calculate the event error and append the event to the predicted events list
        |            val eventError = if (tmpStat(1) < monitor.errors) 0 else 1
        |            tmpPredictedEvents.append((event, currentValues.mkString(","), eventError))
        |
        |            // Continue prediction if a verdict hasn't been found
        |            if (!Options.FOUND_VERDICT) {
        |              prediction(k - 1, tmpPredictedEvents)
        |            }
        |
        |            // Recover the previous state when arriving here
        |            tmpPredictedEvents = tmpPredictedEvents.dropRight(k)
        |            restorePreviousState(monitor, relevantVars.toArray, tmpNow, tmpPre, tmpStat, tmpVariableBdds)
        |            monitor.statistics.eventTable = tmpEventTable
        |
        |            for (index <- relevantVars.indices) {
        |               relevantVars(index).bdds = tmpVarBdds(index)
        |             }
        |
        |          }
        |        }
        |
        |        // Processes relevant events for a given variable and fetched value.
        |        def processRelevantEvents(): Unit = {
        |
        |          // Iterate through all relevant events
        |          allRelevantEvents.foreach { case (value, eventList) =>
        |
        |            // Determine the value to be used for evaluation
        |            val evaluationValue: String = if (value != varName) value else fetchedValue
        |
        |            // Process event
        |            processEvent(eventList, List(evaluationValue), List(varObject))
        |          }
        |        }
        |
        |        /**
        |          * Processes relevant multiple values predicates for a given variable and fetched value.
        |          */
        |        def processMultipleValuesPredicates(): Unit = {
        |          // Iterate through all relevant multiple values predicates
        |          for ((values, eventList) <- relevantMultipleValuesPredicates) {
        |            // Call the helper function processValue with the initial index and an empty list
        |            processValue(0, List.empty[String], List.empty[Variable])
        |
        |            // Helper function to process each value in the values array and submit the event
        |            def processValue(index: Int, currentValues: List[String], relevantVars: List[Variable]): Unit = {
        |              if (index < values.length) {
        |                val item = values(index)
        |
        |                // Match the item and process accordingly
        |                item match {
        |                  case wrappedPattern(value) =>
        |                    processValue(index + 1, currentValues :+ value, relevantVars)
        |
        |                  // If the item is a variable, append the fetched value and continue processing
        |                  case variable if variable == varName =>
        |                    val updatedRelevantVars: List[Variable] = relevantVars :+ varObject
        |                    processValue(index + 1, currentValues :+ fetchedValue, updatedRelevantVars)
        |
        |                  // If the item is not the current variable, process it as another variable
        |                  case _ if item != varName =>
        |                    val relevantVar = G.varMap.filterKeys(_.contains(item))
        |                    if (relevantVar.nonEmpty) {
        |                      val varObject = relevantVar(item)
        |                      var varAssignments = varObject.bdds
        |                      val unseenValue = generateValue(varAssignments)
        |                      varAssignments += (unseenValue -> null)
        |                      val updatedRelevantVars: List[Variable] = relevantVars :+ varObject
        |
        |                      for (assignment <- varAssignments) {
        |
        |                        // Fetch one value
        |                        val anotherFetchedValue = assignment._1.toString
        |                        processValue(index + 1, currentValues :+ anotherFetchedValue, updatedRelevantVars)
        |                      }
        |                    }
        |                }
        |              } else {
        |                // Process event
        |                processEvent(eventList, currentValues, relevantVars)
        |              }
        |            }
        |          }
        |        }
        |
        |        multipleConstValuedPredicatesFlag = false
        |      }
        |    }
        |  }
        |
        |  /**
        |    * In this function we fetching a value which will use for the prediction.
        |    * The fetching process search for a matching value between the 'ia' BDD and the variable
        |    * related values. In a case where no matching was found (should happens only when '11...11'
        |    * appears on is own in equivalence class), we generate a new value which is not seen yet.
        |    *
        |    * @param ia a BDD which hold isomorphic values related to the
        |    *           current variable in the prediction process.
        |    * @param v  the current variable in the prediction process.
        |    * @return the fetched/generated value.
        |    */
        |  def generateValue(varAssignments: Map[Any, BDD]): String = {
        |    var tmpVal = if (varAssignments.nonEmpty) varAssignments.head._1.toString else ""
        |    tmpVal += "1"
        |    var foundNewValue = false
        |    while (!foundNewValue) {
        |      if (varAssignments.contains(tmpVal)) {
        |        tmpVal += "1"
        |      } else {
        |        foundNewValue = true
        |      }
        |    }
        |    tmpVal
        |    }
        |}
       """.stripMargin)
    writeln(
      s"""
         | // ### Prediction Extension ###
         |class SmartPrediction(monitor: Monitor) extends Prediction(monitor) {
         |
         |   /***
         |     * The main prediction function, which is works by recursion.
         |     *
         |     * @param k               the k step for the prediction
         |     * @param predictedEvents   the predicts event seen so far
         |     */
         |     def prediction(k: Int, predictedEvents: ListBuffer[(String, String, Int)]): Unit = {
         |
         |       // Recursion Base case
         |       if (k == 0) {
         |         handleBaseCase(predictedEvents)
         |         return
         |       }
        """.stripMargin)
    writeln(
      """
        |      // Initialize necessary variables
        |      var
        |      (tmpPredictedEvents,
        |      tmpEventTable,
        |      tmpVarBdds,
        |      tmpStat,
        |      tmpVariableBdds,
        |      tmpPre,
        |      tmpNow,
        |      singleConstValuedPredicatesFlag,
        |      multipleConstValuedPredicatesFlag,
        |      multipleValuesPredicatesFlag) = initializeVariables(predictedEvents)
        |
        |       // Loop through all property variables
        |       for ((varName, varObject) <- vars) {
        |         var (ghBdd, gQuantVars) = calculateGhBdd(varName, varObject, formula)
        |         var equivClassesNum = 0
        |
        |         // Filter relevant multiple values predicates
        |         var relevantMultipleValuesPredicates =
        |             monitor.multipleValuesPredicates.filterKeys {
        |               key =>
        |                 val a = key.contains(varName)
        |                 val b = key.forall(item => wrappedPattern.findFirstIn(item).isDefined)
        |                 a || b && multipleConstValuedPredicatesFlag
        |             }
        |
        |         // If false, we are now going to handle multiple values predicates,
        |         // so in the next iteration the handled multiple values predicates will be ignored.
        |         if(relevantMultipleValuesPredicates.nonEmpty) {
        |           for (key <- relevantMultipleValuesPredicates.keys) {
        |             if (multipleValuesPredicatesFlag(key).value) {
        |               multipleValuesPredicatesFlag(key).value = false
        |             } else {
        |               relevantMultipleValuesPredicates = relevantMultipleValuesPredicates.filterKeys(k => k != key)
        |             }
        |           }
        |          }
        |
        |         while (!ghBdd.equals(G.False) && ghBdd != null) {
        |           equivClassesNum += 1
        |
        |           // Represent satisfying assignment and its isomorphic assignments for a given ghBdd
        |           val iaBDD = findSatisfyingAssignment(ghBdd, varObject, gQuantVars)
        |
        |           // Removes all isomorphic assignments of 'satAssignmentG' from 'gh' BDD
        |           ghBdd = ghBdd.and(iaBDD.not())
        |
        |           // Fetch one value of corresponds isomorphic assignment.
        |           val fetchedValue = fetchingValue(iaBDD, varObject)
        |
        |           // Filter relevant event types
        |           val filteredEvents = monitor.eventsInVars.filterKeys(_ == varName)
        |           // Include eventsInConstants if singleConstValuedPredicatesFlag is true
        |           val allRelevantEvents = if (singleConstValuedPredicatesFlag) filteredEvents ++ monitor.eventsInConstants else filteredEvents
        |
        |           // If false, we are now going to handle constants,
        |           // so in the next iteration the handled constants will be ignored.
        |           singleConstValuedPredicatesFlag = false
        |
        |           // Process Events
        |           if(allRelevantEvents.nonEmpty) { processRelevantEvents() }
        |           if(relevantMultipleValuesPredicates.nonEmpty) { processMultipleValuesPredicates() }
        |           debug(s"##### Total Equivalence classes for variable=$varName and for k=$k is $equivClassesNum #####")
        |
        |
        |           // Function to process an event
        |           def processEvent(eventList: List[String], currentValues: List[String], relevantVars: List[Variable]): Unit = {
        |             eventList.foreach { event =>
        |               // Store the current state before taking the recursion step
        |               storePreviousState(monitor, relevantVars.toArray, tmpNow, tmpPre, tmpStat, tmpVariableBdds)
        |               tmpEventTable = monitor.statistics.eventTable
        |
        |               for (index <- relevantVars.indices) {
        |                  tmpVarBdds(index) = relevantVars(index).bdds
        |               }
        |
        |               // Submit the event with the current values
        |               monitor.submit(event, currentValues)
        |
        |               // Calculate the event error and append the event to the predicted events list
        |               val eventError = if (tmpStat(1) < monitor.errors) 0 else 1
        |               tmpPredictedEvents.append((event, currentValues.mkString(","), eventError))
        |
        |               // Continue prediction if a verdict hasn't been found
        |               if (!Options.FOUND_VERDICT) {
        |                 prediction(k - 1, tmpPredictedEvents)
        |               }
        |
        |               // Recover the previous state when arriving here
        |               tmpPredictedEvents = tmpPredictedEvents.dropRight(k)
        |               restorePreviousState(monitor, relevantVars.toArray, tmpNow, tmpPre, tmpStat, tmpVariableBdds)
        |               monitor.statistics.eventTable = tmpEventTable
        |
        |
        |               for (index <- relevantVars.indices) {
        |                 relevantVars(index).bdds = tmpVarBdds(index)
        |               }
        |             }
        |           }
        |
        |           // Processes relevant events for a given variable and fetched value.
        |           def processRelevantEvents(): Unit = {
        |
        |             // Iterate through all relevant events
        |             allRelevantEvents.foreach { case (value, eventList) =>
        |
        |                // Determine the value to be used for evaluation
        |                 val evaluationValue: String = if (value != varName) value else fetchedValue
        |
        |                 // Process event
        |                 processEvent(eventList, List(evaluationValue), List(varObject))
        |             }
        |           }
        |
        |           /**
        |             * Processes relevant multiple values predicates for a given variable and fetched value.
        |             */
        |           def processMultipleValuesPredicates(): Unit = {
        |             // Iterate through all relevant multiple values predicates
        |             for ((values, eventList) <- relevantMultipleValuesPredicates) {
        |                 // Call the helper function processValue with the initial index and an empty list
        |                 processValue(0, List.empty[String], List.empty[Variable])
        |
        |                 // Helper function to process each value in the values array and submit the event
        |                 def processValue(index: Int, currentValues: List[String], relevantVars: List[Variable]): Unit = {
        |                   if (index < values.length) {
        |                     val item = values(index)
        |
        |                     // Match the item and process accordingly
        |                     item match {
        |                       case wrappedPattern(value) =>
        |                         processValue(index + 1, currentValues :+ value, relevantVars)
        |
        |                       // If the item is a variable, append the fetched value and continue processing
        |                       case variable if variable == varName =>
        |                         val updatedRelevantVars: List[Variable] = relevantVars :+ varObject
        |                         processValue(index + 1, currentValues :+ fetchedValue, updatedRelevantVars)
        |
        |                       // If the item is not the current variable, process it as another variable
        |                       case _ if item != varName =>
        |                         val relevantVar = G.varMap.filterKeys(_.contains(item))
        |                         if (relevantVar.nonEmpty) {
        |                           val varObject = relevantVar.getOrElse(item, null)
        |                           val updatedRelevantVars: List[Variable] = relevantVars :+ varObject
        |                           var (ghBdd, gQuantVars) = calculateGhBdd(item, varObject, formula)
        |                           while (!ghBdd.equals(G.False) && ghBdd != null && varObject != null) {
        |                             val iaBDD = findSatisfyingAssignment(ghBdd, varObject, gQuantVars)
        |                             // Removes all isomorphic assignments of 'satAssignmentG' from 'gh' BDD
        |                             ghBdd = ghBdd.and(iaBDD.not())
        |
        |                             // Fetch one value of corresponds isomorphic assignment
        |                             val anotherFetchedValue = fetchingValue(iaBDD, varObject)
        |                             processValue(index + 1, currentValues :+ anotherFetchedValue, updatedRelevantVars)
        |                           }
        |                         }
        |                     }
        |                   } else {
        |                     // Process event
        |                     processEvent(eventList, currentValues, relevantVars)
        |                   }
        |                 }
        |             }
        |           }
        |           multipleConstValuedPredicatesFlag = false
        |         }
        |       }
        |     }
        |
        |   /**
        |    * Calculates the ghBdd and gQuantVars values for a given variable and formula.
        |    *
        |    * @param varName    The name of the variable.
        |    * @param varObject  The Variable object associated with the variable name.
        |    * @return           A tuple containing the calculated ghBdd (BDD) and gQuantVars (Array[BDDVarSet]).
        |    */
        |   def calculateGhBdd(varName: String, varObject: Variable, F: Formula): (BDD, BDD) = {
        |     var ghBdd = G.True
        |     val varBits = varObject.bits
        |     val varBitsLength = varBits.length
        |
        |     val gArrayBits = Array.range(G.totalNumberOfBits, G.totalNumberOfBits + varBitsLength)
        |     val gQuantVars = G.getQuantVars(gArrayBits)
        |
        |     // Loop through all tmp relations, represent as BDDs
        |     // The tmp array holds the latest relations.
        |     for (i <- formula.tmp.indices) {
        |       val otherQuantVars = G.otherQuantVars(varName)
        |
        |       // If the target BDD is true or false, this has no influence on the result.
        |       if (!formula.tmp(i).isZero && !formula.tmp(i).isOne) {
        |         val isoBdd = IsomorphicPairsCalculator(varObject, otherQuantVars, formula.tmp(i), gArrayBits)
        |         ghBdd = ghBdd.and(isoBdd)
        |       }
        |     }
        |
        |     (ghBdd, gQuantVars)
        |   }
        |
        |   /**
        |    * Finds a satisfying assignment and its isomorphic assignments for a given ghBdd, varObject, and gQuantVars.
        |    *
        |    * @param ghBdd      The BDD object representing the GH relation.
        |    * @param varObject  The Variable object associated with the variable name.
        |    * @param gQuantVars The Array of BDDVarSet representing the quantifier variables.
        |    * @return           A tuple containing the satisfying assignment (satAssignmentG: BDD) and its isomorphic assignments (iaBDD: BDD).
        |    */
        |   def findSatisfyingAssignment(ghBdd: BDD, varObject: Variable, gQuantVars: BDD): BDD = {
        |     // Get one satisfying assignment
        |     val satAssignmentG = ghBdd.satOne(gQuantVars, true).exist(varObject.quantvar)
        |
        |     // Gets all isomorphic assignments of 'satAssignmentG'
        |     // 'ia' for isomorphic assignments
        |     val iaBDD = ghBdd.restrict(satAssignmentG)
        |
        |     iaBDD
        |   }
        |
        |  /**
        |   * In this function we fetching a value which will use for the prediction.
        |   * The fetching process search for a matching value between the 'ia' BDD and the variable
        |   * related values. In a case where no matching was found (should happens only when '11...11'
        |   * appears on is own in equivalence class), we generate a new value which is not seen yet.
        |   *
        |   * @param ia  a BDD which hold isomorphic values related to the
        |   *            current variable in the prediction process.
        |   * @param v   the current variable in the prediction process.
        |   * @return    the fetched/generated value.
        |   */
        |
        |   def fetchingValue(ia: BDD, v: Variable): String = {
        |     val varAssignments = v.bdds
        |     val varAssignmentsInList = new ListBuffer[String]()
        |     for (assignment <- varAssignments) {
        |       val assignmentValue = assignment._1.toString
        |       varAssignmentsInList += assignmentValue
        |       if (!assignment._2.and(ia).equals(G.False)) return assignmentValue
        |     }
        |
        |     // If no matching is found, return the max value (alphanumeric order) + 1
        |     if (varAssignmentsInList.isEmpty) "1" else (varAssignmentsInList.max + 1)
        |   }
        |
        |  /**
        |    * Find all isomorphic variable assignments in a given BDD.
        |    * The returned result is a BDD GH over a bit vector 'g1 . . . gn' and 'h1 . . . hn' such
        |    * that the bit vectors 'g1 . . . gn' and 'h1 . . . hn' represent pairs of values 'g' and 'h', where
        |    * 'g' and 'h' are isomorphic over relation R (represented by the input BDD 'bdd').
        |    *
        |    * @param variable       The variable object, which is the target to calculate isomorphic pairs over 'bdd'.
        |    * @param otherQuantVars The other quant vars.
        |    * @param bdd            The target BDD, which the isomorphic pairs will be searched in it.
        |    * @param gArrayBits     The bits indexes for the bit vector 'g1 . . . gn' (which starts from the last defined variable bits).
        |    * @return               BDD GH over a bit vector 'g1 . . . gn' and 'h1 . . . hn'.
        |    */
        |
        |   def IsomorphicPairsCalculator(variable: Variable, otherQuantVars: List[BDD], bdd: BDD, gArrayBits: Array[Int]): BDD = {
        |
        |     // Extract the target variable corresponding bits
        |     val varBits = variable.bits
        |
        |     // Define pairs of bits.
        |     // Used for future bits order changing
        |     val pairsG = G.B.makePair
        |     val pairsH = G.B.makePair
        |
        |     val hStartBit = varBits.head + 1
        |     val hArrayBits = Array.range(hStartBit,
        |       hStartBit + varBits.length)
        |
        |     pairsG.set(varBits.reverse, gArrayBits)
        |     pairsH.set(gArrayBits, hArrayBits)
        |
        |
        |     var ghBdd = bdd.replace(pairsG).biimp(bdd)
        |     for (quantVar <- otherQuantVars) ghBdd = ghBdd.forAll(quantVar)
        |     ghBdd.replaceWith(pairsH)
        |  }
        |}
        """.stripMargin)

    writeln(
      s"""
         | // ### Prediction Extension ###
         |object TraceMonitor {
         |  private val usage: String =
         | \"\"\"Usage: (--logfile <filename>) [--bits numOfBits] [--mode (debug | profile)]
         |    |         [--prediction num] [--prediction_type (smart | brute)] [--expected_verdict (0 | 1)]
         | \"\"\".stripMargin
         |
         |  def main(args: Array[String]): Unit = {
         |
         |    if (2 <= args.length && args.length <= 14 && args.length % 2 == 0) {
         |      val argMapBuilder = Map.newBuilder[String, Any]
         |      args.sliding(2, 2).toList.collect {
         |        case Array("--logfile", logfile: String) => argMapBuilder.+=("logfile" -> logfile)
         |        case Array("--bits", numOfBits: String) => argMapBuilder.+=("bits" -> numOfBits)
         |        case Array("--mode", mode: String) => argMapBuilder.+=("mode" -> mode)
         |        case Array("--prediction", predictionLength: String) => argMapBuilder.+=("prediction" -> predictionLength)
         |        case Array("--prediction_type", predictionType: String) => argMapBuilder.+=("prediction_type" -> predictionType)
         |        case Array("--expected_verdict", expectedVerdict: String) => argMapBuilder.+=("expected_verdict" -> expectedVerdict)
         |        case Array("--resultfile", resultfile: String) => argMapBuilder.+=("resultfile" -> resultfile)
         |      }
         |
         |      val argMap = argMapBuilder.result()
         |
         |      val logFile = argMap.get("logfile")
         |      val logfilePath = logFile match {
         |        case Some(value) => value.toString
         |        case None =>
         |          println(s"*** program must be called with logfile argument")
         |          println(usage)
         |          return
         |      }
         |
         |      var dir = new File(logfilePath)
         |      if (!dir.exists) {
         |        println(s" ***logfile is not a valid file")
         |        return
         |      }
         |
         |      val resultfile = argMap.get("resultfile")
         |      Options.RESULT_FILE = resultfile match {
         |        case Some(value) => value.toString
         |        case None => "$generatedMonitorsPath/dejavu-results"
         |      }
         |
         |      dir = new File(Options.RESULT_FILE)
         |      if (!dir.getParentFile.exists) {
         |        println(s" ***resultfile parent is not a valid folder")
         |        return
         |      }
         |
         |      val bits = argMap.get("bits")
         |      val bitsValue = bits match {
         |        case Some(value) =>
         |          if (!value.toString.matches(\"\"\"\\d+\"\"\")) {
         |            println(s"*** bits argument must be an integer")
         |            return
         |          } else {
         |            value.toString
         |          }
         |        case None => "20" // Default is 20 bits length
         |      }
         |      Options.BITS = bitsValue.toInt
         |
         |      val prediction = argMap.get("prediction")
         |      val predictionValue = prediction match {
         |        case Some(value) =>
         |          if (!value.toString.matches(\"\"\"\\d+\"\"\")) {
         |            println(s"*** prediction argument must be an integer")
         |            return
         |          } else {
         |            Options.PREDICTION = true
         |            value.toString
         |        }
         |        case None => "0"
         |      }
         |      Options.PREDICTION_K = predictionValue.toInt
         |
         |      val predictionType = argMap.get("prediction_type")
         |      predictionType match {
         |        case Some(value) =>
         |          val predictionTypeValue = value.toString.toLowerCase()
         |          if (predictionTypeValue == "brute") Options.PREDICTION_TYPE = "brute"
         |          else if (predictionTypeValue == "smart") Options.PREDICTION_TYPE = "smart"
         |          else {
         |            println(s"*** prediction type argument must be: smart or brute")
         |            return
         |          }
         |        case None => println("No prediction type was selected (default is smart)")
         |      }
         |
         |      val expectedType = argMap.get("expected_verdict")
         |      expectedType match {
         |        case Some(value) =>
         |          val expectedTypeValue = value.toString
         |          if (expectedTypeValue == "0") Options.EXPECTED_VERDICT = 0
         |          else if (expectedTypeValue == "1") Options.EXPECTED_VERDICT = 1
         |          else {
         |            println(s"*** expected verdict argument must be: 1 or 0")
         |            return
         |          }
         |        case None => println("No expected verdict type was selected (default is None)")
         |      }
         |
         |      val mode = argMap.get("mode")
         |      mode match {
         |        case Some(value) =>
         |          val modeValue = value.toString.toLowerCase()
         |          if (modeValue == "debug") Options.DEBUG = true
         |          else if (modeValue == "profile") Options.PROFILE = true
         |          else {
         |            println(s"*** mode argument must be: debug or profile")
         |            return
         |          }
         |        case None => println("No mode was selected")
         |      }
         |
         |      val m = new PropertyMonitor
         |
         |      try {
         |        openResultFile(Options.RESULT_FILE)
         |        if (Options.PROFILE) {
         |          openProfileFile("dejavu-profile.csv")
         |          m.printProfileHeader()
         |        }
         |        m.submitCSVFile(logfilePath)
         """.stripMargin)

    writeln(
      """
        |       if (Options.PREDICTION) {
        |          if (m.multipleValuesPredicates.isEmpty && m.eventsInVars.isEmpty) {
        |            println(s"*** Prediction is not available for this spec without any predicate with values")
        |          } else {
        |            var prediction: Prediction = null
        |            if (Options.PREDICTION_TYPE == "brute") {
        |              prediction = new BruteForcePrediction(m)
        |            } else {
        |              prediction = new SmartPrediction(m)
        |            }
        |            val events = new ListBuffer[(String, String, Int)]()
        |            prediction.prediction(Options.PREDICTION_K, events)
        |            println(s"### Total Predictions: ${prediction.counter}")
        |          }
        |        } else {
        |          println("Prediction was not activated")
        |        }
        |     } catch {
        |        case e: Throwable =>
        |          println(s"\n*** $e\n")
        |        // e.printStackTrace()
        |      } finally {
        |        closeResultFile()
        |        if (Options.PROFILE) closeProfileFile()
        |      }
        |    } else {
        |      println("*** call with these arguments:")
        |      println(usage)
        |    }
        |  }
        |}
      """.stripMargin)
    closeFile()
    if (DEBUG_AST) {
      // debug(LTL.ruleIndex)
      printDot()
    }
  }

  def duplicateRules: Spec = {
    Spec(properties map (_.duplicateRules))
  }

  override def toString: String =
    properties.mkString("\n")

  def printDot() {
    val pw = new PrintWriter(new File(s"${generatedMonitorsPath}/ast.dot"))
    for (p <- properties) {
      pw.write("digraph G {\n")
      pw.write(p.ltl.toDot)
      for (rule <- p.rules) {
        pw.write(rule.ltl.toDot)
      }
      for ((index1, index2) <- LTL.ruleIndex.getIndexToIndexMap()) {
        val connection = s"  $index1 -> $index2 [style=dotted]\n"
        pw.write(connection)
      }
      pw.write("}\n")
    }
    pw.close
  }
}

object SymbolTable {
  var errors: Boolean = false
  var currentConstruct: Definition = null
  var predicateSignatures: Map[String, Int] = Map()
  var definedEvents: Set[String] = Set()
  var calledEvents: Set[String] = Set()
  var definedMacros: Set[String] = Set()
  var calledMacros: Set[String] = Set()
  var definedRules: Set[String] = Set()
  var definedProperties: Set[String] = Set()
  var insideRuleBody: Boolean = false

  def definedPredicates: Set[String] = definedEvents.union(definedMacros)

  def referredEvents: Set[String] = definedEvents.union(calledEvents)

  def reset(): Unit = {
    errors = false
    currentConstruct = null
    predicateSignatures = Map()
    definedEvents = Set()
    calledEvents = Set()
    definedMacros = Set()
    calledMacros = Set()
    definedRules = Set()
    definedProperties = Set()
    insideRuleBody = false
  }

  def setCurrent(construct: Definition) = {
    currentConstruct = construct
  }

  def verify(condition: Boolean)(msg: String, error: Boolean = true): Unit = {
    if (!condition) {
      val prefix = if (error) "*** error ***: " else "+++ warning +++: "
      println("----------------")
      println(s"$prefix $msg")
      if (currentConstruct != null) println(currentConstruct)
      errors = error || errors
    }
  }

  def addEventDef(name: String, size: Int): Unit = {
    verify(!(definedPredicates contains name))(s"predicate $name is defined more than once")
    definedEvents += name
    addPredicate(name, size)
  }

  /**
    * Adds any definition or call of a predicate
    *
    * @param name name of predicate.
    * @param size number of arguments to predicate.
    */

  def addPredicate(name: String, size: Int): Unit = {
    predicateSignatures.get(name) match {
      case None =>
        predicateSignatures += (name -> size)
      case Some(sizeElsewhere) =>
        verify(size == sizeElsewhere)(
          s"predicate $name has $size argument(s), which is inconsistent with other occurrence")
    }
  }

  def addRule(name: String, size: Int): Unit = {
    definedRules += name
    predicateSignatures.get(name) match {
      case None =>
        predicateSignatures += (name -> size)
      case Some(sizeElsewhere) =>
        verify(size == sizeElsewhere)(
          s"rule $name has $size argument(s), which is inconsistent with other occurrence")
        verify(!definedEvents.contains(name))(
          s"rule $name also defined as an event"
        )
        verify(!definedMacros.contains(name))(
          s"rule $name also defined as a macro"
        )
    }
  }

  def removeRules(): Unit = {
    predicateSignatures --= definedRules
    definedRules = Set()
  }

  def isRule(name: String): Boolean =
    definedRules.contains(name)

  def addPredicateDef(name: String, size: Int): Unit = {
    verify(!(definedPredicates contains name))(s"predicate $name is defined more than once")
    definedMacros += name
    addPredicate(name, size)
  }

  def addPredicateCall(name: String, size: Int): Unit = {
    addPredicate(name, size)
  }

  def verifyPredicateCalls(): Unit = {
    val unusedMacros = definedMacros -- calledMacros
    verify(unusedMacros.isEmpty)(s"the following predicate macros are not called: ${unusedMacros.mkString(",")}", error = false)
    if (!definedEvents.isEmpty) {
      val allPredicates = predicateSignatures.keySet
      val oddCalls = allPredicates -- definedPredicates
      verify(oddCalls.isEmpty)(s"undefined predicate(s): ${oddCalls.mkString(",")}")
      val unusedEvents = definedEvents -- calledEvents
      verify(unusedEvents.isEmpty)(s"unused events: ${unusedEvents.mkString(",")}", error = false)
    }
  }

  def addProperty(name: String): Unit = {
    verify(!definedProperties.contains(name))(s"property $name is defined more than once")
    definedProperties += name
  }
}

case class Environment(vars: Set[String] = Set()) {
  def definesVar(x: String) = vars contains x

  def addVar(x: String): Environment = Environment(vars + x)
}

case class Document(definitions: List[Definition]) {
  type CallGraph = Map[String, Set[String]]

  def isWellformed: Boolean = {
    for (definition <- definitions) {
      SymbolTable.setCurrent(definition)
      definition.checkForm()
    }
    SymbolTable.currentConstruct = null
    !SymbolTable.errors
  }

  /**
    * The following version of this method allows a macro to call other macros defined later.
    * It uses a macro map due to the previous older implementation of this method. A map
    * is strictly speaking not needed with this version.
    */

  def expandMacroCalls: Spec = {
    var macros: List[Macro] = definitions.filter(_.isInstanceOf[Macro]).asInstanceOf[List[Macro]]
    macros = sort(macros)
    val macroQuantVars: Set[String] = macros.flatMap(_.ltl.getQuantifiedVariables).map(_._1).toSet
    LTL.resetQuantVarCounters(macroQuantVars, macros = true)
    macros = macros.map(_.renameQuantVars())
    var properties: List[Property] = definitions.filter(_.isInstanceOf[Property]).asInstanceOf[List[Property]]
    properties = properties.map(_.renameQuantVars())
    for (m <- macros) {
      val macroMap: MacroMap = Map(m.name -> m)
      properties = properties.map(_.expandMacros(macroMap))
    }
    SymbolTable.calledEvents = properties.map(_.getEventPredicates).toSet.flatten
    SymbolTable.verifyPredicateCalls()
    Spec(properties)
  }

  def sort(macros: List[Macro]): List[Macro] = {
    val callGraph: CallGraph = mkCallGraph(macros)
    val callGraphClosed = transitiveClosure(callGraph)
    macros.sortWith {
      case (m1, m2) => callGraphClosed(m1.name).contains(m2.name)
    }
  }

  def mkCallGraph(macros: List[Macro]): CallGraph = {
    val macroNames: Set[String] = macros.map(_.name).toSet
    macros.map(m => (m.name -> m.getPredicates.intersect(macroNames))).toMap
  }

  def transitiveClosure[T](dag: Map[T, Set[T]]) = {
    var tc = Map.empty[T, Set[T]]

    def getItemTC(item: T): Set[T] = tc.get(item) match {
      case None =>
        val itemTC = dag(item) flatMap (x => getItemTC(x) + x)
        tc += (item -> itemTC)
        itemTC
      case Some(itemTC) => itemTC
    }

    dag.keys foreach getItemTC
    tc
  }
}


trait Definition {
  def checkForm(): Unit
}

case class Event(name: String, args: List[String]) extends Definition {
  def checkForm(): Unit = {
    val numberOfArgs = args.size
    val argSet = args.toSet
    SymbolTable.addEventDef(name, numberOfArgs)
    SymbolTable.verify(numberOfArgs == argSet.size)(s"multiple occurrences of the same argument: ${args.mkString(",")}")
  }

  override def toString: String = {
    val argStr = if (args.isEmpty) "" else s"(${args.mkString(",")})"
    s"event name" + argStr
  }
}

case class Macro(name: String, args: List[String], ltl: LTL) extends Definition {
  def getPredicates: Set[String] = ltl.getPredicates

  def checkForm: Unit = {
    val numberOfArgs = args.size
    val argSet = args.toSet
    SymbolTable.addPredicateDef(name, numberOfArgs)
    SymbolTable.verify(numberOfArgs == argSet.size)(s"multiple occurrences of the same argument: ${args.mkString(",")}")
    val freeVariables = ltl.getFreeVariables
    for (x <- argSet) {
      SymbolTable.verify(freeVariables contains x)(s"formula does not refer to parameter $x")
    }
    ltl.checkForm(Environment(argSet))
  }

  def renameQuantVars(): Macro = {
    val ltlRenamed = ltl.renameQuantVars()
    Macro(name, args, ltlRenamed)
  }

  override def toString: String =
    s"pred $name(${args.mkString(",")}) = $ltl"
}

case class Property(name: String, ltl: LTL, rules: List[Rule]) extends Definition {

  def checkForm: Unit = {
    SymbolTable.addProperty(name)
    for (Rule(name, args, _) <- rules) {
      SymbolTable.addRule(name, args.size)
    }
    ltl.checkForm(Environment())
    SymbolTable.insideRuleBody = true
    rules foreach (_.checkForm())
    SymbolTable.insideRuleBody = false
    SymbolTable.removeRules()
  }

  def getEventPredicates: Set[String] = {
    val ruleNames = rules.map(_.name)
    val rulePredicates = rules.flatMap(_.getPredicates).toSet
    val allPredicates = ltl.getPredicates.union(rulePredicates)
    allPredicates.filterNot(ruleNames.contains(_))
  }

  def getQuantifiedVariables: List[(String, Boolean)] = {
    val quantifiedVariables =
      ltl.getQuantifiedVariables ++ rules.flatMap(rule => rule.ltl.getQuantifiedVariables)
    var result: List[(String, Boolean)] = Nil
    for (pair@(name, mode) <- quantifiedVariables) {
      result.collectFirst { case (`name`, mode) => mode } match {
        case None => result :+= pair
        case Some(previousMode) =>
          assert(previousMode == mode, s"quantified variable $name occurring with different modes")
      }
    }
    result
  }

  def expandMacros(macros: MacroMap): Property = {
    val ltlExpanded: LTL = ltl.expandMacros(macros)
    val rulesExpanded: List[Rule] = rules.map(_.expandMacros(macros))
    Property(name, ltlExpanded, rulesExpanded)
  }

  def duplicateRules: Property = {
    val ruleNames = rules.map(_.name)
    var ruleCalls = getPredicateTerms.filter(p => ruleNames.contains(p.name))
    var newRules = rules
    while (!ruleCalls.isEmpty) {
      val ruleCall = ruleCalls.head
      ruleCalls = ruleCalls.tail
      val ruleHead = ruleCall.getAsRuleHead
      if (!newRules.exists(_.hasRuleHead(ruleHead))) {
        val oldRule = getRuleWithName(ruleHead.name)
        val oldArgs = oldRule.args
        val newArgs = ruleHead.args
        val substitution = oldArgs.zip(newArgs.map(VPat(_))).toMap
        val newRule = oldRule.substituteRuleBody(substitution)
        newRules :+= newRule
        ruleCalls ++= newRule.getPredicateTerms.filter(p => ruleNames.contains(p.name) && p != ruleCall)
      }
    }
    Property(name, ltl, newRules)
  }

  def getRuleWithName(ruleName: String): Rule =
    rules.find(_.name == ruleName) match {
      case Some(rule) => rule
      case None => error(s"Rule with name $ruleName does not exist")
    }

  def getPredicateTerms: Set[Pred] =
    ltl.getPredicateTerms.union(rules.flatMap(_.ltl.getPredicateTerms).toSet)

  def renameQuantVars(): Property = {
    val quantifiers: List[String] = ltl.getQuantifiedVariables.map(_._1)
    val duplicates: Set[String] = quantifiers.filter(x => quantifiers.count(_ == x) > 1).toSet
    if (duplicates.isEmpty) {
      this
    } else {
      LTL.resetQuantVarCounters(duplicates)
      val ltlRenamed = ltl.renameQuantVars()
      Property(name, ltlRenamed, rules)
    }
  }

  override def toString: String = {
    var ruleDefs = ""
    if (rules != Nil) {
      ruleDefs += "\n  where\n"
      for (rule <- rules) {
        ruleDefs += s"    $rule\n"
      }
    }
    s"prop $name : $ltl $ruleDefs"
  }
}

case class Rule(name: String, args: List[String], ltl: LTL) extends Definition {
  def getPredicates: Set[String] = ltl.getPredicates

  def checkForm(): Unit = {
    val argSet = args.toSet
    SymbolTable.verify(args.size == argSet.size)(s"multiple occurrences of the same argument: ${args.mkString(",")}")
    val freeVariables = ltl.getFreeVariables
    for (x <- argSet) {
      SymbolTable.verify(freeVariables contains x)(s"formula does not refer to parameter $x")
    }
    ltl.checkForm(Environment(argSet))
  }

  def expandMacros(macros: MacroMap): Rule = {
    val ltlExpanded = ltl.expandMacros(macros)
    Rule(name, args, ltlExpanded)
  }

  def getQuantifiedVariables: List[(String, Boolean)] =
    ltl.getQuantifiedVariables

  def renameQuantVars(): Rule = {
    val ltlRenamed = ltl.renameQuantVars()
    Rule(name, args, ltlRenamed)
  }

  def hasRuleHead(r: RuleHead): Boolean = {
    r.name == name && r.args == args
  }

  def getPredicateTerms: Set[Pred] =
    ltl.getPredicateTerms

  def substitute(subst: Substitution): Rule = {
    val substFlattened = for ((x, VPat(y)) <- subst) yield (x -> y)
    val argsNew = args.map(substFlattened(_))
    val ltlNew = ltl.substitute(subst)
    Rule(name, argsNew, ltlNew)
  }

  def substituteRuleBody(subst: Substitution): Rule = {
    val substFlattened = for ((x, VPat(y)) <- subst) yield (x -> y)
    val argsNew = args.map(substFlattened(_))
    val ltlNew = ltl.substituteRuleBody(subst)
    Rule(name, argsNew, ltlNew)
  }

  override def toString: String =
    s"$name(${args.mkString(",")}) := $ltl"
}

trait LTL {

  /**
    * Index of formula, used to index in <code>now</code> and <code>pre</code> arrays.
    */

  var index: Int = 0

  /**
    * True for the top level formula of a rule body. This directs how this formula
    * is displayed in dot format.
    */

  var isRuleBodyTop: Boolean = false

  /**
    * Sets the variable <code>isBelowPrevious</code> to true in this node and all
    * subnodes.
    *
    * @return the same LTL formula (this).
    */

  def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    this
  }

  /**
    * True for an LTL formula that occurs below the <code>@</code> operator
    * (previous time).
    */

  implicit var isBelowPrevious: Boolean = false

  /**
    * True for predicate calls that represent calls to rules.
    *
    * @return true iff this is an object of class <code>Pred</code>,
    *         which is a call to a rule.
    */

  def isRuleCall: Boolean = false

  /**
    * Verifies whether this LTL formula is wellformed (type checking, static analysis).
    * The method accesses the global <code>SymbolTable</code> as well as the local
    * symbol table, named an environment <code>env</code>, passed as parameter.
    *
    * @param env the local symbol table environment.
    */

  def checkForm(env: Environment): Unit = {}

  /**
    * Verifies a particular wellformedness condition. Sets <code>errors</code> flag in symbol table in
    * case the condition is false.
    *
    * @param condition the condition to be verified.
    * @param msg       message to be printed in case it is violated.
    */

  def verify(condition: Boolean)(msg: String, error: Boolean = true): Unit = {
    SymbolTable.verify(condition)(s"$msg - in: $this", error)
  }

  /**
    * Get all quantified variables (and whether they are bounded or not - true = yes)
    * in an LTL term. That is, all variables defined in an existential or universal
    * quantification. Note that this is not the list of free variables in the LTL formula.
    *
    * @return the list of quantified variables and their boundedness status in the LTL term.
    */

  def getQuantifiedVariables: List[(String, Boolean)] = Nil

  /**
    * Get all free variables in an LTL term. That is, all variables not occurring under a
    * corresponding quantification over that same variable.
    *
    * @return the set of free variables.
    */

  def getFreeVariables: Set[String] = Set()

  /**
    * Returns the names of predicates called in a LTL formula. This includes macro calls.
    *
    * @return names of the called predicates.
    */

  def getPredicates: Set[String] = Set()

  /**
    * Returns the predicates referred to in a LTL formula.
    *
    * @return predicates referred to in the LTL formula.
    */

  def getPredicateTerms: Set[Pred] = Set()

  /**
    * Expands calls of macros.
    *
    * @param macros the macros defined in the specification document.
    * @return the LTL formula where macros have been expanded.
    */

  def expandMacros(macros: MacroMap): LTL = this

  /**
    * Renames quantifiers that are introduced more than once, as for example
    * <code>forall x . (p(x) & exists x . q(x))</code>
    * becomes:
    * <code>forall x1 . (p(x1) & exists x2 . q(x2))</code>.
    *
    * @return the renamed LTL formula.
    */

  def renameQuantVars(): LTL = this

  /**
    * Renames a specific quantified expression, given the name quantified over and the body of
    * the quantification. Reused in the four different quantified expressions in DejaVu.
    *
    * @param name the name of the quantified variable.
    * @param ltl  the body of the quantified expression.
    * @return the new name (if renamed) and the renamed body.
    */

  def renameQuantification(name: String, ltl: LTL): (String, LTL) = {
    LTL.quantVarCounters.get(name) match {
      case None =>
        val ltlRenamed = ltl.renameQuantVars()
        (name, ltlRenamed)
      case Some(count) =>
        val newName = variableWithCounter(name, count)
        LTL.quantVarCounters += (name -> (count + 1))
        val ltlSubstituted = ltl.substitute(Map(name -> VPat(newName)))
        val ltlRenamed = ltlSubstituted.renameQuantVars()
        (newName, ltlRenamed)
    }
  }

  /**
    * Produces a new name based on an old quantified variable name and a counter. The result
    * depends on whether we are renaming a macro LTL body or a property LTL body.
    * E.g x and 5 becomes x_5 in property mode and _x_5 in macro mode. The variable
    * <code>LTL.renameMacros</code> indicates whether it is macro mode.
    *
    * @param name  the quantified variable name to be renamed.
    * @param count the variable counter.
    * @return the renamed variable.
    */

  def variableWithCounter(name: String, count: Int): String = {
    var newName = name + "_" + count
    if (LTL.renameMacros) {
      newName = "_" + newName
    }
    newName
  }

  def substitute(subst: Substitution) = this

  def substituteRuleBody(subst: Substitution) = this

  /**
    * Substitutes a variable name for a variable name. Substituting a constant for a variable name results in an error.
    *
    * @param varName the variable name being substituted.
    * @param subst   the substitution.
    * @return the LTL expression after application of substitution.
    */

  def substituteVarForVar(varName: String, subst: Substitution): String = {
    subst.get(varName) match {
      case None => varName
      case Some(constOrVar) =>
        constOrVar match {
          case VPat(varNameNew) =>
            varNameNew
          case CPat(constant) =>
            error(
              "replacing variable with constant in relation not allowed",
              this,
              varName,
              constant)
        }
    }
  }

  def translate(): Int = {
    index = LTL.getIndex()
    LTL.subExpressions ++= List(this)
    index
  }

  def toDot: String = {
    val label = quote(index + " : " + toString)
    if (isRuleCall) {
      s"  $index [shape=invhouse, color=purple, label=$label]\n"
    } else if (isRuleBodyTop) {
      s"  $index [shape=tab, color=blue, label=$label]\n"
    } else if (index == 0) {
      s"  $index [shape=octagon, color=red, label=$label]\n"
    } else {
      s"  $index [shape=box, label=$label]\n"
    }
  }
}

object LTL {
  /**
    * Counter used to keep track of sub-formula indexes.
    */

  var next: Int = 0

  /**
    * Stores the different time limits occurring in timed formulas. E.g
    * the formula `p S[<=k] q` will cause `k` to be stored. The set is used
    * to generate the `timeLimitMap` which is looked up in the semantics of
    * the timed formulas.
    */

  var timeLimits : Set[Int] = Set()

  /**
    * Will after translation of the current LTL property contain all its sub-formulas
    * in positions corresponding to their indexes. This is used to annotate debugging
    * output with which formulas correspond to which indexes.
    */

  var subExpressions: List[LTL] = Nil

  /**
    * Will after translation of the current LTL property contain the indexes of past time
    * formulas, such as S (since), @ (previous), P (sometime), etc. This is used by the
    * garbage collection algorithm.
    */

  var indicesOfPastTimeFormulas: List[Int] = Nil

  /**
    * Stores a counter for each quantified variable <code>x</code>, which is introduced by a quantifier more than
    * once in an LTL formula. Used to rename such variables, as in: <code>x1, x2, x3, ...</code>.
    */

  var quantVarCounters: Map[String, Int] = Map()

  /**
    * Boolean flag being true if properties are being renamed, in contrast to macros
    */

  var renameMacros: Boolean = false

  def resetQuantVarCounters(vars: Set[String], macros: Boolean = false): Unit = {
    LTL.quantVarCounters = vars.map(_ -> 1).toMap
    renameMacros = macros
  }

  /**
    * Compute the index for the next sub-formula.
    *
    * @return the next index.
    */

  def getIndex(): Int = {
    val index = next
    next += 1
    index
  }

  /**
    * Any variables used in a relation will be added to this set.
    */

  var varsInRelations: Set[String] = Set()

  /**
    * Stores information about which AST nodes represent calls of rules, and which
    * AST nodes define the head of rules.
    */

  var ruleIndex: RuleIndex = new RuleIndex

  /**
    * Lists storing assignment statements generated during translation to the
    * <code>now</code> array. They are updated in mixed order but are printed out
    * in order 1 to 5.
    *
    * <code>assignments1</code> will be printed out first (corresponds to
    * events in leaf nodes). Leaf node events must be printed first to provide
    * variable bindings for evaluating relations.
    * That is: <code>Pred & !isRuleCall</code>.
    *
    * <code>assignments2</code> will be printed out thereafter. These
    * correspond to non-leaf rule nodes that do not occur below a @-operator in rules.
    * That is: <code>!updatingMainFormula & !Pred & !isBelowPrevious</code>.
    *
    * <code>assignments3</code> will be printed out thereafter. They correspond
    * to rule calls being assigned the values of finalized
    * rule body values. This includes recursive calls of these as well as calls of
    * rules in the main formula.
    * That is: <code>isRuleCall</code>.
    *
    * <code>assignments4</code> Will be printed out thereafter. These are the remaining
    * rule nodes. That is: nodes that occur below the @-operator, and which
    * are not recursive rule calls and not leaf nodes.
    * That is: <code>!updatingMainFormula & !Pred & isBelowPrevious</code>.
    *
    * <code>assignments5</code> will be printed out last. This is the main formula's
    * non-leaf nodes.
    * That is: <code>updatingMainFormula & !Pred</code>.
    */

  var assignments1: List[String] = Nil
  var assignments2: List[String] = Nil
  var assignments3: List[String] = Nil
  var assignments4: List[String] = Nil
  var assignments5: List[String] = Nil

  /**
    * Is true when the code for the main formula is generated, and is false when the
    * code for the rules is generated. This drives which of the assignment variables
    * above are assigned to.
    */

  var updatingMainFormula: Boolean = true

  /**
    * Create an assignment statement to <code>now(index)</code>.
    *
    * @param index the index of <code>now</code> to assign to.
    * @param rhs   the right-hand side of the assignment statement.
    * @return the assignment statement as a string.
    */

  def mkAssignment(index: Int, rhs: String): String = s"      now($index) = $rhs"

  /**
    * Assign first.
    *
    * @param index the index of <code>now</code> to assign to.
    * @param rhs   the right-hand side of the assignment statement.
    */

  def assignFirst(index: Int)(rhs: String): Unit = {
    assignments1 :+= mkAssignment(index, rhs)
  }

  /**
    * Assign there after.
    *
    * Assign to <code>assignments2</code> if it concerns a rule body.
    * Assign to <code>assignments5</code> if it concerns the main formula.
    * This is controlled by the the Boolean variable <code>updatingMainFormula</code>.
    *
    * This is some hairy code dude.
    *
    * @param index the index of <code>now</code> to assign to.
    * @param rhs   the right-hand side of the assignment statement.
    */

  def assign(index: Int)(rhs: String)(implicit isBelowPrevious: Boolean): Unit = {
    val assignment = mkAssignment(index, rhs)
    if (updatingMainFormula) {
      assignments5 :+= assignment
    } else { // translating rules
      if (isBelowPrevious) {
        assignments4 :+= assignment
      } else {
        assignments2 :+= assignment
      }
    }
  }

  /**
    * Connects a call to a rule to the head of the rule. E.g. if a rule <code>r(x)</code>
    * is called at index <code>index1</code>, and this rule's top node is <code>index2</code>,
    * them the following code is generated:
    *
    * <code>now(index1) = now(index2)</code>
    *
    * @param index1 the index of the rule call.
    * @param index2 the index of the rule definition.
    */

  def assignIndexToIndex(index1: Int, index2: Int): Unit = {
    assignments3 :+= mkAssignment(index1, s"now($index2)")
  }

  /**
    * Resets this LTL object for translating a new property.
    */

  def reset(): Unit = {
    next = 0
    timeLimits = Set()
    subExpressions = Nil
    indicesOfPastTimeFormulas = Nil
    renameMacros = false // added
    quantVarCounters = Map() // added
    assignments1 = Nil
    assignments2 = Nil
    assignments3 = Nil
    assignments4 = Nil // added
    assignments5 = Nil
    updatingMainFormula = true
    varsInRelations = Set()
    ruleIndex = new RuleIndex
  }

  /**
    * Translates a property.
    *
    * @param property the property to be translated.
    */

  def translate(property: Property): Unit = {
    reset()
    for (rule <- property.rules) ruleIndex.addRuleName(rule.name)
    // Update main formula:
    updatingMainFormula = true
    property.ltl.translate()
    // Update rules:
    updatingMainFormula = false
    for (rule <- property.rules) {
      val index = rule.ltl.translate()
      ruleIndex.mapCallToIndex(rule.name, rule.args, index)
    }
    // Generate rule calls:
    for ((index1, index2) <- ruleIndex.getIndexToIndexMap()) {
      assignIndexToIndex(index1, index2)
    }
    // Generate assignments:
    writeln("    // assignments1 (leaf nodes that are not rule calls):")
    assignments1 foreach writeln
    writeln("    // assignments2 (rule nodes excluding what is below @ and excluding leaf nodes):")
    assignments2 foreach writeln
    writeln("    // assignments3 (rule calls):")
    assignments3 foreach writeln
    writeln("    // assignments4 (the rest of rules that are below @ and excluding leaf nodes):")
    assignments4 foreach writeln
    writeln("    // assignments5 (main formula excluding leaf nodes):")
    assignments5 foreach writeln
  }
}

/**
  * represents the call of a rule.
  *
  * @param name the name of the rule called.
  * @param args the arguments of the rule call.
  */

case class RuleHead(name: String, args: List[String])

/**
  * Represents information about which AST nodes are calls to rules, and
  * which AST nodes are the heads of rule definitions.
  */

class RuleIndex {
  var ruleNames: Set[String] = Set()
  var indexToCall: Map[Int, RuleHead] = Map()
  var callToIndex: Map[RuleHead, Int] = Map()

  def addRuleName(name: String) = {
    ruleNames += name
  }

  def isRule(name: String): Boolean = {
    ruleNames contains name
  }

  def mapIndexToCall(index: Int, name: String, args: List[String]) {
    indexToCall += (index -> RuleHead(name, args))
  }

  def mapCallToIndex(name: String, args: List[String], index: Int): Unit = {
    callToIndex += (RuleHead(name, args) -> index)
  }

  def getIndexToIndexMap(): Map[Int, Int] = {
    for ((index, call) <- indexToCall) yield (index, callToIndex(call))
  }

  override def toString: String = {
    s"""
       |RuleIndex:
       |==========
       |ruleNames   : $ruleNames
       |indexToCall : ${indexToCall.mkString("\n  ", ",\n  ", "")}
       |callToIndex : ${callToIndex.mkString("\n  ", ",\n  ", "")}
     """.stripMargin
  }
}

case object True extends LTL {
  override def translate(): Int = {
    super.translate()
    LTL.assign(index)("bddGenerator.True")
    index
  }

  override def toString: String = "true"
}

case object False extends LTL {
  override def translate(): Int = {
    super.translate()
    LTL.assign(index)(s"bddGenerator.False")
    index
  }

  override def toString: String = "false"
}

case class Pred(name: String, values: List[ConstOrVar]) extends LTL {

  override def isRuleCall: Boolean = {
    LTL.ruleIndex.isRule(name)
  }

  override def checkForm(env: Environment): Unit = {
    SymbolTable.addPredicateCall(name, values.size)
    for (VPat(x) <- values) {
      verify(env.definesVar(x))(s"variable $x occurs free")
    }
    verify(!(SymbolTable.insideRuleBody & SymbolTable.isRule(name) & !isBelowPrevious))(s"rule call within rule body is not below @-operator")
  }

  override def getFreeVariables: Set[String] = {
    (for (VPat(x) <- values) yield x).toSet
  }

  override def getPredicates: Set[String] = Set(name)

  override def getPredicateTerms: Set[Pred] = Set(this)

  override def translate(): Int = {
    super.translate()
    if (LTL.ruleIndex.isRule(name)) {
      val args = values.map {
        case VPat(x) => x
        case CPat(v) => error(s"Constant value $v as argument to relation call $this")
      }
      LTL.ruleIndex.mapIndexToCall(index, name, args)
    } else {
      val predName = quote(name)
      val patterns = values.map(_.toStringForSynthesis).mkString(",")
      LTL.assignFirst(index)(s"build($predName)($patterns)")
    }
    index
  }

  override def expandMacros(macros: MacroMap): LTL = {
    macros.get(name) match {
      case None => this
      case Some(Macro(_, args, ltl)) =>
        assert(args.size == values.size)
        SymbolTable.calledMacros += name
        val substitution: Substitution = args.zip(values).toMap
        ltl.substitute(substitution)
    }
  }

  override def substitute(subst: Substitution): LTL = {
    val valuesSubstituted = values.map(v => v.substitute(subst))
    Pred(name, valuesSubstituted)
  }

  override def substituteRuleBody(subst: Substitution): LTL =
    substitute(subst)

  def getAsRuleHead: RuleHead = {
    val args =
      values.map {
        case VPat(x) => x
        case CPat(v) => error(s"Constant now allowed in rule application: $this")
      }
    RuleHead(name, args)
  }

  override def toString: String = {
    val patterns =
      if (values.isEmpty) "" else "(" + values.map(_.toString).mkString(",") + ")"
    s"$name$patterns"
  }
}

trait RelOp {
  def getName: String = this.getClass.getSimpleName.dropRight(1)
}

case object LTOP extends RelOp {
  override def toString: String = "<"
}

case object LEOP extends RelOp {
  override def toString: String = "<="
}

case object GTOP extends RelOp {
  override def toString: String = ">"
}

case object GEOP extends RelOp {
  override def toString: String = ">="
}

case object EQOP extends RelOp {
  override def toString: String = "="
}

case class Rel(varName1: String, op: RelOp, varName2: String) extends LTL {

  override def checkForm(env: Environment): Unit = {
    verify(env.definesVar(varName1))(s"variable name $varName1 occurs free")
    verify(env.definesVar(varName2))(s"variable name $varName2 occurs free")
  }

  override def getFreeVariables: Set[String] = Set(varName1, varName2)

  override def translate(): Int = {
    super.translate()
    val varName1Q = quote(varName1)
    val varName2Q = quote(varName2)
    LTL.assign(index)(s"relation($varName1Q,${op.getName},$varName2Q).or(pre($index))")
    LTL.varsInRelations ++= Set(varName1, varName2)
    index
  }

  override def substitute(subst: Substitution): LTL = {
    val varName1Renamed = substituteVarForVar(varName1, subst)
    subst.get(varName2) match {
      case None => Rel(varName1Renamed, op, varName2)
      case Some(constOrVar) =>
        constOrVar match {
          case VPat(varName2Renamed) => Rel(varName1Renamed, op, varName2Renamed)
          case CPat(constant) => RelConst(varName1Renamed, op, constant)
        }
    }
  }

  override def substituteRuleBody(subst: Substitution): LTL =
    substitute(subst)

  override def toString: String = s"$varName1 $op $varName2"
}

case class RelConst(varName: String, op: RelOp, const: Any) extends LTL {

  override def checkForm(env: Environment): Unit = {
    verify(env.definesVar(varName))(s"variable name $varName occurs free")
  }

  override def getFreeVariables: Set[String] = Set(varName)

  override def translate(): Int = {
    super.translate()
    val varNameQ = quote(varName)
    // val constString = const.asInstanceOf[String]
    // val constQ = if (constString.startsWith("\"")) const else quote(const)
    val constQ = quoteIfNotQuoted(const)
    LTL.assign(index)(s"relationToConstant($varNameQ,${op.getName},$constQ).or(pre($index))")
    LTL.varsInRelations ++= Set(varName)
    index
  }

  override def substitute(subst: Substitution): LTL = {
    val varNameRenamed = substituteVarForVar(varName, subst)
    RelConst(varNameRenamed, op, const)
  }

  override def substituteRuleBody(subst: Substitution): LTL =
    substitute(subst)

  override def toString: String = {
    s"$varName $op $const"
    val constant = requoteIfString(const)
    s"$varName $op $constant"
  }
}

case class Paren(ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    Paren(ltl.expandMacros(macros))
  }

  override def renameQuantVars(): LTL = {
    Paren(ltl.renameQuantVars())
  }

  override def substitute(subst: Substitution): LTL = {
    Paren(ltl.substitute(subst))
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    Paren(ltl.substituteRuleBody(subst))
  }

  override def translate(): Int = {
    index = ltl.translate()
    index
  }

  override def toString: String = s"($ltl)"

  override def toDot: String = ltl.toDot
}

case class Not(ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    Not(ltl.expandMacros(macros))
  }

  override def renameQuantVars(): LTL = {
    Not(ltl.renameQuantVars())
  }

  override def substitute(subst: Substitution): LTL = {
    Not(ltl.substitute(subst))
  }

  override def substituteRuleBody(subst: Substitution): LTL =
    Not(ltl.substituteRuleBody(subst))

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"now($index1).not()")
    index
  }

  override def toString: String = s"!$ltl"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class Or(ltl1: LTL, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    Or(ltl1Expanded, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    Or(ltl1Renamed, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    Or(ltl1Renamed, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    Or(ltl1Renamed, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.assign(index)(s"now($index1).or(now($index2))")
    index
  }

  override def toString: String = s"${bracket(ltl1)} | ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class And(ltl1: LTL, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    And(ltl1Expanded, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    And(ltl1Renamed, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    And(ltl1Renamed, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    And(ltl1Renamed, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.assign(index)(s"now($index1).and(now($index2))")
    index
  }

  override def toString: String = s"${bracket(ltl1)} & ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class BiImpl(ltl1: LTL, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    BiImpl(ltl1Expanded, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    BiImpl(ltl1Renamed, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    BiImpl(ltl1Renamed, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    BiImpl(ltl1Renamed, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.assign(index)(s"now($index1).biimp(now($index2))")
    index
  }

  override def toString: String = s"${bracket(ltl1)} <-> ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class Implies(ltl1: LTL, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    Implies(ltl1Expanded, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    Implies(ltl1Renamed, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    Implies(ltl1Renamed, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    Implies(ltl1Renamed, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.assign(index)(s"now($index1).not().or(now($index2))")
    index
  }

  override def toString: String = s"${bracket(ltl1)} -> ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class Since(ltl1: LTL, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] =
    ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    Since(ltl1Expanded, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    Since(ltl1Renamed, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    Since(ltl1Renamed, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    Since(ltl1Renamed, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.assign(index)(s"now($index2).or(now($index1).and(pre($index)))")
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"${bracket(ltl1)} S ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class SinceTimeLE(ltl1: LTL, timeLimit: Int, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] =
    ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    SinceTimeLE(ltl1Expanded, timeLimit, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    SinceTimeLE(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    SinceTimeLE(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    SinceTimeLE(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.timeLimits += timeLimit
    LTL.assign(index)(
      s"""
         |(now($index2).and(zeroTime)).or(
         |  now($index1)
         |  .and(pre($index))
         |  .and(DeltaBDD)
         |  .and(limitMap($timeLimit))
         |  .and(addConst(tBDDList,uBDDList,dBDDList,cBDDList))
         |  .and(gtConst(uBDDListHighToLow,lBDDListHighToLow).not())
         |  .exist(var_t_quantvar)
         |  .exist(var_d_quantvar)
         |  .exist(var_c_quantvar)
         |  .exist(var_l_quantvar)
         |  .replace(u_to_t_map)
         |)""".stripMargin
    )
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"${bracket(ltl1)} S[<=$timeLimit] ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class ZinceTimeLE(ltl1: LTL, timeLimit: Int, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] =
    ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    ZinceTimeLE(ltl1Expanded, timeLimit, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    ZinceTimeLE(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    ZinceTimeLE(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    ZinceTimeLE(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.timeLimits += timeLimit
    LTL.assign(index)(
      s"""
         |now($index1).and(
         |  (pre($index2)
         |   .and(deltaLessThanTimeLimit($timeLimit))
         |   .and(zeroTime)
         |   .and(DeltaBDD)
         |   .and(addConst(tBDDList, uBDDList, dBDDList, cBDDList))
         |   .exist(var_t_quantvar)
         |   .exist(var_d_quantvar)
         |   .exist(var_c_quantvar)
         |   .replace(u_to_t_map)
         |  ).or(
         |    pre($index2).not()
         |    .and(pre($index))
         |    .and(DeltaBDD)
         |    .and(limitMap($timeLimit))
         |    .and(addConst(tBDDList, uBDDList, dBDDList, cBDDList))
         |    .and(gtConst(uBDDListHighToLow, lBDDListHighToLow).not())
         |    .exist(var_t_quantvar)
         |    .exist(var_d_quantvar)
         |    .exist(var_c_quantvar)
         |    .exist(var_l_quantvar)
         |    .replace(u_to_t_map)
         |  )
         |)""".stripMargin
    )
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"${bracket(ltl1)} Z[<=$timeLimit] ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class SinceTimeGT(ltl1: LTL, timeLimit: Int, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] =
    ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    SinceTimeGT(ltl1Expanded, timeLimit, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    SinceTimeGT(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    SinceTimeGT(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    SinceTimeGT(ltl1Renamed, timeLimit, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.timeLimits += timeLimit
    LTL.assign(index)(
      s"""
         |((pre(${index - 1}).not().or(now($index1).not())).and(now($index2)).and(zeroTime)).or(
         |  now($index1)
         |  .and(pre($index))
         |  .and(DeltaBDD)
         |  .and(limitMap($timeLimit))
         |  .and(gtConst(tBDDListHighToLow, lBDDListHighToLow).ite(
         |     uMap($timeLimit),
         |     addConst(tBDDList, uBDDList, dBDDList, cBDDList)
         |   ))
         |   .exist(var_t_quantvar)
         |   .exist(var_d_quantvar)
         |   .exist(var_c_quantvar)
         |   .exist(var_l_quantvar)
         |   .replace(u_to_t_map)
         |)""".stripMargin
    )
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"${bracket(ltl1)} S[>$timeLimit] ${bracket(ltl2)}"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class Previous(ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    Previous(ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    Previous(ltl.renameQuantVars())
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst)
    Previous(ltlRenamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substituteRuleBody(subst)
    Previous(ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"pre($index1)")
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"@ $ltl"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class Sometime(ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    Sometime(ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    Sometime(ltl.renameQuantVars())
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst)
    Sometime(ltlRenamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substituteRuleBody(subst)
    Sometime(ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"now($index1).or(pre($index))")
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"P $ltl"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class History(ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  var rewrite: LTL = Not(Sometime(Not(ltl)))

  override def getQuantifiedVariables: List[(String, Boolean)] = rewrite.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    rewrite = rewrite.expandMacros(macros)
    this
  }

  override def renameQuantVars(): LTL = {
    History(ltl.renameQuantVars())
  }

  override def substitute(subst: Substitution): LTL = {
    rewrite = rewrite.substitute(subst)
    this
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    rewrite = rewrite.substituteRuleBody(subst)
    this
  }

  override def translate(): Int = {
    rewrite.translate()
    index = rewrite.index // check that this works
    index
  }

  // override def toString: String = s"H $ltl"

  override def toString: String = {
    rewrite.toString
  }

  override def toDot: String = {
    rewrite.toDot
  }
}

case class HistoryLE(ltl: LTL, timeLimit: Int) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  var rewrite: LTL = Not(ExistsTime(SinceTimeLE(True, timeLimit, Not(ltl))))

  override def getQuantifiedVariables: List[(String, Boolean)] = rewrite.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    rewrite = rewrite.expandMacros(macros)
    this
  }

  override def renameQuantVars(): LTL = {
    HistoryLE(ltl.renameQuantVars(), timeLimit)
  }

  override def substitute(subst: Substitution): LTL = {
    rewrite = rewrite.substitute(subst)
    this
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    rewrite = rewrite.substituteRuleBody(subst)
    this
  }

  override def translate(): Int = {
    rewrite.translate()
    index = rewrite.index // check that this works
    index
  }

  // override def toString: String = s"H $ltl"

  override def toString: String = {
    rewrite.toString
  }

  override def toDot: String = {
    rewrite.toDot
  }
}

case class HistoryGT(ltl: LTL, timeLimit: Int) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  var rewrite: LTL = Not(ExistsTimeGT(SinceTimeGT(True, timeLimit, Not(ltl)), timeLimit))

  override def getQuantifiedVariables: List[(String, Boolean)] = rewrite.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    rewrite = rewrite.expandMacros(macros)
    this
  }

  override def renameQuantVars(): LTL = {
    HistoryGT(ltl.renameQuantVars(), timeLimit)
  }

  override def substitute(subst: Substitution): LTL = {
    rewrite = rewrite.substitute(subst)
    this
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    rewrite = rewrite.substituteRuleBody(subst)
    this
  }

  override def translate(): Int = {
    rewrite.translate()
    index = rewrite.index // check that this works
    index
  }

  // override def toString: String = s"H $ltl"

  override def toString: String = {
    rewrite.toString
  }

  override def toDot: String = {
    rewrite.toDot
  }
}

case class SometimeLE(ltl: LTL, timeLimit: Int) extends LTL {
  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  var rewrite: LTL = ExistsTime(SinceTimeLE(True, timeLimit, ltl))

  override def getQuantifiedVariables: List[(String, Boolean)] = rewrite.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    rewrite = rewrite.expandMacros(macros)
    this
  }

  override def renameQuantVars(): LTL = {
    SometimeLE(ltl.renameQuantVars(), timeLimit)
  }

  override def substitute(subst: Substitution): LTL = {
    rewrite = rewrite.substitute(subst)
    this
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    rewrite = rewrite.substituteRuleBody(subst)
    this
  }

  override def translate(): Int = {
    rewrite.translate()
    index = rewrite.index // check that this works
    index
  }

  // override def toString: String = s"H $ltl"

  override def toString: String = {
    rewrite.toString
  }

  override def toDot: String = {
    rewrite.toDot
  }
}

case class SometimeGT(ltl: LTL, timeLimit: Int) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  var rewrite: LTL = ExistsTimeGT(SinceTimeGT(True, timeLimit, ltl), timeLimit)

  override def getQuantifiedVariables: List[(String, Boolean)] = rewrite.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    rewrite = rewrite.expandMacros(macros)
    this
  }

  override def renameQuantVars(): LTL = {
    SometimeGT(ltl.renameQuantVars(), timeLimit)
  }

  override def substitute(subst: Substitution): LTL = {
    rewrite = rewrite.substitute(subst)
    this
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    rewrite = rewrite.substituteRuleBody(subst)
    this
  }

  override def translate(): Int = {
    rewrite.translate()
    index = rewrite.index // check that this works
    index
  }

  // override def toString: String = s"H $ltl"

  override def toString: String = {
    rewrite.toString
  }

  override def toDot: String = {
    rewrite.toDot
  }
}

case class Interval(ltl1: LTL, ltl2: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl1.setBelowPrevious()
    ltl2.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl1.checkForm(env)
    ltl2.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] =
    ltl1.getQuantifiedVariables ++ ltl2.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl1.getFreeVariables.union(ltl2.getFreeVariables)

  override def getPredicates: Set[String] = ltl1.getPredicates.union(ltl2.getPredicates)

  override def getPredicateTerms: Set[Pred] = ltl1.getPredicateTerms.union(ltl2.getPredicateTerms)

  override def expandMacros(macros: MacroMap): LTL = {
    val ltl1Expanded = ltl1.expandMacros(macros)
    val ltl2Expanded = ltl2.expandMacros(macros)
    Interval(ltl1Expanded, ltl2Expanded)
  }

  override def renameQuantVars(): LTL = {
    val ltl1Renamed = ltl1.renameQuantVars()
    val ltl2Renamed = ltl2.renameQuantVars()
    Interval(ltl1Renamed, ltl2Renamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substitute(subst)
    val ltl2Renamed = ltl2.substitute(subst)
    Interval(ltl1Renamed, ltl2Renamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltl1Renamed = ltl1.substituteRuleBody(subst)
    val ltl2Renamed = ltl2.substituteRuleBody(subst)
    Interval(ltl1Renamed, ltl2Renamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl1.translate()
    val index2 = ltl2.translate()
    LTL.assign(index)(s"now($index1).or(now($index2).not().and(pre($index)))")
    LTL.indicesOfPastTimeFormulas ::= index
    index
  }

  override def toString: String = s"[$ltl1,$ltl2)"

  override def toDot: String = {
    super.toDot +
      ltl1.toDot +
      ltl2.toDot +
      s"  $index -> ${ltl1.index}\n" +
      s"  $index -> ${ltl2.index}\n"
  }
}

case class Exists(name: String, ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    verify(ltl.getFreeVariables contains name)(s"quantified variable $name is not used")
    verify(!env.definesVar(name))(s"outer name $name is being hidden by quantification")
    ltl.checkForm(env.addVar(name))
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = List((name, false)) ++ ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables - name

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    Exists(name, ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    val (newName, ltlSubstitutedAndRenamed) = renameQuantification(name, ltl)
    Exists(newName, ltlSubstitutedAndRenamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst - name)
    Exists(name, ltlRenamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val newName = convertQuantifierInRuleBody(name, subst)
    val ltlRenamed = ltl.substituteRuleBody(subst + (name -> VPat(newName)))
    Exists(newName, ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"now($index1).exist(var_$name.quantvar)")
    index
  }

  override def toString: String = s"Exists $name . ${bracket(ltl)}"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class ExistsTime(ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    ExistsTime(ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    ExistsTime(ltl.renameQuantVars())
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst)
    ExistsTime(ltlRenamed)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substituteRuleBody(subst)
    ExistsTime(ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"now($index1).exist(var_t_quantvar)")
    index
  }

  override def toString: String = s"ExistsTime . ${bracket(ltl)}"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class ExistsTimeGT(ltl: LTL, timeLimit: Int) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    ltl.checkForm(env)
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    ExistsTimeGT(ltlExpanded, timeLimit)
  }

  override def renameQuantVars(): LTL = {
    ExistsTimeGT(ltl.renameQuantVars(), timeLimit)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst)
    ExistsTimeGT(ltlRenamed, timeLimit)
  }

  override def substituteRuleBody(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substituteRuleBody(subst)
    ExistsTimeGT(ltlRenamed, timeLimit)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()

    LTL.assign(index)(
      s"""
         |now($index1)
         |.and(limitMap($timeLimit))
         |.and(gtConst(tBDDListHighToLow, lBDDListHighToLow))
         |.exist(var_l_quantvar)
         |.exist(var_t_quantvar)""".stripMargin)

    index
  }

  override def toString: String = s"ExistsTimeGT . ${bracket(ltl)}"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class Forall(name: String, ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    verify(ltl.getFreeVariables contains name)(s"quantified variable $name is not used")
    verify(!env.definesVar(name))(s"outer name $name is being hidden by quantification")
    ltl.checkForm(env.addVar(name))
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = List((name, false)) ++ ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables - name

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    Forall(name, ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    val (newName, ltlSubstitutedAndRenamed) = renameQuantification(name, ltl)
    Forall(newName, ltlSubstitutedAndRenamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst - name)
    Forall(name, ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"now($index1).forAll(var_$name.quantvar)")
    index
  }

  override def toString: String = s"Forall $name . ${bracket(ltl)}"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class ExistsSeen(name: String, ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    verify(ltl.getFreeVariables contains name)(s"quantified variable $name is not used")
    verify(!env.definesVar(name))(s"outer name $name is being hidden by quantification")
    ltl.checkForm(env.addVar(name))
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = List((name, true)) ++ ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables - name

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    ExistsSeen(name, ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    val (newName, ltlSubstitutedAndRenamed) = renameQuantification(name, ltl)
    ExistsSeen(newName, ltlSubstitutedAndRenamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst - name)
    ExistsSeen(name, ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"var_$name.seen.and(now($index1)).exist(var_$name.quantvar)")
    index
  }

  override def toString: String = s"exists $name . ${bracket(ltl)}"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

case class ForallSeen(name: String, ltl: LTL) extends LTL {

  override def setBelowPrevious(): LTL = {
    isBelowPrevious = true
    ltl.setBelowPrevious()
    this
  }

  override def checkForm(env: Environment): Unit = {
    verify(ltl.getFreeVariables contains name)(s"quantified variable $name is not used")
    verify(!env.definesVar(name))(s"outer name $name is being hidden by quantification")
    ltl.checkForm(env.addVar(name))
  }

  override def getQuantifiedVariables: List[(String, Boolean)] = List((name, true)) ++ ltl.getQuantifiedVariables

  override def getFreeVariables: Set[String] = ltl.getFreeVariables - name

  override def getPredicates: Set[String] = ltl.getPredicates

  override def getPredicateTerms: Set[Pred] = ltl.getPredicateTerms

  override def expandMacros(macros: MacroMap): LTL = {
    val ltlExpanded = ltl.expandMacros(macros)
    ForallSeen(name, ltlExpanded)
  }

  override def renameQuantVars(): LTL = {
    val (newName, ltlSubstitutedAndRenamed) = renameQuantification(name, ltl)
    ForallSeen(newName, ltlSubstitutedAndRenamed)
  }

  override def substitute(subst: Substitution): LTL = {
    val ltlRenamed = ltl.substitute(subst - name)
    ForallSeen(name, ltlRenamed)
  }

  override def translate(): Int = {
    super.translate()
    val index1 = ltl.translate()
    LTL.assign(index)(s"var_$name.seen.imp(now($index1)).forAll(var_$name.quantvar)")
    index
  }

  override def toString: String = s"forall $name . ${bracket(ltl)}"

  override def toDot: String = {
    super.toDot +
      ltl.toDot +
      s"  $index -> ${ltl.index}\n"
  }
}

trait ConstOrVar {
  def toStringForSynthesis: String

  def substitute(subst: Substitution): ConstOrVar = this
}

case class CPat(value: Any) extends ConstOrVar {
  def toStringForSynthesis: String = {
    val valueQ = quoteIfNotQuoted(value)
    s"C($valueQ)"
  }

  override def toString: String = {
    requoteIfString(value)
  }
}

case class VPat(variable: String) extends ConstOrVar {
  def toStringForSynthesis: String = {
    s"V(${quote(variable)})"
  }

  override def substitute(subst: Substitution): ConstOrVar = {
    subst.get(variable) match {
      case None => this
      case Some(constOrVar) => constOrVar
    }
  }

  override def toString: String = variable
}
