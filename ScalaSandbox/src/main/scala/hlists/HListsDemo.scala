package hlists

import hlists.HLists.{HNil, stringValueOf}
import hlists.Nats._

object HListsDemo extends App {
  val a = true :: 3 :: "Foo" :: HNil
  val b = Array(1, 2) :: "Bar" :: 4.0 :: HNil

  val c = a ::: b

  println(stringValueOf(a))
  println(stringValueOf(HNil))

  if (c.nth[_0]) {
    val x = 2.0 * c.nth[_5]
    println(s"${c.nth[_2]}, ${c.nth[_4]}, $x")
  }
}
