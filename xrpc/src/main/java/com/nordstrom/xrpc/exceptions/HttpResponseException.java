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

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Base exception for Exceptions that are meant to convert to HTTP Responses. */
@Accessors(fluent = true)
public class HttpResponseException extends RuntimeException {
  /** HTTP status code. */
  @Getter private final int statusCode;

  /** Error object to serialize as the response body. */
  @Getter private final Object error;

  /**
   * Construct HttpResponseException with an empty cause.
   *
   * @param statusCode HTTP status code to send with the response
   * @param error error object, serialized with the response
   * @param message exception message. This is NOT serialized with the respone, and is only used for
   *     logging.
   * @throws IllegalArgumentException if statusCode is outside the valid range, or if error is null
   */
  public HttpResponseException(int statusCode, Object error, String message) {
    this(statusCode, error, message, null);
  }

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP status code to send with the response
   * @param error error object, serialized with the response
   * @param message exception message. This is NOT serialized with the respone, and is only used for
   *     logging.
   * @param cause the cause of the exception, for logging. May be null.
   * @throws IllegalArgumentException if statusCode is outside the valid range, or if error is null
   */
  public HttpResponseException(int statusCode, Object error, String message, Throwable cause) {
    super(message, cause);
    Preconditions.checkArgument(
        statusCode >= 200 && statusCode <= 527, "statusCode must be >= 200 && <= 527");
    Preconditions.checkArgument(error != null, "error must be non-null");
    this.statusCode = statusCode;
    this.error = error;
  }
}
