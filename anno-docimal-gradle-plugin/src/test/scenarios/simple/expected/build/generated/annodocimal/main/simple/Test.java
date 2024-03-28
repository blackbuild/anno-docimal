package simple;

import groovy.lang.GroovyObject;

/**
 * This is a test class
 */
public class Test implements GroovyObject {
  public Test() {
  }

  /**
   * A method
   */
  public void aMethod() {
  }

  /**
   * Inner class
   */
  public static class InnerClass implements GroovyObject {
    /**
     * Inner class method
     */
    public void innerMethod() {
    }
  }
}
