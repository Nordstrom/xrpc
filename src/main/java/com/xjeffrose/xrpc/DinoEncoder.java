package com.xjeffrose.xrpc;

import java.io.IOException;

public class DinoEncoder {

  public static void main(String[] args) throws IOException {

    Dino dino = new Dino.Builder()
      .name(args[0])
      .fav_color(args[1])
      .build();

    byte[] bytes = Dino.ADAPTER.encode(dino);
    System.out.write(bytes, 0, bytes.length);
  }

}
