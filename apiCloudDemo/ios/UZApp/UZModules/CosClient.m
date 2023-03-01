//
//  CosClient.m
//  UZModule
//
//

#import "CosClient.h"
#import "NSDictionaryUtils.h"
#import <QCloudCOSXML/QCloudCOSXML.h>
#import <QCloudCore/QCloudConfiguration_Private.h>
#import "CredentailProvider.h"

#define CLIENT_VERSION @"1.0.1"
static CosClient* _instance;

@interface CosClient ()<CredentailProviderDelegate>

@end

@implementation CosClient

- (void)refreshCredentail{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self evalJs:@"refreshCredentail()" completionHandler:^(NSString * result, NSError *error) {
            NSArray * temps = [result componentsSeparatedByString:@"&"];
            
            QCloudCredential * credential = [[QCloudCredential alloc]init];
            for (NSString * temp in temps) {
                NSArray<NSString *> * infos = [temp componentsSeparatedByString:@"="];
                if(infos.count == 2){
                    if([infos.firstObject isEqualToString:@"secretID"]){
                        credential.secretID = infos.lastObject;
                    }
                    
                    if([infos.firstObject isEqualToString:@"secretKey"]){
                        credential.secretKey = infos.lastObject;
                    }
                    
                    if([infos.firstObject isEqualToString:@"token"]){
                        credential.token = infos.lastObject;
                    }
                    
                    if([infos.firstObject isEqualToString:@"startDate"]){
                        credential.startDate =[NSDate dateWithTimeIntervalSince1970:infos.lastObject.longLongValue];
                    }
                    
                    if([infos.firstObject isEqualToString:@"expirationDate"]){
                        credential.expirationDate = [NSDate dateWithTimeIntervalSince1970:infos.lastObject.longLongValue];
                    }
                }
            }
            CredentailProvider.provider.credential = credential;
        }];
    });
    
}

#pragma mark - Override
+ (void)onAppLaunch:(NSDictionary *)launchOptions {

}

- (id)initWithUZWebView:(UZWebView *)webView {
    if (self = [super initWithUZWebView:webView]) {
        CredentailProvider.provider.delegate = self;
    }
    return self;
}

- (void)dispose {
    // 方法在模块销毁之前被调用
}

-(QCloudCOSXMLService *)getServiceForKey:(NSString *)serviceKey{
    QCloudCOSXMLService * service = [QCloudCOSXMLService defaultCOSXML];
    if(serviceKey.length != 0){
        if([QCloudCOSXMLService hasCosxmlServiceForKey:serviceKey]){
            service = [QCloudCOSXMLService cosxmlServiceForKey:serviceKey];
        }
    }
    return service;
}

-(QCloudCOSTransferMangerService *)getTransferServiceForKey:(NSString *)serviceKey{
    QCloudCOSTransferMangerService * service = [QCloudCOSTransferMangerService defaultCOSTransferManager];
    if(serviceKey.length != 0){
        if([QCloudCOSTransferMangerService hasTransferMangerServiceForKey:serviceKey]){
            service = [QCloudCOSTransferMangerService costransfermangerServiceForKey:serviceKey];
        }
    }
    return service;
}

JS_METHOD(setupPermanentCredentail:(UZModuleMethodContext *)context) {
    NSDictionary * dic = context.param;
    CredentailProvider.provider.secretID = dic[@"secretID"];
    CredentailProvider.provider.secretKey =  dic[@"secretKey"];
}

