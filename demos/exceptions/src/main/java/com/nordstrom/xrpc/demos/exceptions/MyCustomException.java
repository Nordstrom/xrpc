package com.nordstrom.xrpc.demos.exceptions;

import com.nordstrom.xrpc.exceptions.HttpResponseException;

public class MyCustomException extends HttpResponseException {
  private static final int STATUS_CODE = 400;

  /**
   * Construct MyCustomException with 400 error code.
   *
   * @param myError Error message
   */
  public MyCustomException(Object myError) {
    super(STATUS_CODE, myError, "Some error message");
  }

}
