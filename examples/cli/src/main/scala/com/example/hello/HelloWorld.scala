package com.example.hello

class HelloWorld(message: String)
{
  def printMessage() = {
    println(s"Your Message : $message")
  }
}

object HelloWorld {
  def apply(message: String) = new HelloWorld(message)
}
