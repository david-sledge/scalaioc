package com.example.hello;

public class HelloWorld
{

  public final String message;

  public HelloWorld(String message) {
    this.message = message;
  }

  public void printMessage() {
    System.out.println("Your Message : " + message);
  }
}
