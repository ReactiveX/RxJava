//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbasetest/src/main/java/co/touchlab/doppel/testing/PlatformUtils.java
//

#include "CoTouchlabDoppelTestingPlatformUtils.h"
#include "J2ObjC_source.h"
#include "java/lang/System.h"

@implementation CoTouchlabDoppelTestingPlatformUtils

+ (jboolean)isJ2objc {
  return CoTouchlabDoppelTestingPlatformUtils_isJ2objc();
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  CoTouchlabDoppelTestingPlatformUtils_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "Z", 0x9, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(isJ2objc);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcClassInfo _CoTouchlabDoppelTestingPlatformUtils = { "PlatformUtils", "co.touchlab.doppel.testing", NULL, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_CoTouchlabDoppelTestingPlatformUtils;
}

@end

jboolean CoTouchlabDoppelTestingPlatformUtils_isJ2objc() {
  CoTouchlabDoppelTestingPlatformUtils_initialize();
  return [((NSString *) nil_chk(JavaLangSystem_getPropertyWithNSString_(@"java.vendor"))) contains:@"J2ObjC"];
}

void CoTouchlabDoppelTestingPlatformUtils_init(CoTouchlabDoppelTestingPlatformUtils *self) {
  NSObject_init(self);
}

CoTouchlabDoppelTestingPlatformUtils *new_CoTouchlabDoppelTestingPlatformUtils_init() {
  J2OBJC_NEW_IMPL(CoTouchlabDoppelTestingPlatformUtils, init)
}

CoTouchlabDoppelTestingPlatformUtils *create_CoTouchlabDoppelTestingPlatformUtils_init() {
  J2OBJC_CREATE_IMPL(CoTouchlabDoppelTestingPlatformUtils, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(CoTouchlabDoppelTestingPlatformUtils)
