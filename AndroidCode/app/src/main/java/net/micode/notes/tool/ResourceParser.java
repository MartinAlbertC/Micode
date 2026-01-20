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

package net.micode.notes.tool;

import android.content.Context;
import android.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.ui.NotesPreferenceActivity;

/**
 * 资源解析类，管理笔记应用的各种资源
 * 包括背景颜色、字体大小、背景图片等资源的定义和获取方法
 */
public class ResourceParser {

    // 背景颜色常量 - 黄色
    public static final int YELLOW           = 0;
    // 背景颜色常量 - 蓝色
    public static final int BLUE             = 1;
    // 背景颜色常量 - 白色
    public static final int WHITE            = 2;
    // 背景颜色常量 - 绿色
    public static final int GREEN            = 3;
    // 背景颜色常量 - 红色
    public static final int RED              = 4;

    // 默认背景颜色
    public static final int BG_DEFAULT_COLOR = YELLOW;

    // 字体大小常量 - 小
    public static final int TEXT_SMALL       = 0;
    // 字体大小常量 - 中
    public static final int TEXT_MEDIUM      = 1;
    // 字体大小常量 - 大
    public static final int TEXT_LARGE       = 2;
    // 字体大小常量 - 超大
    public static final int TEXT_SUPER       = 3;

    // 默认字体大小
    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM;

    /**
     * 笔记背景资源类，管理笔记编辑界面的背景资源
     */
    public static class NoteBgResources {
        // 编辑界面背景资源数组
        private final static int [] BG_EDIT_RESOURCES = new int [] {
            R.drawable.edit_yellow,
            R.drawable.edit_blue,
            R.drawable.edit_white,
            R.drawable.edit_green,
            R.drawable.edit_red
        };

        // 编辑界面标题栏背景资源数组
        private final static int [] BG_EDIT_TITLE_RESOURCES = new int [] {
            R.drawable.edit_title_yellow,
            R.drawable.edit_title_blue,
            R.drawable.edit_title_white,
            R.drawable.edit_title_green,
            R.drawable.edit_title_red
        };

        /**
         * 获取笔记编辑界面的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getNoteBgResource(int id) {
            return BG_EDIT_RESOURCES[id];
        }

        /**
         * 获取笔记编辑界面标题栏的背景资源
         * @param id 背景颜色ID
         * @return 标题栏背景资源ID
         */
        public static int getNoteTitleBgResource(int id) {
            return BG_EDIT_TITLE_RESOURCES[id];
        }
    }

    /**
     * 获取默认背景颜色ID
     * @param context 上下文对象
     * @return 默认背景颜色ID
     */
    public static int getDefaultBgId(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            return (int) (Math.random() * NoteBgResources.BG_EDIT_RESOURCES.length);
        } else {
            return BG_DEFAULT_COLOR;
        }
    }

    /**
     * 笔记列表项背景资源类，管理笔记列表中各项的背景资源
     */
    public static class NoteItemBgResources {
        // 列表项第一个元素的背景资源数组
        private final static int [] BG_FIRST_RESOURCES = new int [] {
            R.drawable.list_yellow_up,
            R.drawable.list_blue_up,
            R.drawable.list_white_up,
            R.drawable.list_green_up,
            R.drawable.list_red_up
        };

        // 列表项中间元素的背景资源数组
        private final static int [] BG_NORMAL_RESOURCES = new int [] {
            R.drawable.list_yellow_middle,
            R.drawable.list_blue_middle,
            R.drawable.list_white_middle,
            R.drawable.list_green_middle,
            R.drawable.list_red_middle
        };

        // 列表项最后一个元素的背景资源数组
        private final static int [] BG_LAST_RESOURCES = new int [] {
            R.drawable.list_yellow_down,
            R.drawable.list_blue_down,
            R.drawable.list_white_down,
            R.drawable.list_green_down,
            R.drawable.list_red_down,
        };

        // 列表项单独元素的背景资源数组
        private final static int [] BG_SINGLE_RESOURCES = new int [] {
            R.drawable.list_yellow_single,
            R.drawable.list_blue_single,
            R.drawable.list_white_single,
            R.drawable.list_green_single,
            R.drawable.list_red_single
        };

        /**
         * 获取列表项第一个元素的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getNoteBgFirstRes(int id) {
            return BG_FIRST_RESOURCES[id];
        }

        /**
         * 获取列表项最后一个元素的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getNoteBgLastRes(int id) {
            return BG_LAST_RESOURCES[id];
        }

        /**
         * 获取列表项单独元素的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getNoteBgSingleRes(int id) {
            return BG_SINGLE_RESOURCES[id];
        }

        /**
         * 获取列表项中间元素的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getNoteBgNormalRes(int id) {
            return BG_NORMAL_RESOURCES[id];
        }

        /**
         * 获取文件夹的背景资源
         * @return 文件夹背景资源ID
         */
        public static int getFolderBgRes() {
            return R.drawable.list_folder;
        }
    }

    /**
     * 小部件背景资源类，管理笔记小部件的背景资源
     */
    public static class WidgetBgResources {
        // 2x尺寸小部件的背景资源数组
        private final static int [] BG_2X_RESOURCES = new int [] {
            R.drawable.widget_2x_yellow,
            R.drawable.widget_2x_blue,
            R.drawable.widget_2x_white,
            R.drawable.widget_2x_green,
            R.drawable.widget_2x_red,
        };

        /**
         * 获取2x尺寸小部件的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id];
        }

        // 4x尺寸小部件的背景资源数组
        private final static int [] BG_4X_RESOURCES = new int [] {
            R.drawable.widget_4x_yellow,
            R.drawable.widget_4x_blue,
            R.drawable.widget_4x_white,
            R.drawable.widget_4x_green,
            R.drawable.widget_4x_red
        };

        /**
         * 获取4x尺寸小部件的背景资源
         * @param id 背景颜色ID
         * @return 背景资源ID
         */
        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id];
        }
    }

    /**
     * 文本外观资源类，管理笔记文本的各种外观样式
     */
    public static class TextAppearanceResources {
        // 文本外观资源数组
        private final static int [] TEXTAPPEARANCE_RESOURCES = new int [] {
            R.style.TextAppearanceNormal,
            R.style.TextAppearanceMedium,
            R.style.TextAppearanceLarge,
            R.style.TextAppearanceSuper
        };

        /**
         * 获取文本外观资源
         * @param id 字体大小ID
         * @return 文本外观资源ID
         */
        public static int getTexAppearanceResource(int id) {
            /**
             * HACKME: Fix bug of store the resource id in shared preference.
             * The id may larger than the length of resources, in this case,
             * return the {@link ResourceParser#BG_DEFAULT_FONT_SIZE}
             */
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {
                return BG_DEFAULT_FONT_SIZE;
            }
            return TEXTAPPEARANCE_RESOURCES[id];
        }

        /**
         * 获取文本外观资源数组的长度
         * @return 文本外观资源数组的长度
         */
        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;
        }
    }
}
