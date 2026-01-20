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

package net.micode.notes.widget;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NoteEditActivity;
import net.micode.notes.ui.NotesListActivity;

/**
 * NoteWidgetProvider - 便签小部件抽象基类
 * 继承自AppWidgetProvider，为所有便签小部件提供通用功能。
 * 负责管理小部件的更新、删除等操作，以及获取和显示便签数据。
 * 子类需要实现特定的抽象方法来提供小部件的布局和背景资源。
 * 
 * 主要功能：
 * - 管理小部件的生命周期事件（删除、更新）
 * - 从数据库获取便签数据并显示在小部件上
 * - 处理小部件点击事件，启动相应的活动
 * - 支持隐私模式下的小部件显示
 * - 提供抽象方法供子类实现特定的小部件样式
 */
public abstract class NoteWidgetProvider extends AppWidgetProvider {
    /** 数据库查询的投影列，用于获取便签的ID、背景色ID和内容摘要 */
    public static final String [] PROJECTION = new String [] {
        NoteColumns.ID,
        NoteColumns.BG_COLOR_ID,
        NoteColumns.SNIPPET
    };

    /** 投影列索引 - 便签ID */
    public static final int COLUMN_ID           = 0;
    /** 投影列索引 - 背景色ID */
    public static final int COLUMN_BG_COLOR_ID  = 1;
    /** 投影列索引 - 内容摘要 */
    public static final int COLUMN_SNIPPET      = 2;

    private static final String TAG = "NoteWidgetProvider"; // 日志标签

    /**
     * 小部件删除回调 - 处理小部件删除事件
     * 当用户删除小部件时，更新数据库中相关便签的小部件ID为无效值。
     * @param context 上下文环境
     * @param appWidgetIds 被删除的小部件ID数组
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        for (int i = 0; i < appWidgetIds.length; i++) {
            context.getContentResolver().update(Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.WIDGET_ID + "=?",
                    new String[] { String.valueOf(appWidgetIds[i])});
        }
    }

    /**
     * 获取小部件关联的便签信息
     * 根据小部件ID从数据库中查询关联的便签数据，排除已删除的便签。
     * @param context 上下文环境
     * @param widgetId 小部件ID
     * @return 包含便签信息的游标
     */
    private Cursor getNoteWidgetInfo(Context context, int widgetId) {
        return context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(widgetId), String.valueOf(Notes.ID_TRASH_FOLER) },
                null);
    }

    /**
     * 更新小部件 - 默认隐私模式
     * 更新指定的小部件，不使用隐私模式。
     * @param context 上下文环境
     * @param appWidgetManager 小部件管理器
     * @param appWidgetIds 要更新的小部件ID数组
     */
    protected void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, false);
    }

    /**
     * 更新小部件 - 核心实现
     * 更新指定的小部件，根据隐私模式决定显示内容和点击行为。
     * @param context 上下文环境
     * @param appWidgetManager 小部件管理器
     * @param appWidgetIds 要更新的小部件ID数组
     * @param privacyMode 是否处于隐私模式
     */
    private void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
            boolean privacyMode) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            if (appWidgetIds[i] != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int bgId = ResourceParser.getDefaultBgId(context);
                String snippet = "";
                Intent intent = new Intent(context, NoteEditActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_ID, appWidgetIds[i]);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_TYPE, getWidgetType());

                Cursor c = getNoteWidgetInfo(context, appWidgetIds[i]);
                if (c != null && c.moveToFirst()) {
                    if (c.getCount() > 1) {
                        Log.e(TAG, "Multiple message with same widget id:" + appWidgetIds[i]);
                        c.close();
                        return;
                    }
                    snippet = c.getString(COLUMN_SNIPPET);
                    bgId = c.getInt(COLUMN_BG_COLOR_ID);
                    intent.putExtra(Intent.EXTRA_UID, c.getLong(COLUMN_ID));
                    intent.setAction(Intent.ACTION_VIEW);
                } else {
                    snippet = context.getResources().getString(R.string.widget_havenot_content);
                    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                }

                if (c != null) {
                    c.close();
                }

                RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
                rv.setImageViewResource(R.id.widget_bg_image, getBgResourceId(bgId));
                intent.putExtra(Notes.INTENT_EXTRA_BACKGROUND_ID, bgId);
                /**
                 * Generate the pending intent to start host for the widget
                 */
                PendingIntent pendingIntent = null;
                if (privacyMode) {
                    rv.setTextViewText(R.id.widget_text,
                            context.getString(R.string.widget_under_visit_mode));
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], new Intent(
                            context, NotesListActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                } else {
                    rv.setTextViewText(R.id.widget_text, snippet);
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }

                rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        }
    }

    /**
     * 获取背景资源ID - 抽象方法
     * 子类实现此方法，根据背景色ID返回对应的背景资源ID。
     * @param bgId 背景色ID
     * @return 背景资源ID
     */
    protected abstract int getBgResourceId(int bgId);

    /**
     * 获取布局ID - 抽象方法
     * 子类实现此方法，返回小部件的布局资源ID。
     * @return 布局资源ID
     */
    protected abstract int getLayoutId();

    /**
     * 获取小部件类型 - 抽象方法
     * 子类实现此方法，返回小部件的类型标识。
     * @return 小部件类型
     */
    protected abstract int getWidgetType();
}
