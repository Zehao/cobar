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
package com.alibaba.cobar.server.handler;

import com.alibaba.cobar.config.ErrorCode;
import com.alibaba.cobar.server.ServerConnection;
import com.alibaba.cobar.server.parser.ServerParse;
import com.alibaba.cobar.server.parser.ServerParseStart;

/**
 * @author xianmao.hexm
 */
public final class StartHandler {

    public static void handle(String stmt, ServerConnection c, int offset) {
        switch (ServerParseStart.parse(stmt, offset)) {
        case ServerParseStart.TRANSACTION:
            c.writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Unsupported statement");
            break;
        default:
            c.execute(stmt, ServerParse.START);
        }
    }

}
