//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/CombineLatestTests.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxCombineLatestTests")
#ifdef RESTRICT_RxCombineLatestTests
#define INCLUDE_ALL_RxCombineLatestTests 0
#else
#define INCLUDE_ALL_RxCombineLatestTests 1
#endif
#undef RESTRICT_RxCombineLatestTests

#if !defined (RxCombineLatestTests_) && (INCLUDE_ALL_RxCombineLatestTests || defined(INCLUDE_RxCombineLatestTests))
#define RxCombineLatestTests_

@protocol RxFunctionsAction1;
@protocol RxFunctionsFunc2;

@interface RxCombineLatestTests : NSObject {
 @public
  id<RxFunctionsFunc2> combine_;
  id<RxFunctionsAction1> action_;
  id<RxFunctionsAction1> extendedAction_;
}

#pragma mark Public

- (instancetype)init;

- (void)testCovarianceOfCombineLatest;

- (void)testNullEmitting;

@end

J2OBJC_EMPTY_STATIC_INIT(RxCombineLatestTests)

J2OBJC_FIELD_SETTER(RxCombineLatestTests, combine_, id<RxFunctionsFunc2>)
J2OBJC_FIELD_SETTER(RxCombineLatestTests, action_, id<RxFunctionsAction1>)
J2OBJC_FIELD_SETTER(RxCombineLatestTests, extendedAction_, id<RxFunctionsAction1>)

FOUNDATION_EXPORT void RxCombineLatestTests_init(RxCombineLatestTests *self);

FOUNDATION_EXPORT RxCombineLatestTests *new_RxCombineLatestTests_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxCombineLatestTests *create_RxCombineLatestTests_init();

J2OBJC_TYPE_LITERAL_HEADER(RxCombineLatestTests)

#endif

#pragma pop_macro("INCLUDE_ALL_RxCombineLatestTests")
