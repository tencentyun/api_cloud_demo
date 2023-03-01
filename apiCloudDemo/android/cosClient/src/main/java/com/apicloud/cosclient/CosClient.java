/**
 * Copyright (C), 2015-2022, XXX有限公司
 * FileName: CosClient
 * Author: garenwang
 * Date: 2022/12/30 15:21
 * Description:
 * History:
 * <author> <time> <version> <desc>
 * 作者姓名 修改时间 版本号 描述
 */
package com.apicloud.cosclient;

import android.content.Context;
import android.net.wifi.hotspot2.pps.Credential;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.cos.xml.CosXmlBaseService;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.common.COSStorageClass;
import com.tencent.cos.xml.common.ClientErrorCode;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.bucket.DeleteBucketRequest;
import com.tencent.cos.xml.model.bucket.GetBucketRequest;
import com.tencent.cos.xml.model.bucket.GetBucketResult;
import com.tencent.cos.xml.model.bucket.PutBucketRequest;
import com.tencent.cos.xml.model.object.DeleteObjectRequest;
import com.tencent.cos.xml.model.object.GetObjectRequest;
import com.tencent.cos.xml.model.object.HeadObjectRequest;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.model.service.GetServiceRequest;
import com.tencent.cos.xml.model.service.GetServiceResult;
import com.tencent.cos.xml.model.tag.InitiateMultipartUpload;
import com.tencent.cos.xml.model.tag.ListAllMyBuckets;
import com.tencent.cos.xml.model.tag.ListBucket;
import com.tencent.cos.xml.transfer.COSXMLDownloadTask;
import com.tencent.cos.xml.transfer.COSXMLTask;
import com.tencent.cos.xml.transfer.COSXMLUploadTask;
import com.tencent.cos.xml.transfer.InitMultipleUploadListener;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.qcloud.core.auth.BasicLifecycleCredentialProvider;
import com.tencent.qcloud.core.auth.QCloudCredentialProvider;
import com.tencent.qcloud.core.auth.QCloudLifecycleCredentials;
import com.tencent.qcloud.core.auth.SessionQCloudCredentials;
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @ClassName: CosClient
 * @Description: java类作用描述
 * @Author: garenwang
 * @Date: 2022/12/30 15:21
 */
public class CosClient extends UZModule {


    private static final String TAG = "CosClient";
    private static final String CLIENT_VERSION = "1.0.1";
    private static final String DEFAULT_KEY = "";
    private final static Map<String, CosXmlService> cosServices = new HashMap<>();
    private final static Map<String, TransferManager> transferManagers = new HashMap<>();
    private final Map<String, COSXMLTask> taskMap = new HashMap<>();

    private String secretID;
    private String secretKey;

    public CosClient(UZWebView webView) {
        super(webView);
    }


    public void jsmethod_setupPermanentCredentail(UZModuleContext moduleContext){
        secretID = moduleContext.optString("secretID");
        secretKey = moduleContext.optString("secretKey");
    }

    public void jsmethod_registerServiceForKey(final UZModuleContext moduleContext) {
        String serviceKey = moduleContext.optString("serviceKey");
        if(serviceKey.isEmpty()){
            if (cosServices.get(DEFAULT_KEY) == null){
                cosServices.put(DEFAULT_KEY, buildCosXmlService(moduleContext.getContext(), moduleContext));
            }
        }else{
            if (cosServices.get(serviceKey) == null){
                cosServices.put(serviceKey, buildCosXmlService(moduleContext.getContext(), moduleContext));
            }
        }

    }

    public void jsmethod_registerTransferManger(final UZModuleContext moduleContext) {
        String serviceKey = moduleContext.optString("serviceKey");
        if (serviceKey.isEmpty()) {
            if (transferManagers.get(DEFAULT_KEY) == null){
                transferManagers.put(DEFAULT_KEY, buildTransferManager(moduleContext.getContext(), moduleContext));
            }
        }else{
            if (transferManagers.get(serviceKey) == null){
                transferManagers.put(serviceKey, buildTransferManager(moduleContext.getContext(), moduleContext));
            }
        }

    }

