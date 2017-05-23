package xrpc;

import xrpc.Server;

import java.net.InetSocketAddress;


public class Hello {

  public static void main(String[] args) {
      Server s = new Server(4, 20);

      try {
	  s.start(8080);
      } catch (Exception e) {

      }
  }

}
