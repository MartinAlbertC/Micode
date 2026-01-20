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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import net.micode.notes.R;

/**
 * 下拉菜单封装类，用于创建和管理带有下拉菜单的按钮
 * 该类封装了Android的PopupMenu功能，提供了一个简单的接口来创建带有下拉菜单的按钮，
 * 点击按钮时会显示下拉菜单。
 */
public class DropdownMenu {
    // 下拉菜单的按钮
    private Button mButton;
    // 弹出式菜单
    private PopupMenu mPopupMenu;
    // 菜单对象
    private Menu mMenu;

    /**
     * 构造方法，创建下拉菜单对象
     * @param context 上下文对象
     * @param button 下拉菜单的按钮
     * @param menuId 菜单资源ID
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button;
        // 设置按钮的背景为下拉图标
        mButton.setBackgroundResource(R.drawable.dropdown_icon);
        // 创建弹出式菜单
        mPopupMenu = new PopupMenu(context, mButton);
        // 获取菜单对象
        mMenu = mPopupMenu.getMenu();
        // 从资源文件中加载菜单
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
        // 设置按钮的点击监听器，点击时显示下拉菜单
        mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPopupMenu.show();
            }
        });
    }

    /**
     * 设置下拉菜单项的点击监听器
     * @param listener 菜单项点击监听器
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) {
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }

    /**
     * 根据ID查找菜单项
     * @param id 菜单项ID
     * @return 找到的菜单项，如果没有找到则返回null
     */
    public MenuItem findItem(int id) {
        return mMenu.findItem(id);
    }

    /**
     * 设置下拉菜单按钮的标题
     * @param title 按钮标题
     */
    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}
