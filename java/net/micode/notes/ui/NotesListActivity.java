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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.tool.BackupUtils;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * NotesListActivity - 便签应用的主界面活动类
 * 作为应用的核心入口点，负责展示便签和文件夹的列表视图，提供完整的便签管理功能。
 * 实现了MVC架构中的Controller角色，协调数据层和视图层的交互。
 * 
 * 主要功能：
 * - 便签列表的显示与更新（支持不同列表状态：根文件夹、子文件夹、通话记录文件夹）
 * - 文件夹的创建、查看、重命名和删除管理
 * - 便签的新建、编辑、删除和批量操作
 * - 搜索功能
 * - 与Google Tasks的同步功能
 * - 便签内容的导出功能
 * - 应用小部件（Widget）的更新支持
 * 
 * 设计特点：
 * - 使用AsyncQueryHandler处理异步数据查询，避免UI阻塞
 * - 实现多选操作模式（ActionMode）支持批量处理
 * - 基于状态模式（ListEditState）管理不同的列表显示状态
 * - 通过ContentResolver与NotesProvider交互，实现数据访问抽象
 * - 支持触摸手势识别和自定义视图交互
 */
public class NotesListActivity extends Activity implements OnClickListener, OnItemLongClickListener {
    // 异步查询令牌常量
    private static final int FOLDER_NOTE_LIST_QUERY_TOKEN = 0; // 查询文件夹内便签列表的令牌
    private static final int FOLDER_LIST_QUERY_TOKEN      = 1; // 查询文件夹列表的令牌

    // 文件夹上下文菜单ID常量
    private static final int MENU_FOLDER_DELETE = 0;         // 删除文件夹菜单ID
    private static final int MENU_FOLDER_VIEW = 1;           // 查看文件夹内容菜单ID
    private static final int MENU_FOLDER_CHANGE_NAME = 2;    // 重命名文件夹菜单ID

    // SharedPreferences键名常量
    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction"; // 首次使用引导标记

    /**
     * ListEditState - 列表编辑状态枚举
     * 定义当前列表视图的显示状态，用于控制不同类型内容的展示和相应的功能可用状态。
     * 实现了简单的状态模式，根据不同状态切换列表行为和UI元素。
     */
    private enum ListEditState {
        NOTE_LIST,        // 根文件夹状态：显示所有顶级文件夹和便签
        SUB_FOLDER,       // 子文件夹状态：显示特定文件夹下的便签
        CALL_RECORD_FOLDER // 通话记录文件夹状态：显示与通话记录相关的便签
    };

    // 实例变量
    private ListEditState mState;                       // 当前列表编辑状态
    private BackgroundQueryHandler mBackgroundQueryHandler; // 后台查询处理器
    private NotesListAdapter mNotesListAdapter;         // 便签列表适配器
    private ListView mNotesListView;                    // 便签列表视图
    private Button mAddNewNote;                         // 新建便签按钮
    private boolean mDispatch;                          // 触摸事件分发标记
    private int mOriginY;                               // 触摸事件原始Y坐标
    private int mDispatchY;                             // 触摸事件分发Y坐标
    private TextView mTitleBar;                         // 标题栏视图
    private long mCurrentFolderId;                      // 当前文件夹ID
    private ContentResolver mContentResolver;           // 内容解析器，用于数据访问
    private ModeCallback mModeCallBack;                 // 多选模式回调
    private static final String TAG = "NotesListActivity"; // 日志标记
    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30; // 列表滚动速率
    private NoteItemData mFocusNoteDataItem;            // 当前聚焦的便签数据项
    private static final String NORMAL_SELECTION = NoteColumns.PARENT_ID + "=?"; // 普通文件夹查询条件
    private static final String ROOT_FOLDER_SELECTION = "(" + NoteColumns.TYPE + "<>"
            + Notes.TYPE_SYSTEM + " AND " + NoteColumns.PARENT_ID + "=?)" + " OR ("
            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER + " AND "
            + NoteColumns.NOTES_COUNT + ">0)"; // 根文件夹查询条件

    private final static int REQUEST_CODE_OPEN_NODE = 102; // 打开便签的请求码
    private final static int REQUEST_CODE_NEW_NODE  = 103; // 新建便签的请求码

    private boolean mInSearchMode = false;
    private String mSearchQuery;

