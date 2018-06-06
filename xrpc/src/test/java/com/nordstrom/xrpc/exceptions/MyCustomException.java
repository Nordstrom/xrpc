package com.nordstrom.xrpc.exceptions;

public class MyCustomException extends HttpResponseException {
  // Http status code to client
  private static final int STATUS_CODE = 513;

  public MyCustomException(Object myError) {
    super(STATUS_CODE, myError, "Some error message");
  }
}
