package utils

sealed trait Result[+A] {
	def flatMap[B](f: A => Result[B]): Result[B]
	def map[B](f: A => B): Result[B]
}

case class Success[A](value: A) extends Result[A] {
	override def flatMap[B](f: A => Result[B]) = f(value)
	override def map[B](f: A => B) = new Success(f(value))
}

case class Failure[A](msg: String) extends Result[A] {
	override def flatMap[B](f: A => Result[B]) = new Failure[B](msg)
	override def map[B](f: A => B) = new Failure[B](msg)
}
