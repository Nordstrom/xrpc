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
  /** HTTP Status Code. */
  @Getter private final int statusCode;

  /** Error object . */
  @Getter private final Object errorObject;

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP Status Code
   * @param error Error object.
   * @param message Error message.
   */
  public HttpResponseException(int statusCode, Object error, String message) {
    super(message);
    Preconditions.checkArgument(
        statusCode >= 200 && statusCode <= 527, "statusCode should be >= 200 && <= 527");
    this.statusCode = statusCode;
    this.errorObject = error;
  }

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP Status Code
   * @param error Error object.
   * @param message Error message.
   * @param cause The Throwable that caused this error.
   */
  public HttpResponseException(int statusCode, Object error, String message, Throwable cause) {
    super(message, cause);
    Preconditions.checkArgument(
        statusCode >= 200 && statusCode <= 527, "statusCode should be >= 200 && <= 527");
    this.statusCode = statusCode;
    this.errorObject = error;
  }

  /**
   * Get the error response for this exception. This is used for response encoding.
   *
   * @return Error object based on this exception.
   */
  public Object error() {
    return errorObject;
  }
}
