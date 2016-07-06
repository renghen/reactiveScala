package app.impl

import org.junit.Test


class Implicit {


  /**
    * Implicit parameter tell is used in case that there´s no parameters passed to the function,
    * the compiler will take the one implicit val defined.
    * if you define more than one implicit val the code wont compile.
    */
  @Test def implicitParameter(): Unit = {
    println(multiply)
    println(multiply(3, 3))
  }

  implicit val multiplier = 2


  implicit def multiply(implicit x: Int, y: Int) = {
    x * y
  }

  @Test def implicitClass(): Unit = {
    println("HAL".increment)
    println(2.exponencial)
    println(2.increment(1))
    println(2.decrement(1))
    println(2.multiply(3))
  }

  /**
    * An implicit class is an instance class that Scala compiler automatically initialize and attach to the object type,
    * as part of the API of the class.
    * In our example increment become part of String API
    * As is logical only 1 implicit class/type bound per class is allowed
    *
    * @param s
    */
  implicit class StringImprovements(s: String) {
    def increment = s.map(c => (c + 1).toChar)
  }

  implicit class IntegerImprovements(i: Int) {
    def exponencial = i * i

    def increment(n: Int) = i + n

    def decrement(n: Int) = i - n

    def multiply(n: Int) = i * n

  }

}