//suffix serviceName isPrefixURL regionName appId serviceKey useHttps
JS_METHOD(registerServiceForKey:(UZModuleMethodContext *)context) {
    NSString * serviceKey = context.param[@"serviceKey"];
    NSString * appId = context.param[@"appId"];
    NSString * region = context.param[@"region"];
    BOOL isPrefixURL = context.param[@"isPrefixURL"] == nil?YES:[context.param[@"isPrefixURL"] boolValue];
    NSInteger timeOut = [context.param[@"timeOut"] integerValue];
    NSString * serviceName = context.param[@"serviceName"];
    NSString * suffix = context.param[@"suffix"];
    BOOL useHttps = context.param[@"useHttps"] == nil ? YES :[context.param[@"useHttps"] boolValue];

    QCloudServiceConfiguration* configuration = [QCloudServiceConfiguration new];
    
    NSString * userAgent = @"cos-xml-ios-sdk-v6.1.7-apicloud_ios";
    NSString *customAgent = context.param[@"userAgent"];
    if(customAgent.length != 0){
        userAgent = [userAgent stringByAppendingString:@"-"];
        userAgent =[userAgent stringByAppendingString:customAgent];
    }
    configuration.userAgentProductKey = userAgent;
    configuration.productVersion = CLIENT_VERSION;
    QCloudCOSXMLEndPoint* endpoint = [[QCloudCOSXMLEndPoint alloc] init];
    endpoint.useHTTPS = useHttps;
    if(serviceName.length > 0){
        endpoint.serviceName = serviceName;
    }
    endpoint.regionName = region;
    endpoint.isPrefixURL = isPrefixURL;
    endpoint.suffix = suffix;
    configuration.endpoint = endpoint;
    configuration.appID = appId;
    if(timeOut != 0){
        configuration.timeoutInterval = timeOut;
    }
    
    configuration.signatureProvider = CredentailProvider.provider;
    if(serviceKey .length == 0){
        @try {
            [QCloudCOSXMLService defaultCOSXML];
        } @catch (NSException *exception) {
            [QCloudCOSXMLService registerDefaultCOSXMLWithConfiguration:configuration];
        }
    }else{
        if(![QCloudCOSXMLService hasCosxmlServiceForKey:serviceKey]){
            [QCloudCOSXMLService registerCOSXMLWithConfiguration:configuration withKey:serviceKey];
        }
        
    }
    
}

JS_METHOD(registerTransferManger:(UZModuleMethodContext *)context) {
    NSString * serviceKey = context.param[@"serviceKey"];
    NSString * appId = context.param[@"appId"];
    NSString * region = context.param[@"region"];
    BOOL isPrefixURL = context.param[@"isPrefixURL"] == nil?YES:[context.param[@"isPrefixURL"] boolValue];
    NSInteger timeOut = [context.param[@"timeOut"] integerValue];
    NSString * serviceName = context.param[@"serviceName"];
    NSString * suffix = context.param[@"suffix"];
    BOOL useHttps = context.param[@"useHttps"] == nil ? YES :[context.param[@"useHttps"] boolValue];
    
    QCloudServiceConfiguration* configuration = [QCloudServiceConfiguration new];
    
    NSString * userAgent = @"cos-xml-ios-sdk-v6.1.7-apicloud_ios";
    NSString *customAgent = context.param[@"userAgent"];
    if(customAgent.length != 0){
        userAgent = [userAgent stringByAppendingString:@"-"];
        userAgent = [userAgent stringByAppendingString:customAgent];
    }
    configuration.userAgentProductKey = userAgent;
    configuration.productVersion = CLIENT_VERSION;
    
    QCloudCOSXMLEndPoint* endpoint = [[QCloudCOSXMLEndPoint alloc] init];
    endpoint.useHTTPS = useHttps;
    if(serviceName.length > 0){
        endpoint.serviceName = serviceName;
    }
    if(region){
        endpoint.regionName = region;
    }
    
    endpoint.isPrefixURL = isPrefixURL;
    endpoint.suffix = suffix;
    configuration.endpoint = endpoint;
    configuration.appID = appId;
    if(timeOut != 0){
        configuration.timeoutInterval = timeOut;
    }
    
    configuration.signatureProvider = CredentailProvider.provider;
    
    if(serviceKey.length == 0){
        @try {
            [QCloudCOSTransferMangerService defaultCOSTransferManager];
        } @catch (NSException *exception) {
            [QCloudCOSTransferMangerService registerDefaultCOSTransferMangerWithConfiguration:configuration];
        }
    }else{
        if(![QCloudCOSTransferMangerService hasTransferMangerServiceForKey:serviceKey]){
            [QCloudCOSTransferMangerService registerCOSTransferMangerWithConfiguration:configuration withKey:serviceKey];
        }
    }
    
    if([QCloudCOSXMLService hasCosxmlServiceForKey:serviceKey]){
        
    }
}

