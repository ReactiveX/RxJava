//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/CompletableTestFunc1a.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxCompletableTestFunc1a")
#ifdef RESTRICT_RxCompletableTestFunc1a
#define INCLUDE_ALL_RxCompletableTestFunc1a 0
#else
#define INCLUDE_ALL_RxCompletableTestFunc1a 1
#endif
#undef RESTRICT_RxCompletableTestFunc1a

#if !defined (RxCompletableTestFunc1a_) && (INCLUDE_ALL_RxCompletableTestFunc1a || defined(INCLUDE_RxCompletableTestFunc1a))
#define RxCompletableTestFunc1a_

#define RESTRICT_RxFunctionsFunc1 1
#define INCLUDE_RxFunctionsFunc1 1
#include "RxFunctionsFunc1.h"

@protocol RxCompletable_OnSubscribe;

@interface RxCompletableTestFunc1a : NSObject < RxFunctionsFunc1 >

#pragma mark Public

- (instancetype)init;

- (id<RxCompletable_OnSubscribe>)callWithId:(id<RxCompletable_OnSubscribe>)t;

@end

J2OBJC_EMPTY_STATIC_INIT(RxCompletableTestFunc1a)

FOUNDATION_EXPORT void RxCompletableTestFunc1a_init(RxCompletableTestFunc1a *self);

FOUNDATION_EXPORT RxCompletableTestFunc1a *new_RxCompletableTestFunc1a_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxCompletableTestFunc1a *create_RxCompletableTestFunc1a_init();

J2OBJC_TYPE_LITERAL_HEADER(RxCompletableTestFunc1a)

#endif

#pragma pop_macro("INCLUDE_ALL_RxCompletableTestFunc1a")
