//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/build/generated/source/apt/test/rx/doppl/mock/MSubscriber$Moxy.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxDopplMockMSubscriber$Moxy")
#ifdef RESTRICT_RxDopplMockMSubscriber$Moxy
#define INCLUDE_ALL_RxDopplMockMSubscriber$Moxy 0
#else
#define INCLUDE_ALL_RxDopplMockMSubscriber$Moxy 1
#endif
#undef RESTRICT_RxDopplMockMSubscriber$Moxy

#if !defined (RxDopplMockMSubscriber_Moxy_) && (INCLUDE_ALL_RxDopplMockMSubscriber$Moxy || defined(INCLUDE_RxDopplMockMSubscriber_Moxy))
#define RxDopplMockMSubscriber_Moxy_

#define RESTRICT_RxDopplMockMSubscriber 1
#define INCLUDE_RxDopplMockMSubscriber 1
#include "RxDopplMockMSubscriber.h"

@protocol JavaLangReflectInvocationHandler;
@protocol RxProducer;

@interface RxDopplMockMSubscriber_Moxy : RxDopplMockMSubscriber {
 @public
  id<JavaLangReflectInvocationHandler> $__handler_;
}

#pragma mark Public

- (jboolean)isEqual:(id)arg0;

- (id<JavaLangReflectInvocationHandler>)getHandler;

- (NSUInteger)hash;

- (void)j2objcCleanup;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)t;

- (void)onStart;

- (void)setHandlerWithJavaLangReflectInvocationHandler:(id<JavaLangReflectInvocationHandler>)handler;

- (void)setProducerWithRxProducer:(id<RxProducer>)p;

- (jboolean)super$equalsWithId:(id)arg0;

- (jint)super$hashCode;

- (void)super$j2objcCleanup;

- (void)super$onCompleted;

- (void)super$onErrorWithNSException:(NSException *)e;

- (void)super$onNextWithId:(id)t;

- (void)super$onStart;

- (void)super$setProducerWithRxProducer:(id<RxProducer>)p;

- (NSString *)super$toString;

- (NSString *)description;

#pragma mark Protected

- (id)clone;

- (id)super$clone;

#pragma mark Package-Private

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxDopplMockMSubscriber_Moxy)

J2OBJC_FIELD_SETTER(RxDopplMockMSubscriber_Moxy, $__handler_, id<JavaLangReflectInvocationHandler>)

FOUNDATION_EXPORT void RxDopplMockMSubscriber_Moxy_init(RxDopplMockMSubscriber_Moxy *self);

FOUNDATION_EXPORT RxDopplMockMSubscriber_Moxy *new_RxDopplMockMSubscriber_Moxy_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxDopplMockMSubscriber_Moxy *create_RxDopplMockMSubscriber_Moxy_init();

J2OBJC_TYPE_LITERAL_HEADER(RxDopplMockMSubscriber_Moxy)

@compatibility_alias RxDopplMockMSubscriber$Moxy RxDopplMockMSubscriber_Moxy;

#endif

#pragma pop_macro("INCLUDE_ALL_RxDopplMockMSubscriber$Moxy")
