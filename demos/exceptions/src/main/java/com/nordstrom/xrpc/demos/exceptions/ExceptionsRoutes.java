package com.nordstrom.xrpc.demos.exceptions;

import com.nordstrom.xrpc.exceptions.BadRequestException;
import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.RouteBuilder;

public class ExceptionsRoutes extends RouteBuilder {

  public ExceptionsRoutes() {

    Handler badRequestException =
        request -> {
          throw new BadRequestException("Some bad request", null);
        };

    // This route demonstrate how to throw  Badrequest exception(400 status code) to client without
    // a response body
    get("/show-badrequest", badRequestException);

    Handler badRequestExceptionWithBody =
        request -> {
          ErrorResponse errorResponse = new ErrorResponse();
          errorResponse.setBusinessReason("Some bad request");
          errorResponse.setBusinessStatusCode("4.2.3000");
          throw new BadRequestException("Some bad request", errorResponse);
        };

    // This route demonstrate how to throw  Badrequest exception(400 status code) to client with a
    // response body
    // {"businessReason":"Some bad request","businessStatusCode":"4.2.3000"}
    get("/show-badrequest-withbody", badRequestExceptionWithBody);

    Handler customException =
        request -> {
          ErrorResponse errorResponse = new ErrorResponse();
          errorResponse.setBusinessReason("Some business reason");
          errorResponse.setBusinessStatusCode("4.2.3000");
          throw new MyCustomException(errorResponse);
        };

    // This route demonstrate how to throw custom exception payload to the client in xrpc
    // Output of this route will be 400 with this json payload {"businessReason":"Some business
    // reason","businessStatusCode":"4.2.3000"}
    get("/show-customexception", customException);
  }
}
