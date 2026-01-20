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
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义的便签编辑框
 * 扩展自EditText，支持链接检测与处理（电话、网页、邮件）、
 * 键盘事件监听（删除键、回车键）以及与NoteEditActivity的交互。
 */
public class NoteEditText extends EditText {
    private static final String TAG = "NoteEditText";
    
    /** 当前编辑框在列表中的索引位置 */
    private int mIndex;
    
    /** 删除键按下前的光标位置 */
    private int mSelectionStartBeforeDelete;

    /** 电话链接协议 */
    private static final String SCHEME_TEL = "tel:" ;
    /** 网页链接协议 */
    private static final String SCHEME_HTTP = "http:" ;
    /** 邮件链接协议 */
    private static final String SCHEME_EMAIL = "mailto:" ;

    /** 链接协议与对应操作资源ID的映射表 */
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    
    /** 初始化链接协议与操作资源ID的映射关系 */
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
    }

    /**
     * 编辑框变化监听器接口
     * 由{@link NoteEditActivity}调用，用于处理编辑框的删除、添加和文本变化事件。
     */
    public interface OnTextViewChangeListener {
        /**
         * Delete current edit text when {@link KeyEvent#KEYCODE_DEL} happens
         * and the text is null
         */
        void onEditTextDelete(int index, String text);

        /**
         * Add edit text after current edit text when {@link KeyEvent#KEYCODE_ENTER}
         * happen
         */
        void onEditTextEnter(int index, String text);

        /**
         * Hide or show item option when text change
         */
        void onTextChange(int index, boolean hasText);
    }

    /** 编辑框变化监听器实例 */
    private OnTextViewChangeListener mOnTextViewChangeListener;

    /**
     * 构造函数
     * @param context 上下文对象
     */
    public NoteEditText(Context context) {
        super(context, null);
        mIndex = 0;
    }

    /**
     * 设置当前编辑框在列表中的索引位置
     * @param index 索引位置
     */
    public void setIndex(int index) {
        mIndex = index;
    }

    /**
     * 设置编辑框变化监听器
     * @param listener 监听器实例
     */
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }

    /**
     * 构造函数
     * @param context 上下文对象
     * @param attrs 属性集合
     */
    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
    }

    /**
     * 构造函数
     * @param context 上下文对象
     * @param attrs 属性集合
     * @param defStyle 默认样式
     */
    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 处理触摸事件
     * 重写父类方法，实现点击位置的精确光标定位功能。
     * @param event 触摸事件对象
     * @return 是否消费了该事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 计算点击位置相对于文本内容的坐标
                int x = (int) event.getX();
                int y = (int) event.getY();
                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();
                x += getScrollX();
                y += getScrollY();

                // 根据坐标获取对应的行和偏移量，并设置光标位置
                Layout layout = getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                Selection.setSelection(getText(), off);
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 处理按键按下事件
     * 重写父类方法，对回车键和删除键进行特殊处理。
     * @param keyCode 按键编码
     * @param event 按键事件对象
     * @return 是否消费了该事件
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                // 如果设置了监听器，则不处理回车键按下事件，由onKeyUp处理
                if (mOnTextViewChangeListener != null) {
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                // 记录删除键按下前的光标位置
                mSelectionStartBeforeDelete = getSelectionStart();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 处理按键释放事件
     * 重写父类方法，对删除键和回车键释放事件进行特殊处理，实现编辑框的删除和添加功能。
     * 
     * @param keyCode 按键编码
     * @param event 按键事件对象
     * @return 是否消费了该事件
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                // 处理删除键释放事件，如果光标在开头且不是第一个编辑框，则删除当前编辑框
                if (mOnTextViewChangeListener != null) {
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        return true;
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                // 处理回车键释放事件，在当前编辑框后添加新的编辑框
                if (mOnTextViewChangeListener != null) {
                    int selectionStart = getSelectionStart();
                    // 获取光标后的文本内容
                    String text = getText().subSequence(selectionStart, length()).toString();
                    // 截断当前编辑框的文本到光标位置
                    setText(getText().subSequence(0, selectionStart));
                    // 通知监听器添加新的编辑框
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 处理焦点变化事件
     * 重写父类方法，当焦点变化时通知监听器文本内容状态。
     * @param focused 是否获得焦点
     * @param direction 焦点移动方向
     * @param previouslyFocusedRect 上一个获得焦点的矩形区域
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            if (!focused && TextUtils.isEmpty(getText())) {
                // 失去焦点且文本为空，通知监听器
                mOnTextViewChangeListener.onTextChange(mIndex, false);
            } else {
                // 获得焦点或文本不为空，通知监听器
                mOnTextViewChangeListener.onTextChange(mIndex, true);
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * 创建上下文菜单
     * 重写父类方法，为链接文本添加上下文菜单项，支持电话、网页和邮件链接的快捷操作。
     * @param menu 上下文菜单对象
     */
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        // 检查文本是否包含链接
        if (getText() instanceof Spanned) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            // 获取选择区域的起始和结束位置
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            // 获取选择区域内的URLSpan
            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            if (urls.length == 1) {
                // 根据链接协议获取对应的操作资源ID
                int defaultResId = 0;
                for(String schema: sSchemaActionResMap.keySet()) {
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                // 如果没有匹配的协议，则使用默认操作
                if (defaultResId == 0) {
                    defaultResId = R.string.note_link_other;
                }

                // 添加上下文菜单项并设置点击事件
                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                // 执行链接点击操作
                                urls[0].onClick(NoteEditText.this);
                                return true;
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu);
    }
}
