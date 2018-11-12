package com.jzby.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.jzby.vmwork.ConnectionBean;
import com.jzby.vmwork.Constants;


public class TenantProcess {

    //public static String SERVER = "http://10.7.2.161:";
    public static String SERVER = "http://10.7.14.40:";
    //static String SERVER = "http://10.7.14.58:";
    final static int PORT_1 = 35357;
    final static int PORT_2 = 8774;
    final static int PORT_3 = 80 ;
    public final static String URL_GET_USERID = "/v3/auth/tokens";
    public final static String URL_GET_PROJECTID = "/v3/auth/projects";
    public final static String URL_GET_SERVER_LIST_A = "/v2.1/";
    public final static String URL_GET_SERVER_LIST_B = "/servers/detail";
    public final static String URL_GET_SERVER_ADDRESS_A = "/servers/";
    public final static String URL_GET_SERVER_ADDRESS_B = "/remote-consoles";
    public final static String URL_ACTION_SERVER_START = "/action";
    public final static String URL_ACTION_CHANGE_PWD="/v3/users/";
    public final static String URL_ACTION_RESET="/cgi-bin/force_shut_down_vm.sh?";

    final static String SERVER_TOKEN_KEY = "X-Subject-Token";
    final static String TOKEN_KEY = "X-Auth-Token";

    public static int CONNECTION_TYPE=-1;

    public static String TOKEN_VALUE = "";
    static String pwd = "";
    public static String userId = "";
    public static String projectId = "";

    HttpProcessInterface listener = null;

    private static TenantProcess mInstance;

    public static List<ConnectionBean> remoteServerList;

    private TenantProcess() {
    }

    public static TenantProcess getInstance()
    {
        if(mInstance==null)
        {
            mInstance=new TenantProcess();
        }
        return  mInstance;
    }

    public void setHttpProcessListener(HttpProcessInterface listener) {
        this.listener = listener;
    }

    public void TenantProcess_GetInfolist(final String username, final String password) {
        System.out.println("======" + username + "======" + password);
        new Thread(new Runnable() {

            @Override
            public void run() {
                String url = SERVER + PORT_1 + URL_GET_USERID;
                String data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": { \"name\": \""
                        + username + "\",\"domain\": {\"name\": \"Default\"},\"password\": \"" + password + "\" }}}}}";

                // one 获取用户ID 和 用户Token
                userId = getUserId(url, data);
                pwd = password;

                //two获取ProjectID(可能会有多个)
                //getProjectId();

                // three 获取服务端Token
                /*url = SERVER + PORT_1 + URL_GET_USERID;
				data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
						+ "\", " + "\"password\": \"" + password + "\", \"domain\": {\"name\": \"Default\"}}}}, "
						+ "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
				getServerToken(url, data);

				// four获取虚拟机列表
				url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_LIST_B;
				data = "{\"remote_console\": {\"type\": \"spice-html5\", \"protocol\": \"spice\"}}";
				getServerList(url, data);*/

            }
        }).start();
    }

