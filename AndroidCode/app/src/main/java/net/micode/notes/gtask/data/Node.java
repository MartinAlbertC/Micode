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

package net.micode.notes.gtask.data;

import android.database.Cursor;

import org.json.JSONObject;

/**
 * 抽象同步节点类，定义了同步操作的基本结构和接口
 * 是Task和TaskList的父类，用于处理本地与远程Google Tasks的同步
 */
public abstract class Node {
    // 无同步操作
    public static final int SYNC_ACTION_NONE = 0;

    // 向远程添加数据
    public static final int SYNC_ACTION_ADD_REMOTE = 1;

    // 向本地添加数据
    public static final int SYNC_ACTION_ADD_LOCAL = 2;

    // 从远程删除数据
    public static final int SYNC_ACTION_DEL_REMOTE = 3;

    // 从本地删除数据
    public static final int SYNC_ACTION_DEL_LOCAL = 4;

    // 更新远程数据
    public static final int SYNC_ACTION_UPDATE_REMOTE = 5;

    // 更新本地数据
    public static final int SYNC_ACTION_UPDATE_LOCAL = 6;

    // 同步冲突
    public static final int SYNC_ACTION_UPDATE_CONFLICT = 7;

    // 同步错误
    public static final int SYNC_ACTION_ERROR = 8;

    // Google Task唯一标识符
    private String mGid;

    // 节点名称
    private String mName;

    // 最后修改时间
    private long mLastModified;

    // 是否已删除
    private boolean mDeleted;

    /**
     * 构造函数，初始化节点基本属性
     */
    public Node() {
        mGid = null;
        mName = "";
        mLastModified = 0;
        mDeleted = false;
    }

    /**
     * 获取创建操作的JSON对象
     * @param actionId 操作ID
     * @return 创建操作的JSON对象
     */
    public abstract JSONObject getCreateAction(int actionId);

    /**
     * 获取更新操作的JSON对象
     * @param actionId 操作ID
     * @return 更新操作的JSON对象
     */
    public abstract JSONObject getUpdateAction(int actionId);

    /**
     * 根据远程JSON对象设置内容
     * @param js 远程JSON对象
     */
    public abstract void setContentByRemoteJSON(JSONObject js);

    /**
     * 根据本地JSON对象设置内容
     * @param js 本地JSON对象
     */
    public abstract void setContentByLocalJSON(JSONObject js);

    /**
     * 从内容获取本地JSON对象
     * @return 本地JSON对象
     */
    public abstract JSONObject getLocalJSONFromContent();

    /**
     * 获取同步操作类型
     * @param c 数据库游标
     * @return 同步操作类型
     */
    public abstract int getSyncAction(Cursor c);

    /**
     * 设置Google Task唯一标识符
     * @param gid Google Task ID
     */
    public void setGid(String gid) {
        this.mGid = gid;
    }

    /**
     * 设置节点名称
     * @param name 节点名称
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * 设置最后修改时间
     * @param lastModified 最后修改时间
     */
    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    /**
     * 设置删除状态
     * @param deleted 是否删除
     */
    public void setDeleted(boolean deleted) {
        this.mDeleted = deleted;
    }

    /**
     * 获取Google Task唯一标识符
     * @return Google Task ID
     */
    public String getGid() {
        return this.mGid;
    }

    /**
     * 获取节点名称
     * @return 节点名称
     */
    public String getName() {
        return this.mName;
    }

    /**
     * 获取最后修改时间
     * @return 最后修改时间
     */
    public long getLastModified() {
        return this.mLastModified;
    }

    /**
     * 获取删除状态
     * @return 是否已删除
     */
    public boolean getDeleted() {
        return this.mDeleted;
    }

}
