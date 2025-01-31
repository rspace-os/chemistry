package com.researchspace.chemistry;

public class ChemistryException extends RuntimeException {
  public ChemistryException(String message, Exception cause) {
    super(message, cause);
  }

  public ChemistryException(String message) {
    super(message);
  }
}
