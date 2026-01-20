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

import java.util.Calendar;

import net.micode.notes.R;
import net.micode.notes.ui.DateTimePicker;
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

/**
 * 日期时间选择对话框，用于在对话框中选择日期和时间
 * 该对话框封装了DateTimePicker控件，提供了一个友好的界面让用户选择日期和时间，
 * 并支持12小时制和24小时制显示。
 */
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {

    // 当前选择的日期时间
    private Calendar mDate = Calendar.getInstance();
    // 是否使用24小时制显示
    private boolean mIs24HourView;
    // 日期时间设置监听器
    private OnDateTimeSetListener mOnDateTimeSetListener;
    // 日期时间选择器控件
    private DateTimePicker mDateTimePicker;

    /**
     * 日期时间设置监听器接口，用于监听用户确定选择的日期时间
     */
    public interface OnDateTimeSetListener {
        /**
         * 当用户点击确定按钮时调用
         * @param dialog 日期时间选择对话框
         * @param date 选择的日期时间（毫秒）
         */
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    /**
     * 构造方法，使用指定日期初始化日期时间选择对话框
     * @param context 上下文对象
     * @param date 初始日期时间（毫秒）
     */
    public DateTimePickerDialog(Context context, long date) {
        super(context);
        // 创建日期时间选择器控件
        mDateTimePicker = new DateTimePicker(context);
        // 设置对话框的内容视图为日期时间选择器
        setView(mDateTimePicker);
        // 设置日期时间变化监听器
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                    int dayOfMonth, int hourOfDay, int minute) {
                // 更新当前选择的日期时间
                mDate.set(Calendar.YEAR, year);
                mDate.set(Calendar.MONTH, month);
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mDate.set(Calendar.MINUTE, minute);
                // 更新对话框标题为当前选择的日期时间
                updateTitle(mDate.getTimeInMillis());
            }
        });
        // 设置初始日期时间，忽略秒数
        mDate.setTimeInMillis(date);
        mDate.set(Calendar.SECOND, 0);
        // 设置日期时间选择器的当前日期时间
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis());
        // 设置确定按钮
        setButton(context.getString(R.string.datetime_dialog_ok), this);
        // 设置取消按钮
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener)null);
        // 设置时间格式为系统默认格式
        set24HourView(DateFormat.is24HourFormat(this.getContext()));
        // 更新对话框标题
        updateTitle(mDate.getTimeInMillis());
    }

    /**
     * 设置是否使用24小时制显示时间
     * @param is24HourView 是否使用24小时制
     */
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
    }

    /**
     * 设置日期时间设置监听器
     * @param callBack 日期时间设置监听器
     */
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    /**
     * 更新对话框标题为指定的日期时间
     * @param date 日期时间（毫秒）
     */
    private void updateTitle(long date) {
        // 设置日期时间格式为显示年、月、日、时、分
        int flag = 
            DateUtils.FORMAT_SHOW_YEAR |
            DateUtils.FORMAT_SHOW_DATE |
            DateUtils.FORMAT_SHOW_TIME;
        // 设置时间格式为24小时制或12小时制
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_24HOUR;
        // 格式化日期时间并设置为对话框标题
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag));
    }

    /**
     * 处理确定按钮的点击事件
     * @param arg0 对话框接口
     * @param arg1 按钮索引
     */
    public void onClick(DialogInterface arg0, int arg1) {
        // 如果设置了监听器，则调用监听器的方法
        if (mOnDateTimeSetListener != null) {
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }

}