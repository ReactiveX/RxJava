//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/plugins/RxJavaSchedulersHookTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxPluginsRxJavaSchedulersHookTest")
#ifdef RESTRICT_RxPluginsRxJavaSchedulersHookTest
#define INCLUDE_ALL_RxPluginsRxJavaSchedulersHookTest 0
#else
#define INCLUDE_ALL_RxPluginsRxJavaSchedulersHookTest 1
#endif
#undef RESTRICT_RxPluginsRxJavaSchedulersHookTest

#if !defined (RxPluginsRxJavaSchedulersHookTest_) && (INCLUDE_ALL_RxPluginsRxJavaSchedulersHookTest || defined(INCLUDE_RxPluginsRxJavaSchedulersHookTest))
#define RxPluginsRxJavaSchedulersHookTest_

@interface RxPluginsRxJavaSchedulersHookTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)computationSchedulerUsesSuppliedThreadFactory;

- (void)ioSchedulerUsesSuppliedThreadFactory;

- (void)newThreadSchedulerUsesSuppliedThreadFactory OBJC_METHOD_FAMILY_NONE;

- (void)schedulerFactoriesDisallowNull;

@end

J2OBJC_EMPTY_STATIC_INIT(RxPluginsRxJavaSchedulersHookTest)

FOUNDATION_EXPORT void RxPluginsRxJavaSchedulersHookTest_init(RxPluginsRxJavaSchedulersHookTest *self);

FOUNDATION_EXPORT RxPluginsRxJavaSchedulersHookTest *new_RxPluginsRxJavaSchedulersHookTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxPluginsRxJavaSchedulersHookTest *create_RxPluginsRxJavaSchedulersHookTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxPluginsRxJavaSchedulersHookTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxPluginsRxJavaSchedulersHookTest")
