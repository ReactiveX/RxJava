//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/doppl/mock/MObserver.java
//

#include "J2ObjC_source.h"
#include "RxDopplMockMObserver.h"

@implementation RxDopplMockMObserver

- (void)onCompleted {
}

- (void)onErrorWithNSException:(NSException *)e {
}

- (void)onNextWithId:(id)o {
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxDopplMockMObserver_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onCompleted);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onNextWithId:);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "onError", "LNSException;", "onNext", "LNSObject;", "(Ljava/lang/Object;)V", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observer<TT;>;" };
  static const J2ObjcClassInfo _RxDopplMockMObserver = { "MObserver", "rx.doppl.mock", ptrTable, methods, NULL, 7, 0x1, 4, 0, -1, -1, -1, 5, -1 };
  return &_RxDopplMockMObserver;
}

@end

void RxDopplMockMObserver_init(RxDopplMockMObserver *self) {
  NSObject_init(self);
}

RxDopplMockMObserver *new_RxDopplMockMObserver_init() {
  J2OBJC_NEW_IMPL(RxDopplMockMObserver, init)
}

RxDopplMockMObserver *create_RxDopplMockMObserver_init() {
  J2OBJC_CREATE_IMPL(RxDopplMockMObserver, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxDopplMockMObserver)
