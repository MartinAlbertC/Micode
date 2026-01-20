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
import android.util.Log;

import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * 元数据类，继承自Task，用于存储任务相关的元信息
 * 主要用于保存与远程Google Task相关联的本地任务元数据
 */
public class MetaData extends Task {
    // 日志标签
    private final static String TAG = MetaData.class.getSimpleName();

    // 关联的Google Task ID
    private String mRelatedGid = null;

    /**
     * 设置元数据信息
     * @param gid Google Task ID
     * @param metaInfo 元数据JSON对象
     */
    public void setMeta(String gid, JSONObject metaInfo) {
        try {
            metaInfo.put(GTaskStringUtils.META_HEAD_GTASK_ID, gid);
        } catch (JSONException e) {
            Log.e(TAG, "failed to put related gid");
        }
        setNotes(metaInfo.toString());
        setName(GTaskStringUtils.META_NOTE_NAME);
    }

    /**
     * 获取关联的Google Task ID
     * @return 关联的Google Task ID
     */
    public String getRelatedGid() {
        return mRelatedGid;
    }

    /**
     * 判断元数据是否值得保存
     * @return 如果元数据不为null则返回true
     */
    @Override
    public boolean isWorthSaving() {
        return getNotes() != null;
    }

    /**
     * 从远程JSON设置内容
     * @param js 远程JSON对象
     */
    @Override
    public void setContentByRemoteJSON(JSONObject js) {
        super.setContentByRemoteJSON(js);
        if (getNotes() != null) {
            try {
                JSONObject metaInfo = new JSONObject(getNotes().trim());
                mRelatedGid = metaInfo.getString(GTaskStringUtils.META_HEAD_GTASK_ID);
            } catch (JSONException e) {
                Log.w(TAG, "failed to get related gid");
                mRelatedGid = null;
            }
        }
    }

    /**
     * 从本地JSON设置内容，该方法不应被调用
     * @param js 本地JSON对象
     * @throws IllegalAccessError 总是抛出该异常
     */
    @Override
    public void setContentByLocalJSON(JSONObject js) {
        // this function should not be called
        throw new IllegalAccessError("MetaData:setContentByLocalJSON should not be called");
    }

    /**
     * 从内容获取本地JSON，该方法不应被调用
     * @return 本地JSON对象
     * @throws IllegalAccessError 总是抛出该异常
     */
    @Override
    public JSONObject getLocalJSONFromContent() {
        throw new IllegalAccessError("MetaData:getLocalJSONFromContent should not be called");
    }

    /**
     * 获取同步操作类型，该方法不应被调用
     * @param c 游标
     * @return 同步操作类型
     * @throws IllegalAccessError 总是抛出该异常
     */
    @Override
    public int getSyncAction(Cursor c) {
        throw new IllegalAccessError("MetaData:getSyncAction should not be called");
    }

}
