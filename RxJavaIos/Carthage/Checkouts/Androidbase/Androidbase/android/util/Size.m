//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/util/Size.java
//

#include "J2ObjC_source.h"
#include "android/util/Size.h"
#include "java/lang/Integer.h"
#include "java/lang/NumberFormatException.h"

@interface AndroidUtilSize () {
 @public
  jint mWidth_;
  jint mHeight_;
}

+ (JavaLangNumberFormatException *)invalidSizeWithNSString:(NSString *)s;

@end

__attribute__((unused)) static JavaLangNumberFormatException *AndroidUtilSize_invalidSizeWithNSString_(NSString *s);

@implementation AndroidUtilSize

- (instancetype)initWithInt:(jint)width
                    withInt:(jint)height {
  AndroidUtilSize_initWithInt_withInt_(self, width, height);
  return self;
}

- (jint)getWidth {
  return mWidth_;
}

- (jint)getHeight {
  return mHeight_;
}

- (jboolean)isEqual:(id)obj {
  if (obj == nil) {
    return false;
  }
  if (self == obj) {
    return true;
  }
  if ([obj isKindOfClass:[AndroidUtilSize class]]) {
    AndroidUtilSize *other = (AndroidUtilSize *) cast_chk(obj, [AndroidUtilSize class]);
    return mWidth_ == other->mWidth_ && mHeight_ == other->mHeight_;
  }
  return false;
}

- (NSString *)description {
  return JreStrcat("ICI", mWidth_, 'x', mHeight_);
}

+ (JavaLangNumberFormatException *)invalidSizeWithNSString:(NSString *)s {
  return AndroidUtilSize_invalidSizeWithNSString_(s);
}

+ (AndroidUtilSize *)parseSizeWithNSString:(NSString *)string {
  return AndroidUtilSize_parseSizeWithNSString_(string);
}

- (NSUInteger)hash {
  return mHeight_ ^ ((JreLShift32(mWidth_, (JavaLangInteger_SIZE / 2))) | (JreURShift32(mWidth_, (JavaLangInteger_SIZE / 2))));
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "I", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 1, 2, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0x1, 3, -1, -1, -1, -1, -1 },
    { NULL, "LJavaLangNumberFormatException;", 0xa, 4, 5, -1, -1, -1, -1 },
    { NULL, "LAndroidUtilSize;", 0x9, 6, 5, 7, -1, -1, -1 },
    { NULL, "I", 0x1, 8, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithInt:withInt:);
  methods[1].selector = @selector(getWidth);
  methods[2].selector = @selector(getHeight);
  methods[3].selector = @selector(isEqual:);
  methods[4].selector = @selector(description);
  methods[5].selector = @selector(invalidSizeWithNSString:);
  methods[6].selector = @selector(parseSizeWithNSString:);
  methods[7].selector = @selector(hash);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "mWidth_", "I", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "mHeight_", "I", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "II", "equals", "LNSObject;", "toString", "invalidSize", "LNSString;", "parseSize", "LJavaLangNumberFormatException;", "hashCode" };
  static const J2ObjcClassInfo _AndroidUtilSize = { "Size", "android.util", ptrTable, methods, fields, 7, 0x11, 8, 2, -1, -1, -1, -1, -1 };
  return &_AndroidUtilSize;
}

@end

void AndroidUtilSize_initWithInt_withInt_(AndroidUtilSize *self, jint width, jint height) {
  NSObject_init(self);
  self->mWidth_ = width;
  self->mHeight_ = height;
}

AndroidUtilSize *new_AndroidUtilSize_initWithInt_withInt_(jint width, jint height) {
  J2OBJC_NEW_IMPL(AndroidUtilSize, initWithInt_withInt_, width, height)
}

AndroidUtilSize *create_AndroidUtilSize_initWithInt_withInt_(jint width, jint height) {
  J2OBJC_CREATE_IMPL(AndroidUtilSize, initWithInt_withInt_, width, height)
}

JavaLangNumberFormatException *AndroidUtilSize_invalidSizeWithNSString_(NSString *s) {
  AndroidUtilSize_initialize();
  @throw create_JavaLangNumberFormatException_initWithNSString_(JreStrcat("$$C", @"Invalid Size: \"", s, '"'));
}

AndroidUtilSize *AndroidUtilSize_parseSizeWithNSString_(NSString *string) {
  AndroidUtilSize_initialize();
  jint sep_ix = [((NSString *) nil_chk(string)) indexOf:'*'];
  if (sep_ix < 0) {
    sep_ix = [string indexOf:'x'];
  }
  if (sep_ix < 0) {
    @throw AndroidUtilSize_invalidSizeWithNSString_(string);
  }
  @try {
    return create_AndroidUtilSize_initWithInt_withInt_(JavaLangInteger_parseIntWithNSString_([string substring:0 endIndex:sep_ix]), JavaLangInteger_parseIntWithNSString_([string substring:sep_ix + 1]));
  }
  @catch (JavaLangNumberFormatException *e) {
    @throw AndroidUtilSize_invalidSizeWithNSString_(string);
  }
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidUtilSize)
