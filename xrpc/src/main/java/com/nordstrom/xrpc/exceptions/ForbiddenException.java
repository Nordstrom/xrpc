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

/** Exception to be converted to an HTTP 403 Forbidden response. */
public class ForbiddenException extends DefaultHttpResponseException {
  private static final int STATUS_CODE = 403;
  private static final String ERROR_CODE = "Forbidden";

  /**
   * Construct ForbiddenException with default error code.
   *
   * @param message Error message
   */
  public ForbiddenException(String message) {
    super(STATUS_CODE, ERROR_CODE, message);
  }

  /**
   * Construct ForbiddenException with a default error code and root cause.
   *
   * @param message Error message
   * @param cause Cause of this error.
   */
  public ForbiddenException(String message, Throwable cause) {
    super(STATUS_CODE, ERROR_CODE, message, cause);
  }
}
