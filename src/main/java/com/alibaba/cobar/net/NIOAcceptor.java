/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cobar.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.log4j.Logger;

import com.alibaba.cobar.net.factory.FrontendConnectionFactory;

/**
 * @author xianmao.hexm
 */
public final class NIOAcceptor extends Thread {
    private static final Logger LOGGER = Logger.getLogger(NIOAcceptor.class);
    private static final AcceptIdGenerator ID_GENERATOR = new AcceptIdGenerator();

    private final int port;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final FrontendConnectionFactory factory;
    private NIOProcessor[] processors;
    private int nextProcessor;
    
    public NIOAcceptor(String name, int port, FrontendConnectionFactory factory) throws IOException {
        LOGGER.info("Init NIOAcceptor :" + name +",at port:" + port);
        super.setName(name);
        this.port = port;
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.socket().bind(new InetSocketAddress(port));
        this.serverChannel.configureBlocking(false);
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.factory = factory;
    }

    public int getPort() {
        return port;
    }


    public void setProcessors(NIOProcessor[] processors) {
        this.processors = processors;
    }

    @Override
    public void run() {
        final Selector selector = this.selector;
        for (;;) {
            try {
                selector.select(1000L);
                Set<SelectionKey> keys = selector.selectedKeys();
                try {
                    for (SelectionKey key : keys) {
                        if (key.isValid() && key.isAcceptable()) {
                            accept();
                        } else {
                            key.cancel();
                        }
                    }
                } finally {
                    keys.clear();
                }
            } catch (Throwable e) {
                LOGGER.warn(getName(), e);
            }
        }
    }
    
    /**
     * accept过程：
     * 1. 通过FrontendConnectionFactory得到一个具体的FrontendConnection c，设置此连接的socket的属性和
<<<<<<< HEAD
     *      c的packetHeaderSize等信息以及一个用来向此写入数据的BufferQueue。还有一个FrontendAuthenticator。
=======
     *      c的packetHeaderSize等信息以及一个用来向此写入数据的BufferQueue。
>>>>>>> origin/master
     * 
     * 2. 分配一个NIOProcessor用来处理此连接，并从NIOProcessor中获得一个用来读数据的BufferPool分配的ByteBuffer。
     * 
     * 3. NIOProcessor向其事件反应器reactor注册此连接。reactor有专门处理读写的2个线程R/W，reactor
     *       首先将此NIOConnection加入到registerQueue中，唤醒R线程中没有注册任何事件的Selector。
     * 
     * 4.  R从阻塞队列中的到此NIOConnection c，c向R中的selector注册读事件，同时向socket中写入mysql协议的握手数据包
     *   （NIOAcceptor中只注册了accept事件），并将自己（NIOConnection）作为attachment放入selector中。（未注册写事件）
     * 
     * 
     */

    private void accept() {
        SocketChannel channel = null;
        try {
            channel = serverChannel.accept();
            channel.configureBlocking(false);
            FrontendConnection c = factory.make(channel);
            c.setAccepted(true);
            c.setId(ID_GENERATOR.getId());
            NIOProcessor processor = nextProcessor();
            c.setProcessor(processor);
            processor.postRegister(c);
        } catch (Throwable e) {
            closeChannel(channel);
            LOGGER.warn(getName(), e);
        }
    }

    private NIOProcessor nextProcessor() {
        if (++nextProcessor == processors.length) {
            nextProcessor = 0;
        }
        return processors[nextProcessor];
    }

    private static void closeChannel(SocketChannel channel) {
        if (channel == null) {
            return;
        }
        Socket socket = channel.socket();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        try {
            channel.close();
        } catch (IOException e) {
        }
    }

    /**
     * 前端连接ID生成器
     * 
     * @author xianmao.hexm
     */
    private static class AcceptIdGenerator {

        private static final long MAX_VALUE = 0xffffffffL;

        private long acceptId = 0L;
        private final Object lock = new Object();

        private long getId() {
            synchronized (lock) {
                if (acceptId >= MAX_VALUE) {
                    acceptId = 0L;
                }
                return ++acceptId;
            }
        }
    }

}
