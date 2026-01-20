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

package net.micode.notes.model;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;

import java.util.ArrayList;


/**
 * 笔记模型类，用于管理笔记的创建、更新和同步操作
 * 支持文本笔记和通话记录笔记，通过ContentResolver与数据库交互
 */
public class Note {
    // 日志标签
    private static final String TAG = "Note";
    
    // 存储笔记基本信息的变更值
    private ContentValues mNoteDiffValues;
    
    // 存储笔记数据内容的内部对象
    private NoteData mNoteData;
    /**
     * 为添加新笔记到数据库创建一个新的笔记ID
     * @param context 上下文对象
     * @param folderId 文件夹ID，新笔记将被添加到该文件夹
     * @return 新创建的笔记ID
     * @throws IllegalStateException 如果创建笔记失败
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // Create a new note in the database
        ContentValues values = new ContentValues();
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime);
        values.put(NoteColumns.MODIFIED_DATE, createdTime);
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        values.put(NoteColumns.PARENT_ID, folderId);
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values);

        long noteId = 0;
        try {
            noteId = Long.valueOf(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Get note id error :" + e.toString());
            noteId = 0;
        }
        if (noteId == -1) {
            throw new IllegalStateException("Wrong note id:" + noteId);
        }
        return noteId;
    }

    /**
     * 构造方法，初始化笔记对象
     */
    public Note() {
        mNoteDiffValues = new ContentValues();
        mNoteData = new NoteData();
    }

    /**
     * 设置笔记的基本属性值
     * @param key 属性键名
     * @param value 属性值
     */
    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value);
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
    }

    /**
     * 设置笔记的文本数据内容
     * @param key 数据键名
     * @param value 数据值
     */
    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value);
    }

    /**
     * 设置文本数据的ID
     * @param id 文本数据ID
     */
    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id);
    }

    /**
     * 获取文本数据的ID
     * @return 文本数据ID
     */
    public long getTextDataId() {
        return mNoteData.mTextDataId;
    }

    /**
     * 设置通话记录数据的ID
     * @param id 通话记录数据ID
     */
    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id);
    }

    /**
     * 设置笔记的通话记录数据内容
     * @param key 数据键名
     * @param value 数据值
     */
    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value);
    }

    /**
     * 检查笔记是否在本地被修改
     * @return 如果笔记被修改返回true，否则返回false
     */
    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified();
    }

    /**
     * 将本地修改的笔记数据同步到ContentResolver
     * @param context 上下文对象
     * @param noteId 要同步的笔记ID
     * @return 如果同步成功返回true，否则返回false
     * @throws IllegalArgumentException 如果笔记ID无效
     */
    public boolean syncNote(Context context, long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Wrong note id:" + noteId);
        }

        if (!isLocalModified()) {
            return true;
        }

        /**
         * 理论上，一旦数据改变，笔记的{@link NoteColumns#LOCAL_MODIFIED}和{@link NoteColumns#MODIFIED_DATE}字段应该被更新
         * 为了数据安全，即使更新笔记失败，我们也会更新笔记数据信息
         */
        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            Log.e(TAG, "Update note error, should not happen");
            // 不返回，继续执行
        }
        mNoteDiffValues.clear();

        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false;
        }

        return true;
    }

    /**
     * 笔记数据内部类，用于管理笔记的具体数据内容
     * 包括文本数据和通话记录数据
     */
    private class NoteData {
        // 日志标签
        private static final String TAG = "NoteData";
        
        // 文本数据ID
        private long mTextDataId;

        // 文本数据内容的变更值
        private ContentValues mTextDataValues;

        // 通话记录数据ID
        private long mCallDataId;

        // 通话记录数据内容的变更值
        private ContentValues mCallDataValues;

        /**
         * 构造方法，初始化笔记数据对象
         */
        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        }

        /**
         * 检查笔记数据是否在本地被修改
         * @return 如果数据被修改返回true，否则返回false
         */
        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0;
        }

        /**
         * 设置文本数据ID
         * @param id 文本数据ID
         * @throws IllegalArgumentException 如果ID无效
         */
        void setTextDataId(long id) {
            if(id <= 0) {
                throw new IllegalArgumentException("Text data id should larger than 0");
            }
            mTextDataId = id;
        }

        /**
         * 设置通话记录数据ID
         * @param id 通话记录数据ID
         * @throws IllegalArgumentException 如果ID无效
         */
        void setCallDataId(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("Call data id should larger than 0");
            }
            mCallDataId = id;
        }

        /**
         * 设置通话记录数据内容
         * @param key 数据键名
         * @param value 数据值
         */
        void setCallData(String key, String value) {
            mCallDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }

        /**
         * 设置文本数据内容
         * @param key 数据键名
         * @param value 数据值
         */
        void setTextData(String key, String value) {
            mTextDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }

        /**
         * 将笔记数据推送到ContentResolver
         * @param context 上下文对象
         * @param noteId 笔记ID
         * @return 如果操作成功返回笔记的Uri，否则返回null
         * @throws IllegalArgumentException 如果笔记ID无效
         */
        Uri pushIntoContentResolver(Context context, long noteId) {
            /**
             * 安全检查
             */
            if (noteId <= 0) {
                throw new IllegalArgumentException("Wrong note id:" + noteId);
            }

            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;

            if(mTextDataValues.size() > 0) {
                mTextDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mTextDataId == 0) {
                    // 新建文本数据
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mTextDataValues);
                    try {
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new text data fail with noteId" + noteId);
                        mTextDataValues.clear();
                        return null;
                    }
                } else {
                    // 更新已有文本数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues);
                    operationList.add(builder.build());
                }
                mTextDataValues.clear();
            }

            if(mCallDataValues.size() > 0) {
                mCallDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mCallDataId == 0) {
                    // 新建通话记录数据
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mCallDataValues);
                    try {
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new call data fail with noteId" + noteId);
                        mCallDataValues.clear();
                        return null;
                    }
                } else {
                    // 更新已有通话记录数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues);
                    operationList.add(builder.build());
                }
                mCallDataValues.clear();
            }

            // 执行批量操作
            if (operationList.size() > 0) {
                try {
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(
                            Notes.AUTHORITY, operationList);
                    return (results == null || results.length == 0 || results[0] == null) ? null
                            : ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId);
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                }
            }
            return null;
        }
    }
}
