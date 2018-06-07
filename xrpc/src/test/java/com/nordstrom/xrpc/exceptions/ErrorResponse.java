package com.nordstrom.xrpc.exceptions;

import lombok.Value;

@Value
public class ErrorResponse {
  private String businessReason;
  private String businessStatusCode;
}
