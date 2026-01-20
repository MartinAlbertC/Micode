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
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 表示Google Tasks中的任务项，继承自Node类
 * 用于管理任务的各种属性和操作，包括创建、更新、同步等功能
 */
public class Task extends Node {
    // 日志标签
    private static final String TAG = Task.class.getSimpleName();

    // 任务是否已完成
    private boolean mCompleted;

    // 任务的详细说明
    private String mNotes;

    // 任务的元信息JSON对象
    private JSONObject mMetaInfo;

    // 前一个兄弟任务（用于排序）
    private Task mPriorSibling;

    // 父任务列表
    private TaskList mParent;

    /**
     * 构造函数，初始化任务对象
     */
    public Task() {
        super();
        mCompleted = false;
        mNotes = null;
        mPriorSibling = null;
        mParent = null;
        mMetaInfo = null;
    }

    /**
     * 获取创建任务的JSON操作对象
     * @param actionId 操作ID
     * @return 创建任务的JSON操作对象
     * @throws ActionFailureException 生成JSON对象失败时抛出
     */
    public JSONObject getCreateAction(int actionId) {
        JSONObject js = new JSONObject();

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE);

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);

            // index
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mParent.getChildTaskIndex(this));

            // entity_delta
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_TASK);
            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
            }
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

            // parent_id
            js.put(GTaskStringUtils.GTASK_JSON_PARENT_ID, mParent.getGid());

            // dest_parent_type
            js.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);

            // list_id
            js.put(GTaskStringUtils.GTASK_JSON_LIST_ID, mParent.getGid());

            // prior_sibling_id
            if (mPriorSibling != null) {
                js.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, mPriorSibling.getGid());
            }

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate task-create jsonobject");
        }

        return js;
    }

    /**
     * 获取更新任务的JSON操作对象
     * @param actionId 操作ID
     * @return 更新任务的JSON操作对象
     * @throws ActionFailureException 生成JSON对象失败时抛出
     */
    public JSONObject getUpdateAction(int actionId) {
        JSONObject js = new JSONObject();

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE);

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);

            // id
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid());

            // entity_delta
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
            }
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted());
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate task-update jsonobject");
        }

        return js;
    }

    /**
     * 根据远程JSON对象设置任务内容
     * @param js 远程JSON对象
     * @throws ActionFailureException 解析JSON对象失败时抛出
     */
    public void setContentByRemoteJSON(JSONObject js) {
        if (js != null) {
            try {
                // id
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) {
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID));
                }

                // last_modified
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) {
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED));
                }

                // name
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) {
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME));
                }

                // notes
                if (js.has(GTaskStringUtils.GTASK_JSON_NOTES)) {
                    setNotes(js.getString(GTaskStringUtils.GTASK_JSON_NOTES));
                }

                // deleted
                if (js.has(GTaskStringUtils.GTASK_JSON_DELETED)) {
                    setDeleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_DELETED));
                }

                // completed
                if (js.has(GTaskStringUtils.GTASK_JSON_COMPLETED)) {
                    setCompleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_COMPLETED));
                }
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("fail to get task content from jsonobject");
            }
        }
    }

    /**
     * 根据本地JSON对象设置任务内容
     * @param js 本地JSON对象
     */
    public void setContentByLocalJSON(JSONObject js) {
        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)
                || !js.has(GTaskStringUtils.META_HEAD_DATA)) {
            Log.w(TAG, "setContentByLocalJSON: nothing is avaiable");
        }

        try {
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);

            if (note.getInt(NoteColumns.TYPE) != Notes.TYPE_NOTE) {
                Log.e(TAG, "invalid type");
                return;
            }

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject data = dataArray.getJSONObject(i);
                if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) {
                    setName(data.getString(DataColumns.CONTENT));
                    break;
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 从任务内容生成本地JSON对象
     * @return 本地JSON对象，若生成失败则返回null
     */
    public JSONObject getLocalJSONFromContent() {
        String name = getName();
        try {
            if (mMetaInfo == null) {
                // new task created from web
                if (name == null) {
                    Log.w(TAG, "the note seems to be an empty one");
                    return null;
                }

                JSONObject js = new JSONObject();
                JSONObject note = new JSONObject();
                JSONArray dataArray = new JSONArray();
                JSONObject data = new JSONObject();
                data.put(DataColumns.CONTENT, name);
                dataArray.put(data);
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray);
                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);
                return js;
            } else {
                // synced task
                JSONObject note = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                JSONArray dataArray = mMetaInfo.getJSONArray(GTaskStringUtils.META_HEAD_DATA);

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);
                    if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) {
                        data.put(DataColumns.CONTENT, getName());
                        break;
                    }
                }

                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
                return mMetaInfo;
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置任务的元数据
     * @param metaData 元数据对象
     */
    public void setMetaInfo(MetaData metaData) {
        if (metaData != null && metaData.getNotes() != null) {
            try {
                mMetaInfo = new JSONObject(metaData.getNotes());
            } catch (JSONException e) {
                Log.w(TAG, e.toString());
                mMetaInfo = null;
            }
        }
    }

    /**
     * 获取同步操作类型
     * @param c 数据库游标
     * @return 同步操作类型（SYNC_ACTION_NONE、SYNC_ACTION_UPDATE_REMOTE等）
     */
    public int getSyncAction(Cursor c) {
        try {
            JSONObject noteInfo = null;
            if (mMetaInfo != null && mMetaInfo.has(GTaskStringUtils.META_HEAD_NOTE)) {
                noteInfo = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            }

            if (noteInfo == null) {
                Log.w(TAG, "it seems that note meta has been deleted");
                return SYNC_ACTION_UPDATE_REMOTE;
            }

            if (!noteInfo.has(NoteColumns.ID)) {
                Log.w(TAG, "remote note id seems to be deleted");
                return SYNC_ACTION_UPDATE_LOCAL;
            }

            // validate the note id now
            if (c.getLong(SqlNote.ID_COLUMN) != noteInfo.getLong(NoteColumns.ID)) {
                Log.w(TAG, "note id doesn't match");
                return SYNC_ACTION_UPDATE_LOCAL;
            }

            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                // there is no local update
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // no update both side
                    return SYNC_ACTION_NONE;
                } else {
                    // apply remote to local
                    return SYNC_ACTION_UPDATE_LOCAL;
                }
            } else {
                // validate gtask id
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                    Log.e(TAG, "gtask id doesn't match");
                    return SYNC_ACTION_ERROR;
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // local modification only
                    return SYNC_ACTION_UPDATE_REMOTE;
                } else {
                    return SYNC_ACTION_UPDATE_CONFLICT;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return SYNC_ACTION_ERROR;
    }

    /**
     * 判断任务是否值得保存
     * @return 是否值得保存
     */
    public boolean isWorthSaving() {
        return mMetaInfo != null || (getName() != null && getName().trim().length() > 0)
                || (getNotes() != null && getNotes().trim().length() > 0);
    }

    /**
     * 设置任务的完成状态
     * @param completed 是否完成
     */
    public void setCompleted(boolean completed) {
        this.mCompleted = completed;
    }

    /**
     * 设置任务的详细说明
     * @param notes 详细说明内容
     */
    public void setNotes(String notes) {
        this.mNotes = notes;
    }

    /**
     * 设置前一个兄弟任务
     * @param priorSibling 前一个兄弟任务
     */
    public void setPriorSibling(Task priorSibling) {
        this.mPriorSibling = priorSibling;
    }

    /**
     * 设置父任务列表
     * @param parent 父任务列表
     */
    public void setParent(TaskList parent) {
        this.mParent = parent;
    }

    /**
     * 获取任务的完成状态
     * @return 是否完成
     */
    public boolean getCompleted() {
        return this.mCompleted;
    }

    /**
     * 获取任务的详细说明
     * @return 详细说明内容
     */
    public String getNotes() {
        return this.mNotes;
    }

    /**
     * 获取前一个兄弟任务
     * @return 前一个兄弟任务
     */
    public Task getPriorSibling() {
        return this.mPriorSibling;
    }

    /**
     * 获取父任务列表
     * @return 父任务列表
     */
    public TaskList getParent() {
        return this.mParent;
    }

}
