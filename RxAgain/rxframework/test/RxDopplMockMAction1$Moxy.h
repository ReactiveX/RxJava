//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/build/generated/source/apt/test/rx/doppl/mock/MAction1$Moxy.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxDopplMockMAction1$Moxy")
#ifdef RESTRICT_RxDopplMockMAction1$Moxy
#define INCLUDE_ALL_RxDopplMockMAction1$Moxy 0
#else
#define INCLUDE_ALL_RxDopplMockMAction1$Moxy 1
#endif
#undef RESTRICT_RxDopplMockMAction1$Moxy

#if !defined (RxDopplMockMAction1_Moxy_) && (INCLUDE_ALL_RxDopplMockMAction1$Moxy || defined(INCLUDE_RxDopplMockMAction1_Moxy))
#define RxDopplMockMAction1_Moxy_

#define RESTRICT_RxDopplMockMAction1 1
#define INCLUDE_RxDopplMockMAction1 1
#include "RxDopplMockMAction1.h"

@protocol JavaLangReflectInvocationHandler;

@interface RxDopplMockMAction1_Moxy : RxDopplMockMAction1 {
 @public
  id<JavaLangReflectInvocationHandler> $__handler_;
}

#pragma mark Public

- (void)callWithId:(id)t;

- (jboolean)isEqual:(id)arg0;

- (id<JavaLangReflectInvocationHandler>)getHandler;

- (NSUInteger)hash;

- (void)setHandlerWithJavaLangReflectInvocationHandler:(id<JavaLangReflectInvocationHandler>)handler;

- (void)super$callWithId:(id)t;

- (jboolean)super$equalsWithId:(id)arg0;

- (jint)super$hashCode;

- (NSString *)super$toString;

- (NSString *)description;

#pragma mark Protected

- (id)clone;

- (id)super$clone;

#pragma mark Package-Private

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxDopplMockMAction1_Moxy)

J2OBJC_FIELD_SETTER(RxDopplMockMAction1_Moxy, $__handler_, id<JavaLangReflectInvocationHandler>)

FOUNDATION_EXPORT void RxDopplMockMAction1_Moxy_init(RxDopplMockMAction1_Moxy *self);

FOUNDATION_EXPORT RxDopplMockMAction1_Moxy *new_RxDopplMockMAction1_Moxy_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxDopplMockMAction1_Moxy *create_RxDopplMockMAction1_Moxy_init();

J2OBJC_TYPE_LITERAL_HEADER(RxDopplMockMAction1_Moxy)

@compatibility_alias RxDopplMockMAction1$Moxy RxDopplMockMAction1_Moxy;

#endif

#pragma pop_macro("INCLUDE_ALL_RxDopplMockMAction1$Moxy")