    private CosXmlService buildCosXmlService(Context context, final UZModuleContext moduleContext) {
        CosXmlServiceConfig.Builder serviceConfigBuilder = new CosXmlServiceConfig.Builder();
        if (!moduleContext.optString("region") .isEmpty()) {
            serviceConfigBuilder.setRegion(moduleContext.optString("region"));
        }
        if (moduleContext.optInt("timeOut") != 0) {
            serviceConfigBuilder.setConnectionTimeout(moduleContext.optInt("timeOut"));
        }
        if (moduleContext.optBoolean("useHttps")) {
            serviceConfigBuilder.isHttps(moduleContext.optBoolean("useHttps"));
        }

        if (!moduleContext.optString("host").isEmpty()) {
            serviceConfigBuilder.setHost(moduleContext.optString("host"));
        }

        if (moduleContext.optInt("port") != 0) {
            serviceConfigBuilder.setPort(moduleContext.optInt("port"));
        }
        String userAgent = "apicloud_android";
        if (!moduleContext.optString("userAgent").isEmpty()) {
            userAgent += "-";
            userAgent += moduleContext.optString("userAgent");
        }
        userAgent = userAgent + "-" + CLIENT_VERSION;
        serviceConfigBuilder.setUserAgentExtended(userAgent);

        return new CosXmlService(context,
                serviceConfigBuilder.builder(), getQCloudCredentialProvider());
    }

    private TransferManager buildTransferManager(Context context,final UZModuleContext moduleContext) {
        TransferConfig.Builder builder = new TransferConfig.Builder();
        CosXmlService cosXmlService = buildCosXmlService(context, moduleContext);
        return new TransferManager(cosXmlService, builder.build());
    }


    private static CosXmlService getCosXmlService(String serviceKey) {
        return cosServices.get(serviceKey);
    }

    private static TransferManager getTransferManager(String transferKey) {
        return transferManagers.get(transferKey);
    }

    private void runMainThread(Runnable runnable){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(runnable);
    }

    private QCloudCredentialProvider getQCloudCredentialProvider() {
        if(secretID != null && secretKey != null){
            return new ShortTimeCredentialProvider(
                    secretID,
                    secretKey,
                    300
            );
        }else{
            return new BasicLifecycleCredentialProvider() {
                @Override
                protected QCloudLifecycleCredentials fetchNewCredentials() throws CosXmlClientException {
                    CountDownLatch countDownLatch = new CountDownLatch(1);
                    final Object[] result = new Object[1];
                    //此处调用有可能不是在主线程中 需要切换到主线程 因为调用flutter只能在主线程
                    runMainThread(()->{
                        execScript("refreshCredentail()", new ValueCallback() {
                            @Override
                            public void onReceiveValue(Object value) {
                                if (value .getClass() == String.class){
                                    String resultString = (String) value;
                                    resultString = resultString.replaceAll("\"","");
                                    String[] temps = resultString.split("&");
                                    com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
                                    for (String temp : temps) {
                                        String[] infos = temp.split("=");
                                        if(infos.length == 2){
                                            if(infos[0].equals("secretID")){
                                                jsonObject.put("secretID",infos[1]);
                                            }
                                            if(infos[0].equals("secretKey")){
                                                jsonObject.put("secretKey",infos[1]);
                                            }
                                            if(infos[0].equals("token")){
                                                jsonObject.put("token",infos[1]);
                                            }
                                            if(infos[0].equals("startDate")){
                                                jsonObject.put("startDate",infos[1]);
                                            }
                                            if(infos[0].equals("expirationDate")){
                                                jsonObject.put("expirationDate",infos[1]);
                                            }
                                        }
                                    }
                                    result[0] = jsonObject;
                                }
                                countDownLatch.countDown();
                            }
                        });
                    });

                    try {
                        countDownLatch.await();
                        com.alibaba.fastjson.JSONObject credentials = (com.alibaba.fastjson.JSONObject)result[0];
                        Log.e("credentials",credentials.toString());
                        String startDate = credentials.getString("startDate");
                        if (startDate == null) {
                            return new SessionQCloudCredentials(
                                    credentials.getString("secretID"),
                                    credentials.getString("secretKey"),
                                    credentials.getString("token"),
                                    Long.parseLong(credentials.getString("expirationDate")));
                        } else {
                            return new SessionQCloudCredentials(
                                    credentials.getString("secretID"),
                                    credentials.getString("secretKey"),
                                    credentials.getString("token"),
                                    Long.parseLong(startDate),
                                    Long.parseLong(credentials.getString("expirationDate")));
                        }

                    } catch ( InterruptedException e) {
                        e.printStackTrace();
                        throw new CosXmlClientException(ClientErrorCode.INVALID_CREDENTIALS.getCode(), e);
                    }
                }
            };
        }
    }

