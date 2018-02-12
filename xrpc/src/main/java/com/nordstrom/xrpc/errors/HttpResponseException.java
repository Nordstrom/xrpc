package com.nordstrom.xrpc.errors;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class HttpResponseException extends RuntimeException {
  @Getter private final int statusCode;

  @Getter private final String detailedMessage;

  public HttpResponseException(int statusCode, String detailedMessage) {
    this.statusCode = statusCode;
    this.detailedMessage = detailedMessage;
  }

  public HttpResponseException(String message, int statusCode, String detailedMessage) {
    super(message);
    this.statusCode = statusCode;
    this.detailedMessage = detailedMessage;
  }

  public HttpResponseException(
      String message, Throwable cause, int statusCode, String detailedMessage) {
    super(message, cause);
    this.statusCode = statusCode;
    this.detailedMessage = detailedMessage;
  }

  public HttpResponseException(Throwable cause, int statusCode, String detailedMessage) {
    super(cause);
    this.statusCode = statusCode;
    this.detailedMessage = detailedMessage;
  }

  public HttpResponseException(
      String message,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace,
      int statusCode,
      String detailedMessage) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.statusCode = statusCode;
    this.detailedMessage = detailedMessage;
  }
}
