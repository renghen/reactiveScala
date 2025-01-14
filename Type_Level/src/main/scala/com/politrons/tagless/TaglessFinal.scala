package com.politrons.tagless

import cats.effect.IO
import cats.effect.unsafe.IORuntime

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Try
import scala.concurrent.duration._
import scala.language.postfixOps

object TaglessFinal {

  def main(args: Array[String]): Unit = {
    runProgramIO()
    runProgramTry()
    runProgramFuture()
  }
  /**
    * Domain
    * -----------
    */
  case class Product(id: String, description: String)

  case class ShoppingCart(id: String, products: List[Product])

  /**
    * ------------------------
    * [ADT] Algebra Data Types
    * -------------------------
    * We describe the DSL that we will use in our interpreters. We will expose to the client only this contract
    * and we will make as abstract as possible the details of the implementation.
    * The idea is to hide the specific effect system we will use.
    * That's the reason to use Higher Kinded Types F[_]
    */
  trait ShoppingCarts[F[_]] {
    def create(id: String): F[Unit]

    def find(id: String): F[ShoppingCart]

    def add(sc: ShoppingCart, product: Product): F[ShoppingCart]
  }

  /**
    * ------------------------------
    * [DSL] Domain Specific Language
    * ------------------------------
    * Here we define the DSL we will use in the program we will define, each of this operators
    * has an implicit interpreter that implement the ADT trait.
    * Each implementation is just used as decorator, so we just invoke the interpreter method
    * associated with this operator.
    */

  def createShoppingCart[F[_]](id: String)(implicit interpreter: ShoppingCarts[F]): F[Unit] = {
    interpreter.create(id)
  }

  def findShoppingCart[F[_]](id: String)(implicit interpreter: ShoppingCarts[F]): F[ShoppingCart] = {
    interpreter.find(id)
  }

  def addInShoppingCart[F[_]](sc: ShoppingCart, product: Product)(implicit interpreter: ShoppingCarts[F]): F[ShoppingCart] = {
    interpreter.add(sc, product)
  }

  /**
    * -------------
    * Interpreters
    * -------------
    * Here we implement the the Behavior of the program. We use the contract described in the ADT to
    * provide a specific implementation that replace that Higher kinded type
    */

  /**
    * IO interpreter
    * ---------------
    * Here we choose monad IO to control throwable side-effect
    */
  val ioInterpreter: ShoppingCarts[IO] = new ShoppingCarts[IO] {

    var shoppingCartMap: Map[String, ShoppingCart] = Map()

    override def create(id: String): IO[Unit] = IO {
      shoppingCartMap = Map(id -> ShoppingCart(id, List())) ++ shoppingCartMap
    }

    override def find(id: String): IO[ShoppingCart] = IO {
      shoppingCartMap(id)
    }

    override def add(sc: ShoppingCart, product: Product): IO[ShoppingCart] = IO {
      sc.copy(products = product +: sc.products)
    }
  }

  /**
    * Try interpreter
    * ---------------
    * Here we choose monad Try to control throwable side-effect
    */
  val tryInterpreter: ShoppingCarts[Try] = new ShoppingCarts[Try] {

    var shoppingCartMap: Map[String, ShoppingCart] = Map()

    override def create(id: String): Try[Unit] = Try {
      shoppingCartMap = Map(id -> ShoppingCart(id, List())) ++ shoppingCartMap
    }

    override def find(id: String): Try[ShoppingCart] = Try {
      shoppingCartMap(id)
    }

    override def add(sc: ShoppingCart, product: Product): Try[ShoppingCart] = Try {
      sc.copy(products = product +: sc.products)
    }
  }

  /**
    * Future interpreter
    * -------------------
    * Here we choose monad Future to control time/throwable side-effect
    */
  val futureInterpreter: ShoppingCarts[Future] = new ShoppingCarts[Future] {

    var shoppingCartMap: Map[String, ShoppingCart] = Map()

    override def create(id: String): Future[Unit] = Future {
      shoppingCartMap = Map(id -> ShoppingCart(id, List())) ++ shoppingCartMap
    }(scala.concurrent.ExecutionContext.global)

    override def find(id: String): Future[ShoppingCart] = Future {
      shoppingCartMap(id)
    }(scala.concurrent.ExecutionContext.global)

    override def add(sc: ShoppingCart, product: Product): Future[ShoppingCart] = Future {
      sc.copy(products = product +: sc.products)
    }(scala.concurrent.ExecutionContext.global)
  }

  /**
    * ---------
    * Programs
    * ---------
    * Here we can define using the DSL we define before all the
    */

  def runProgramIO(): Unit = {
    implicit val runtime: IORuntime = IORuntime.global
    implicit val interpreter: ShoppingCarts[IO] = ioInterpreter
    val shoppingCartProgram: IO[ShoppingCart] = for {
      _ <- createShoppingCart("1981")
      sc <- findShoppingCart("1981")
      sc <- addInShoppingCart(sc, Product("111", "Coca-cola"))
    } yield sc
    println(shoppingCartProgram.unsafeRunSync())
  }

  def runProgramTry(): Unit = {
    implicit val interpreter: ShoppingCarts[Try] = tryInterpreter
    val shoppingCartProgram: Try[ShoppingCart] = for {
      _ <- createShoppingCart("1981")
      sc <- findShoppingCart("1981")
      sc <- addInShoppingCart(sc, Product("111", "Coca-cola"))
    } yield sc
    println(shoppingCartProgram)
  }

  def runProgramFuture(): Unit = {
    implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
    implicit val interpreter: ShoppingCarts[Future] = futureInterpreter
    val shoppingCartProgram: Future[ShoppingCart] = for {
      _ <- createShoppingCart("1984")
      sc <- findShoppingCart("1984")
      sc <- addInShoppingCart(sc, Product("200", "Twix"))
      sc <- addInShoppingCart(sc, Product("300", "Pepsi"))
    } yield sc
    println(Await.result(shoppingCartProgram, 10 seconds))
  }
}
