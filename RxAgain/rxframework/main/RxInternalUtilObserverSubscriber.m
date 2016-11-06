//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/ObserverSubscriber.java
//

#include "J2ObjC_source.h"
#include "RxInternalUtilObserverSubscriber.h"
#include "RxObserver.h"
#include "RxSubscriber.h"

@implementation RxInternalUtilObserverSubscriber

- (instancetype)initWithRxObserver:(id<RxObserver>)observer {
  RxInternalUtilObserverSubscriber_initWithRxObserver_(self, observer);
  return self;
}

- (void)onNextWithId:(id)t {
  [((id<RxObserver>) nil_chk(observer_)) onNextWithId:t];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((id<RxObserver>) nil_chk(observer_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  [((id<RxObserver>) nil_chk(observer_)) onCompleted];
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalUtilObserverSubscriber class]);
  RELEASE_(observer_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObserver:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "observer_", "LRxObserver;", .constantValue.asLong = 0, 0x0, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "LRxObserver;", "(Lrx/Observer<-TT;>;)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "Lrx/Observer<-TT;>;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalUtilObserverSubscriber = { "ObserverSubscriber", "rx.internal.util", ptrTable, methods, fields, 7, 0x11, 4, 1, -1, -1, -1, 8, -1 };
  return &_RxInternalUtilObserverSubscriber;
}

@end

void RxInternalUtilObserverSubscriber_initWithRxObserver_(RxInternalUtilObserverSubscriber *self, id<RxObserver> observer) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->observer_, observer);
}

RxInternalUtilObserverSubscriber *new_RxInternalUtilObserverSubscriber_initWithRxObserver_(id<RxObserver> observer) {
  J2OBJC_NEW_IMPL(RxInternalUtilObserverSubscriber, initWithRxObserver_, observer)
}

RxInternalUtilObserverSubscriber *create_RxInternalUtilObserverSubscriber_initWithRxObserver_(id<RxObserver> observer) {
  J2OBJC_CREATE_IMPL(RxInternalUtilObserverSubscriber, initWithRxObserver_, observer)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilObserverSubscriber)