JS_METHOD(createBucket:(UZModuleMethodContext *)context) {
    NSString * serviceKey = context.param[@"serviceKey"];
    NSString * name = context.param[@"name"];
    NSString * region = context.param[@"region"];
    BOOL enableMAZ = [ context.param[@"enableMAZ"]boolValue];
    NSString * authority = context.param[@"authority"];
    NSString * accessControlList = context.param[@"accessControlList"];
    NSString * readAccount = context.param[@"readAccount"];
    NSString * writeAccount = context.param[@"writeAccount"];
    NSString * readWriteAccount = context.param[@"readWriteAccount"];
    NSString * appId = context.param[@"appId"];
    QCloudPutBucketRequest* request = [QCloudPutBucketRequest new];
    request.accessControlList = accessControlList;
    request.grantRead = readAccount;
    request.grantWrite = writeAccount;
    request.grantFullControl = readWriteAccount;
    request.bucket = [NSString stringWithFormat:@"%@-%@",name,appId];
    request.regionName = region;
    if (enableMAZ) {
        QCloudCreateBucketConfiguration *config = [QCloudCreateBucketConfiguration new];
        config.bucketAZConfig = @"MAZ";
        request.createBucketConfiguration = config;
    }
    request.accessControlList = authority;
    
    [request setFinishBlock:^(id outputObject, NSError* error) {
        if(error){
            [self handleErrorInfo:context error:error];
        }else{
            [context callbackWithRet:@{@"result":@"success"} err:nil delete:YES];
        }
    }];
    
    if(![self getServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    
    [[self getServiceForKey:serviceKey] PutBucket:request];
}

JS_METHOD(getBucketList:(UZModuleMethodContext *)context) {
    NSString * serviceKey = context.param[@"serviceKey"];
    // 获取所属账户的所有存储空间列表的方法
    QCloudGetServiceRequest* request = [[QCloudGetServiceRequest alloc] init];
    [request setFinishBlock:^(QCloudListAllMyBucketsResult* result,
                              NSError* error) {
        if(error){
            [self handleErrorInfo:context error:error];
        }else{
            NSMutableDictionary * resultDic = @{}.mutableCopy;
            NSMutableArray * buckets = @[].mutableCopy;
            for (QCloudBucket * bucket in result.buckets) {
                [buckets addObject:@{@"name":bucket.name?:@"",@"type":bucket.type?:@"",@"location":bucket.location?:@"",@"createDate":bucket.createDate?:@""}];
            }
            
            [resultDic setObject:@{@"id":result.owner.identifier?:@"",@"displayName":result.owner.displayName?:@""} forKey:@"owner"];
            [resultDic setObject:buckets forKey:@"buckets"];
            
            [context callbackWithRet:@{@"result":@"success",@"data":resultDic.qcloud_modelToJSONString?:@""} err:@"" delete:YES];
        }
        
    }];
    if(![self getServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    [[self getServiceForKey:serviceKey] GetService:request];
}

JS_METHOD(deleteBucket:(UZModuleMethodContext *)context) {
    
    NSString * bucket = context.param[@"bucket"];
    NSString * region = context.param[@"region"];
    NSString * serviceKey = context.param[@"serviceKey"];
    QCloudDeleteBucketRequest* deleteBucketRequest = [[QCloudDeleteBucketRequest alloc] init];
    deleteBucketRequest.bucket = bucket;
    deleteBucketRequest.regionName = region;
    [deleteBucketRequest setFinishBlock:^(id outputObject, NSError *error) {
        if(error){
            [self handleErrorInfo:context error:error];
        }else{
            [context callbackWithRet:@{@"result":@"success"} err:@"" delete:YES];
        }
    }];
    if(![self getServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    [[self getServiceForKey:serviceKey] DeleteBucket:deleteBucketRequest];
}

JS_METHOD(listBucketContent:(UZModuleMethodContext *)context) {
 
    NSString * region = context.param[@"region"];
    NSString * bucket = context.param[@"bucket"];
    NSString * serviceKey = context.param[@"serviceKey"];
    NSString * prefix = context.param[@"prefix"];
    NSString * delimiter = context.param[@"delimiter"];
    NSString * marker = context.param[@"marker"];
    NSString * maxKeys = context.param[@"maxKeys"];
    
    QCloudGetBucketRequest* request = [QCloudGetBucketRequest new];
    request.regionName = region;
    request.bucket = bucket;
    request.prefix = prefix;
    
    request.delimiter = delimiter?:@"/";
    
    request.maxKeys = [maxKeys intValue] > 0?:1000;
    request.marker = marker;

    [request setFinishBlock:^(QCloudListBucketResult * _Nonnull result, NSError * _Nonnull error) {
    
        if (error) {
            [self handleErrorInfo:context error:error];
        } else {
            [context callbackWithRet:@{@"result":@"success",@"data":result.qcloud_modelToJSONString} err:@"" delete:YES];
        }
    }];
    if(![self getServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    [[self getServiceForKey:serviceKey] GetBucket:request];
    
}

JS_METHOD(uploadObject:(UZModuleMethodContext *)context) {
    NSString * region = context.param[@"region"];
    NSString * filePath = context.param[@"filePath"];
    NSString * bucket = context.param[@"bucket"];
    NSString * uploadId = context.param[@"uploadId"];
    NSString * object = context.param[@"object"];
    NSString * serviceKey = context.param[@"serviceKey"];
    NSString * stroageClass = context.param[@"stroageClass"];
    NSString * enableVerification = context.param[@"enableVerification"];
    NSString * trafficLimit = context.param[@"trafficLimit"];
    filePath = [self getPathWithUZSchemeURL:filePath];
    __block QCloudCOSXMLUploadObjectRequest* uploadReq = nil;
    
    uploadReq = [QCloudCOSXMLUploadObjectRequest new];
    uploadReq.bucket = bucket;
    uploadReq.regionName = region;
    uploadReq.object = object;
    if(trafficLimit.integerValue > 0){
        uploadReq.trafficLimit = trafficLimit.integerValue;
    }
    uploadReq.body = [NSURL fileURLWithPath:filePath];
    if(uploadId.length > 0){
        uploadReq.uploadid = uploadId;
    }
    uploadReq.enableVerification = enableVerification.boolValue;
    uploadReq.storageClass = QCloudCOSStorageClassDumpFromString(stroageClass);
    __weak QCloudCOSXMLUploadObjectRequest* weakReq = uploadReq;
    [uploadReq setInitMultipleUploadFinishBlock:^(QCloudInitiateMultipartUploadResult * _Nullable multipleUploadInitResult, QCloudCOSXMLUploadObjectResumeData  _Nullable resumeData) {
        [context callbackWithRet:@{@"result":@"begin",@"data":[@{@"taskId":@(weakReq.requestID).stringValue,@"uploadId":multipleUploadInitResult.uploadId?:@""} qcloud_modelToJSONString]} err:@"" delete:NO];
    }];
    
    [CredentailProvider.provider cacheRequest:uploadReq];
    
    
    [uploadReq setSendProcessBlock:^(int64_t bytesSent, int64_t totalBytesSent, int64_t totalBytesExpectedToSend) {
        [context callbackWithRet:@{@"result":@"processing",@"data":[@{@"totalBytesSent":@(totalBytesSent),@"totalBytesExpectedToSend":@(totalBytesExpectedToSend)}qcloud_modelToJSONString]} err:@"" delete:NO];
    }];
    
    [uploadReq setFinishBlock:^(QCloudUploadObjectResult *result, NSError *error) {
        if (error) {
            [self handleErrorInfo:context error:error];
        } else {
            
            NSMutableDictionary * data = @{}.mutableCopy;
            [data setObject:result.eTag?:@"" forKey:@"eTag"];
            [data setObject:result.location?:@"" forKey:@"url"];
            NSDictionary * headers = [result __originHTTPURLResponse__].allHeaderFields;
            for (NSString * key in headers.allKeys) {
                [data setObject:headers[key] forKey:key];
            }
            [context callbackWithRet:@{@"result":@"success",@"data":[data qcloud_modelToJSONString]} err:@"" delete:YES];
        }
        [[CredentailProvider provider] removeRequestForKey:@(weakReq.requestID).stringValue];
    }];
    if(![self getTransferServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：传输服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    [[self getTransferServiceForKey:serviceKey] UploadObject:uploadReq];
}

JS_METHOD(pauseUploadObject:(UZModuleMethodContext *)context) {
    NSString * taskId = context.param[@"taskId"];
    QCloudCOSXMLUploadObjectRequest *req = (QCloudCOSXMLUploadObjectRequest *)[CredentailProvider.provider requestForKey:taskId];
    __block NSError* error;
    QCloudCOSXMLUploadObjectResumeData resumeData = [req cancelByProductingResumeData:&error];
    if(error != nil){
        [self handleErrorInfo:context error:error];
    }else{
        [context callbackWithRet:@{@"result":@"success"} err:@"" delete:YES];
    }
}

JS_METHOD(downloadObject:(UZModuleMethodContext *)context) {
    
    NSString * region = context.param[@"region"];
    NSString * localPath = context.param[@"localPath"];
    NSString * bucket = context.param[@"bucket"];
    NSString * object = context.param[@"object"];
    NSString * versionId = context.param[@"versionId"];
    NSString * serviceKey = context.param[@"serviceKey"];
    NSString * trafficLimit = context.param[@"trafficLimit"];
    localPath = [self getPathWithUZSchemeURL:localPath];
    
    QCloudCOSXMLDownloadObjectRequest * request = [QCloudCOSXMLDownloadObjectRequest new];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [context callbackWithRet:@{@"result":@"begin",@"data":[@{@"taskId":@(request.requestID)}qcloud_modelToJSONString]} err:@"" delete:NO];
    });
    if(trafficLimit.integerValue > 0){
        request.trafficLimit = trafficLimit.integerValue;
    }
    request.versionID = versionId;
    request.bucket = bucket;
    request.object = object;
    request.regionName = region;
    request.downloadingURL = [NSURL fileURLWithPath:localPath];
    request.resumableDownload = YES;
    [request setFinishBlock:^(id outputObject, NSError *error) {
        if(error){
            [self handleErrorInfo:context error:error];
        }else{
            NSDictionary* info = (NSDictionary *) outputObject;
            [context callbackWithRet:@{@"result":@"success",@"data":info.qcloud_modelToJSONString} err:@"" delete:YES];
        }
    }];

    [request setDownProcessBlock:^(int64_t bytesDownload,
                                   int64_t totalBytesDownload,
                                   int64_t totalBytesExpectedToDownload) {
        [context callbackWithRet:@{@"result":@"processing",@"data":[@{@"totalBytesDownload":@(totalBytesDownload),@"totalBytesExpectedToDownload":@(totalBytesExpectedToDownload)}qcloud_modelToJSONString]} err:@"" delete:NO];
    }];
    [CredentailProvider.provider cacheRequest:request];
    if(![self getServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：传输服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    [[self getTransferServiceForKey:serviceKey] DownloadObject:request];
}

JS_METHOD(pauseDownloadObject:(UZModuleMethodContext *)context) {
    NSString * taskId = @([context.param[@"taskId"] integerValue]).stringValue;
    Boolean deleteLocalFile = [context.param[@"deleteLocalFile"] boolValue];
    QCloudCOSXMLDownloadObjectRequest *req = (QCloudCOSXMLDownloadObjectRequest *) [CredentailProvider.provider requestForKey:taskId];
    if(deleteLocalFile){
        QCloudRemoveFileByPath(req.downloadingURL.absoluteString);
    }
    [req cancel];
    
    [context callbackWithRet:@{@"result":@"success"} err:nil delete:YES];
}

JS_METHOD(deleteObject:(UZModuleMethodContext *)context) {
    
    
    NSString * region = context.param[@"region"];
    NSString * bucket = context.param[@"bucket"];
    NSString * object = context.param[@"object"];
    NSString * versionId = context.param[@"versionId"];
    NSString * serviceKey = context.param[@"serviceKey"];
    
    QCloudDeleteObjectRequest* deleteRequest = [QCloudDeleteObjectRequest new];
    deleteRequest.versionID = versionId;
    deleteRequest.object = object;
    deleteRequest.bucket = bucket;
    deleteRequest.regionName = region;
    
    [deleteRequest setFinishBlock:^(id outputObject, NSError *error) {
        if(error){
            [self handleErrorInfo:context error:error];
        }else{
            [context callbackWithRet:@{@"result":@"success"} err:@"" delete:YES];
        }
        
    }];
    if(![self getServiceForKey:serviceKey]){
        [context callbackWithRet:nil err:@{@"result":@"error",@"data":[@{@"message":[NSString stringWithFormat:@"%@：服务没有注册，请注册后再进行网络请求",(serviceKey.length > 0)?serviceKey:@"默认"]} qcloud_modelToJSONString]} delete:YES];
        return;
    }
    [[self getServiceForKey:serviceKey] DeleteObject:deleteRequest];
}

JS_METHOD(headObject:(UZModuleMethodContext *)context) {
    
    NSString * region = context.param[@"region"];
    NSString * bucket = context.param[@"bucket"];
    NSString * object = context.param[@"object"];
    NSString * versionId = context.param[@"versionId"];
    NSString * serviceKey = context.param[@"serviceKey"];
    QCloudHeadObjectRequest *request = [QCloudHeadObjectRequest new];
    request.bucket = bucket;
    request.object = object;
    request.regionName = region;
    request.versionID = versionId;
    [request setFinishBlock:^(NSDictionary * outputObject, NSError *error) {
        if (error) {
            [self handleErrorInfo:context error:error];
        } else {
            [context callbackWithRet:@{@"result":@"success",@"data":outputObject.qcloud_modelToJSONString} err:@"" delete:YES];
        }
    }];
    [[self getServiceForKey:serviceKey] HeadObject:request];
}
JS_METHOD(cancelAll:(UZModuleMethodContext *)context) {
    
    NSString * serviceKey = context.param[@"serviceKey"];
    QCloudCOSXMLService * services = [self getServiceForKey:serviceKey];
    [services.sessionManager cancelAllRequest];
    
    QCloudCOSTransferMangerService * tservices = [self getTransferServiceForKey:serviceKey];
    [tservices.sessionManager cancelAllRequest];
}

-(void)handleErrorInfo:(UZModuleMethodContext *)context error:(NSError *)error{
    NSDictionary * errorInfo = @{@"result":@"error",@"data":[@{@"message":error.userInfo[@"Message"]?:(error.userInfo[NSLocalizedDescriptionKey]?:@"") ,@"errorCode":error.userInfo[@"Code"]?:@(error.code).stringValue,@"RequestId":error.userInfo[@"RequestId"]?:@""} qcloud_modelToJSONString]};
    [context callbackWithRet:@"" err:errorInfo delete:YES];
}

@end
