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

package net.micode.notes.ui;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import net.micode.notes.data.Contact;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.DataUtils;


/**
 * 便签列表项数据模型类
 * 用于从数据库Cursor中提取和封装便签数据，提供访问便签各种属性的方法。
 * 包含便签的基本信息、样式设置、位置状态等。
 */
public class NoteItemData {
    /**
     * 数据库查询的字段投影数组
     * 定义了从Notes表中查询的字段列表，用于构建数据库查询。
     */
    static final String [] PROJECTION = new String [] {
        NoteColumns.ID,
        NoteColumns.ALERTED_DATE,
        NoteColumns.BG_COLOR_ID,
        NoteColumns.CREATED_DATE,
        NoteColumns.HAS_ATTACHMENT,
        NoteColumns.MODIFIED_DATE,
        NoteColumns.NOTES_COUNT,
        NoteColumns.PARENT_ID,
        NoteColumns.SNIPPET,
        NoteColumns.TYPE,
        NoteColumns.WIDGET_ID,
        NoteColumns.WIDGET_TYPE,
    };

    /** ID列索引 */
    private static final int ID_COLUMN                    = 0;
    /** 提醒日期列索引 */
    private static final int ALERTED_DATE_COLUMN          = 1;
    /** 背景颜色ID列索引 */
    private static final int BG_COLOR_ID_COLUMN           = 2;
    /** 创建日期列索引 */
    private static final int CREATED_DATE_COLUMN          = 3;
    /** 是否有附件列索引 */
    private static final int HAS_ATTACHMENT_COLUMN        = 4;
    /** 修改日期列索引 */
    private static final int MODIFIED_DATE_COLUMN         = 5;
    /** 便签数量列索引 */
    private static final int NOTES_COUNT_COLUMN           = 6;
    /** 父文件夹ID列索引 */
    private static final int PARENT_ID_COLUMN             = 7;
    /** 摘要文本列索引 */
    private static final int SNIPPET_COLUMN               = 8;
    /** 便签类型列索引 */
    private static final int TYPE_COLUMN                  = 9;
    /** 小部件ID列索引 */
    private static final int WIDGET_ID_COLUMN             = 10;
    /** 小部件类型列索引 */
    private static final int WIDGET_TYPE_COLUMN           = 11;

    /** 便签ID */
    private long mId;
    /** 提醒日期 */
    private long mAlertDate;
    /** 背景颜色ID */
    private int mBgColorId;
    /** 创建日期 */
    private long mCreatedDate;
    /** 是否有附件 */
    private boolean mHasAttachment;
    /** 修改日期 */
    private long mModifiedDate;
    /** 便签数量（文件夹使用） */
    private int mNotesCount;
    /** 父文件夹ID */
    private long mParentId;
    /** 便签摘要文本 */
    private String mSnippet;
    /** 便签类型 */
    private int mType;
    /** 小部件ID */
    private int mWidgetId;
    /** 小部件类型 */
    private int mWidgetType;
    /** 联系人姓名（通话记录便签使用） */
    private String mName;
    /** 电话号码（通话记录便签使用） */
    private String mPhoneNumber;

    /** 是否为列表中的最后一项 */
    private boolean mIsLastItem;
    /** 是否为列表中的第一项 */
    private boolean mIsFirstItem;
    /** 是否为列表中的唯一一项 */
    private boolean mIsOnlyOneItem;
    /** 是否为文件夹下的唯一便签 */
    private boolean mIsOneNoteFollowingFolder;
    /** 是否为文件夹下的多个便签之一 */
    private boolean mIsMultiNotesFollowingFolder;

    /**
     * 构造函数
     * 从数据库Cursor中提取便签数据并初始化对象。
     * @param context 上下文对象
     * @param cursor 数据库查询结果Cursor
     */
    public NoteItemData(Context context, Cursor cursor) {
        mId = cursor.getLong(ID_COLUMN);
        mAlertDate = cursor.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = cursor.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = cursor.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = (cursor.getInt(HAS_ATTACHMENT_COLUMN) > 0) ? true : false;
        mModifiedDate = cursor.getLong(MODIFIED_DATE_COLUMN);
        mNotesCount = cursor.getInt(NOTES_COUNT_COLUMN);
        mParentId = cursor.getLong(PARENT_ID_COLUMN);
        mSnippet = cursor.getString(SNIPPET_COLUMN);
        // 移除待办事项标记
        mSnippet = mSnippet.replace(NoteEditActivity.TAG_CHECKED, "").replace(
                NoteEditActivity.TAG_UNCHECKED, "");
        mType = cursor.getInt(TYPE_COLUMN);
        mWidgetId = cursor.getInt(WIDGET_ID_COLUMN);
        mWidgetType = cursor.getInt(WIDGET_TYPE_COLUMN);

        mPhoneNumber = "";
        // 如果是通话记录便签，获取电话号码和联系人信息
        if (mParentId == Notes.ID_CALL_RECORD_FOLDER) {
            mPhoneNumber = DataUtils.getCallNumberByNoteId(context.getContentResolver(), mId);
            if (!TextUtils.isEmpty(mPhoneNumber)) {
                mName = Contact.getContact(context, mPhoneNumber);
                if (mName == null) {
                    mName = mPhoneNumber;
                }
            }
        }

        if (mName == null) {
            mName = "";
        }
        // 检查便签在列表中的位置状态
        checkPostion(cursor);
    }

