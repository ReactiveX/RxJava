//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/singles/BlockingSingleTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxSinglesBlockingSingleTest")
#ifdef RESTRICT_RxSinglesBlockingSingleTest
#define INCLUDE_ALL_RxSinglesBlockingSingleTest 0
#else
#define INCLUDE_ALL_RxSinglesBlockingSingleTest 1
#endif
#undef RESTRICT_RxSinglesBlockingSingleTest

#if !defined (RxSinglesBlockingSingleTest_) && (INCLUDE_ALL_RxSinglesBlockingSingleTest || defined(INCLUDE_RxSinglesBlockingSingleTest))
#define RxSinglesBlockingSingleTest_

@interface RxSinglesBlockingSingleTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)testSingleError;

- (void)testSingleErrorChecked;

- (void)testSingleGet;

- (void)testSingleToFuture;

@end

J2OBJC_EMPTY_STATIC_INIT(RxSinglesBlockingSingleTest)

FOUNDATION_EXPORT void RxSinglesBlockingSingleTest_init(RxSinglesBlockingSingleTest *self);

FOUNDATION_EXPORT RxSinglesBlockingSingleTest *new_RxSinglesBlockingSingleTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxSinglesBlockingSingleTest *create_RxSinglesBlockingSingleTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxSinglesBlockingSingleTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxSinglesBlockingSingleTest")
