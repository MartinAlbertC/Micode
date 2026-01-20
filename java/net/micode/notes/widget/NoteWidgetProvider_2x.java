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
 * NoteWidgetProvider_2x - 2x大小的便签小部件实现类
 * 继承自NoteWidgetProvider，实现2x大小的便签小部件功能。
 * 负责处理2x大小小部件的更新、布局和背景资源。
 */
public class NoteWidgetProvider_2x extends NoteWidgetProvider {
    /**
     * 小部件更新回调 - 更新2x大小的小部件
     * 重写父类的onUpdate方法，调用父类的update方法更新小部件。
     * @param context 上下文环境
     * @param appWidgetManager 小部件管理器
     * @param appWidgetIds 要更新的小部件ID数组
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.update(context, appWidgetManager, appWidgetIds);
    }

    /**
     * 获取布局ID - 返回2x小部件的布局
     * 实现父类的抽象方法，返回2x大小小部件的布局资源ID。
     * @return 2x小部件的布局资源ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.widget_2x;
    }

    /**
     * 获取背景资源ID - 返回2x小部件的背景
     * 实现父类的抽象方法，根据背景色ID返回2x大小小部件的背景资源ID。
     * @param bgId 背景色ID
     * @return 2x小部件的背景资源ID
     */
    @Override
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget2xBgResource(bgId);
    }

    /**
     * 获取小部件类型 - 返回2x小部件类型
     * 实现父类的抽象方法，返回2x大小小部件的类型标识。
     * @return 2x小部件的类型标识
     */
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_2X;
    }
}
