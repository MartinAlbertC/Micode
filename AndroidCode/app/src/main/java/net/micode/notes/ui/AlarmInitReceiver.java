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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;


/**
 * 闹钟初始化接收器，用于在系统启动或应用安装时重新设置所有笔记的提醒闹钟
 * 该接收器负责在系统重启后或应用数据清除后，重新为所有设置了提醒时间且
 * 提醒时间尚未到达的笔记设置闹钟，确保提醒功能的完整性。
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    // 查询数据库时使用的投影列
    private static final String [] PROJECTION = new String [] {
        NoteColumns.ID,
        NoteColumns.ALERTED_DATE
    };

    // ID 列的索引
    private static final int COLUMN_ID                = 0;
    // 提醒日期列的索引
    private static final int COLUMN_ALERTED_DATE      = 1;

    /**
     * 接收广播时的处理方法
     * @param context 上下文对象
     * @param intent 接收到的广播意图
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        long currentDate = System.currentTimeMillis();
        // 查询所有设置了提醒时间且提醒时间尚未到达的笔记
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) },
                null);

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    // 为每个符合条件的笔记设置闹钟
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
                    AlarmManager alermManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);
                    alermManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext());
            }
            c.close();
        }
    }
}
