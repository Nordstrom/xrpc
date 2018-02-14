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

import lombok.Getter;

/** Base exception for Exceptions that are meant to convert to HTTP Responses. */
public abstract class HttpResponseException extends RuntimeException {
  /** HTTP Status Code. */
  @Getter private final int statusCode;

  /** Code used to provide a more specific code for the error. */
  @Getter private final String errorCode;

  /** A more detailed error message that can be used for logging. */
  @Getter private final String detailedMessage;

  /**
   * Construct HttpResponseException.
   *
   * @param statusCode HTTP Status Code
   * @param errorCode Code used to provide a more specific code for the error.
   * @param message Error message
   * @param detailedMessage A more detailed error message that can be used for logging.
   */
  public HttpResponseException(
      int statusCode, String errorCode, String message, String detailedMessage) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.detailedMessage = detailedMessage;
  }

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP Status Code
   * @param errorCode Code used to provide a more specific code for the error.
   * @param message Error message
   * @param detailedMessage A more detailed error message that can be used for logging.
   * @param cause The Throwable that caused this error.
   */
  public HttpResponseException(
      int statusCode, String errorCode, String message, String detailedMessage, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.detailedMessage = detailedMessage;
  }
}
