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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import net.micode.notes.data.Notes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 * NotesListAdapter - 便签列表适配器
 * 继承自CursorAdapter，负责为便签列表提供数据绑定和视图管理功能。
 * 支持多选模式、便签数量统计、选中项目管理以及应用小部件属性处理。
 * 
 * 主要功能：
 * - 创建和绑定NotesListItem视图
 * - 管理多选模式下的选中状态
 * - 统计普通便签的数量
 * - 获取选中项目的ID集合
 * - 处理应用小部件相关的属性
 */
public class NotesListAdapter extends CursorAdapter {
    private static final String TAG = "NotesListAdapter"; // 日志标签
    private Context mContext;                              // 上下文环境
    private HashMap<Integer, Boolean> mSelectedIndex;      // 记录选中项目的位置映射
    private int mNotesCount;                               // 普通便签的数量
    private boolean mChoiceMode;                           // 是否处于多选模式

    /**
     * AppWidgetAttribute - 应用小部件属性类
     * 用于存储应用小部件的ID和类型信息，便于在多选操作中管理相关小部件。
     */
    public static class AppWidgetAttribute {
        public int widgetId;   // 小部件ID
        public int widgetType; // 小部件类型
    };

    /**
     * 构造方法 - 创建便签列表适配器
     * 初始化适配器，设置上下文环境和选中项目的映射表。
     * @param context 上下文环境
     */
    public NotesListAdapter(Context context) {
        super(context, null);
        mSelectedIndex = new HashMap<Integer, Boolean>();
        mContext = context;
        mNotesCount = 0;
    }

    /**
     * 创建新的视图 - 生成NotesListItem实例
     * 当列表需要显示新的项目时调用此方法，创建一个新的NotesListItem视图。
     * @param context 上下文环境
     * @param cursor 当前位置的游标
     * @param parent 父视图组
     * @return 创建的新视图
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new NotesListItem(context);
    }

    /**
     * 绑定视图 - 将游标数据绑定到NotesListItem
     * 将当前游标的数据转换为NoteItemData对象，并绑定到NotesListItem视图中。
     * @param view 要绑定的视图
     * @param context 上下文环境
     * @param cursor 包含数据的游标
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof NotesListItem) {
            NoteItemData itemData = new NoteItemData(context, cursor);
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
        }
    }

    /**
     * 设置选中项目 - 更新指定位置的选中状态
     * 更新指定位置的选中状态，并通知数据集合变化。
     * @param position 项目位置
     * @param checked 选中状态
     */
    public void setCheckedItem(final int position, final boolean checked) {
        mSelectedIndex.put(position, checked);
        notifyDataSetChanged();
    }

    /**
     * 检查是否处于多选模式
     * 返回当前适配器是否处于多选模式。
     * @return true表示处于多选模式，false表示不处于多选模式
     */
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    /**
     * 设置多选模式 - 开启或关闭多选模式
     * 设置适配器的多选模式状态，并清空已选项目的映射表。
     * @param mode true表示开启多选模式，false表示关闭多选模式
     */
    public void setChoiceMode(boolean mode) {
        mSelectedIndex.clear();
        mChoiceMode = mode;
    }

    /**
     * 全选/取消全选 - 选择或取消选择所有普通便签
     * 遍历所有项目，选择或取消选择所有类型为普通便签的项目。
     * @param checked true表示全选，false表示取消全选
     */
    public void selectAll(boolean checked) {
        Cursor cursor = getCursor();
        for (int i = 0; i < getCount(); i++) {
            if (cursor.moveToPosition(i)) {
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    /**
     * 获取选中项目的ID集合
     * 返回所有选中项目的ID集合，排除根文件夹ID。
     * @return 选中项目的ID集合
     */
    public HashSet<Long> getSelectedItemIds() {
        HashSet<Long> itemSet = new HashSet<Long>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Long id = getItemId(position);
                if (id == Notes.ID_ROOT_FOLDER) {
                    Log.d(TAG, "Wrong item id, should not happen");
                } else {
                    itemSet.add(id);
                }
            }
        }

        return itemSet;
    }

    /**
     * 获取选中项目的应用小部件属性
     * 返回所有选中项目的应用小部件属性集合，包括小部件ID和类型。
     * @return 选中项目的应用小部件属性集合，无效时返回null
     */
    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Cursor c = (Cursor) getItem(position);
                if (c != null) {
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    NoteItemData item = new NoteItemData(mContext, c);
                    widget.widgetId = item.getWidgetId();
                    widget.widgetType = item.getWidgetType();
                    itemSet.add(widget);
                    /**
                     * Don't close cursor here, only the adapter could close it
                     */
                } else {
                    Log.e(TAG, "Invalid cursor");
                    return null;
                }
            }
        }
        return itemSet;
    }

    /**
     * 获取选中项目的数量
     * 统计当前选中项目的数量。
     * @return 选中项目的数量
     */
    public int getSelectedCount() {
        Collection<Boolean> values = mSelectedIndex.values();
        if (null == values) {
            return 0;
        }
        Iterator<Boolean> iter = values.iterator();
        int count = 0;
        while (iter.hasNext()) {
            if (true == iter.next()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查是否全选
     * 检查当前是否所有普通便签都被选中。
     * @return true表示所有普通便签都被选中，false表示不是
     */
    public boolean isAllSelected() {
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    /**
     * 检查指定位置的项目是否被选中
     * 检查指定位置的项目在选中项目映射表中的状态。
     * @param position 项目位置
     * @return true表示项目被选中，false表示未被选中
     */
    public boolean isSelectedItem(final int position) {
        if (null == mSelectedIndex.get(position)) {
            return false;
        }
        return mSelectedIndex.get(position);
    }

    /**
     * 内容变化回调 - 重新计算便签数量
     * 当适配器的内容发生变化时调用此方法，重新计算普通便签的数量。
     */
    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        calcNotesCount();
    }

    /**
     * 改变游标 - 更新数据集合
     * 更新适配器的游标，并重新计算普通便签的数量。
     * @param cursor 新的游标
     */
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        calcNotesCount();
    }

    /**
     * 计算便签数量 - 统计普通便签的数量
     * 遍历所有项目，统计类型为普通便签的项目数量。
     */
    private void calcNotesCount() {
        mNotesCount = 0;
        for (int i = 0; i < getCount(); i++) {
            Cursor c = (Cursor) getItem(i);
            if (c != null) {
                if (NoteItemData.getNoteType(c) == Notes.TYPE_NOTE) {
                    mNotesCount++;
                }
            } else {
                Log.e(TAG, "Invalid cursor");
                return;
            }
        }
    }
}