    /**
     * 检查便签在列表中的位置状态
     * 确定便签是否为列表的第一项、最后一项、唯一一项，以及是否为文件夹下的单个或多个便签之一。
     * @param cursor 数据库查询结果Cursor
     */
    private void checkPostion(Cursor cursor) {
        mIsLastItem = cursor.isLast() ? true : false;
        mIsFirstItem = cursor.isFirst() ? true : false;
        mIsOnlyOneItem = (cursor.getCount() == 1);
        mIsMultiNotesFollowingFolder = false;
        mIsOneNoteFollowingFolder = false;

        // 检查是否为文件夹下的便签
        if (mType == Notes.TYPE_NOTE && !mIsFirstItem) {
            int position = cursor.getPosition();
            if (cursor.moveToPrevious()) {
                // 检查前一项是否为文件夹
                if (cursor.getInt(TYPE_COLUMN) == Notes.TYPE_FOLDER
                        || cursor.getInt(TYPE_COLUMN) == Notes.TYPE_SYSTEM) {
                    // 检查是否为文件夹下的多个便签之一
                    if (cursor.getCount() > (position + 1)) {
                        mIsMultiNotesFollowingFolder = true;
                    } else {
                        mIsOneNoteFollowingFolder = true;
                    }
                }
                // 恢复Cursor位置
                if (!cursor.moveToNext()) {
                    throw new IllegalStateException("cursor move to previous but can't move back");
                }
            }
        }
    }

    /**
     * 是否为文件夹下的唯一便签
     * @return 是否为文件夹下的唯一便签
     */
    public boolean isOneFollowingFolder() {
        return mIsOneNoteFollowingFolder;
    }

    /**
     * 是否为文件夹下的多个便签之一
     * @return 是否为文件夹下的多个便签之一
     */
    public boolean isMultiFollowingFolder() {
        return mIsMultiNotesFollowingFolder;
    }

    /**
     * 是否为列表中的最后一项
     * @return 是否为列表中的最后一项
     */
    public boolean isLast() {
        return mIsLastItem;
    }

    /**
     * 获取联系人姓名
     * @return 联系人姓名
     */
    public String getCallName() {
        return mName;
    }

    /**
     * 是否为列表中的第一项
     * @return 是否为列表中的第一项
     */
    public boolean isFirst() {
        return mIsFirstItem;
    }

    /**
     * 是否为列表中的唯一一项
     * @return 是否为列表中的唯一一项
     */
    public boolean isSingle() {
        return mIsOnlyOneItem;
    }

    /**
     * 获取便签ID
     * @return 便签ID
     */
    public long getId() {
        return mId;
    }

    /**
     * 获取提醒日期
     * @return 提醒日期
     */
    public long getAlertDate() {
        return mAlertDate;
    }

    /**
     * 获取创建日期
     * @return 创建日期
     */
    public long getCreatedDate() {
        return mCreatedDate;
    }

    /**
     * 是否有附件
     * @return 是否有附件
     */
    public boolean hasAttachment() {
        return mHasAttachment;
    }

    /**
     * 获取修改日期
     * @return 修改日期
     */
    public long getModifiedDate() {
        return mModifiedDate;
    }

    /**
     * 获取背景颜色ID
     * @return 背景颜色ID
     */
    public int getBgColorId() {
        return mBgColorId;
    }

    /**
     * 获取父文件夹ID
     * @return 父文件夹ID
     */
    public long getParentId() {
        return mParentId;
    }

    /**
     * 获取便签数量（文件夹使用）
     * @return 便签数量
     */
    public int getNotesCount() {
        return mNotesCount;
    }

    /**
     * 获取文件夹ID（与getParentId相同）
     * @return 文件夹ID
     */
    public long getFolderId () {
        return mParentId;
    }

    /**
     * 获取便签类型
     * @return 便签类型
     */
    public int getType() {
        return mType;
    }

    /**
     * 获取小部件类型
     * @return 小部件类型
     */
    public int getWidgetType() {
        return mWidgetType;
    }

    /**
     * 获取小部件ID
     * @return 小部件ID
     */
    public int getWidgetId() {
        return mWidgetId;
    }

    /**
     * 获取便签摘要文本
     * @return 便签摘要文本
     */
    public String getSnippet() {
        return mSnippet;
    }

    /**
     * 是否设置了提醒
     * @return 是否设置了提醒
     */
    public boolean hasAlert() {
        return (mAlertDate > 0);
    }

    /**
     * 是否为通话记录便签
     * @return 是否为通话记录便签
     */
    public boolean isCallRecord() {
        return (mParentId == Notes.ID_CALL_RECORD_FOLDER && !TextUtils.isEmpty(mPhoneNumber));
    }

    /**
     * 从Cursor中获取便签类型
     * @param cursor 数据库查询结果Cursor
     * @return 便签类型
     */
    public static int getNoteType(Cursor cursor) {
        return cursor.getInt(TYPE_COLUMN);
    }
}
