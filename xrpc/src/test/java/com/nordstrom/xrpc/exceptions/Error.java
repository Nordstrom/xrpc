package com.nordstrom.xrpc.exceptions;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Error {
  private String errorCode;
  private String message;
}
