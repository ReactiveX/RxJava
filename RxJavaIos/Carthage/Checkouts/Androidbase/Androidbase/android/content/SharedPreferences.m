//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/content/SharedPreferences.java
//

#include "J2ObjC_source.h"
#include "android/content/SharedPreferences.h"

@interface AndroidContentSharedPreferences : NSObject

@end

@interface AndroidContentSharedPreferences_OnSharedPreferenceChangeListener : NSObject

@end

@interface AndroidContentSharedPreferences_Editor : NSObject

@end

@implementation AndroidContentSharedPreferences

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LJavaUtilMap;", 0x401, -1, -1, -1, 0, -1, -1 },
    { NULL, "LNSString;", 0x401, 1, 2, -1, -1, -1, -1 },
    { NULL, "I", 0x401, 3, 4, -1, -1, -1, -1 },
    { NULL, "J", 0x401, 5, 6, -1, -1, -1, -1 },
    { NULL, "F", 0x401, 7, 8, -1, -1, -1, -1 },
    { NULL, "Z", 0x401, 9, 10, -1, -1, -1, -1 },
    { NULL, "Z", 0x401, 11, 12, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x401, 13, 14, -1, -1, -1, -1 },
    { NULL, "V", 0x401, 15, 14, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(getAll);
  methods[1].selector = @selector(getStringWithNSString:withNSString:);
  methods[2].selector = @selector(getIntWithNSString:withInt:);
  methods[3].selector = @selector(getLongWithNSString:withLong:);
  methods[4].selector = @selector(getFloatWithNSString:withFloat:);
  methods[5].selector = @selector(getBooleanWithNSString:withBoolean:);
  methods[6].selector = @selector(containsWithNSString:);
  methods[7].selector = @selector(edit);
  methods[8].selector = @selector(registerOnSharedPreferenceChangeListenerWithAndroidContentSharedPreferences_OnSharedPreferenceChangeListener:);
  methods[9].selector = @selector(unregisterOnSharedPreferenceChangeListenerWithAndroidContentSharedPreferences_OnSharedPreferenceChangeListener:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "()Ljava/util/Map<Ljava/lang/String;*>;", "getString", "LNSString;LNSString;", "getInt", "LNSString;I", "getLong", "LNSString;J", "getFloat", "LNSString;F", "getBoolean", "LNSString;Z", "contains", "LNSString;", "registerOnSharedPreferenceChangeListener", "LAndroidContentSharedPreferences_OnSharedPreferenceChangeListener;", "unregisterOnSharedPreferenceChangeListener", "LAndroidContentSharedPreferences_OnSharedPreferenceChangeListener;LAndroidContentSharedPreferences_Editor;" };
  static const J2ObjcClassInfo _AndroidContentSharedPreferences = { "SharedPreferences", "android.content", ptrTable, methods, NULL, 7, 0x609, 10, 0, -1, 16, -1, -1, -1 };
  return &_AndroidContentSharedPreferences;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(AndroidContentSharedPreferences)

@implementation AndroidContentSharedPreferences_OnSharedPreferenceChangeListener

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x401, 0, 1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onSharedPreferenceChangedWithAndroidContentSharedPreferences:withNSString:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "onSharedPreferenceChanged", "LAndroidContentSharedPreferences;LNSString;", "LAndroidContentSharedPreferences;" };
  static const J2ObjcClassInfo _AndroidContentSharedPreferences_OnSharedPreferenceChangeListener = { "OnSharedPreferenceChangeListener", "android.content", ptrTable, methods, NULL, 7, 0x609, 1, 0, 2, -1, -1, -1, -1 };
  return &_AndroidContentSharedPreferences_OnSharedPreferenceChangeListener;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(AndroidContentSharedPreferences_OnSharedPreferenceChangeListener)

@implementation AndroidContentSharedPreferences_Editor

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, 0, 1, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, 2, 3, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, 4, 5, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, 6, 7, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, 8, 9, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, 10, 11, -1, -1, -1, -1 },
    { NULL, "LAndroidContentSharedPreferences_Editor;", 0x401, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x401, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x401, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(putStringWithNSString:withNSString:);
  methods[1].selector = @selector(putIntWithNSString:withInt:);
  methods[2].selector = @selector(putLongWithNSString:withLong:);
  methods[3].selector = @selector(putFloatWithNSString:withFloat:);
  methods[4].selector = @selector(putBooleanWithNSString:withBoolean:);
  methods[5].selector = @selector(removeWithNSString:);
  methods[6].selector = @selector(clear);
  methods[7].selector = @selector(commit);
  methods[8].selector = @selector(apply);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "putString", "LNSString;LNSString;", "putInt", "LNSString;I", "putLong", "LNSString;J", "putFloat", "LNSString;F", "putBoolean", "LNSString;Z", "remove", "LNSString;", "LAndroidContentSharedPreferences;" };
  static const J2ObjcClassInfo _AndroidContentSharedPreferences_Editor = { "Editor", "android.content", ptrTable, methods, NULL, 7, 0x609, 9, 0, 12, -1, -1, -1, -1 };
  return &_AndroidContentSharedPreferences_Editor;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(AndroidContentSharedPreferences_Editor)
