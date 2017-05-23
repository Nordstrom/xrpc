package com.xjeffrose.xrpc;


import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.netty.handler.codec.rtsp.RtspHeaderNames.CONTENT_LENGTH;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Example {

  public static void main(String[] args) {
    List<String> people = new ArrayList<>();

    // Build your router
    Router router = new Router(4,20);


    // Define a complex function call
    BiFunction<HttpRequest, Map<String, String>, HttpResponse> personHandler = (x, y) -> {
      people.add(y.get("person"));

      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, 0);

      return response;
    };


    // Define a simple function call
    Function<HttpRequest, HttpResponse> peopleHandler = x -> {
      ByteBuf bb =  Unpooled.compositeBuffer();
      people.forEach(xs -> bb.writeBytes(xs.getBytes()));
      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, bb.readableBytes());


      return response;
    };


    // Create your route mapping
    router.addRoute("/people/:person", personHandler);
    router.addRoute("/people", peopleHandler);


    try {
      // Fire away
      router.listenAndServe(8080);
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }

  }

}
