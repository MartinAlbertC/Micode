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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;
import net.micode.notes.gtask.exception.ActionFailureException;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * 数据库数据处理类，用于管理笔记的数据部分
 * 提供与ContentResolver的交互，实现数据的创建、更新和提交
 */
public class SqlData {
    // 日志标签
    private static final String TAG = SqlData.class.getSimpleName();

    // 无效ID常量
    private static final int INVALID_ID = -99999;

    // 数据查询投影数组
    public static final String[] PROJECTION_DATA = new String[] {
            DataColumns.ID, DataColumns.MIME_TYPE, DataColumns.CONTENT, DataColumns.DATA1,
            DataColumns.DATA3
    };

    // 投影数组中ID列的索引
    public static final int DATA_ID_COLUMN = 0;

    // 投影数组中MIME类型列的索引
    public static final int DATA_MIME_TYPE_COLUMN = 1;

    // 投影数组中内容列的索引
    public static final int DATA_CONTENT_COLUMN = 2;

    // 投影数组中DATA1列的索引
    public static final int DATA_CONTENT_DATA_1_COLUMN = 3;

    // 投影数组中DATA3列的索引
    public static final int DATA_CONTENT_DATA_3_COLUMN = 4;

    // 内容解析器
    private ContentResolver mContentResolver;

    // 是否为创建操作
    private boolean mIsCreate;

    // 数据ID
    private long mDataId;

    // 数据MIME类型
    private String mDataMimeType;

    // 数据内容
    private String mDataContent;

    // 数据内容DATA1字段
    private long mDataContentData1;

    // 数据内容DATA3字段
    private String mDataContentData3;

    // 差异数据值，用于记录需要更新的字段
    private ContentValues mDiffDataValues;

    /**
     * 构造函数，创建新的数据对象
     * @param context 上下文对象
     */
    public SqlData(Context context) {
        mContentResolver = context.getContentResolver();
        mIsCreate = true;
        mDataId = INVALID_ID;
        mDataMimeType = DataConstants.NOTE;
        mDataContent = "";
        mDataContentData1 = 0;
        mDataContentData3 = "";
        mDiffDataValues = new ContentValues();
    }

    /**
     * 构造函数，从游标创建数据对象
     * @param context 上下文对象
     * @param c 数据库游标
     */
    public SqlData(Context context, Cursor c) {
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(c);
        mDiffDataValues = new ContentValues();
    }

    /**
     * 从游标加载数据
     * @param c 数据库游标
     */
    private void loadFromCursor(Cursor c) {
        mDataId = c.getLong(DATA_ID_COLUMN);
        mDataMimeType = c.getString(DATA_MIME_TYPE_COLUMN);
        mDataContent = c.getString(DATA_CONTENT_COLUMN);
        mDataContentData1 = c.getLong(DATA_CONTENT_DATA_1_COLUMN);
        mDataContentData3 = c.getString(DATA_CONTENT_DATA_3_COLUMN);
    }

    /**
     * 根据JSON对象设置数据内容
     * @param js JSON对象
     * @throws JSONException JSON解析异常
     */
    public void setContent(JSONObject js) throws JSONException {
        long dataId = js.has(DataColumns.ID) ? js.getLong(DataColumns.ID) : INVALID_ID;
        if (mIsCreate || mDataId != dataId) {
            mDiffDataValues.put(DataColumns.ID, dataId);
        }
        mDataId = dataId;

        String dataMimeType = js.has(DataColumns.MIME_TYPE) ? js.getString(DataColumns.MIME_TYPE)
                : DataConstants.NOTE;
        if (mIsCreate || !mDataMimeType.equals(dataMimeType)) {
            mDiffDataValues.put(DataColumns.MIME_TYPE, dataMimeType);
        }
        mDataMimeType = dataMimeType;

        String dataContent = js.has(DataColumns.CONTENT) ? js.getString(DataColumns.CONTENT) : "";
        if (mIsCreate || !mDataContent.equals(dataContent)) {
            mDiffDataValues.put(DataColumns.CONTENT, dataContent);
        }
        mDataContent = dataContent;

        long dataContentData1 = js.has(DataColumns.DATA1) ? js.getLong(DataColumns.DATA1) : 0;
        if (mIsCreate || mDataContentData1 != dataContentData1) {
            mDiffDataValues.put(DataColumns.DATA1, dataContentData1);
        }
        mDataContentData1 = dataContentData1;

        String dataContentData3 = js.has(DataColumns.DATA3) ? js.getString(DataColumns.DATA3) : "";
        if (mIsCreate || !mDataContentData3.equals(dataContentData3)) {
            mDiffDataValues.put(DataColumns.DATA3, dataContentData3);
        }
        mDataContentData3 = dataContentData3;
    }

    /**
     * 获取数据内容的JSON对象
     * @return JSON对象
     * @throws JSONException JSON解析异常
     */
    public JSONObject getContent() throws JSONException {
        if (mIsCreate) {
            Log.e(TAG, "it seems that we haven't created this in database yet");
            return null;
        }
        JSONObject js = new JSONObject();
        js.put(DataColumns.ID, mDataId);
        js.put(DataColumns.MIME_TYPE, mDataMimeType);
        js.put(DataColumns.CONTENT, mDataContent);
        js.put(DataColumns.DATA1, mDataContentData1);
        js.put(DataColumns.DATA3, mDataContentData3);
        return js;
    }

    /**
     * 提交数据到数据库
     * @param noteId 笔记ID
     * @param validateVersion 是否验证版本
     * @param version 版本号
     * @throws ActionFailureException 创建笔记失败时抛出
     */
    public void commit(long noteId, boolean validateVersion, long version) {

        if (mIsCreate) {
            if (mDataId == INVALID_ID && mDiffDataValues.containsKey(DataColumns.ID)) {
                mDiffDataValues.remove(DataColumns.ID);
            }

            mDiffDataValues.put(DataColumns.NOTE_ID, noteId);
            Uri uri = mContentResolver.insert(Notes.CONTENT_DATA_URI, mDiffDataValues);
            try {
                mDataId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Get note id error :" + e.toString());
                throw new ActionFailureException("create note failed");
            }
        } else {
            if (mDiffDataValues.size() > 0) {
                int result = 0;
                if (!validateVersion) {
                    result = mContentResolver.update(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mDataId), mDiffDataValues, null, null);
                } else {
                    result = mContentResolver.update(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mDataId), mDiffDataValues,
                            " ? in (SELECT " + NoteColumns.ID + " FROM " + TABLE.NOTE
                                    + " WHERE " + NoteColumns.VERSION + "=?)", new String[] {
                                    String.valueOf(noteId), String.valueOf(version)
                            });
                }
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }
        }

        mDiffDataValues.clear();
        mIsCreate = false;
    }

    /**
     * 获取数据ID
     * @return 数据ID
     */
    public long getId() {
        return mDataId;
    }
}
