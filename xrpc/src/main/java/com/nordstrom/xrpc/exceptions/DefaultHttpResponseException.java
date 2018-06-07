package com.nordstrom.xrpc.exceptions;

import com.google.common.base.Strings;
import com.nordstrom.xrpc.exceptions.proto.Error;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class DefaultHttpResponseException extends HttpResponseException {
  @Getter private final String errorCode;

  public DefaultHttpResponseException(int statusCode, String errorCode, String message) {
    super(
        statusCode,
        Error.newBuilder().setMessage(message).setErrorCode(errorCode).build(),
        message);
    this.errorCode = errorCode;
  }

  public DefaultHttpResponseException(
      int statusCode, String errorCode, String message, Throwable cause) {
    super(
        statusCode,
        Error.newBuilder().setMessage(message).setErrorCode(errorCode).build(),
        message,
        cause);
    this.errorCode = errorCode;
  }

  @Override
  public String toString() {
    StringBuilder buffer =
        new StringBuilder()
            .append(getClass().getName())
            .append(": [")
            .append(statusCode())
            .append("] ")
            .append(errorCode());

    String message = getLocalizedMessage();
    if (!Strings.isNullOrEmpty(message)) {
      buffer.append(": ").append(message);
    }
    return buffer.toString();
  }
}
