//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/unsafe/QueueProgressIndicators.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalUtilUnsafeQueueProgressIndicators")
#ifdef RESTRICT_RxInternalUtilUnsafeQueueProgressIndicators
#define INCLUDE_ALL_RxInternalUtilUnsafeQueueProgressIndicators 0
#else
#define INCLUDE_ALL_RxInternalUtilUnsafeQueueProgressIndicators 1
#endif
#undef RESTRICT_RxInternalUtilUnsafeQueueProgressIndicators

#if !defined (RxInternalUtilUnsafeQueueProgressIndicators_) && (INCLUDE_ALL_RxInternalUtilUnsafeQueueProgressIndicators || defined(INCLUDE_RxInternalUtilUnsafeQueueProgressIndicators))
#define RxInternalUtilUnsafeQueueProgressIndicators_

@protocol RxInternalUtilUnsafeQueueProgressIndicators < JavaObject >

- (jlong)currentProducerIndex;

- (jlong)currentConsumerIndex;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilUnsafeQueueProgressIndicators)

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilUnsafeQueueProgressIndicators)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalUtilUnsafeQueueProgressIndicators")
