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

package net.micode.notes.gtask.remote;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.ui.NotesPreferenceActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


/**
 * Google Tasks客户端类
 * 负责与Google Tasks服务器进行通信，处理登录、任务创建、更新、删除等操作
 * 采用单例模式实现
 */
public class GTaskClient {
    // 日志标签
    private static final String TAG = GTaskClient.class.getSimpleName();

    // Google Tasks基础URL
    private static final String GTASK_URL = "https://mail.google.com/tasks/";

    // Google Tasks GET请求URL
    private static final String GTASK_GET_URL = "https://mail.google.com/tasks/ig";

    // Google Tasks POST请求URL
    private static final String GTASK_POST_URL = "https://mail.google.com/tasks/r/ig";

    // 单例实例
    private static GTaskClient mInstance = null;

    // HTTP客户端实例
    private DefaultHttpClient mHttpClient;

    // 当前使用的GET请求URL
    private String mGetUrl;

    // 当前使用的POST请求URL
    private String mPostUrl;

    // 客户端版本号
    private long mClientVersion;

    // 是否已登录
    private boolean mLoggedin;

    // 上次登录时间
    private long mLastLoginTime;

    // 操作ID计数器
    private int mActionId;

    // 登录的Google账户
    private Account mAccount;

    // 更新操作的JSON数组
    private JSONArray mUpdateArray;

    /**
     * 私有构造函数
     * 初始化客户端参数
     */
    private GTaskClient() {
        mHttpClient = null;
        mGetUrl = GTASK_GET_URL;
        mPostUrl = GTASK_POST_URL;
        mClientVersion = -1;
        mLoggedin = false;
        mLastLoginTime = 0;
        mActionId = 1;
        mAccount = null;
        mUpdateArray = null;
    }

