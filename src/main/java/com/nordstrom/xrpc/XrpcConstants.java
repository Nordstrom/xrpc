package com.nordstrom.xrpc;

import com.nordstrom.xrpc.server.XrpcConnectionContext;
import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.util.AttributeKey;

public class XrpcConstants {
  public static final AttributeKey<XrpcRequest> XRPC_REQUEST = AttributeKey.valueOf("XrpcRequest");
  public static final AttributeKey<XrpcConnectionContext> CONNECTION_CONTEXT =
      AttributeKey.valueOf("XrpcConnectionContext");
}
