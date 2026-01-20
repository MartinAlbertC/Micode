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

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;


/**
 * 4x大小的便签小部件实现类
 * 继承自NoteWidgetProvider，负责4x尺寸小部件的布局、背景和类型设置
 */
public class NoteWidgetProvider_4x extends NoteWidgetProvider {
    /**
     * 更新小部件时调用，实际调用父类的update方法执行更新逻辑
     * @param context 上下文
     * @param appWidgetManager 小部件管理器
     * @param appWidgetIds 要更新的小部件ID数组
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.update(context, appWidgetManager, appWidgetIds);
    }

    /**
     * 获取4x小部件的布局资源ID
     * @return 4x小部件布局资源ID
     */
    protected int getLayoutId() {
        return R.layout.widget_4x;
    }

    /**
     * 根据背景ID获取4x小部件的背景资源ID
     * @param bgId 背景ID
     * @return 4x小部件背景资源ID
     */
    @Override
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget4xBgResource(bgId);
    }

    /**
     * 获取4x小部件的类型标识
     * @return 4x小部件类型标识Notes.TYPE_WIDGET_4X
     */
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_4X;
    }
}
