package com.nordstrom.xrpc.client;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XrpcClient {
  private final ListeningExecutorService service =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

  public Call newCall(HttpRequest req) {

    return new Call();
  }

  public Call newCall(FullHttpRequest req) {

    return new Call();
  }

  //  public Call newCall(Http2Request req) {
  //
  //    return new Call();
  //  };

}
