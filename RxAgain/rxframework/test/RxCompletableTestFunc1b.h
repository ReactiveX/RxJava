//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/CompletableTestFunc1b.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxCompletableTestFunc1b")
#ifdef RESTRICT_RxCompletableTestFunc1b
#define INCLUDE_ALL_RxCompletableTestFunc1b 0
#else
#define INCLUDE_ALL_RxCompletableTestFunc1b 1
#endif
#undef RESTRICT_RxCompletableTestFunc1b

#if !defined (RxCompletableTestFunc1b_) && (INCLUDE_ALL_RxCompletableTestFunc1b || defined(INCLUDE_RxCompletableTestFunc1b))
#define RxCompletableTestFunc1b_

#define RESTRICT_RxFunctionsFunc2 1
#define INCLUDE_RxFunctionsFunc2 1
#include "RxFunctionsFunc2.h"

@class RxCompletable;
@protocol RxCompletable_OnSubscribe;

@interface RxCompletableTestFunc1b : NSObject < RxFunctionsFunc2 >

#pragma mark Public

- (instancetype)init;

- (id<RxCompletable_OnSubscribe>)callWithId:(RxCompletable *)t1
                                     withId:(id<RxCompletable_OnSubscribe>)t2;

@end

J2OBJC_EMPTY_STATIC_INIT(RxCompletableTestFunc1b)

FOUNDATION_EXPORT void RxCompletableTestFunc1b_init(RxCompletableTestFunc1b *self);

FOUNDATION_EXPORT RxCompletableTestFunc1b *new_RxCompletableTestFunc1b_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxCompletableTestFunc1b *create_RxCompletableTestFunc1b_init();

J2OBJC_TYPE_LITERAL_HEADER(RxCompletableTestFunc1b)

#endif

#pragma pop_macro("INCLUDE_ALL_RxCompletableTestFunc1b")