    private String getUserId(String url, String data) {
        System.out.println("====getUserId()=====");
        String userId = "";
        String response = httpPost(url, data);
        if (!TextUtils.isEmpty(response)) {

            if("-1".equals(response))
            {
                if (listener != null) {
                    listener.onHttpFailed(url, 404);
                }
                return userId;
            }

            try {
                JSONObject obj = new JSONObject(response);
                if (obj.has("token")) {
                    JSONObject tokenObj = obj.getJSONObject("token");
                    if (tokenObj.has("user")) {
                        JSONObject userObj = tokenObj.getJSONObject("user");
                        if (userObj.has("id")) {
                            userId = userObj.getString("id");
                            System.out.println("====userId====" + userId);
                            if (listener != null) {
                                listener.onHttpSuccess(url);
                            }
                        } else {
                            if (listener != null) {
                                listener.onHttpFailed(url, 400);
                            }
                        }
                    } else {
                        if (listener != null) {
                            listener.onHttpFailed(url, 400);
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (listener != null) {
                listener.onHttpFailed(url, 400);
            }
        }

        return userId;
    }

    public void getProjectId() {
        System.out.println("====getProjectId()=====");
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(TOKEN_VALUE)) {
            System.out.println("=====has not authorized yet!=======");
            return;
        }

        remoteServerList = new ArrayList<ConnectionBean>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    String url = SERVER + PORT_1 + URL_GET_PROJECTID;
                    final String response = httpGet(url);
                    if (!TextUtils.isEmpty(response)) {

                        JSONObject obj = new JSONObject(response);
                        if (obj.has("projects")) {
                            JSONArray proArrays = obj.getJSONArray("projects");
                            if (proArrays != null && proArrays.length() > 0) {
                                for (int i = 0; i < proArrays.length(); i++) {
                                    JSONObject proObj = proArrays.getJSONObject(i);
                                    if (proObj.has("name") && proObj.has("id")) {
                                        projectId = proObj.getString("id");
                                        System.out.println("=====projectId===" + projectId);
                                        // three 获取服务端Token
                                        String newUrl = SERVER + PORT_1 + URL_GET_USERID;
                                        String  data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
                                                    + "\", " + "\"password\": \"" + pwd + "\", \"domain\": {\"name\": \"Default\"}}}}, "
                                                    + "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
                                            getServerToken(newUrl, data);

                                        // four获取虚拟机列表
                                        newUrl = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_LIST_B;
                                        data = "{\"remote_console\": {\"type\": \"spice-html5\", \"protocol\": \"spice\"}}";
                                        getServerList(newUrl, data);
                                    }

                                    if (proArrays.length() > (i + 1)) {
                                        System.out.println("=====sleep 1 second=====");
                                        Thread.sleep(500);
                                    }
                                }
                            } else {
                                System.out.println("======= projects size 0 ======");
                            }
                        } else {
                            System.out.println("=======no projects======");
                        }

                        if (listener != null) {
                            listener.onHttpSuccess(url);
                        }
                    } else {
                        if (listener != null) {
                            listener.onHttpFailed(url, 400);
                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private String getServerToken(String url, String data) {
        System.out.println("====getServerToken()=====");
        String response = httpPost(url, data);
        if (!TextUtils.isEmpty(response)) {

            if("-1".equals(response))
            {
                if (listener != null) {
                    listener.onHttpFailed(url, 404);
                }
                return "";
            }

            if (listener != null) {
                listener.onHttpSuccess(url);
            }
        } else {
            if (listener != null) {
                listener.onHttpFailed(url, 400);
            }
        }
        return response;
    }

    private String getServerList(String url, String data) {
        System.out.println("====getServerList()=====");
        String response = httpGet(url);
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject obj = new JSONObject(response);
                if (obj.has("servers")) {

                    int serverTotal = remoteServerList.size();
                    System.out.println("======before server size=======" + serverTotal);
                    JSONArray serverArrays = obj.getJSONArray("servers");
                    if (serverArrays != null && serverArrays.length() > 0) {
                        for (int i = 0; i < serverArrays.length(); i++) {
                            ConnectionBean server = new ConnectionBean();
                            server.setSshUser(projectId);
                            server.setAddress("0.0.0.0");
                            server.setPort(0);
                            JSONObject serverObj = serverArrays.getJSONObject(i);
                            String serverId = "";
                            String status = "";
                            if (serverObj.has("status")) {
                                status = serverObj.getString("status");
                                System.out.println("======status=======" + status);
                                //set status
                                server.setSshPrivKey(status);
                            }

                            if(serverObj.has("name"))
                            {
                                String name=serverObj.getString("name");
                                System.out.println("======name====="+name);
                                server.setNickname(name);
                            }

                            if (serverObj.has("links")) {
                                JSONArray linksArrays = serverObj.getJSONArray("links");
                                if (linksArrays != null && linksArrays.length() > 0) {
                                    JSONObject child = linksArrays.getJSONObject(0);
                                    if (child.has("href")) {
                                        String hrefValue = child.getString("href");
                                        System.out.println("========hrefValue=====" + hrefValue);
                                        serverId = hrefValue.substring(hrefValue.lastIndexOf("/") + 1);
                                        System.out.println("=======serverId======" + serverId);
                                        //set serverId
                                        server.setSshPubKey(serverId);
                                    }
                                }
                            }
                            //add one server
                            remoteServerList.add(server);

                            if ("ACTIVE".equals(status)) {
                                // five获取虚拟机的IP地址和端口
                                String newUrl = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId
                                        + URL_GET_SERVER_ADDRESS_A + serverId + URL_GET_SERVER_ADDRESS_B;
                                getServerAddress(newUrl, serverTotal + i, data);
                            }
                        }

                        if (remoteServerList != null && remoteServerList.size() > 0) {
                            System.out.println("=======remoteServerList=======" + remoteServerList.size());
                        }
                    } else {
                        System.out.println("=====server size is 0=====");
                    }

                    if (listener != null) {
                        listener.onHttpSuccess(url);
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    public String getServerAddress(String url, int position, String data) {
        System.out.println("====getServerAddress()=====");
        String response = httpPost2(url, data);
        String host = null;
        String port = null;
        String tlsPort = null;
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject lastObj = new JSONObject(response);
                if (lastObj.has("remote_console")) {
                    JSONObject myObj = lastObj.getJSONObject("remote_console");
                    if (myObj.has("host") && myObj.has("port")) {
                        try {
                            host = myObj.getString("host");
                        }catch (Exception e){
                            e.printStackTrace();
                        };
                        try {
                            port = myObj.getString("port");
                        }catch (Exception e){
                            e.printStackTrace();
                        };
                        try {
                            tlsPort = myObj.getString("tlsPort");
                        }catch (Exception e){
                            e.printStackTrace();
                        };
                      //  String host = myObj.getString("host");
                      //  String port = myObj.getString("port");
                      //  String tlsPort=myObj.getString("tlsPort");
                         System.out.println("=====" + host + "======" + port+"====="+tlsPort);
                        //set server host
                        remoteServerList.get(position).setAddress(host);
                        String reg = "^[0-9]+";
                        Pattern pattern = Pattern.compile(reg);
                        Matcher matcher = pattern.matcher(port);
                        //set server port
                        if (port != null && matcher.find()) {
                            remoteServerList.get(position).setPort(Integer.parseInt(port));
                        } else {
                            remoteServerList.get(position).setPort(0);
                        }
                        //set tlsPort
                        matcher = pattern.matcher(tlsPort);
                        if(tlsPort != null && matcher.find())
                        {
                            int numTlsPort=Integer.parseInt(tlsPort);
                            remoteServerList.get(position).setTlsPort(numTlsPort);
                        }
                        else
                        {
                            remoteServerList.get(position).setTlsPort(0);
                        }

                        if (listener != null) {
                            listener.onHttpSuccess(url);
                        }
                    } else {
                        if (listener != null) {
                            listener.onHttpFailed(url, 400);
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (listener != null) {
                listener.onHttpFailed(url, 400);
            }
        }
        return response;
    }

    public void refreshServerList() {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(TOKEN_VALUE)) {
            System.out.println("=====has not authorized yet!=======");
            if (listener != null) {
                listener.onHttpFailed(URL_GET_SERVER_LIST_B, 400);
            }
            return;
        }
        remoteServerList.clear();
        remoteServerList = new ArrayList<ConnectionBean>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getProjectId();
            }
        }).start();
    }

    public void refreshServerList2() {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(TOKEN_VALUE)) {
            System.out.println("=====has not authorized yet!=======");
            if (listener != null) {
                listener.onHttpFailed(URL_GET_SERVER_LIST_B, 400);
            }
            return;
        }

        if (remoteServerList == null || remoteServerList.size() == 0) {
            System.out.println("=======server size 0=======");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int updateCount = 0;
                    for (int i = 0; i < remoteServerList.size(); i++) {
                        projectId = remoteServerList.get(i).getSshUser();
                        String serverId = remoteServerList.get(i).getSshPubKey();

                        String url = SERVER + PORT_1 + URL_GET_USERID;
                        String data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
                                + "\", " + "\"password\": \"" + pwd + "\", \"domain\": {\"name\": \"Default\"}}}}, "
                                + "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
                        String response=getServerToken(url, data);
                        if (!TextUtils.isEmpty(response)) {
                            url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_ADDRESS_A + serverId;
                            response = httpGet2(url);
                            if (!TextUtils.isEmpty(response)) {
                                JSONObject object = new JSONObject(response);
                                if (object.has("server")) {
                                    JSONObject serverObj = object.getJSONObject("server");
                                    if (serverObj.has("status")) {
                                        String status = serverObj.getString("status");
                                        System.out.println("=======Server==Status=====" + status);
                                        remoteServerList.get(i).setSshPrivKey(status);
                                        updateCount++;
                                        if (Constants.SERVER_STATUS_SHUTOFF.equals(status)) {
                                            remoteServerList.get(i).setAddress("0.0.0.0");
                                            remoteServerList.get(i).setPort(0);
                                        }
                                    } else {
                                        System.out.println("=====no status=======");
                                    }
                                } else {
                                    System.out.println("=====has no server=======");
                                }
                            }
                            else
                            {
                                System.out.println("=====obtain status failed=======");
                            }
                        }
                        else
                        {
                            System.out.println("=====obtain token failed=======");
                        }
                    }
                    System.out.println("=====update server======" + updateCount);
                    if (updateCount > 0) {
                        if (listener != null) {
                            listener.onHttpSuccess(URL_GET_SERVER_ADDRESS_A);
                        }
                    }
                    else
                    {
                        if (listener != null) {
                            listener.onHttpFailed(URL_GET_SERVER_ADDRESS_A, 400);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void changeUserPassword(final String oldPwd, final String newPwd)
    {
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                String url = SERVER + PORT_1 + URL_ACTION_CHANGE_PWD+userId+"/password";

                String data ="{\"user\": {\"original_password\": \""+oldPwd+"\", \"password\": \""+newPwd+"\"}}";

                int code=httpPost3(url,data);

                if (code >= 200 && code < 300) {
                    if (listener != null) {
                        listener.onHttpSuccess(url);
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }

            }
        }).start();
    }

    public void resetRemoteServer(final int position)
    {
        System.out.println("=====resetRemoteServer======"+position);

        new Thread(new Runnable() {
            @Override
            public void run() {

                String serverAddress=remoteServerList.get(position).getAddress();
                String serverId = remoteServerList.get(position).getSshPubKey();
                projectId = remoteServerList.get(position).getSshUser();
                //获取服务端Token
                String url = SERVER + PORT_1 + URL_GET_USERID;
                String data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
                        + "\", " + "\"password\": \"" + pwd + "\", \"domain\": {\"name\": \"Default\"}}}}, "
                        + "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
                getServerToken(url, data);

                //强制重置虚拟机
                url ="http://"+ serverAddress+":" + PORT_3 + URL_ACTION_RESET + serverId;

                int code = httpPost3(url,"");
                if (code >= 200 && code < 300) {
                    remoteServerList.get(position).setAddress("0.0.0.0");
                    remoteServerList.get(position).setPort(0);
                    remoteServerList.get(position).setTlsPort(0);
                    remoteServerList.get(position).setSshPrivKey(Constants.SERVER_STATUS_SHUTOFF);
                    if (listener != null) {
                        listener.onHttpSuccess(url);
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }
            }
        }).start();
    }

    public void restartRemoteServer(final int position, final int type) {
        {
            System.out.println("====restartRemoteServer()=====" + pwd + "=======" + position);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String serverId = remoteServerList.get(position).getSshPubKey();
                    projectId = remoteServerList.get(position).getSshUser();
                    //获取服务端Token
                    String url = SERVER + PORT_1 + URL_GET_USERID;
                    String data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
                            + "\", " + "\"password\": \"" + pwd + "\", \"domain\": {\"name\": \"Default\"}}}}, "
                            + "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
                    getServerToken(url, data);

                    //重启虚拟机
                    url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_ADDRESS_A + serverId + URL_ACTION_SERVER_START;
                    if (type == 1) {
                        //软重启
                        data = "{\"reboot\" : {\"type\" : \"SOFT\"}}";
                    } else {
                        //硬重启
                        data = "{\"reboot\" : {\"type\" : \"HARD\"}}";
                    }
                    int code = httpPost3(url, data);
                    if (code >= 200 && code < 300) {
                        if (listener != null) {
                            listener.onHttpSuccess(url);
                        }
                    } else {
                        if (listener != null) {
                            listener.onHttpFailed(url, 400);
                        }
                    }
                }
            }).start();
        }
    }

    public void shutdownRemoteServer(final int position) {
        System.out.println("====shutdownRemoteServer()=====" + pwd + "=======" + position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverId = remoteServerList.get(position).getSshPubKey();
                projectId = remoteServerList.get(position).getSshUser();
                //获取服务端Token
                String url = SERVER + PORT_1 + URL_GET_USERID;
                String data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
                        + "\", " + "\"password\": \"" + pwd + "\", \"domain\": {\"name\": \"Default\"}}}}, "
                        + "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
                getServerToken(url, data);

                //关闭虚拟机
                url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_ADDRESS_A + serverId + URL_ACTION_SERVER_START;
                data = "{\"os-stop\": null}";
                int code = httpPost3(url, data);
                if (code >= 200 && code < 300) {
                    remoteServerList.get(position).setAddress("0.0.0.0");
                    remoteServerList.get(position).setPort(0);
                    remoteServerList.get(position).setTlsPort(0);
                    remoteServerList.get(position).setSshPrivKey(Constants.SERVER_STATUS_SHUTOFF);
                    if (listener != null) {
                        listener.onHttpSuccess(url);
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }
            }
        }).start();
    }

    public void startRemoteServer(final int position) {
        System.out.println("====startRemoteServer()=====" + pwd + "=======" + position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverId = remoteServerList.get(position).getSshPubKey();
                projectId = remoteServerList.get(position).getSshUser();
                //获取服务端Token
                String url = SERVER + PORT_1 + URL_GET_USERID;
                String data = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"id\": \"" + userId
                        + "\", " + "\"password\": \"" + pwd + "\", \"domain\": {\"name\": \"Default\"}}}}, "
                        + "\"scope\": { \"project\": {\"id\": \"" + projectId + "\"}}}}";
                getServerToken(url, data);

                //启动虚拟机
                url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_ADDRESS_A + serverId + URL_ACTION_SERVER_START;
                data = "{\"os-start\": null}";
                int code = httpPost3(url, data);
                if (code >= 200 && code < 300) {
                    if (listener != null) {
                        listener.onHttpSuccess(url);
                    }
                } else {
                    if (listener != null) {
                        listener.onHttpFailed(url, 400);
                    }
                }
            }
        }).start();
    }

    public void refreshServerStatus(final int position, final boolean updatePort) {
        System.out.println("====refreshServerStatus()=====" + pwd + "=======" + position + "====" + updatePort);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverId = remoteServerList.get(position).getSshPubKey();
                String url = "";
                String response = "";
                try {
                    if (!updatePort) {
                        //Status
                        url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_ADDRESS_A + serverId;
                        if (listener != null) {
                            listener.onHttpSuccess(url);
                        }
                        System.out.println("=======shutdown and return=====");
                        return;
                    }

                    //Update Port
                    String data = "{\"remote_console\": {\"type\": \"spice-html5\", \"protocol\": \"spice\"}}";
                    url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId
                            + URL_GET_SERVER_ADDRESS_A + serverId + URL_GET_SERVER_ADDRESS_B;
                    response = httpPost2(url, data);
                    if (!TextUtils.isEmpty(response)) {
                        JSONObject lastObj = new JSONObject(response);
                        if (lastObj.has("remote_console")) {
                            JSONObject myObj = lastObj.getJSONObject("remote_console");
                            if (myObj.has("host") && myObj.has("port")) {
                                String host = myObj.getString("host");
                                String port = myObj.getString("port");
                                String tlsPort=myObj.getString("tlsPort");
                                System.out.println("=====" + host + "======" + port+"====="+tlsPort);

                                if (!TextUtils.isEmpty(host)) {
                                    remoteServerList.get(position).setAddress(host);
                                }

                                String reg = "^[0-9]+";
                                Pattern pattern = Pattern.compile(reg);
                                Matcher matcher = pattern.matcher(port);
                                if (port != null && matcher.find()) {
                                    remoteServerList.get(position).setPort(Integer.parseInt(port));
                                } else {
                                    remoteServerList.get(position).setPort(0);
                                }

                                matcher = pattern.matcher(tlsPort);
                                if(tlsPort != null && matcher.find())
                                {
                                    int numTlsPort=Integer.parseInt(tlsPort);
                                    remoteServerList.get(position).setTlsPort(numTlsPort);
                                }
                                else
                                {
                                    remoteServerList.get(position).setTlsPort(0);
                                }

                                if(remoteServerList.get(position).getPort()==0 && remoteServerList.get(position).getTlsPort()==0)
                                {
                                    if (listener != null) {
                                        listener.onHttpFailed(url,400);
                                    }
                                    return;
                                }

                                if (listener != null) {
                                    listener.onHttpSuccess(url);
                                }
                            } else {
                                if (listener != null) {
                                    listener.onHttpFailed(url, 400);
                                }
                            }
                        } else {
                            if (listener != null) {
                                listener.onHttpFailed(url, 400);
                            }
                        }
                    } else {
                        if (listener != null) {
                            listener.onHttpFailed(url, 400);
                        }
                    }

                    //Update Status
                    url = SERVER + PORT_2 + URL_GET_SERVER_LIST_A + projectId + URL_GET_SERVER_ADDRESS_A + serverId;
                    response = httpGet2(url);
                    if (!TextUtils.isEmpty(response)) {
                        JSONObject object = new JSONObject(response);
                        if (object.has("server")) {
                            JSONObject serverObj = object.getJSONObject("server");
                            if (serverObj.has("status")) {
                                String status = serverObj.getString("status");
                                System.out.println("=======Server==Status=====" + status);
                                remoteServerList.get(position).setSshPrivKey(status);
                                if (listener != null) {
                                    listener.onHttpSuccess(url);
                                }
                            } else {
                                if (listener != null) {
                                    listener.onHttpFailed(url, 400);
                                }
                            }
                        } else {
                            if (listener != null) {
                                listener.onHttpFailed(url, 400);
                            }
                        }
                    } else {
                        if (listener != null) {
                            listener.onHttpFailed(url, 400);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void clearNativeCache() {
        System.out.println("======clearNativeCache()======");
        CONNECTION_TYPE=-1;
        TOKEN_VALUE = "";
        pwd = "";
        userId = "";
        projectId = "";
        remoteServerList=null;
    }

    private String httpGet2(String url) {
        String response = "";
        HttpURLConnection urlConnection = null;
        URL netUrl = null;
        try {
            System.out.println("=====httpGet()===url========" + url);
            netUrl = new URL(url);
            urlConnection = (HttpURLConnection) netUrl.openConnection();
            // urlConnection.setDoOutput(true);
            // urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(60000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("User-Agent", "python-novaclient");
            urlConnection.setRequestProperty("X-OpenStack-Nova-API-Version", "2.25");
            urlConnection.setRequestProperty(TOKEN_KEY, TOKEN_VALUE);
            System.out.println("===========Token=====" + TOKEN_VALUE);
            urlConnection.connect();
            System.out.println("====getResponseCode====" + urlConnection.getResponseCode());
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream in = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line = null;
                StringBuffer buffer = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    buffer.append(line);
                }
                in.close();
                br.close();
                response = buffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }

        System.out.println("====content====" + response);
        return response;
    }

    private String httpGet(String url) {
        String response = "";
        HttpURLConnection urlConnection = null;
        URL netUrl = null;
        try {
            System.out.println("=====httpGet()===url========" + url);
            netUrl = new URL(url);
            urlConnection = (HttpURLConnection) netUrl.openConnection();
            // urlConnection.setDoOutput(true);
            // urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(60000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("User-Agent", "jiuzhoutech");
            urlConnection.setRequestProperty(TOKEN_KEY, TOKEN_VALUE);
            System.out.println("===========Token=====" + TOKEN_VALUE);
            urlConnection.connect();
            System.out.println("====getResponseCode====" + urlConnection.getResponseCode());
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream in = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line = null;
                StringBuffer buffer = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    buffer.append(line);
                }
                in.close();
                br.close();
                response = buffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }

        System.out.println("====content====" + response);
        return response;
    }

    private String httpPost2(String url, String data) {
        String responseBody = "";
        HttpURLConnection connection = null;
        try {
            System.out.println("======httpPost()===" + url);
            URL netUrl = new URL(url);
            connection = (HttpURLConnection) netUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "python-novaclient");
            connection.setRequestProperty("X-OpenStack-Nova-API-Version", "2.25");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty(TOKEN_KEY, TOKEN_VALUE);
            System.out.println("===========Token=====" + TOKEN_VALUE);
            connection.connect();
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            System.out.println("===senddata====" + data);
            out.writeBytes(data);
            out.flush();
            out.close();

            int httpCode = connection.getResponseCode();
            System.out.println("=====getResponseCode=====" + httpCode);
            if (httpCode >= HttpURLConnection.HTTP_OK && httpCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String lines;
                StringBuffer sb = new StringBuffer("");
                while ((lines = reader.readLine()) != null) {
                    sb.append(lines);
                }
                responseBody = sb.toString();
                System.out.println("====content=====" + responseBody);
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return responseBody;
    }

    private int httpPost3(String url, String data) {
        int httpCode = -1;
        HttpURLConnection connection = null;
        try {
            System.out.println("======httpPost()===" + url);
            URL netUrl = new URL(url);
            connection = (HttpURLConnection) netUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "python-novaclient");
            connection.setRequestProperty("X-OpenStack-Nova-API-Version", "2.25");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty(TOKEN_KEY, TOKEN_VALUE);
            System.out.println("===========Token=====" + TOKEN_VALUE);
            connection.connect();
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            System.out.println("===senddata====" + data);
            out.writeBytes(data);
            out.flush();
            out.close();
            httpCode = connection.getResponseCode();
            System.out.println("=====getResponseCode=====" + httpCode);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return httpCode;
    }

    private String httpPost(String url, String data) {
        String responseBody = "";
        HttpURLConnection connection = null;
        try {
            System.out.println("======httpPost()===" + url);
            URL netUrl = new URL(url);
            connection = (HttpURLConnection) netUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty(TOKEN_KEY, TOKEN_VALUE);
            System.out.println("===========Token=====" + TOKEN_VALUE);
            connection.connect();
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            System.out.println("===senddata====" + data);
            out.writeBytes(data);
            out.flush();
            out.close();

            int httpCode = connection.getResponseCode();
            System.out.println("=====getResponseCode=====" + httpCode);
            if (httpCode >= HttpURLConnection.HTTP_OK && httpCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String lines;
                StringBuffer sb = new StringBuffer("");
                while ((lines = reader.readLine()) != null) {
                    sb.append(lines);
                }
                responseBody = sb.toString();
                System.out.println("====content=====" + responseBody);
                Set<String> headerKeys = connection.getHeaderFields().keySet();
                Iterator<String> it = headerKeys.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    System.out.println("=======" + key + "========" + connection.getHeaderField(key));
                    if (key != null && !TextUtils.isEmpty(key)) {
                        if (SERVER_TOKEN_KEY.equals(key)) {
                            TOKEN_VALUE = connection.getHeaderField(key);
                            System.out.println("====update token=====" + TOKEN_VALUE);
                        }
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            responseBody="-1";
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return responseBody;
    }

    public static interface HttpProcessInterface {
        public void onHttpSuccess(String url);

        public void onHttpFailed(String url, int httpCode);
    }
}