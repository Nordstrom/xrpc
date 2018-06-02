package com.nordstrom.xrpc.demos.people;

import com.nordstrom.xrpc.exceptions.HttpResponseException;
import lombok.Getter;

public class MyCustomException extends HttpResponseException {
  private static final int STATUS_CODE = 400;
  private static final String ERROR_CODE = "BadRequest";

  @Getter private final Object myError;

  /**
   * Construct MyCustomException with 400 error code.
   *
   * @param myError Error message
   */
  public MyCustomException(Object myError) {
    super(STATUS_CODE, ERROR_CODE, "Some error message");
    this.myError = myError;
  }

  @Override
  public Object error() {
    return myError;
  }
}
