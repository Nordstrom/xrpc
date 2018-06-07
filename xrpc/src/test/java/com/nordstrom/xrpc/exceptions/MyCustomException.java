package com.nordstrom.xrpc.exceptions;

public class MyCustomException extends HttpResponseException {
  // Http status code to client
  private static final int STATUS_CODE = 513;

  public MyCustomException(String businessReason, String businessStatusCode) {
    super(STATUS_CODE, new ErrorResponse(businessReason, businessStatusCode), "Some error message");
  }
}
