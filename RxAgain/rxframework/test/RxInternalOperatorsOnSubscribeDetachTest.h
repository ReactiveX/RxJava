//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OnSubscribeDetachTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeDetachTest")
#ifdef RESTRICT_RxInternalOperatorsOnSubscribeDetachTest
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeDetachTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeDetachTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOnSubscribeDetachTest

#if !defined (RxInternalOperatorsOnSubscribeDetachTest_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeDetachTest || defined(INCLUDE_RxInternalOperatorsOnSubscribeDetachTest))
#define RxInternalOperatorsOnSubscribeDetachTest_

@interface RxInternalOperatorsOnSubscribeDetachTest : NSObject {
 @public
  id o_;
}

#pragma mark Public

- (instancetype)init;

- (void)backpressured;

- (void)deferredUpstreamProducer;

- (void)empty;

- (void)error;

- (void)just;

- (void)justUnsubscribed;

- (void)range;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeDetachTest)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeDetachTest, o_, id)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeDetachTest_init(RxInternalOperatorsOnSubscribeDetachTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeDetachTest *new_RxInternalOperatorsOnSubscribeDetachTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeDetachTest *create_RxInternalOperatorsOnSubscribeDetachTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeDetachTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeDetachTest")