    /**
     * Activity生命周期方法：创建活动时调用
     * 初始化活动布局、资源和数据，设置首次使用引导信息。
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_list); // 设置布局文件
        initResources(); // 初始化资源

        /**
         * 首次使用应用时插入引导信息
         */
        setAppInfoFromRawRes();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                mSearchQuery = query;
                mInSearchMode = true;
                String selection = NoteColumns.SNIPPET + " LIKE ? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER;
                String[] selectionArgs = { "%" + query + "%" };
                mBackgroundQueryHandler.startQuery(FOLDER_NOTE_LIST_QUERY_TOKEN, null,
                        Notes.CONTENT_NOTE_URI, NoteItemData.PROJECTION, selection, selectionArgs,
                        NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
                if (mTitleBar != null) {
                    mTitleBar.setText(query);
                    mTitleBar.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Activity生命周期方法：处理其他活动返回的结果
     * 当从便签编辑活动返回时，刷新便签列表数据。
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 返回的意图数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && (requestCode == REQUEST_CODE_OPEN_NODE || requestCode == REQUEST_CODE_NEW_NODE)) {
            // 当便签编辑完成后，重置列表适配器的游标以刷新数据
            mNotesListAdapter.changeCursor(null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 设置首次使用应用时的引导信息
     * 从raw资源中读取引导文本，创建一个新的便签并保存到数据库中。
     * 通过SharedPreferences标记引导信息已添加，避免重复添加。
     */
    private void setAppInfoFromRawRes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sp.getBoolean(PREFERENCE_ADD_INTRODUCTION, false)) {
            StringBuilder sb = new StringBuilder();
            InputStream in = null;
            try {
                 in = getResources().openRawResource(R.raw.introduction);
                if (in != null) {
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    char [] buf = new char[1024];
                    int len = 0;
                    while ((len = br.read(buf)) > 0) {
                        sb.append(buf, 0, len);
                    }
                } else {
                    Log.e(TAG, "Read introduction file error");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            WorkingNote note = WorkingNote.createEmptyNote(this, Notes.ID_ROOT_FOLDER,
                    AppWidgetManager.INVALID_APPWIDGET_ID, Notes.TYPE_WIDGET_INVALIDE,
                    ResourceParser.RED);
            note.setWorkingText(sb.toString());
            if (note.saveNote()) {
                sp.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).commit();
            } else {
                Log.e(TAG, "Save introduction note error");
                return;
            }
        }
    }

    /**
     * Activity生命周期方法：活动可见时调用
     * 启动异步查询，加载便签列表数据。
     */
    @Override
    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery();
    }

    /**
     * 初始化活动资源和视图组件
     * 设置所有UI组件、适配器、监听器和初始状态。
     */
    private void initResources() {
        mContentResolver = this.getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(this.getContentResolver());
        mCurrentFolderId = Notes.ID_ROOT_FOLDER;
        mNotesListView = (ListView) findViewById(R.id.notes_list);
        mNotesListView.addFooterView(LayoutInflater.from(this).inflate(R.layout.note_list_footer, null),
                null, false);
        mNotesListView.setOnItemClickListener(new OnListItemClickListener());
        mNotesListView.setOnItemLongClickListener(this);
        mNotesListAdapter = new NotesListAdapter(this);
        mNotesListView.setAdapter(mNotesListAdapter);
        mAddNewNote = (Button) findViewById(R.id.btn_new_note);
        mAddNewNote.setOnClickListener(this);
        mAddNewNote.setOnTouchListener(new NewNoteOnTouchListener());
        mDispatch = false;
        mDispatchY = 0;
        mOriginY = 0;
        mTitleBar = (TextView) findViewById(R.id.tv_title_bar);
        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
        mInSearchMode = false;
        mSearchQuery = null;
    }

    /**
     * ModeCallback - 多选操作模式回调类
     * 实现ListView.MultiChoiceModeListener和OnMenuItemClickListener接口，
     * 负责处理便签列表的多选操作模式，包括选择状态管理、菜单创建和操作执行。
     */
    private class ModeCallback implements ListView.MultiChoiceModeListener, OnMenuItemClickListener {
        private DropdownMenu mDropDownMenu; // 下拉菜单组件，用于选择全部/取消选择
        private ActionMode mActionMode;     // 当前的操作模式实例
        private MenuItem mMoveMenu;         // 移动菜单项，根据条件显示或隐藏

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            menu.findItem(R.id.delete).setOnMenuItemClickListener(this);
            mMoveMenu = menu.findItem(R.id.move);
            if (mFocusNoteDataItem.getParentId() == Notes.ID_CALL_RECORD_FOLDER
                    || DataUtils.getUserFolderCount(mContentResolver) == 0) {
                mMoveMenu.setVisible(false);
            } else {
                mMoveMenu.setVisible(true);
                mMoveMenu.setOnMenuItemClickListener(this);
            }
            mActionMode = mode;
            mNotesListAdapter.setChoiceMode(true);
            mNotesListView.setLongClickable(false);
            mAddNewNote.setVisibility(View.GONE);

            View customView = LayoutInflater.from(NotesListActivity.this).inflate(
                    R.layout.note_list_dropdown_menu, null);
            mode.setCustomView(customView);
            mDropDownMenu = new DropdownMenu(NotesListActivity.this,
                    (Button) customView.findViewById(R.id.selection_menu),
                    R.menu.note_list_dropdown);
            mDropDownMenu.setOnDropdownMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                public boolean onMenuItemClick(MenuItem item) {
                    mNotesListAdapter.selectAll(!mNotesListAdapter.isAllSelected());
                    updateMenu();
                    return true;
                }

            });
            return true;
        }

        private void updateMenu() {
            int selectedCount = mNotesListAdapter.getSelectedCount();
            // Update dropdown menu
            String format = getResources().getString(R.string.menu_select_title, selectedCount);
            mDropDownMenu.setTitle(format);
            MenuItem item = mDropDownMenu.findItem(R.id.action_select_all);
            if (item != null) {
                if (mNotesListAdapter.isAllSelected()) {
                    item.setChecked(true);
                    item.setTitle(R.string.menu_deselect_all);
                } else {
                    item.setChecked(false);
                    item.setTitle(R.string.menu_select_all);
                }
            }
        }

        /**
         * 准备操作模式菜单
         * 当操作模式菜单需要更新时调用，目前未实现任何自定义准备逻辑。
         * @param mode 当前操作模式
         * @param menu 要准备的菜单
         * @return false表示不更新菜单，true表示更新菜单
         */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * 操作模式菜单项点击事件处理
         * 处理操作模式菜单的选项点击事件，目前未实现任何自定义逻辑。
         * 实际操作处理通过onMenuItemClickListener接口实现。
         * @param mode 当前操作模式
         * @param item 选择的菜单项
         * @return false表示未处理事件，true表示已处理事件
         */
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * 销毁操作模式
         * 当操作模式结束时调用，恢复列表的正常显示状态：
         * - 关闭选择模式
         * - 恢复长按功能
         * - 显示新建便签按钮
         * @param mode 要销毁的操作模式
         */
        public void onDestroyActionMode(ActionMode mode) {
            mNotesListAdapter.setChoiceMode(false);
            mNotesListView.setLongClickable(true);
            mAddNewNote.setVisibility(View.VISIBLE);
        }

        /**
         * 结束当前操作模式
         * 手动关闭当前的操作模式，触发onDestroyActionMode回调。
         */
        public void finishActionMode() {
            mActionMode.finish();
        }

        /**
         * 列表项选择状态变化事件处理
         * 当列表项的选择状态发生变化时调用，更新适配器中的选择状态
         * 并刷新菜单显示（更新选中项数量和选择状态）。
         * @param mode 当前操作模式
         * @param position 列表项位置
         * @param id 列表项ID
         * @param checked 新的选择状态
         */
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            mNotesListAdapter.setCheckedItem(position, checked);
            updateMenu();
        }

        public boolean onMenuItemClick(MenuItem item) {
            if (mNotesListAdapter.getSelectedCount() == 0) {
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            switch (item.getItemId()) {
                case R.id.delete:
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(getString(R.string.alert_title_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(getString(R.string.alert_message_delete_notes,
                                             mNotesListAdapter.getSelectedCount()));
                    builder.setPositiveButton(android.R.string.ok,
                                             new DialogInterface.OnClickListener() {
                                                 public void onClick(DialogInterface dialog,
                                                         int which) {
                                                     batchDelete();
                                                 }
                                             });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();
                    break;
                case R.id.move:
                    startQueryDestinationFolders();
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    /**
     * NewNoteOnTouchListener - 新建便签按钮触摸事件监听器
     * 自定义触摸事件处理，实现特殊的UI交互效果。
     * 主要功能是将新建便签按钮透明区域的触摸事件分发给下方的列表视图。
     */
    private class NewNoteOnTouchListener implements OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    Display display = getWindowManager().getDefaultDisplay();
                    int screenHeight = display.getHeight();
                    int newNoteViewHeight = mAddNewNote.getHeight();
                    int start = screenHeight - newNoteViewHeight;
                    int eventY = start + (int) event.getY();
                    /**
                     * Minus TitleBar's height
                     */
                    if (mState == ListEditState.SUB_FOLDER) {
                        eventY -= mTitleBar.getHeight();
                        start -= mTitleBar.getHeight();
                    }
                    /**
                     * HACKME:When click the transparent part of "New Note" button, dispatch
                     * the event to the list view behind this button. The transparent part of
                     * "New Note" button could be expressed by formula y=-0.12x+94（Unit:pixel）
                     * and the line top of the button. The coordinate based on left of the "New
                     * Note" button. The 94 represents maximum height of the transparent part.
                     * Notice that, if the background of the button changes, the formula should
                     * also change. This is very bad, just for the UI designer's strong requirement.
                     */
                    if (event.getY() < (event.getX() * (-0.12) + 94)) {
                        View view = mNotesListView.getChildAt(mNotesListView.getChildCount() - 1
                                - mNotesListView.getFooterViewsCount());
                        if (view != null && view.getBottom() > start
                                && (view.getTop() < (start + 94))) {
                            mOriginY = (int) event.getY();
                            mDispatchY = eventY;
                            event.setLocation(event.getX(), mDispatchY);
                            mDispatch = true;
                            return mNotesListView.dispatchTouchEvent(event);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mDispatch) {
                        mDispatchY += (int) event.getY() - mOriginY;
                        event.setLocation(event.getX(), mDispatchY);
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
                default: {
                    if (mDispatch) {
                        event.setLocation(event.getX(), mDispatchY);
                        mDispatch = false;
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
            }
            return false;
        }

    };

    /**
     * 启动异步查询获取便签列表数据
     * 根据当前文件夹ID选择合适的查询条件，使用BackgroundQueryHandler在后台线程执行查询，
     * 避免阻塞UI线程。查询结果将通过onQueryComplete回调方法更新到列表适配器。
     */
    private void startAsyncNotesListQuery() {
        if (mInSearchMode && !TextUtils.isEmpty(mSearchQuery)) {
            String selection = NoteColumns.SNIPPET + " LIKE ? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER;
            String[] selectionArgs = { "%" + mSearchQuery + "%" };
            mBackgroundQueryHandler.startQuery(FOLDER_NOTE_LIST_QUERY_TOKEN, null,
                    Notes.CONTENT_NOTE_URI, NoteItemData.PROJECTION, selection, selectionArgs,
                    NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
        } else {
            String selection = (mCurrentFolderId == Notes.ID_ROOT_FOLDER) ? ROOT_FOLDER_SELECTION
                    : NORMAL_SELECTION;
            mBackgroundQueryHandler.startQuery(FOLDER_NOTE_LIST_QUERY_TOKEN, null,
                    Notes.CONTENT_NOTE_URI, NoteItemData.PROJECTION, selection, new String[] {
                        String.valueOf(mCurrentFolderId)
                    }, NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
        }
    }

    /**
     * BackgroundQueryHandler - 后台查询处理器
     * 继承自AsyncQueryHandler，用于在后台线程执行数据库查询操作，避免阻塞UI线程。
     * 通过token参数区分不同类型的查询请求，并在查询完成后更新UI或执行相应操作。
     */
    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case FOLDER_NOTE_LIST_QUERY_TOKEN:
                    // 更新便签列表适配器的数据源
                    mNotesListAdapter.changeCursor(cursor);
                    break;
                case FOLDER_LIST_QUERY_TOKEN:
                    // 显示文件夹选择菜单
                    if (cursor != null && cursor.getCount() > 0) {
                        showFolderListMenu(cursor);
                    } else {
                        Log.e(TAG, "Query folder failed");
                    }
                    break;
                default:
                    return;
            }
        }
    }

    /**
     * 显示文件夹选择菜单
     * 创建并显示一个对话框，用于选择目标文件夹来移动选中的便签。
     * 使用FoldersListAdapter将查询结果显示为文件夹列表，并为每个文件夹项设置点击监听器。
     * 当用户选择文件夹后，执行批量移动操作并显示操作结果提示。
     * @param cursor 包含文件夹数据的游标
     */
    private void showFolderListMenu(Cursor cursor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(R.string.menu_title_select_folder);
        final FoldersListAdapter adapter = new FoldersListAdapter(this, cursor);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                DataUtils.batchMoveToFolder(mContentResolver,
                        mNotesListAdapter.getSelectedItemIds(), adapter.getItemId(which));
                Toast.makeText(
                        NotesListActivity.this,
                        getString(R.string.format_move_notes_to_folder,
                                mNotesListAdapter.getSelectedCount(),
                                adapter.getFolderName(NotesListActivity.this, which)),
                        Toast.LENGTH_SHORT).show();
                mModeCallBack.finishActionMode();
            }
        });
        builder.show();
    }

    /**
     * 创建新便签
     * 启动NoteEditActivity活动，用于创建新的便签。
     * 设置意图操作为插入或编辑，并传递当前文件夹ID作为额外参数，
     * 以便新便签能够保存在正确的文件夹中。
     */
    private void createNewNote() {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mCurrentFolderId);
        this.startActivityForResult(intent, REQUEST_CODE_NEW_NODE);
    }

    /**
     * 批量删除选中的便签
     * 使用异步任务在后台线程执行批量删除操作，避免阻塞UI线程。
     * 根据是否启用同步模式执行不同操作：
     * - 非同步模式：直接删除便签
     * - 同步模式：将便签移动到垃圾箱文件夹
     * 删除完成后更新相关的应用小部件并退出多选操作模式。
     */
    private void batchDelete() {
        new AsyncTask<Void, Void, HashSet<AppWidgetAttribute>>() {
            protected HashSet<AppWidgetAttribute> doInBackground(Void... unused) {
                HashSet<AppWidgetAttribute> widgets = mNotesListAdapter.getSelectedWidget();
                if (!isSyncMode()) {
                    // if not synced, delete notes directly
                    if (DataUtils.batchDeleteNotes(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds())) {
                    } else {
                        Log.e(TAG, "Delete notes error, should not happens");
                    }
                } else {
                    // in sync mode, we'll move the deleted note into the trash
                    // folder
                    if (!DataUtils.batchMoveToFolder(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds(), Notes.ID_TRASH_FOLER)) {
                        Log.e(TAG, "Move notes to trash folder error, should not happens");
                    }
                }
                return widgets;
            }

            @Override
            protected void onPostExecute(HashSet<AppWidgetAttribute> widgets) {
                if (widgets != null) {
                    for (AppWidgetAttribute widget : widgets) {
                        if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                                && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                            updateWidget(widget.widgetId, widget.widgetType);
                        }
                    }
                }
                mModeCallBack.finishActionMode();
            }
        }.execute();
    }

    /**
     * 删除文件夹
     * 根据是否启用同步模式执行不同的文件夹删除操作：
     * - 非同步模式：直接删除文件夹
     * - 同步模式：将文件夹移动到垃圾箱文件夹
     * 根文件夹无法删除，会直接返回错误日志。
     * 删除完成后更新相关的应用小部件。
     * @param folderId 要删除的文件夹ID
     */
    private void deleteFolder(long folderId) {
        if (folderId == Notes.ID_ROOT_FOLDER) {
            Log.e(TAG, "Wrong folder id, should not happen " + folderId);
            return;
        }

        HashSet<Long> ids = new HashSet<Long>();
        ids.add(folderId);
        HashSet<AppWidgetAttribute> widgets = DataUtils.getFolderNoteWidget(mContentResolver,
                folderId);
        if (!isSyncMode()) {
            // if not synced, delete folder directly
            DataUtils.batchDeleteNotes(mContentResolver, ids);
        } else {
            // in sync mode, we'll move the deleted folder into the trash folder
            DataUtils.batchMoveToFolder(mContentResolver, ids, Notes.ID_TRASH_FOLER);
        }
        if (widgets != null) {
            for (AppWidgetAttribute widget : widgets) {
                if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                        && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                    updateWidget(widget.widgetId, widget.widgetType);
                }
            }
        }
    }

    /**
     * 打开便签进行查看或编辑
     * 启动NoteEditActivity活动，用于查看或编辑指定的便签。
     * 设置意图操作为查看，并传递便签ID作为额外参数。
     * @param data 要打开的便签数据
     */
    private void openNode(NoteItemData data) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_UID, data.getId());
        this.startActivityForResult(intent, REQUEST_CODE_OPEN_NODE);
    }

    /**
     * 打开文件夹并显示其内容
     * 设置当前文件夹ID为选中的文件夹ID，启动异步查询获取该文件夹下的便签列表。
     * 根据文件夹类型设置不同的列表状态：
     * - 通话记录文件夹：设置为CALL_RECORD_FOLDER状态并隐藏新建便签按钮
     * - 普通文件夹：设置为SUB_FOLDER状态
     * 同时更新标题栏显示当前文件夹名称。
     * @param data 要打开的文件夹数据
     */
    private void openFolder(NoteItemData data) {
        mCurrentFolderId = data.getId();
        mInSearchMode = false;
        mSearchQuery = null;
        startAsyncNotesListQuery();
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mState = ListEditState.CALL_RECORD_FOLDER;
            mAddNewNote.setVisibility(View.GONE);
        } else {
            mState = ListEditState.SUB_FOLDER;
        }
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mTitleBar.setText(R.string.call_record_folder_name);
        } else {
            mTitleBar.setText(data.getSnippet());
        }
        mTitleBar.setVisibility(View.VISIBLE);
    }

    /**
     * 处理视图点击事件
     * 实现OnClickListener接口，处理UI组件的点击事件。
     * 目前主要处理新建便签按钮的点击事件。
     * @param v 被点击的视图组件
     */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_new_note:
                createNewNote();
                break;
            default:
                break;
        }
    }

    /**
     * 显示软键盘
     * 获取系统输入法服务，并强制显示软键盘。
     */
    private void showSoftInput() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    /**
     * 隐藏软键盘
     * 获取系统输入法服务，并从指定视图的窗口隐藏软键盘。
     * @param view 用于获取窗口令牌的视图
     */
    private void hideSoftInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 显示创建或修改文件夹的对话框
     * 根据参数决定是创建新文件夹还是修改现有文件夹：
     * - 创建模式：显示空输入框，标题为"创建文件夹"
     * - 修改模式：显示现有文件夹名称，标题为"重命名文件夹"
     * 验证文件夹名称的唯一性，确保不会创建重复名称的文件夹。
     * 操作完成后更新数据库并关闭对话框。
     * @param create true表示创建新文件夹，false表示修改现有文件夹
     */
    private void showCreateOrModifyFolderDialog(final boolean create) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
        showSoftInput();
        if (!create) {
            if (mFocusNoteDataItem != null) {
                etName.setText(mFocusNoteDataItem.getSnippet());
                builder.setTitle(getString(R.string.menu_folder_change_name));
            } else {
                Log.e(TAG, "The long click data item is null");
                return;
            }
        } else {
            etName.setText("");
            builder.setTitle(this.getString(R.string.menu_create_folder));
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                hideSoftInput(etName);
            }
        });

        final Dialog dialog = builder.setView(view).show();
        final Button positive = (Button)dialog.findViewById(android.R.id.button1);
        positive.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                hideSoftInput(etName);
                String name = etName.getText().toString();
                if (DataUtils.checkVisibleFolderName(mContentResolver, name)) {
                    Toast.makeText(NotesListActivity.this, getString(R.string.folder_exist, name),
                            Toast.LENGTH_LONG).show();
                    etName.setSelection(0, etName.length());
                    return;
                }
                if (!create) {
                    if (!TextUtils.isEmpty(name)) {
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SNIPPET, name);
                        values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                        values.put(NoteColumns.LOCAL_MODIFIED, 1);
                        mContentResolver.update(Notes.CONTENT_NOTE_URI, values, NoteColumns.ID
                                + "=?", new String[] {
                            String.valueOf(mFocusNoteDataItem.getId())
                        });
                    }
                } else if (!TextUtils.isEmpty(name)) {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.SNIPPET, name);
                    values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                    mContentResolver.insert(Notes.CONTENT_NOTE_URI, values);
                }
                dialog.dismiss();
            }
        });

        if (TextUtils.isEmpty(etName.getText())) {
            positive.setEnabled(false);
        }
        /**
         * When the name edit text is null, disable the positive button
         */
        etName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(etName.getText())) {
                    positive.setEnabled(false);
                } else {
                    positive.setEnabled(true);
                }
            }

            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
    }

    /**
     * 处理返回键点击事件
     * 根据当前列表状态执行不同的返回操作：
     * - SUB_FOLDER状态：返回到根文件夹视图
     * - CALL_RECORD_FOLDER状态：返回到根文件夹视图并显示新建便签按钮
     * - NOTE_LIST状态：执行默认的返回操作（退出应用）
     */
    @Override
    public void onBackPressed() {
        switch (mState) {
            case SUB_FOLDER:
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                startAsyncNotesListQuery();
                mTitleBar.setVisibility(View.GONE);
                break;
            case CALL_RECORD_FOLDER:
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                mAddNewNote.setVisibility(View.VISIBLE);
                mTitleBar.setVisibility(View.GONE);
                startAsyncNotesListQuery();
                break;
            case NOTE_LIST:
                if (mInSearchMode) {
                    mInSearchMode = false;
                    mSearchQuery = null;
                    startAsyncNotesListQuery();
                    mTitleBar.setVisibility(View.GONE);
                } else {
                    super.onBackPressed();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 更新应用小部件
     * 根据小部件类型和ID发送广播，通知小部件更新其内容。
     * 支持2x和4x两种小部件类型，分别对应不同的小部件提供者。
     * @param appWidgetId 要更新的小部件ID
     * @param appWidgetType 小部件类型（2x或4x）
     */
    private void updateWidget(int appWidgetId, int appWidgetType) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        if (appWidgetType == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (appWidgetType == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unspported widget type");
            return;
        }

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
            appWidgetId
        });

        sendBroadcast(intent);
        setResult(RESULT_OK, intent);
    }

    /**
     * 文件夹上下文菜单创建监听器
     * 当长按文件夹项时创建上下文菜单，提供文件夹的常用操作选项：
     * - 查看文件夹内容
     * - 删除文件夹
     * - 重命名文件夹
     */
    private final OnCreateContextMenuListener mFolderOnCreateContextMenuListener = new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (mFocusNoteDataItem != null) {
                menu.setHeaderTitle(mFocusNoteDataItem.getSnippet());
                menu.add(0, MENU_FOLDER_VIEW, 0, R.string.menu_folder_view);
                menu.add(0, MENU_FOLDER_DELETE, 0, R.string.menu_folder_delete);
                menu.add(0, MENU_FOLDER_CHANGE_NAME, 0, R.string.menu_folder_change_name);
            }
        }
    };

    /**
     * 上下文菜单关闭事件处理
     * 当上下文菜单关闭时，移除列表视图的上下文菜单监听器，
     * 防止重复创建菜单或内存泄漏。
     * @param menu 关闭的菜单
     */
    @Override
    public void onContextMenuClosed(Menu menu) {
        if (mNotesListView != null) {
            mNotesListView.setOnCreateContextMenuListener(null);
        }
        super.onContextMenuClosed(menu);
    }

    /**
     * 上下文菜单项选择事件处理
     * 处理文件夹上下文菜单的选项点击事件，执行相应的文件夹操作：
     * - 查看文件夹内容
     * - 删除文件夹（显示确认对话框）
     * - 重命名文件夹
     * @param item 选择的菜单项
     * @return true表示事件已处理，false表示未处理
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mFocusNoteDataItem == null) {
            Log.e(TAG, "The long click data item is null");
            return false;
        }
        switch (item.getItemId()) {
            case MENU_FOLDER_VIEW:
                openFolder(mFocusNoteDataItem);
                break;
            case MENU_FOLDER_DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_folder));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFolder(mFocusNoteDataItem.getId());
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case MENU_FOLDER_CHANGE_NAME:
                showCreateOrModifyFolderDialog(false);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * 准备选项菜单
     * 根据当前列表状态动态加载不同的菜单资源：
     * - NOTE_LIST状态：加载主菜单，包括新建文件夹、导出文本、同步等选项
     * - SUB_FOLDER状态：加载子文件夹菜单
     * - CALL_RECORD_FOLDER状态：加载通话记录文件夹菜单
     * 同时根据同步服务状态更新同步菜单项的标题。
     * @param menu 要准备的菜单
     * @return true表示菜单已准备好显示
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mState == ListEditState.NOTE_LIST) {
            getMenuInflater().inflate(R.menu.note_list, menu);
            // set sync or sync_cancel
            menu.findItem(R.id.menu_sync).setTitle(
                    GTaskSyncService.isSyncing() ? R.string.menu_sync_cancel : R.string.menu_sync);
        } else if (mState == ListEditState.SUB_FOLDER) {
            getMenuInflater().inflate(R.menu.sub_folder, menu);
        } else if (mState == ListEditState.CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_record_folder, menu);
        } else {
            Log.e(TAG, "Wrong state:" + mState);
        }
        return true;
    }

    /**
     * 选项菜单项选择事件处理
     * 处理应用顶部操作栏的选项菜单点击事件，执行相应功能：
     * - 创建新文件夹
     * - 导出便签为文本
     * - 同步与Google Tasks的数据
     * - 打开应用设置
     * - 创建新便签
     * - 启动搜索功能
     * @param item 选择的菜单项
     * @return true表示事件已处理，false表示未处理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                showCreateOrModifyFolderDialog(true);
                break;
            }
            case R.id.menu_export_text: {
                exportNoteToText();
                break;
            }
            case R.id.menu_sync: {
                if (isSyncMode()) {
                    if (TextUtils.equals(item.getTitle(), getString(R.string.menu_sync))) {
                        GTaskSyncService.startSync(this);
                    } else {
                        GTaskSyncService.cancelSync(this);
                    }
                } else {
                    startPreferenceActivity();
                }
                break;
            }
            case R.id.menu_setting: {
                startPreferenceActivity();
                break;
            }
            case R.id.menu_new_note: {
                createNewNote();
                break;
            }
            case R.id.menu_search:
                onSearchRequested();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 搜索请求处理方法
     * 启动系统搜索功能，允许用户在便签中进行搜索。
     * @return true表示搜索已启动
     */
    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /* appData */, false);
        return true;
    }

    /**
     * 导出便签为文本文件
     * 使用BackupUtils在后台线程将所有便签导出为文本文件，支持SD卡存储。
     * 导出过程中显示进度对话框，并在完成后根据结果显示不同的提示信息：
     * - SD卡未挂载错误
     * - 导出成功（显示文件路径）
     * - 系统错误
     */
    private void exportNoteToText() {
        final BackupUtils backup = BackupUtils.getInstance(NotesListActivity.this);
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... unused) {
                return backup.exportToText();
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == BackupUtils.STATE_SD_CARD_UNMOUONTED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.failed_sdcard_export));
                    builder.setMessage(NotesListActivity.this
                            .getString(R.string.error_sdcard_unmounted));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                } else if (result == BackupUtils.STATE_SUCCESS) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.success_sdcard_export));
                    builder.setMessage(NotesListActivity.this.getString(
                            R.string.format_exported_file_location, backup
                                    .getExportedTextFileName(), backup.getExportedTextFileDir()));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                } else if (result == BackupUtils.STATE_SYSTEM_ERROR) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.failed_sdcard_export));
                    builder.setMessage(NotesListActivity.this
                            .getString(R.string.error_sdcard_export));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
            }

        }.execute();
    }

    /**
     * 检查是否处于同步模式
     * 通过读取SharedPreferences中的同步账户名称判断是否启用了Google Tasks同步功能。
     * @return true表示已启用同步模式，false表示未启用
     */
    private boolean isSyncMode() {
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    /**
     * 启动设置活动
     * 打开应用的设置界面，允许用户配置应用的各种选项，
     * 如同步账户、显示设置等。
     */
    private void startPreferenceActivity() {
        Activity from = getParent() != null ? getParent() : this;
        Intent intent = new Intent(from, NotesPreferenceActivity.class);
        from.startActivityIfNeeded(intent, -1);
    }

    /**
     * OnListItemClickListener - 列表项点击事件监听器
     * 处理便签列表项的点击事件，根据当前列表状态和点击的项目类型执行不同操作：
     * - 在多选模式下：切换便签的选择状态
     * - 在普通模式下：根据项目类型（文件夹或便签）执行相应操作
     */
    private class OnListItemClickListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (view instanceof NotesListItem) {
                NoteItemData item = ((NotesListItem) view).getItemData();
                
                // 如果处于多选模式，切换项目选择状态
                if (mNotesListAdapter.isInChoiceMode()) {
                    if (item.getType() == Notes.TYPE_NOTE) {
                        position = position - mNotesListView.getHeaderViewsCount();
                        mModeCallBack.onItemCheckedStateChanged(null, position, id,
                                !mNotesListAdapter.isSelectedItem(position));
                    }
                    return;
                }

                // 根据当前列表状态和项目类型执行不同操作
                switch (mState) {
                    case NOTE_LIST:
                        if (item.getType() == Notes.TYPE_FOLDER
                                || item.getType() == Notes.TYPE_SYSTEM) {
                            openFolder(item);
                        } else if (item.getType() == Notes.TYPE_NOTE) {
                            openNode(item);
                        } else {
                            Log.e(TAG, "Wrong note type in NOTE_LIST");
                        }
                        break;
                    case SUB_FOLDER:
                    case CALL_RECORD_FOLDER:
                        if (item.getType() == Notes.TYPE_NOTE) {
                            openNode(item);
                        } else {
                            Log.e(TAG, "Wrong note type in SUB_FOLDER");
                        }
                        break;
                    default:
                        break;
                }
            }
        }

    }

    /**
     * 查询目标文件夹列表
     * 异步查询所有可用的文件夹（不包括垃圾箱文件夹和当前文件夹），
     * 用于便签移动操作时选择目标文件夹。
     * 在非根文件夹状态下，还会包含根文件夹作为可选目标。
     */
    private void startQueryDestinationFolders() {
        String selection = NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>? AND " + NoteColumns.ID + "<>?";
        selection = (mState == ListEditState.NOTE_LIST) ? selection:
            "(" + selection + ") OR (" + NoteColumns.ID + "=" + Notes.ID_ROOT_FOLDER + ")";

        mBackgroundQueryHandler.startQuery(FOLDER_LIST_QUERY_TOKEN,
                null,
                Notes.CONTENT_NOTE_URI,
                FoldersListAdapter.PROJECTION,
                selection,
                new String[] {
                        String.valueOf(Notes.TYPE_FOLDER),
                        String.valueOf(Notes.ID_TRASH_FOLER),
                        String.valueOf(mCurrentFolderId)
                },
                NoteColumns.MODIFIED_DATE + " DESC");
    }

    /**
     * 列表项长按事件处理
     * 根据长按的项目类型执行不同操作：
     * - 便签：启动多选操作模式，允许批量处理便签
     * - 文件夹：为文件夹创建上下文菜单
     * 同时提供触觉反馈（震动）以增强用户体验。
     * @param parent 父适配器视图
     * @param view 长按的视图
     * @param position 项目在列表中的位置
     * @param id 项目的ID
     * @return false表示未消耗事件，允许其他监听器处理
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view instanceof NotesListItem) {
            mFocusNoteDataItem = ((NotesListItem) view).getItemData();
            if (mFocusNoteDataItem.getType() == Notes.TYPE_NOTE && !mNotesListAdapter.isInChoiceMode()) {
                if (mNotesListView.startActionMode(mModeCallBack) != null) {
                    mModeCallBack.onItemCheckedStateChanged(null, position, id, true);
                    mNotesListView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } else {
                    Log.e(TAG, "startActionMode fails");
                }
            } else if (mFocusNoteDataItem.getType() == Notes.TYPE_FOLDER) {
                mNotesListView.setOnCreateContextMenuListener(mFolderOnCreateContextMenuListener);
            }
        }
        return false;
    }
}
