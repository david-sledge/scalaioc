/**
 * 
 */
package com.example;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * @author sledge
 *
 */
public final class CliParser {

  public static void parseOptions(String[] args) {

    OptionParser parser = new OptionParser( "s:a:" );

    OptionSet options = parser.parse(args);

    String salutation = options.has("s") ? (String) options.valueOf("s") : "Hello";

    String addressee = options.has("a") ? (String) options.valueOf("a") : "World";

    System.out.println(salutation + ", " + addressee + "!");

  }

}