    public void jsmethod_createBucket(final UZModuleContext moduleContext){
        String region = moduleContext.optString("region");
        String name = moduleContext.optString("name");
        String serviceKey = moduleContext.optString("serviceKey");
        String accessControlList = moduleContext.optString("accessControlList");
        String readAccount = moduleContext.optString("readAccount");
        String writeAccount = moduleContext.optString("writeAccount");
        String readWriteAccount = moduleContext.optString("readWriteAccount");
        Boolean enableMAZ = moduleContext.optBoolean("enableMAZ");
        String appId = moduleContext.optString("appId");
        CosXmlService service = getCosXmlService(serviceKey);

        if(service == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }

        PutBucketRequest request = new PutBucketRequest(name + "-" + appId);
        if (!region.isEmpty()) {
            request.setRegion(region);
        }

        request.enableMAZ(enableMAZ);

        if (!accessControlList.isEmpty()) {
            request.setXCOSACL(accessControlList);
        }
        if (!readAccount.isEmpty()) {
            request.setXCOSGrantRead(readAccount);
        }
        if (!writeAccount.isEmpty()) {
            request.setXCOSGrantWrite(writeAccount);
        }
        if (!readWriteAccount.isEmpty()) {
            request.setXCOSReadWrite(readWriteAccount);
        }
        service.putBucketAsync(request, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","success");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(result,true);
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                handleError(moduleContext,e,e1);
            }
        });
    }

    public void jsmethod_getBucketList(final UZModuleContext moduleContext){
        String serviceKey = moduleContext.optString("serviceKey");
        CosXmlService service = getCosXmlService(serviceKey);
        if(service == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        GetServiceRequest request = new GetServiceRequest();
        service.getServiceAsync(request, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                try {
                    ListAllMyBuckets listAllMyBuckets = ((GetServiceResult) cosXmlResult).listAllMyBuckets;
                    JSONObject result = new JSONObject();
                    result.put("result","success");
                    result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(listAllMyBuckets));
                    moduleContext.success(result,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                handleError(moduleContext,e,e1);
            }
        });
    }

    public void jsmethod_listBucketContent(final UZModuleContext moduleContext){

        String region = moduleContext.optString("region");
        String bucket = moduleContext.optString("bucket");
        String serviceKey = moduleContext.optString("serviceKey");
        String prefix = moduleContext.optString("prefix");
        String delimiter = moduleContext.optString("delimiter");
        String marker = moduleContext.optString("marker");
        long maxKeys = moduleContext.optLong("maxKeys");

        CosXmlService service = getCosXmlService(serviceKey);
        if(service == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        GetBucketRequest request = new GetBucketRequest(bucket);
        if (!region.isEmpty()) {
            request.setRegion(region);
        }
        if (!prefix.isEmpty()) {
            request.setPrefix(prefix);
        }
        if (!delimiter.isEmpty()) {
            request.setDelimiter(delimiter);
        }

        if (!marker.isEmpty()) {
            request.setMarker(marker);
        }
        if (maxKeys != 0) {
            request.setMaxKeys(maxKeys);
        }
        service.getBucketAsync(request, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                try {
                    ListBucket listBucket = ((GetBucketResult) cosXmlResult).listBucket;
                    JSONObject result = new JSONObject();
                    try {
                        result.put("result","success");
                        result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(listBucket));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    moduleContext.success(result,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                handleError(moduleContext,e,e1);
            }
        });
    }

    public void jsmethod_deleteBucket(final UZModuleContext moduleContext){

        String region = moduleContext.optString("region");
        String bucket = moduleContext.optString("bucket");
        String serviceKey = moduleContext.optString("serviceKey");

        CosXmlService service = getCosXmlService(serviceKey);
        if(service == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        DeleteBucketRequest request = new DeleteBucketRequest(bucket);
        if (!region .isEmpty()) {
            request.setRegion(region);
        }
        service.deleteBucketAsync(request, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","success");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(result,true);
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                handleError(moduleContext,e,e1);
            }
        });
    }

    public void jsmethod_uploadObject(final UZModuleContext moduleContext){

        String region = moduleContext.optString("region");
        String filePath = moduleContext.optString("filePath");
        String bucket = moduleContext.optString("bucket");
        String object = moduleContext.optString("object");
        String serviceKey = moduleContext.optString("serviceKey");
        String stroageClass = moduleContext.optString("stroageClass");
        String uploadId = moduleContext.optString("uploadId");
        int trafficLimit = moduleContext.optInt("trafficLimit");
        filePath = this.makeRealPath(filePath);
        TransferManager transferManager = getTransferManager(serviceKey);
        if(transferManager == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：传输服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        PutObjectRequest request = new PutObjectRequest(bucket, object, filePath);
        if (!region.isEmpty()) {
            request.setRegion(region);
        }
        if (!stroageClass.isEmpty()) {
            request.setStroageClass(COSStorageClass.fromString(stroageClass));
        }

        if (trafficLimit != 0){
            request.setTrafficLimit(trafficLimit);
        }

        COSXMLUploadTask task = transferManager.upload(request, uploadId);

        task.setCosXmlProgressListener(new CosXmlProgressListener() {
            @Override
            public void onProgress(long l, long l1) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","processing");
                    com.alibaba.fastjson.JSONObject data = new com.alibaba.fastjson.JSONObject();
                    data.put("totalBytesSent",l);
                    data.put("totalBytesExpectedToSend",l1);
                    result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(data));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i("uploadObject",result.toString());
                moduleContext.success(result,false);
            }
        });

        task.setCosXmlResultListener(new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                JSONObject result = new JSONObject();
                try {
                    COSXMLUploadTask.COSXMLUploadTaskResult taskResult = (COSXMLUploadTask.COSXMLUploadTaskResult)cosXmlResult;
                    result.put("result","success");

                    com.alibaba.fastjson.JSONObject data = new com.alibaba.fastjson.JSONObject();
                    data.put("eTag",taskResult.eTag);
                    data.put("url",taskResult.accessUrl);
                    for (String key : cosXmlResult.headers.keySet()){
                        ArrayList item = (ArrayList)cosXmlResult.headers.get(key);
                        if(item.size() ==1){
                            data.put(key,item.get(0));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                taskMap.remove(task.hashCode());
                Log.i("uploadObject",result.toString());
                moduleContext.success(result,true);
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                taskMap.remove(task.hashCode());
                handleError(moduleContext,e,e1);
            }
        });

        String taskKey = String.valueOf(task.hashCode());
        taskMap.put(taskKey, task);
        task.setInitMultipleUploadListener(new InitMultipleUploadListener() {
            @Override
            public void onSuccess(InitiateMultipartUpload initiateMultipartUpload) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","begin");
                    com.alibaba.fastjson.JSONObject data = new com.alibaba.fastjson.JSONObject();
                    data.put("taskId",taskKey);
                    data.put("uploadId",initiateMultipartUpload.uploadId);
                    result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(data));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(result,false);
            }
        });
    }

    public void jsmethod_pauseUploadObject(final UZModuleContext moduleContext){
        String taskId = moduleContext.optString("taskId");
        COSXMLTask task = taskMap.get(taskId);
        if (task != null) {
            task.cancel();
            JSONObject result = new JSONObject();
            try {
                result.put("result","success");
            } catch (JSONException exception) {
            }
            moduleContext.success(result,true);
        } else {
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
            } catch (JSONException exception) {
            }
            moduleContext.error(result,true);
        }
    }

    public void jsmethod_downloadObject(final UZModuleContext moduleContext){
        String region = moduleContext.optString("region");
        String localPath = moduleContext.optString("localPath");
        String bucket = moduleContext.optString("bucket");
        String object = moduleContext.optString("object");
        String versionId = moduleContext.optString("versionId");
        String serviceKey = moduleContext.optString("serviceKey");
        localPath = this.makeRealPath(localPath);
        int trafficLimit = moduleContext.optInt("trafficLimit");
        TransferManager transferManager = getTransferManager(serviceKey);
        if(transferManager == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：传输服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        int separator = localPath.lastIndexOf("/");

        GetObjectRequest request = new GetObjectRequest(bucket, object,
                localPath.substring(0, separator + 1),
                localPath.substring(separator + 1));
        if (!region.isEmpty()) {
            request.setRegion(region);
        }
        if (!versionId.isEmpty()) {
            request.setVersionId(versionId);
        }

        if (trafficLimit != 0){
            request.setTrafficLimit(trafficLimit);
        }

        COSXMLDownloadTask task = transferManager.download(moduleContext.getContext(), request);

        task.setCosXmlProgressListener(new CosXmlProgressListener() {
            @Override
            public void onProgress(long l, long l1) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","processing");
                    com.alibaba.fastjson.JSONObject data = new com.alibaba.fastjson.JSONObject();
                    data.put("totalBytesDownload",l);
                    data.put("totalBytesExpectedToDownload",l1);

                    result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(data));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i("downloadObject",result.toString());
                moduleContext.success(result,false);
            }
        });

        task.setCosXmlResultListener(new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","success");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                taskMap.remove(task.hashCode());
                moduleContext.success(result,true);
                Log.i("downloadObject",result.toString());
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                taskMap.remove(task.hashCode());
                handleError(moduleContext,e,e1);
            }
        });

        String taskKey = String.valueOf(task.hashCode());
        taskMap.put(taskKey, task);

        JSONObject result = new JSONObject();
        try {
            result.put("result","begin");
            com.alibaba.fastjson.JSONObject data = new com.alibaba.fastjson.JSONObject();
            data.put("taskId",taskKey);
            result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(data));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("downloadObject",result.toString());
        moduleContext.success(result,false);
    }

    public void jsmethod_pauseDownloadObject(final UZModuleContext moduleContext){
        String taskId = moduleContext.optString("taskId");
        Boolean deleteLocalFile = moduleContext.optBoolean("deleteLocalFile");
        COSXMLTask task = taskMap.get(taskId);
        if (task != null) {
            if (deleteLocalFile){
                task.cancel();
            }else{
                task.pause();
            }
            JSONObject result = new JSONObject();
            try {
                result.put("result","success");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            moduleContext.success(result,true);
        } else {
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            moduleContext.error(result,true);
        }
    }

    public void jsmethod_deleteObject(final UZModuleContext moduleContext){
        String serviceKey = moduleContext.optString("serviceKey");
        String bucket = moduleContext.optString("bucket");
        String object = moduleContext.optString("object");
        String region = moduleContext.optString("region");
        String versionId = moduleContext.optString("versionId");

        CosXmlService service = getCosXmlService(serviceKey);
        if(service == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(
                bucket, object);
        if (region != null) {
            deleteObjectRequest.setRegion(region);
        }
        if (versionId != null) {
            deleteObjectRequest.setVersionId(versionId);
        }

        service.deleteObjectAsync(deleteObjectRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                JSONObject result = new JSONObject();
                try {
                    result.put("result","success");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                moduleContext.success(result,true);
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                handleError(moduleContext,e,e1);
            }
        });
    }

    public void jsmethod_headObject(final UZModuleContext moduleContext){
        String serviceKey = moduleContext.optString("serviceKey");
        String bucket = moduleContext.optString("bucket");
        String object = moduleContext.optString("object");
        String region = moduleContext.optString("region");
        String versionId = moduleContext.optString("versionId");
        CosXmlService service = getCosXmlService(serviceKey);
        if(service == null){
            JSONObject result = new JSONObject();
            try {
                result.put("result","error");
                com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
                error.put("message",(serviceKey.isEmpty() ? "默认" : serviceKey) + "：服务没有注册，请注册后再进行网络请求");
                error.put("errorCode","");
                error.put("requestId","");
                result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            moduleContext.error(result,true);
            return;
        }
        HeadObjectRequest headObjectRequest = new HeadObjectRequest(
                bucket, object);
        if (region != null) {
            headObjectRequest.setRegion(region);
        }
        if (versionId != null) {
            headObjectRequest.setVersionId(versionId);
        }
        service.headObjectAsync(headObjectRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest cosXmlRequest, CosXmlResult cosXmlResult) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("result","success");
                    com.alibaba.fastjson.JSONObject data = new com.alibaba.fastjson.JSONObject();
                    for (String key : cosXmlResult.headers.keySet()){
                        ArrayList item = (ArrayList)cosXmlResult.headers.get(key);
                        if(item.size() ==1){
                            data.put(key,item.get(0));
                        }
                    }
                    result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(data));
                    moduleContext.success(result,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, @Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1) {
                handleError(moduleContext,e,e1);
            }
        });
    }

    private void handleError(UZModuleContext moduleContext,@Nullable CosXmlClientException e, @Nullable CosXmlServiceException e1){

        JSONObject result = new JSONObject();
        try {
            result.put("result","error");
            com.alibaba.fastjson.JSONObject error = new com.alibaba.fastjson.JSONObject();
            error.put("message",e==null?(e1.getErrorMessage()!= null?e1.getErrorMessage():e1.getHttpMessage()):e.getMessage());
            error.put("errorCode",e==null?(e1.getErrorCode()!= null?e1.getErrorCode():e1.getStatusCode()):e.errorCode);
            error.put("requestId",e==null?e1.getRequestId():"");
            result.put("data", com.alibaba.fastjson.JSONObject.toJSONString(error));
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
        moduleContext.error(result,true);
    }

    public void jsmethod_cancelAll(final UZModuleContext moduleContext){
        String serviceKey = moduleContext.optString("serviceKey");
        CosXmlService service = getCosXmlService(serviceKey);
        service.cancelAll();
    }
}
