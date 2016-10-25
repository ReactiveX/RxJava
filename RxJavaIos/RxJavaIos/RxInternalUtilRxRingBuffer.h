//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/RxRingBuffer.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalUtilRxRingBuffer")
#ifdef RESTRICT_RxInternalUtilRxRingBuffer
#define INCLUDE_ALL_RxInternalUtilRxRingBuffer 0
#else
#define INCLUDE_ALL_RxInternalUtilRxRingBuffer 1
#endif
#undef RESTRICT_RxInternalUtilRxRingBuffer

#if !defined (RxInternalUtilRxRingBuffer_) && (INCLUDE_ALL_RxInternalUtilRxRingBuffer || defined(INCLUDE_RxInternalUtilRxRingBuffer))
#define RxInternalUtilRxRingBuffer_

#define RESTRICT_RxSubscription 1
#define INCLUDE_RxSubscription 1
#include "RxSubscription.h"

@class RxInternalUtilObjectPool;
@protocol RxObserver;

@interface RxInternalUtilRxRingBuffer : NSObject < RxSubscription > {
 @public
  volatile_id terminalState_;
}

#pragma mark Public

- (jboolean)acceptWithId:(id)o
          withRxObserver:(id<RxObserver>)child;

- (NSException *)asErrorWithId:(id)o;

- (jint)available;

- (jint)capacity;

- (jint)count;

+ (RxInternalUtilRxRingBuffer *)getSpmcInstance;

+ (RxInternalUtilRxRingBuffer *)getSpscInstance;

- (id)getValueWithId:(id)o;

- (jboolean)isCompletedWithId:(id)o;

- (jboolean)isEmpty;

- (jboolean)isErrorWithId:(id)o;

- (jboolean)isUnsubscribed;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)t;

- (void)onNextWithId:(id)o;

- (id)peek;

- (id)poll;

- (void)release__;

- (void)unsubscribe;

#pragma mark Package-Private

- (instancetype)init;

@end

J2OBJC_STATIC_INIT(RxInternalUtilRxRingBuffer)

J2OBJC_VOLATILE_FIELD_SETTER(RxInternalUtilRxRingBuffer, terminalState_, id)

inline jint RxInternalUtilRxRingBuffer_get_SIZE();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT jint RxInternalUtilRxRingBuffer_SIZE;
J2OBJC_STATIC_FIELD_PRIMITIVE_FINAL(RxInternalUtilRxRingBuffer, SIZE, jint)

inline RxInternalUtilObjectPool *RxInternalUtilRxRingBuffer_get_SPSC_POOL();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT RxInternalUtilObjectPool *RxInternalUtilRxRingBuffer_SPSC_POOL;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalUtilRxRingBuffer, SPSC_POOL, RxInternalUtilObjectPool *)

inline RxInternalUtilObjectPool *RxInternalUtilRxRingBuffer_get_SPMC_POOL();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT RxInternalUtilObjectPool *RxInternalUtilRxRingBuffer_SPMC_POOL;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalUtilRxRingBuffer, SPMC_POOL, RxInternalUtilObjectPool *)

FOUNDATION_EXPORT RxInternalUtilRxRingBuffer *RxInternalUtilRxRingBuffer_getSpscInstance();

FOUNDATION_EXPORT RxInternalUtilRxRingBuffer *RxInternalUtilRxRingBuffer_getSpmcInstance();

FOUNDATION_EXPORT void RxInternalUtilRxRingBuffer_init(RxInternalUtilRxRingBuffer *self);

FOUNDATION_EXPORT RxInternalUtilRxRingBuffer *new_RxInternalUtilRxRingBuffer_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilRxRingBuffer *create_RxInternalUtilRxRingBuffer_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilRxRingBuffer)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalUtilRxRingBuffer")
