//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/Producer.java
//

#include "J2ObjC_source.h"
#include "RxProducer.h"

@interface RxProducer : NSObject

@end

@implementation RxProducer

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x401, 0, 1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "request", "J" };
  static const J2ObjcClassInfo _RxProducer = { "Producer", "rx", ptrTable, methods, NULL, 7, 0x609, 1, 0, -1, -1, -1, -1, -1 };
  return &_RxProducer;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(RxProducer)
