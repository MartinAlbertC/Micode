/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.gtask.exception;

/**
 * Google Tasks操作失败异常类
 * 用于表示与Google Tasks服务器交互时的操作失败情况
 */
public class ActionFailureException extends RuntimeException {
    // 序列化版本UID
    private static final long serialVersionUID = 4425249765923293627L;

    /**
     * 无参构造函数
     */
    public ActionFailureException() {
        super();
    }

    /**
     * 构造函数
     * @param paramString 异常信息
     */
    public ActionFailureException(String paramString) {
        super(paramString);
    }

    /**
     * 构造函数
     * @param paramString 异常信息
     * @param paramThrowable 引起此异常的原因
     */
    public ActionFailureException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }
}
