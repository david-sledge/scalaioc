package com.example.hello;

import java.time.ZonedDateTime;

public class DateTimeHelloWorld
{

  public final String message;

  public final ZonedDateTime dateTime;

  public DateTimeHelloWorld(String message, ZonedDateTime dateTime) {

    this.message = message;
    this.dateTime = dateTime;

  }

  public void printMessage() {
    System.out.println("Your Message : " + message + "\nI was given the specific time of " + dateTime + ".");
  }
}
