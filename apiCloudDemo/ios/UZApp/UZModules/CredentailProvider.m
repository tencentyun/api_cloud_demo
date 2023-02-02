//
//  CredentailProvider.m
//  UZApp
//
//  Created by garenwang on 2022/12/30.
//  Copyright © 2022 APICloud. All rights reserved.
//

#import "CredentailProvider.h"

@interface CredentailProvider ()
@property (nonatomic) QCloudCredentailFenceQueue* credentialFenceQueue;
@property (nonatomic) NSMutableDictionary * requestPool;
@property (nonatomic) NSRecursiveLock * lock;
@property (nonatomic)QCloudCredentailFenceQueueContinue continueBlock;
@end

@implementation CredentailProvider
static CredentailProvider* _instance;
+(CredentailProvider *)provider{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        if (_instance == nil) {
            _instance = [[self alloc]init];
        }
    });
    return _instance;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.requestPool = [NSMutableDictionary new];
        self.lock = [NSRecursiveLock new];
        
        QCloudServiceConfiguration* configuration = [QCloudServiceConfiguration new];
        QCloudCOSXMLEndPoint* endpoint = [[QCloudCOSXMLEndPoint alloc] init];
        endpoint.useHTTPS = true;
        configuration.endpoint = endpoint;
        configuration.signatureProvider = self;
        [QCloudCOSXMLService registerDefaultCOSXMLWithConfiguration:configuration];
        [QCloudCOSTransferMangerService registerDefaultCOSTransferMangerWithConfiguration:
         configuration];
        
        self.credentialFenceQueue = [QCloudCredentailFenceQueue new];
        self.credentialFenceQueue.delegate = self;
    }
    return self;
}


- (void) fenceQueue:(QCloudCredentailFenceQueue * )queue requestCreatorWithContinue:(QCloudCredentailFenceQueueContinue)continueBlock
{
    [self.delegate refreshCredentail];
    self.continueBlock = continueBlock;
}

- (void)setCredential:(QCloudCredential *)credential{
    _credential = credential;
    if(self.continueBlock){
        QCloudAuthentationV5Creator* creator = [[QCloudAuthentationV5Creator alloc]
                                                initWithCredential:_credential];
        self.continueBlock(creator, nil);
        self.continueBlock = nil;
    }
}

// 获取签名的方法入口，这里演示了获取临时密钥并计算签名的过程
// 您也可以自定义计算签名的过程
- (void) signatureWithFields:(QCloudSignatureFields*)fileds
                     request:(QCloudBizHTTPRequest*)request
                  urlRequest:(NSMutableURLRequest*)urlRequst
                   compelete:(QCloudHTTPAuthentationContinueBlock)continueBlock
{
    if(self.secretID != nil && self.secretKey != nil){
        QCloudCredential *credential = [QCloudCredential new];
        credential.secretID = self.secretID;
        credential.secretKey = self.secretKey;
        QCloudAuthentationV5Creator* creator = [[QCloudAuthentationV5Creator alloc]
                                                initWithCredential:credential];
        QCloudSignature *signature = [creator signatureForData:urlRequst];
        continueBlock(signature, nil);
    }else{
        [self.credentialFenceQueue performAction:^(QCloudAuthentationCreator *creator,
                                                   NSError *error) {
            if (error) {
                continueBlock(nil, error);
            } else {
                // 注意 这里不要对urlRequst 进行copy以及mutableCopy操作
                QCloudSignature* signature =  [creator signatureForData:urlRequst];
                continueBlock(signature, nil);
            }
        }];
    }
}

-(void)cacheRequest:(QCloudHTTPRequest *)request{
    [self.lock lock];
    [self.requestPool setObject:request forKey:@(request.requestID).stringValue];
    [self.lock unlock];
}

-(void)removeRequestForKey:(NSString *)requestId{
    [self.lock lock];
    [self.requestPool removeObjectForKey:requestId];
    [self.lock unlock];
}
-(QCloudHTTPRequest *)requestForKey:(NSString *)requestId{
    return [self.requestPool objectForKey:requestId];
}
@end
