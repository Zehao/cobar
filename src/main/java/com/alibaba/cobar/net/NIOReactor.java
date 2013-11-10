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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.alibaba.cobar.config.ErrorCode;

/**
 * 网络事件反应器
 *
 * @author xianmao.hexm
 */
public final class NIOReactor {

    private static final Logger LOGGER = Logger.getLogger(NIOReactor.class);

    private final String name;
    private final R reactorR;
    private final W reactorW;

    public NIOReactor(String name) throws IOException {
        this.name = name;
        this.reactorR = new R();
        this.reactorW = new W();
    }

    final void startup() {
        new Thread(reactorR, name + "-R").start();
        new Thread(reactorW, name + "-W").start();
    }

    final void postRegister(NIOConnection c) {
        reactorR.registerQueue.offer(c);
        reactorR.selector.wakeup();
    }

    final BlockingQueue<NIOConnection> getRegisterQueue() {
        return reactorR.registerQueue;
    }

    final void postWrite(NIOConnection c) {
        reactorW.writeQueue.offer(c);
    }

    final BlockingQueue<NIOConnection> getWriteQueue() {
        return reactorW.writeQueue;
    }

    /**
     * read/write过程。
     *
     * 首先 验证过程： 
     * 1.
     * 此FrontendConnection在register时绑定了一个FrontendAuthenticator，由FrontendAuthenticator来handle第一次
     * 接收到的client数据包。 
     * 2.
     * 验证完用户名，密码，schema后，将此FrontendConnection的handler改为FrontendCommandHandler，FrontendAuthenticator
     * 的作用到此为止。同时向socket写入OK Packet。
     * 3.
     * 在写入ok包的时候，c会将包保存在writeQueue上，同时向reactor的W线程的阻塞队列中加入此自己。 
     * 4.
     * W线程从阻塞队列中取出c，执行c的writeByQueue()。 
     * 5.
     * 开始命令的接收与处理。
     */
    private final class R implements Runnable {

        private final Selector selector;
        private final BlockingQueue<NIOConnection> registerQueue;

        private R() throws IOException {
            this.selector = Selector.open();
            this.registerQueue = new LinkedBlockingQueue<NIOConnection>();
        }

        @Override
        public void run() {
            final Selector selector = this.selector;
            for (;;) {
                try {
                    selector.select(1000L);
                    register();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    try {
                        for (SelectionKey key : keys) {
                            Object att = key.attachment();
                            if (att != null && key.isValid()) {
                                int readyOps = key.readyOps();
                                if ((readyOps & SelectionKey.OP_READ) != 0) {
                                    read((NIOConnection) att);
                                } else if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                                    write((NIOConnection) att);
                                } else {
                                    key.cancel();
                                }
                            } else {
                                key.cancel();
                            }
                        }
                    } finally {
                        keys.clear();
                    }
                } catch (Throwable e) {
                    LOGGER.warn(name, e);
                }
            }
        }

        private void register() {
            NIOConnection c = null;
            while ((c = registerQueue.poll()) != null) {
                try {
                    c.register(selector);
                } catch (Throwable e) {
                    c.error(ErrorCode.ERR_REGISTER, e);
                }
            }
        }

        private void read(NIOConnection c) {
            try {
                c.read();
            } catch (Throwable e) {
                c.error(ErrorCode.ERR_READ, e);
            }
        }

        private void write(NIOConnection c) {
            try {
                LOGGER.info("write by event");
                c.writeByEvent();
            } catch (Throwable e) {
                c.error(ErrorCode.ERR_WRITE_BY_EVENT, e);
            }
        }
    }

    private final class W implements Runnable {

        private final BlockingQueue<NIOConnection> writeQueue;

        private W() {
            this.writeQueue = new LinkedBlockingQueue<NIOConnection>();
        }

        @Override
        public void run() {
            NIOConnection c = null;
            for (;;) {
                try {
                    if ((c = writeQueue.take()) != null) {
                        write(c);
                    }
                } catch (Throwable e) {
                    LOGGER.warn(name, e);
                }
            }
        }

        private void write(NIOConnection c) {
            try {
                LOGGER.info("write by queue");
                c.writeByQueue();
            } catch (Throwable e) {
                c.error(ErrorCode.ERR_WRITE_BY_QUEUE, e);
            }
        }
    }

}
