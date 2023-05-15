package org.mts.common.rpc.uds;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class OldUdsClient {
    private String socketPath;
    public OldUdsClient(String unName) throws Exception{
        this.socketPath="/tmp/sock/"+unName;
    }
    public void start(){
        try {
            SocketChannel socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
            socketChannel.configureBlocking(false);
            Selector selector = Selector.open();

            UnixDomainSocketAddress of = UnixDomainSocketAddress.of(socketPath);
            boolean connect = socketChannel.connect(of);

            socketChannel.register(selector, SelectionKey.OP_READ);
            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

                ByteBuffer buffer = ByteBuffer.allocate(1024);
                String msg;
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()) {
                        //client = (SocketChannel) key.channel();
                        int len = socketChannel.read(buffer);
                        msg = readStringFromByteBuffer(buffer, len);
                        System.out.println("server response:" + msg);

                        // 读操作完成之后，关心写操作，因此注册写事件
                        //client.register(selector, SelectionKey.OP_WRITE);
                    } else {
                        System.out.println("do nothing");
                    }

                    keyIterator.remove();
                }
            }

//            ByteBuffer buf = ByteBuffer.allocate(48);
//            buf.clear();
//            buf.put(newData.getBytes());
//
//            buf.flip();
//
//            while (buf.hasRemaining()) {
//                socketChannel.write(buf);
//            }
            //socketChannel.close();
        }catch (Exception ex){

        }

    }

    private String readStringFromByteBuffer(ByteBuffer buffer, int count) {
        byte[] msgBytes = new byte[count];
        for (int i = 0; i < count; i++) {
            msgBytes[i] = buffer.get(i);
        }
        buffer.clear();
        return new String(msgBytes);
    }

    /**
     * 将buffer中的数据写入到Channel
     */
    public void writeDataToChannel(SocketChannel channel, ByteBuffer buffer) throws Exception {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        buffer.clear();
    }

}
