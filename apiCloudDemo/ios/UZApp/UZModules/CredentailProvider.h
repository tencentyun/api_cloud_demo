//
//  CredentailProvider.h
//  UZApp
//
//  Created by garenwang on 2022/12/30.
//  Copyright © 2022 APICloud. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <QCloudCOSXML/QCloudCOSXML.h>
NS_ASSUME_NONNULL_BEGIN

@protocol CredentailProviderDelegate <NSObject>
-(void)refreshCredentail;
@end

@interface CredentailProvider : NSObject <QCloudSignatureProvider,QCloudCredentailFenceQueueDelegate>
@property (nonatomic) QCloudCredential* credential;
@property (nonatomic,strong) NSString* secretID;
@property (nonatomic,strong) NSString* secretKey;
@property (nonatomic,weak) id<CredentailProviderDelegate> delegate;

+ (instancetype) new NS_UNAVAILABLE;
- (instancetype) init NS_UNAVAILABLE;
+(CredentailProvider *)provider;

-(void)cacheRequest:(QCloudHTTPRequest *)request;
-(void)removeRequestForKey:(NSString *)requestId;
-(QCloudHTTPRequest *)requestForKey:(NSString *)requestId;
@end

NS_ASSUME_NONNULL_END
