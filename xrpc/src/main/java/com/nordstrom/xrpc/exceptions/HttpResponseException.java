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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.nordstrom.xrpc.encoding.TextEncodable;
import java.io.IOException;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Base exception for Exceptions that are meant to convert to HTTP Responses. */
@JsonSerialize(using = HttpResponseException.JsonSerializer.class)
@Accessors(fluent = true)
public abstract class HttpResponseException extends RuntimeException implements TextEncodable {
  /** HTTP Status Code. */
  @Getter private final int statusCode;

  /** Code used to provide a more specific code for the error. */
  @Getter private final String errorCode;

  /** A more detailed error message that can be used for logging. */
  @Getter private final String logMessage;

  /**
   * Construct HttpResponseException.
   *
   * @param statusCode HTTP Status Code
   * @param errorCode Code used to provide a more specific code for the error.
   * @param message Error message
   * @param logMessage A more detailed error message that can be used for logging.
   */
  public HttpResponseException(
      int statusCode, String errorCode, String message, String logMessage) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.logMessage = logMessage;
  }

  /**
   * Construct HttpResponseException with a cause.
   *
   * @param statusCode HTTP Status Code
   * @param errorCode Code used to provide a more specific code for the error.
   * @param message Error message
   * @param logMessage A more detailed error message that can be used for logging.
   * @param cause The Throwable that caused this error.
   */
  public HttpResponseException(
      int statusCode, String errorCode, String message, String logMessage, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.logMessage = logMessage;
  }

  @Override
  public String encode() {
    return String.format("[%s] %s", errorCode(), getMessage());
  }

  public static class JsonSerializer extends StdSerializer<HttpResponseException> {

    public JsonSerializer() {
      this(null);
    }

    public JsonSerializer(Class<HttpResponseException> t) {
      super(t);
    }

    @Override
    public void serialize(
        HttpResponseException value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeStartObject();
      jgen.writeStringField("errorCode", value.getErrorCode());
      jgen.writeStringField("message", value.getMessage());
      jgen.writeEndObject();
    }
  }
}
