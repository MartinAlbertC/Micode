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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 闹钟广播接收器，用于处理系统闹钟广播并启动闹钟提醒活动
 * 当设置的笔记提醒时间到达时，系统会发送广播，该接收器会捕获此广播
 * 并启动AlarmAlertActivity来显示提醒对话框和播放闹钟声音。
 */
public class AlarmReceiver extends BroadcastReceiver {
    /**
     * 接收闹钟广播时的处理方法
     * @param context 上下文对象
     * @param intent 包含闹钟信息的广播意图
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 将广播意图的目标类设置为AlarmAlertActivity
        intent.setClass(context, AlarmAlertActivity.class);
        // 添加新任务标志，确保在非Activity上下文环境中也能启动活动
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 启动闹钟提醒活动
        context.startActivity(intent);
    }
}
