package rx.lang.scala

import scala.reflect.runtime.universe._
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import rx.util.functions._

class CompletenessTest extends JUnitSuite {
  
  case class Op(name: String, argTypes: Type*) {
    override def toString = name + argTypes.mkString("(", ", ", ")")
  }
  
  val correspondence = Map(
      Op("toList") -> Op("toSeq"),
      Op("window", typeOf[Int]) -> Op("window", typeOf[Int])
  )
  
  def check(cond: Boolean, ifGood: String, ifBad: String): Unit = {
    if (cond) println(ifGood) else println(ifBad)
  }
  
  def checkOperatorPresence(op: Op, tp: Type): Unit = {
    val paramTypeLists = for (alt <- tp.member(newTermName(op.name)).asTerm.alternatives)
                           yield alt.asMethod.paramss.headOption match {
						     case Some(paramList) => paramList.map(symb => symb.typeSignature)
						     case None => List()
                           }

    println(paramTypeLists)
    
    check(paramTypeLists.contains(op.argTypes.toList), 
        s"$op is present in $tp", s"$op is NOT present in $tp")
  }
  
  @Test def test3() {
    val javaObs = typeOf[rx.Observable[_]]
    val scalaObs = typeOf[rx.lang.scala.Observable[_]]
    
    for ((javaOp, scalaOp) <- correspondence) {
      checkOperatorPresence(javaOp, javaObs)
      checkOperatorPresence(scalaOp, scalaObs)
    }
  }
  

  @Test def test1() {
    val c = Class.forName("rx.Observable")
    for (method <- c.getMethods()) {
      println(method.getName())
    }
    
  }
  
  @Test def test2() {
    val tp = typeOf[rx.Observable[_]]
    for (member <- tp.members) println(member)
    println("00000")
    //println(tp.member(stringToTermName("all")))
    //println(tp.member("all"))
    
    val methodAll = tp.member(newTermName("all")).asMethod
    println(methodAll.fullName)
    
    val methodBufferAlts = tp.member(newTermName("buffer")).asTerm.alternatives
  }
  
}