    /**
     * 获取单例实例
     * @return GTaskClient实例
     */
    public static synchronized GTaskClient getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskClient();
        }
        return mInstance;
    }

    /**
     * 登录Google Tasks
     * 检查是否需要重新登录，处理Google账户认证和Tasks登录
     * @param activity 调用此方法的Activity实例
     * @return 是否登录成功
     */
    public boolean login(Activity activity) {
        // 假设Cookie 5分钟后过期，需要重新登录
        final long interval = 1000 * 60 * 5;
        if (mLastLoginTime + interval < System.currentTimeMillis()) {
            mLoggedin = false;
        }

        // 切换账户后需要重新登录
        if (mLoggedin
                && !TextUtils.equals(getSyncAccount().name, NotesPreferenceActivity
                        .getSyncAccountName(activity))) {
            mLoggedin = false;
        }

        if (mLoggedin) {
            Log.d(TAG, "already logged in");
            return true;
        }

        mLastLoginTime = System.currentTimeMillis();
        String authToken = loginGoogleAccount(activity, false);
        if (authToken == null) {
            Log.e(TAG, "login google account failed");
            return false;
        }

        // 必要时使用自定义域名登录
        if (!(mAccount.name.toLowerCase().endsWith("gmail.com") || mAccount.name.toLowerCase()
                .endsWith("googlemail.com"))) {
            StringBuilder url = new StringBuilder(GTASK_URL).append("a/");
            int index = mAccount.name.indexOf('@') + 1;
            String suffix = mAccount.name.substring(index);
            url.append(suffix + "/");
            mGetUrl = url.toString() + "ig";
            mPostUrl = url.toString() + "r/ig";

            if (tryToLoginGtask(activity, authToken)) {
                mLoggedin = true;
            }
        }

        // 尝试使用Google官方URL登录
        if (!mLoggedin) {
            mGetUrl = GTASK_GET_URL;
            mPostUrl = GTASK_POST_URL;
            if (!tryToLoginGtask(activity, authToken)) {
                return false;
            }
        }

        mLoggedin = true;
        return true;
    }

    /**
     * 登录Google账户，获取认证令牌
     * @param activity 调用此方法的Activity实例
     * @param invalidateToken 是否使现有令牌失效
     * @return 认证令牌，如果获取失败则返回null
     */
    private String loginGoogleAccount(Activity activity, boolean invalidateToken) {
        String authToken;
        AccountManager accountManager = AccountManager.get(activity);
        Account[] accounts = accountManager.getAccountsByType("com.google");

        if (accounts.length == 0) {
            Log.e(TAG, "there is no available google account");
            return null;
        }

        String accountName = NotesPreferenceActivity.getSyncAccountName(activity);
        Account account = null;
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                account = a;
                break;
            }
        }
        if (account != null) {
            mAccount = account;
        } else {
            Log.e(TAG, "unable to get an account with the same name in the settings");
            return null;
        }

        // 获取认证令牌
        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account,
                "goanna_mobile", null, activity, null, null);
        try {
            Bundle authTokenBundle = accountManagerFuture.getResult();
            authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            if (invalidateToken) {
                accountManager.invalidateAuthToken("com.google", authToken);
                loginGoogleAccount(activity, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "get auth token failed");
            authToken = null;
        }

        return authToken;
    }

    /**
     * 尝试登录Google Tasks
     * 如果登录失败，尝试使令牌失效并重新登录
     * @param activity 调用此方法的Activity实例
     * @param authToken Google账户认证令牌
     * @return 是否登录成功
     */
    private boolean tryToLoginGtask(Activity activity, String authToken) {
        if (!loginGtask(authToken)) {
            // 认证令牌可能已过期，使令牌失效并重新尝试
            authToken = loginGoogleAccount(activity, true);
            if (authToken == null) {
                Log.e(TAG, "login google account failed");
                return false;
            }

            if (!loginGtask(authToken)) {
                Log.e(TAG, "login gtask failed");
                return false;
            }
        }
        return true;
    }

    /**
     * 使用认证令牌登录Google Tasks
     * 设置HTTP客户端，执行登录请求并获取Cookie和客户端版本
     * @param authToken Google账户认证令牌
     * @return 是否登录成功
     */
    private boolean loginGtask(String authToken) {
        int timeoutConnection = 10000;
        int timeoutSocket = 15000;
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        mHttpClient = new DefaultHttpClient(httpParameters);
        BasicCookieStore localBasicCookieStore = new BasicCookieStore();
        mHttpClient.setCookieStore(localBasicCookieStore);
        HttpProtocolParams.setUseExpectContinue(mHttpClient.getParams(), false);

        // 登录Google Tasks
        try {
            String loginUrl = mGetUrl + "?auth=" + authToken;
            HttpGet httpGet = new HttpGet(loginUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // 获取认证Cookie
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
            boolean hasAuthCookie = false;
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("GTL")) {
                    hasAuthCookie = true;
                }
            }
            if (!hasAuthCookie) {
                Log.w(TAG, "it seems that there is no auth cookie");
            }

            // 获取客户端版本
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            mClientVersion = js.getLong("v");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // 捕获所有异常
            Log.e(TAG, "httpget gtask_url failed");
            return false;
        }

        return true;
    }

    /**
     * 获取操作ID
     * 自增并返回当前操作ID
     * @return 唯一的操作ID
     */
    private int getActionId() {
        return mActionId++;
    }

    /**
     * 创建HTTP POST请求
     * 设置请求头和URL
     * @return HttpPost实例
     */
    private HttpPost createHttpPost() {
        HttpPost httpPost = new HttpPost(mPostUrl);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        httpPost.setHeader("AT", "1");
        return httpPost;
    }

    /**
     * 获取HTTP响应内容
     * 处理不同的编码格式（gzip、deflate等）
     * @param entity HTTP响应实体
     * @return 响应内容字符串
     * @throws IOException 如果读取响应内容时发生错误
     */
    private String getResponseContent(HttpEntity entity) throws IOException {
        String contentEncoding = null;
        if (entity.getContentEncoding() != null) {
            contentEncoding = entity.getContentEncoding().getValue();
            Log.d(TAG, "encoding: " + contentEncoding);
        }

        InputStream input = entity.getContent();
        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
            input = new GZIPInputStream(entity.getContent());
        } else if (contentEncoding != null && contentEncoding.equalsIgnoreCase("deflate")) {
            Inflater inflater = new Inflater(true);
            input = new InflaterInputStream(entity.getContent(), inflater);
        }

        try {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            while (true) {
                String buff = br.readLine();
                if (buff == null) {
                    return sb.toString();
                }
                sb = sb.append(buff);
            }
        } finally {
            input.close();
        }
    }

    /**
     * 发送POST请求
     * 向Google Tasks服务器发送请求并返回响应
     * @param js 请求的JSON对象
     * @return 响应的JSON对象
     * @throws NetworkFailureException 如果网络请求失败
     */
    private JSONObject postRequest(JSONObject js) throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        HttpPost httpPost = createHttpPost();
        try {
            LinkedList<BasicNameValuePair> list = new LinkedList<BasicNameValuePair>();
            list.add(new BasicNameValuePair("r", js.toString()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
            httpPost.setEntity(entity);

            // 执行POST请求
            HttpResponse response = mHttpClient.execute(httpPost);
            String jsString = getResponseContent(response.getEntity());
            return new JSONObject(jsString);

        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("unable to convert response content to jsonobject");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("error occurs when posting request");
        }
    }

    /**
     * 创建任务
     * 向Google Tasks服务器发送创建任务请求
     * @param task 要创建的任务对象
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void createTask(Task task) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            actionList.put(task.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送请求
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            task.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create task: handing jsonobject failed");
        }
    }

    /**
     * 创建任务列表
     * 向Google Tasks服务器发送创建任务列表请求
     * @param tasklist 要创建的任务列表对象
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void createTaskList(TaskList tasklist) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            actionList.put(tasklist.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送请求
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            tasklist.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create tasklist: handing jsonobject failed");
        }
    }

    /**
     * 提交更新
     * 将待更新的操作发送到Google Tasks服务器
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void commitUpdate() throws NetworkFailureException {
        if (mUpdateArray != null) {
            try {
                JSONObject jsPost = new JSONObject();

                // action_list
                jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, mUpdateArray);

                // client_version
                jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

                postRequest(jsPost);
                mUpdateArray = null;
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("commit update: handing jsonobject failed");
            }
        }
    }

    /**
     * 添加更新节点
     * 将节点的更新操作添加到待更新列表中
     * 如果待更新列表超过10项，则立即提交更新
     * @param node 要更新的节点
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void addUpdateNode(Node node) throws NetworkFailureException {
        if (node != null) {
            // 过多的更新项可能导致错误
            // 最大设置为10项
            if (mUpdateArray != null && mUpdateArray.length() > 10) {
                commitUpdate();
            }

            if (mUpdateArray == null)
                mUpdateArray = new JSONArray();
            mUpdateArray.put(node.getUpdateAction(getActionId()));
        }
    }

    /**
     * 移动任务
     * 将任务从一个任务列表移动到另一个任务列表，或在同一任务列表内移动
     * @param task 要移动的任务
     * @param preParent 移动前的父任务列表
     * @param curParent 移动后的父任务列表
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void moveTask(Task task, TaskList preParent, TaskList curParent)
            throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_MOVE);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_ID, task.getGid());
            if (preParent == curParent && task.getPriorSibling() != null) {
                // 只有在同一任务列表内移动且不是第一个任务时，才设置prior_sibling_id
                action.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, task.getPriorSibling());
            }
            action.put(GTaskStringUtils.GTASK_JSON_SOURCE_LIST, preParent.getGid());
            action.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT, curParent.getGid());
            if (preParent != curParent) {
                // 只有在不同任务列表之间移动时，才设置dest_list
                action.put(GTaskStringUtils.GTASK_JSON_DEST_LIST, curParent.getGid());
            }
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("move task: handing jsonobject failed");
        }
    }

    /**
     * 删除节点
     * 将节点标记为已删除并发送到服务器
     * @param node 要删除的节点
     * @throws NetworkFailureException 如果网络请求失败
     */
    public void deleteNode(Node node) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            node.setDeleted(true);
            actionList.put(node.getUpdateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost);
            mUpdateArray = null;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("delete node: handing jsonobject failed");
        }
    }

    /**
     * 获取所有任务列表
     * 从Google Tasks服务器获取用户的所有任务列表
     * @return 任务列表的JSON数组
     * @throws NetworkFailureException 如果网络请求失败
     */
    public JSONArray getTaskLists() throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        try {
            HttpGet httpGet = new HttpGet(mGetUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // get the task list
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            return js.getJSONObject("t").getJSONArray(GTaskStringUtils.GTASK_JSON_LISTS);
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task lists: handing jasonobject failed");
        }
    }

    /**
     * 获取指定任务列表的任务
     * 从Google Tasks服务器获取指定任务列表中的所有任务
     * @param listGid 任务列表的GID
     * @return 任务的JSON数组
     * @throws NetworkFailureException 如果网络请求失败
     */
    public JSONArray getTaskList(String listGid) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_GETALL);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_LIST_ID, listGid);
            action.put(GTaskStringUtils.GTASK_JSON_GET_DELETED, false);
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            JSONObject jsResponse = postRequest(jsPost);
            return jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_TASKS);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task list: handing jsonobject failed");
        }
    }

    /**
     * 获取当前同步账户
     * @return 当前使用的Google账户
     */
    public Account getSyncAccount() {
        return mAccount;
    }

    /**
     * 重置更新数组
     * 清空待更新的操作列表
     */
    public void resetUpdateArray() {
        mUpdateArray = null;
    }
}
