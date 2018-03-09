/*
 * Copyright 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.xrpc.exceptions;

import com.nordstrom.xrpc.exceptions.proto.Error;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Base exception for Exceptions that are meant to convert to HTTP Responses. */
@Accessors(fluent = true)
public abstract class HttpResponseException extends RuntimeException {
  /** HTTP Status Code. */
  @Getter private final int statusCode;

  /** Code used to provide a more specific code for the error. */
  @Getter private final String errorCode;

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP Status Code
   * @param errorCode Code used to provide a more specific code for the error.
   * @param message Error message.
   */
  public HttpResponseException(int statusCode, String errorCode, String message) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP Status Code
   * @param errorCode Code used to provide a more specific code for the error.
   * @param message Error message.
   * @param cause The Throwable that caused this error.
   */
  public HttpResponseException(int statusCode, String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }

  /**
   * Get the error response for this exception. This is used for response encoding. Error is a proto
   * generated class that can encode to proto and json.
   *
   * @return Error proto generated object based on this exception.
   */
  public Error error() {
    return Error.newBuilder().setErrorCode(errorCode).setMessage(getMessage()).build();
  }
}
