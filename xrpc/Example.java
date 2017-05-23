package xrpc;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;


class MyServer {


    Router router = new Router(4, 20);

    class Thing1 {
	private String x;
	private String y;
    }

    class PersonHandler extends extends SimpleChannelInboundHandler<Object> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
	    if (msg instanceof HttpRequest) {
		HttpRequest request = this.request = (HttpRequest) msg;
	    }
	}

    }

    class PeopleHandler extends extends SimpleChannelInboundHandler<Object> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
	    if (msg instanceof HttpRequest) {
		HttpRequest request = this.request = (HttpRequest) msg;
	    }
	}

    }

    public static void main(String[] args) {

	router.addRoute("/people/:person", new PersonHandler(stats));
	router.addRoute("/people", new PeopleHandler(stats)):

	try {
	    s.listenAndServe("8080");
	} catch (IOException e) {

	} finally {
	    s.shutdown();
	}

    }

}
