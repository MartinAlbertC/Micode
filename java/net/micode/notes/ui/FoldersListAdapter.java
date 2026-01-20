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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;


/**
 * 文件夹列表适配器，用于在列表中显示文件夹项
 * 该适配器继承自CursorAdapter，用于将数据库查询结果中的文件夹数据显示在列表中，
 * 支持自定义文件夹列表项的布局和数据绑定。
 */
public class FoldersListAdapter extends CursorAdapter {
    // 数据库查询的列投影
    public static final String [] PROJECTION = {
        NoteColumns.ID,
        NoteColumns.SNIPPET
    };

    // ID列的索引
    public static final int ID_COLUMN   = 0;
    // 文件夹名称列的索引
    public static final int NAME_COLUMN = 1;

    /**
     * 构造方法，创建文件夹列表适配器
     * @param context 上下文对象
     * @param c 包含文件夹数据的Cursor对象
     */
    public FoldersListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    /**
     * 创建新的列表项视图
     * @param context 上下文对象
     * @param cursor 包含文件夹数据的Cursor对象
     * @param parent 父视图组
     * @return 新创建的列表项视图
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new FolderListItem(context);
    }

    /**
     * 将文件夹数据绑定到列表项视图
     * @param view 列表项视图
     * @param context 上下文对象
     * @param cursor 包含文件夹数据的Cursor对象
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof FolderListItem) {
            // 根文件夹显示特殊名称，其他文件夹显示实际名称
            String folderName = (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                    .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
            ((FolderListItem) view).bind(folderName);
        }
    }

    /**
     * 根据位置获取文件夹名称
     * @param context 上下文对象
     * @param position 文件夹在列表中的位置
     * @return 文件夹名称
     */
    public String getFolderName(Context context, int position) {
        Cursor cursor = (Cursor) getItem(position);
        // 根文件夹显示特殊名称，其他文件夹显示实际名称
        return (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
    }

    /**
     * 文件夹列表项视图类，用于显示单个文件夹项
     */
    private class FolderListItem extends LinearLayout {
        // 显示文件夹名称的TextView
        private TextView mName;

        /**
         * 构造方法，创建文件夹列表项视图
         * @param context 上下文对象
         */
        public FolderListItem(Context context) {
            super(context);
            // 加载文件夹列表项布局
            inflate(context, R.layout.folder_list_item, this);
            // 获取文件夹名称TextView
            mName = (TextView) findViewById(R.id.tv_folder_name);
        }

        /**
         * 将文件夹名称绑定到列表项视图
         * @param name 文件夹名称
         */
        public void bind(String name) {
            mName.setText(name);
        }
    }

}
