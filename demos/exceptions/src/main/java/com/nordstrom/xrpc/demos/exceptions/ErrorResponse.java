package com.nordstrom.xrpc.demos.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {

  private String businessReason;
  private String businessStatusCode;
}
