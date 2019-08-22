/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.exception;

public class GitEmailException extends Exception {

  private static final long serialVersionUID = 1L;

  public GitEmailException(String msg) {
    super(msg);
  }

  public GitEmailException(String msg, Throwable t) {
    super(msg, t);
  }

}