//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/build/generated/source/apt/test/rx/internal/operators/OperatorScanTest__InnerProducer$Moxy.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy")
#ifdef RESTRICT_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy
#define INCLUDE_ALL_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy

#if !defined (RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy_) && (INCLUDE_ALL_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy || defined(INCLUDE_RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy))
#define RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy_

#define RESTRICT_RxInternalOperatorsOperatorScanTest 1
#define INCLUDE_RxInternalOperatorsOperatorScanTest_InnerProducer 1
#include "RxInternalOperatorsOperatorScanTest.h"

@class RxSubscriber;
@protocol JavaLangReflectInvocationHandler;

@interface RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy : RxInternalOperatorsOperatorScanTest_InnerProducer {
 @public
  id<JavaLangReflectInvocationHandler> $__handler_;
}

#pragma mark Public

- (jboolean)isEqual:(id)arg0;

- (id<JavaLangReflectInvocationHandler>)getHandler;

- (NSUInteger)hash;

- (void)requestWithLong:(jlong)n;

- (void)setHandlerWithJavaLangReflectInvocationHandler:(id<JavaLangReflectInvocationHandler>)handler;

- (jboolean)super$equalsWithId:(id)arg0;

- (jint)super$hashCode;

- (void)super$requestWithLong:(jlong)n;

- (NSString *)super$toString;

- (NSString *)description;

#pragma mark Protected

- (id)clone;

- (id)super$clone;

#pragma mark Package-Private

- (instancetype)initWithRxSubscriber:(RxSubscriber *)a0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy, $__handler_, id<JavaLangReflectInvocationHandler>)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy_initWithRxSubscriber_(RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy *self, RxSubscriber *a0);

FOUNDATION_EXPORT RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy *new_RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy_initWithRxSubscriber_(RxSubscriber *a0) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy *create_RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy_initWithRxSubscriber_(RxSubscriber *a0);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy)

@compatibility_alias RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy RxInternalOperatorsOperatorScanTest__InnerProducer_Moxy;

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorScanTest__InnerProducer$Moxy")
