package com.example.hello;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

public class MainApp {

  public static void run(
      Supplier<HelloWorld> obj0,
      Supplier<ZonedDateTime> dateTime0,
      Supplier<ZonedDateTime> anotherDateTime0,
      Supplier<DateTimeHelloWorld> dateTimeHelloWorld0
  ) {

    obj0.get().printMessage();

    // gimme the time, ya lazy bum!
    final ZonedDateTime dateTime = dateTime0.get();
    // you, people-pleaser, gimme the time, too.
    final ZonedDateTime anotherDateTime = anotherDateTime0.get();

    // let's wait half a second
    try {
        Thread.sleep(500);
    }
    catch(InterruptedException e) {
        throw new RuntimeException(e);
    }

    // who will give the same ol', and who will give something new?
    System.out.println("Will the lazy manager produce the same as before?"
        + " " + (dateTime0.get().equals(dateTime) ? "Yes" : "No"));
    System.out.println("Will the eager manager produce the same as before?"
        + " " + (anotherDateTime0.get().equals(anotherDateTime) ? "Yes" : "No"));

    // The productive senior manager puts the lazy one to work.
    final DateTimeHelloWorld dateTimeHelloWorld = dateTimeHelloWorld0.get();
    dateTimeHelloWorld.printMessage();
    // Will the lazy manager give the productive one the same ol'?
    System.out.println("The senior manager will make even the lazy manager perform some work.");
    System.out.println("Will the lazy manager give senior manager the same as before?"
        + " " + (dateTimeHelloWorld.dateTime.equals(dateTime) ? "Yes" : "No"));
  }
}
