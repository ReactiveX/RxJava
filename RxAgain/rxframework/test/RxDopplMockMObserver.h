//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/doppl/mock/MObserver.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxDopplMockMObserver")
#ifdef RESTRICT_RxDopplMockMObserver
#define INCLUDE_ALL_RxDopplMockMObserver 0
#else
#define INCLUDE_ALL_RxDopplMockMObserver 1
#endif
#undef RESTRICT_RxDopplMockMObserver

#if !defined (RxDopplMockMObserver_) && (INCLUDE_ALL_RxDopplMockMObserver || defined(INCLUDE_RxDopplMockMObserver))
#define RxDopplMockMObserver_

#define RESTRICT_RxObserver 1
#define INCLUDE_RxObserver 1
#include "RxObserver.h"

@interface RxDopplMockMObserver : NSObject < RxObserver >

#pragma mark Public

- (instancetype)init;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)o;

@end

J2OBJC_EMPTY_STATIC_INIT(RxDopplMockMObserver)

FOUNDATION_EXPORT void RxDopplMockMObserver_init(RxDopplMockMObserver *self);

FOUNDATION_EXPORT RxDopplMockMObserver *new_RxDopplMockMObserver_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxDopplMockMObserver *create_RxDopplMockMObserver_init();

J2OBJC_TYPE_LITERAL_HEADER(RxDopplMockMObserver)

#endif

#pragma pop_macro("INCLUDE_ALL_RxDopplMockMObserver")
