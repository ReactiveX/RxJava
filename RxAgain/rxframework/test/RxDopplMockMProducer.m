//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/doppl/mock/MProducer.java
//

#include "J2ObjC_source.h"
#include "RxDopplMockMProducer.h"

@implementation RxDopplMockMProducer

- (void)requestWithLong:(jlong)n {
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxDopplMockMProducer_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "request", "J" };
  static const J2ObjcClassInfo _RxDopplMockMProducer = { "MProducer", "rx.doppl.mock", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_RxDopplMockMProducer;
}

@end

void RxDopplMockMProducer_init(RxDopplMockMProducer *self) {
  NSObject_init(self);
}

RxDopplMockMProducer *new_RxDopplMockMProducer_init() {
  J2OBJC_NEW_IMPL(RxDopplMockMProducer, init)
}

RxDopplMockMProducer *create_RxDopplMockMProducer_init() {
  J2OBJC_CREATE_IMPL(RxDopplMockMProducer, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxDopplMockMProducer)
