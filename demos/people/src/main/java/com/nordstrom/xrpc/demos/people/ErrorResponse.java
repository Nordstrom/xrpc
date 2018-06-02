package com.nordstrom.xrpc.demos.people;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {

  private String businessReason;
  private String businessStatusCode;
}
