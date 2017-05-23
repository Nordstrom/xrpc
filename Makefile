classpath = lib/netty-all-4.1.11.Final.jar:lib/guava-21.0.jar
out_dir = classes

all: server

server:
	javac -d $(out_dir) -cp $(classpath) xrpc/Server.java
