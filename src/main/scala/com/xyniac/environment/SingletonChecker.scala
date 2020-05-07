package com.xyniac.environment

object SingletonChecker {
  def isSingleton[A](a: A)(implicit ev: A <:< Singleton = null) =
    Option(ev).isDefined
}
