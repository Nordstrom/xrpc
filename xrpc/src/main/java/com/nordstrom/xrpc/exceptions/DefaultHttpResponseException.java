package com.nordstrom.xrpc.exceptions;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.nordstrom.xrpc.exceptions.proto.Error;

/** Base exception for responses using a simple string error code in the response. */
public class DefaultHttpResponseException extends HttpResponseException {
  private final String errorCode;

  public DefaultHttpResponseException(int statusCode, String errorCode, String message) {
    this(statusCode, errorCode, message, null);
  }

  public DefaultHttpResponseException(
      int statusCode, String errorCode, String message, Throwable cause) {
    super(statusCode, buildError(errorCode, message), message, cause);

    this.errorCode = errorCode;
  }

  @Override
  public Error error() {
    // Workaround for the fact that we can't make HttpResponseException generic.
    return (Error) super.error();
  }

  @Override
  public String toString() {
    StringBuilder buffer =
        new StringBuilder()
            .append(getClass().getName())
            .append(": [")
            .append(statusCode())
            .append("] ")
            .append(errorCode);

    String message = getLocalizedMessage();
    if (!Strings.isNullOrEmpty(message)) {
      buffer.append(": ").append(message);
    }
    return buffer.toString();
  }

  /**
   * Builds an error message from the given fields.
   *
   * @throws IllegalArgumentException if errorCode is empty or null
   */
  private static Error buildError(String errorCode, String message) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(errorCode), "errorCode must be non-null and non-empty");
    return Error.newBuilder().setErrorCode(errorCode).setMessage(message).build();
  }
}
