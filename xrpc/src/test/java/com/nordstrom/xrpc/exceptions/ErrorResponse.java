package com.nordstrom.xrpc.exceptions;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorResponse {
  private String businessReason;
  private String businessStatusCode;
}
