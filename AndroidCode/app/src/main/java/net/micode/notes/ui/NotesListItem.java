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
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;


/**
 * NotesListItem - 便签列表项自定义视图组件
 * 继承自LinearLayout，用于在便签列表中显示单个便签或文件夹项。
 * 负责根据不同类型的便签数据（普通便签、文件夹、通话记录便签）展示不同的UI样式和内容。
 * 支持多选模式下的复选框显示和状态管理。
 * 
 * 主要功能：
 * - 根据便签数据类型（普通便签、文件夹、通话记录）显示不同的UI样式
 * - 支持多选模式下的复选框显示和状态控制
 * - 显示便签的标题、时间、提醒图标等信息
 * - 根据便签背景色ID设置适当的背景样式
 * - 管理通话记录便签的特殊显示需求
 */
public class NotesListItem extends LinearLayout {
    private ImageView mAlert;          // 提醒图标，显示便签的提醒状态
    private TextView mTitle;           // 便签标题，显示便签的核心内容
    private TextView mTime;            // 时间文本，显示便签的修改时间
    private TextView mCallName;        // 通话记录名称，仅用于通话记录便签
    private NoteItemData mItemData;    // 当前列表项绑定的便签数据
    private CheckBox mCheckBox;        // 复选框，用于多选模式

    /**
     * 构造方法 - 创建便签列表项视图
     * 初始化视图组件，从布局文件中加载UI元素。
     * @param context 上下文环境，用于加载布局和资源
     */
    public NotesListItem(Context context) {
        super(context);
        inflate(context, R.layout.note_item, this);
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mTime = (TextView) findViewById(R.id.tv_time);
        mCallName = (TextView) findViewById(R.id.tv_name);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    /**
     * 绑定便签数据到视图组件
     * 根据便签数据的类型和属性，配置视图组件的显示内容和样式。
     * 支持处理普通便签、文件夹和通话记录便签三种类型的数据。
     * @param context 上下文环境，用于获取字符串资源和样式
     * @param data 要绑定的便签数据对象
     * @param choiceMode 是否处于多选模式
     * @param checked 多选模式下的选中状态
     */
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        // 配置多选模式下的复选框显示
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setChecked(checked);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }

        mItemData = data;
        
        // 处理通话记录文件夹的特殊显示
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCallName.setVisibility(View.GONE);
            mAlert.setVisibility(View.VISIBLE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            mAlert.setImageResource(R.drawable.call_record);
        }
        // 处理通话记录便签的特殊显示
        else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCallName.setVisibility(View.VISIBLE);
            mCallName.setText(data.getCallName());
            mTitle.setTextAppearance(context,R.style.TextAppearanceSecondaryItem);
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
            if (data.hasAlert()) {
                mAlert.setImageResource(R.drawable.clock);
                mAlert.setVisibility(View.VISIBLE);
            } else {
                mAlert.setVisibility(View.GONE);
            }
        }
        // 处理普通便签和文件夹的显示
        else {
            mCallName.setVisibility(View.GONE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);

            if (data.getType() == Notes.TYPE_FOLDER) {
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));
                mAlert.setVisibility(View.GONE);
            } else {
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
                if (data.hasAlert()) {
                    mAlert.setImageResource(R.drawable.clock);
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    mAlert.setVisibility(View.GONE);
                }
            }
        }
        
        // 设置便签的修改时间（相对时间格式）
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));
        
        // 设置背景样式
        setBackground(data);
    }

    /**
     * 设置列表项的背景样式
     * 根据便签数据的类型、背景色ID和位置（首项、末项、单项等）选择合适的背景资源。
     * 普通便签和文件夹使用不同的背景资源，普通便签还会根据位置和数量选择不同的边角样式。
     * @param data 便签数据对象，包含背景色ID、类型和位置信息
     */
    private void setBackground(NoteItemData data) {
        int id = data.getBgColorId();
        
        // 为普通便签设置背景
        if (data.getType() == Notes.TYPE_NOTE) {
            if (data.isSingle() || data.isOneFollowingFolder()) {
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            } else if (data.isLast()) {
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        }
        // 为文件夹设置背景
        else {
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }

    /**
     * 获取当前列表项绑定的便签数据
     * 返回当前视图组件所绑定的NoteItemData对象，用于获取便签的详细信息。
     * @return 当前列表项绑定的便签数据对象
     */
    public NoteItemData getItemData() {
        return mItemData;
    }
}
