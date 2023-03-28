package com.bingbei.mts.common.ipc.uds;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Connection {
    private String socketId="test.sock";
    private String socketPath="/tmp/sock";
    public void startServer() throws  Exception{
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        UnixDomainSocketAddress of = UnixDomainSocketAddress.of(socketPath+"/"+socketId);
        serverSocketChannel.bind(of);
        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            ByteBuffer buf = ByteBuffer.allocate(48);
            int bytesRead = socketChannel.read(buf);
            while (bytesRead > 0) {
                buf.flip();
                while (buf.hasRemaining()) {
                    System.out.print((char) buf.get());
                }
                System.out.println();
                buf.clear();
                bytesRead = socketChannel.read(buf);
            }
        }
    }
    public void startClient(String client) throws  Exception{
        SocketChannel socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
        UnixDomainSocketAddress of = UnixDomainSocketAddress.of(socketPath+"/"+socketId);
        boolean connect = socketChannel.connect(of);
        String newData = "this is domain socket..." + System.currentTimeMillis();
        ByteBuffer buf = ByteBuffer.allocate(48);
        buf.clear();
        buf.put(newData.getBytes());

        buf.flip();

        while (buf.hasRemaining()) {
            socketChannel.write(buf);
        }
        socketChannel.close();
    }
}
