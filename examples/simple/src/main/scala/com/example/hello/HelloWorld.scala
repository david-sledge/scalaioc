package com.example.hello

import java.util.Calendar

class HelloWorld(message: String)
{
  private val madeOnTimeStamp = Calendar.getInstance().getTime

  def getMessage(){
    println(s"Your Message : $message.  The time is now $madeOnTimeStamp")
  }
}
