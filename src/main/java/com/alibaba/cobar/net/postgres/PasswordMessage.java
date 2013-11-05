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
package com.alibaba.cobar.net.postgres;

/**
 * <pre>
 * PasswordMessage (F) 
 * Byte1('p') Identifies the message as a password response. Note that this is 
 *            also used for GSSAPI and SSPI response messages (which is really
 *            a design error, since the contained data is not a null-terminated 
 *            string in that case, but can be arbitrary binary data).
 * Int32 Length of message contents in bytes, including self. 
 * String The password(encrypted, if requested).
 * </pre>
 * 
 * @author xianmao.hexm 2012-6-26
 */
public class PasswordMessage extends PostgresPacket {

}
