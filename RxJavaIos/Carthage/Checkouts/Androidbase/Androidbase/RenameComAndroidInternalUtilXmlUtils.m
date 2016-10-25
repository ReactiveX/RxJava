//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/rename/com/android/internal/util/XmlUtils.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "IOSPrimitiveArray.h"
#include "J2ObjC_source.h"
#include "RenameComAndroidInternalUtilFastXmlSerializer.h"
#include "RenameComAndroidInternalUtilXmlUtils.h"
#include "android/util/Xml.h"
#include "java/io/InputStream.h"
#include "java/io/OutputStream.h"
#include "java/lang/Boolean.h"
#include "java/lang/CharSequence.h"
#include "java/lang/Double.h"
#include "java/lang/Float.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/lang/NullPointerException.h"
#include "java/lang/NumberFormatException.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "java/util/Iterator.h"
#include "java/util/List.h"
#include "java/util/Map.h"
#include "java/util/Set.h"
#include "org/xmlpull/v1/XmlPullParser.h"
#include "org/xmlpull/v1/XmlPullParserException.h"
#include "org/xmlpull/v1/XmlPullParserFactory.h"
#include "org/xmlpull/v1/XmlSerializer.h"

@interface RenameComAndroidInternalUtilXmlUtils ()

+ (id)readThisValueXmlWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                                  withNSStringArray:(IOSObjectArray *)name;

@end

__attribute__((unused)) static id RenameComAndroidInternalUtilXmlUtils_readThisValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(id<OrgXmlpullV1XmlPullParser> parser, IOSObjectArray *name);

@implementation RenameComAndroidInternalUtilXmlUtils

+ (void)skipCurrentTagWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser {
  RenameComAndroidInternalUtilXmlUtils_skipCurrentTagWithOrgXmlpullV1XmlPullParser_(parser);
}

+ (jint)convertValueToListWithJavaLangCharSequence:(id<JavaLangCharSequence>)value
                                 withNSStringArray:(IOSObjectArray *)options
                                           withInt:(jint)defaultValue {
  return RenameComAndroidInternalUtilXmlUtils_convertValueToListWithJavaLangCharSequence_withNSStringArray_withInt_(value, options, defaultValue);
}

+ (jboolean)convertValueToBooleanWithJavaLangCharSequence:(id<JavaLangCharSequence>)value
                                              withBoolean:(jboolean)defaultValue {
  return RenameComAndroidInternalUtilXmlUtils_convertValueToBooleanWithJavaLangCharSequence_withBoolean_(value, defaultValue);
}

+ (jint)convertValueToIntWithJavaLangCharSequence:(id<JavaLangCharSequence>)charSeq
                                          withInt:(jint)defaultValue {
  return RenameComAndroidInternalUtilXmlUtils_convertValueToIntWithJavaLangCharSequence_withInt_(charSeq, defaultValue);
}

+ (jint)convertValueToUnsignedIntWithNSString:(NSString *)value
                                      withInt:(jint)defaultValue {
  return RenameComAndroidInternalUtilXmlUtils_convertValueToUnsignedIntWithNSString_withInt_(value, defaultValue);
}

+ (jint)parseUnsignedIntAttributeWithJavaLangCharSequence:(id<JavaLangCharSequence>)charSeq {
  return RenameComAndroidInternalUtilXmlUtils_parseUnsignedIntAttributeWithJavaLangCharSequence_(charSeq);
}

+ (void)writeMapXmlWithJavaUtilMap:(id<JavaUtilMap>)val
            withJavaIoOutputStream:(JavaIoOutputStream *)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeMapXmlWithJavaUtilMap_withJavaIoOutputStream_(val, outArg);
}

+ (void)writeListXmlWithJavaUtilList:(id<JavaUtilList>)val
              withJavaIoOutputStream:(JavaIoOutputStream *)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeListXmlWithJavaUtilList_withJavaIoOutputStream_(val, outArg);
}

+ (void)writeMapXmlWithJavaUtilMap:(id<JavaUtilMap>)val
                      withNSString:(NSString *)name
     withOrgXmlpullV1XmlSerializer:(id<OrgXmlpullV1XmlSerializer>)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeMapXmlWithJavaUtilMap_withNSString_withOrgXmlpullV1XmlSerializer_(val, name, outArg);
}

+ (void)writeListXmlWithJavaUtilList:(id<JavaUtilList>)val
                        withNSString:(NSString *)name
       withOrgXmlpullV1XmlSerializer:(id<OrgXmlpullV1XmlSerializer>)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeListXmlWithJavaUtilList_withNSString_withOrgXmlpullV1XmlSerializer_(val, name, outArg);
}

+ (void)writeByteArrayXmlWithByteArray:(IOSByteArray *)val
                          withNSString:(NSString *)name
         withOrgXmlpullV1XmlSerializer:(id<OrgXmlpullV1XmlSerializer>)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeByteArrayXmlWithByteArray_withNSString_withOrgXmlpullV1XmlSerializer_(val, name, outArg);
}

+ (void)writeIntArrayXmlWithIntArray:(IOSIntArray *)val
                        withNSString:(NSString *)name
       withOrgXmlpullV1XmlSerializer:(id<OrgXmlpullV1XmlSerializer>)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeIntArrayXmlWithIntArray_withNSString_withOrgXmlpullV1XmlSerializer_(val, name, outArg);
}

+ (void)writeValueXmlWithId:(id)v
               withNSString:(NSString *)name
withOrgXmlpullV1XmlSerializer:(id<OrgXmlpullV1XmlSerializer>)outArg {
  RenameComAndroidInternalUtilXmlUtils_writeValueXmlWithId_withNSString_withOrgXmlpullV1XmlSerializer_(v, name, outArg);
}

+ (JavaUtilHashMap *)readMapXmlWithJavaIoInputStream:(JavaIoInputStream *)inArg {
  return RenameComAndroidInternalUtilXmlUtils_readMapXmlWithJavaIoInputStream_(inArg);
}

+ (JavaUtilArrayList *)readListXmlWithJavaIoInputStream:(JavaIoInputStream *)inArg {
  return RenameComAndroidInternalUtilXmlUtils_readListXmlWithJavaIoInputStream_(inArg);
}

+ (JavaUtilHashMap *)readThisMapXmlWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                                                    withNSString:(NSString *)endTag
                                               withNSStringArray:(IOSObjectArray *)name {
  return RenameComAndroidInternalUtilXmlUtils_readThisMapXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(parser, endTag, name);
}

+ (JavaUtilArrayList *)readThisListXmlWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                                                       withNSString:(NSString *)endTag
                                                  withNSStringArray:(IOSObjectArray *)name {
  return RenameComAndroidInternalUtilXmlUtils_readThisListXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(parser, endTag, name);
}

+ (IOSIntArray *)readThisIntArrayXmlWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                                                     withNSString:(NSString *)endTag
                                                withNSStringArray:(IOSObjectArray *)name {
  return RenameComAndroidInternalUtilXmlUtils_readThisIntArrayXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(parser, endTag, name);
}

+ (id)readValueXmlWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                              withNSStringArray:(IOSObjectArray *)name {
  return RenameComAndroidInternalUtilXmlUtils_readValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, name);
}

+ (id)readThisValueXmlWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                                  withNSStringArray:(IOSObjectArray *)name {
  return RenameComAndroidInternalUtilXmlUtils_readThisValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, name);
}

+ (void)beginDocumentWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser
                                      withNSString:(NSString *)firstElementName {
  RenameComAndroidInternalUtilXmlUtils_beginDocumentWithOrgXmlpullV1XmlPullParser_withNSString_(parser, firstElementName);
}

+ (void)nextElementWithOrgXmlpullV1XmlPullParser:(id<OrgXmlpullV1XmlPullParser>)parser {
  RenameComAndroidInternalUtilXmlUtils_nextElementWithOrgXmlpullV1XmlPullParser_(parser);
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RenameComAndroidInternalUtilXmlUtils_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x9, 0, 1, 2, -1, -1, -1 },
    { NULL, "I", 0x19, 3, 4, -1, -1, -1, -1 },
    { NULL, "Z", 0x19, 5, 6, -1, -1, -1, -1 },
    { NULL, "I", 0x19, 7, 8, -1, -1, -1, -1 },
    { NULL, "I", 0x19, 9, 10, -1, -1, -1, -1 },
    { NULL, "I", 0x19, 11, 12, -1, -1, -1, -1 },
    { NULL, "V", 0x19, 13, 14, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 15, 16, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 13, 17, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 15, 18, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 19, 20, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 21, 22, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 23, 24, 2, -1, -1, -1 },
    { NULL, "LJavaUtilHashMap;", 0x19, 25, 26, 2, -1, -1, -1 },
    { NULL, "LJavaUtilArrayList;", 0x19, 27, 26, 2, -1, -1, -1 },
    { NULL, "LJavaUtilHashMap;", 0x19, 28, 29, 2, -1, -1, -1 },
    { NULL, "LJavaUtilArrayList;", 0x19, 30, 29, 2, -1, -1, -1 },
    { NULL, "[I", 0x19, 31, 29, 2, -1, -1, -1 },
    { NULL, "LNSObject;", 0x19, 32, 33, 2, -1, -1, -1 },
    { NULL, "LNSObject;", 0x1a, 34, 33, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 35, 36, 2, -1, -1, -1 },
    { NULL, "V", 0x19, 37, 1, 2, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(skipCurrentTagWithOrgXmlpullV1XmlPullParser:);
  methods[1].selector = @selector(convertValueToListWithJavaLangCharSequence:withNSStringArray:withInt:);
  methods[2].selector = @selector(convertValueToBooleanWithJavaLangCharSequence:withBoolean:);
  methods[3].selector = @selector(convertValueToIntWithJavaLangCharSequence:withInt:);
  methods[4].selector = @selector(convertValueToUnsignedIntWithNSString:withInt:);
  methods[5].selector = @selector(parseUnsignedIntAttributeWithJavaLangCharSequence:);
  methods[6].selector = @selector(writeMapXmlWithJavaUtilMap:withJavaIoOutputStream:);
  methods[7].selector = @selector(writeListXmlWithJavaUtilList:withJavaIoOutputStream:);
  methods[8].selector = @selector(writeMapXmlWithJavaUtilMap:withNSString:withOrgXmlpullV1XmlSerializer:);
  methods[9].selector = @selector(writeListXmlWithJavaUtilList:withNSString:withOrgXmlpullV1XmlSerializer:);
  methods[10].selector = @selector(writeByteArrayXmlWithByteArray:withNSString:withOrgXmlpullV1XmlSerializer:);
  methods[11].selector = @selector(writeIntArrayXmlWithIntArray:withNSString:withOrgXmlpullV1XmlSerializer:);
  methods[12].selector = @selector(writeValueXmlWithId:withNSString:withOrgXmlpullV1XmlSerializer:);
  methods[13].selector = @selector(readMapXmlWithJavaIoInputStream:);
  methods[14].selector = @selector(readListXmlWithJavaIoInputStream:);
  methods[15].selector = @selector(readThisMapXmlWithOrgXmlpullV1XmlPullParser:withNSString:withNSStringArray:);
  methods[16].selector = @selector(readThisListXmlWithOrgXmlpullV1XmlPullParser:withNSString:withNSStringArray:);
  methods[17].selector = @selector(readThisIntArrayXmlWithOrgXmlpullV1XmlPullParser:withNSString:withNSStringArray:);
  methods[18].selector = @selector(readValueXmlWithOrgXmlpullV1XmlPullParser:withNSStringArray:);
  methods[19].selector = @selector(readThisValueXmlWithOrgXmlpullV1XmlPullParser:withNSStringArray:);
  methods[20].selector = @selector(beginDocumentWithOrgXmlpullV1XmlPullParser:withNSString:);
  methods[21].selector = @selector(nextElementWithOrgXmlpullV1XmlPullParser:);
  methods[22].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "skipCurrentTag", "LOrgXmlpullV1XmlPullParser;", "LOrgXmlpullV1XmlPullParserException;LJavaIoIOException;", "convertValueToList", "LJavaLangCharSequence;[LNSString;I", "convertValueToBoolean", "LJavaLangCharSequence;Z", "convertValueToInt", "LJavaLangCharSequence;I", "convertValueToUnsignedInt", "LNSString;I", "parseUnsignedIntAttribute", "LJavaLangCharSequence;", "writeMapXml", "LJavaUtilMap;LJavaIoOutputStream;", "writeListXml", "LJavaUtilList;LJavaIoOutputStream;", "LJavaUtilMap;LNSString;LOrgXmlpullV1XmlSerializer;", "LJavaUtilList;LNSString;LOrgXmlpullV1XmlSerializer;", "writeByteArrayXml", "[BLNSString;LOrgXmlpullV1XmlSerializer;", "writeIntArrayXml", "[ILNSString;LOrgXmlpullV1XmlSerializer;", "writeValueXml", "LNSObject;LNSString;LOrgXmlpullV1XmlSerializer;", "readMapXml", "LJavaIoInputStream;", "readListXml", "readThisMapXml", "LOrgXmlpullV1XmlPullParser;LNSString;[LNSString;", "readThisListXml", "readThisIntArrayXml", "readValueXml", "LOrgXmlpullV1XmlPullParser;[LNSString;", "readThisValueXml", "beginDocument", "LOrgXmlpullV1XmlPullParser;LNSString;", "nextElement" };
  static const J2ObjcClassInfo _RenameComAndroidInternalUtilXmlUtils = { "XmlUtils", "rename.com.android.internal.util", ptrTable, methods, NULL, 7, 0x1, 23, 0, -1, -1, -1, -1, -1 };
  return &_RenameComAndroidInternalUtilXmlUtils;
}

@end

void RenameComAndroidInternalUtilXmlUtils_skipCurrentTagWithOrgXmlpullV1XmlPullParser_(id<OrgXmlpullV1XmlPullParser> parser) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  jint outerDepth = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) getDepth];
  jint type;
  while ((type = [parser next]) != OrgXmlpullV1XmlPullParser_END_DOCUMENT && (type != OrgXmlpullV1XmlPullParser_END_TAG || [parser getDepth] > outerDepth)) {
  }
}

jint RenameComAndroidInternalUtilXmlUtils_convertValueToListWithJavaLangCharSequence_withNSStringArray_withInt_(id<JavaLangCharSequence> value, IOSObjectArray *options, jint defaultValue) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (nil != value) {
    for (jint i = 0; i < ((IOSObjectArray *) nil_chk(options))->size_; i++) {
      if ([value isEqual:IOSObjectArray_Get(options, i)]) return i;
    }
  }
  return defaultValue;
}

jboolean RenameComAndroidInternalUtilXmlUtils_convertValueToBooleanWithJavaLangCharSequence_withBoolean_(id<JavaLangCharSequence> value, jboolean defaultValue) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  jboolean result = false;
  if (nil == value) return defaultValue;
  if ([value isEqual:@"1"] || [value isEqual:@"true"] || [value isEqual:@"TRUE"]) result = true;
  return result;
}

jint RenameComAndroidInternalUtilXmlUtils_convertValueToIntWithJavaLangCharSequence_withInt_(id<JavaLangCharSequence> charSeq, jint defaultValue) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (nil == charSeq) return defaultValue;
  NSString *nm = [charSeq description];
  jint value;
  jint sign = 1;
  jint index = 0;
  jint len = ((jint) [((NSString *) nil_chk(nm)) length]);
  jint base = 10;
  if ('-' == [nm charAtWithInt:0]) {
    sign = -1;
    index++;
  }
  if ('0' == [nm charAtWithInt:index]) {
    if (index == (len - 1)) return 0;
    jchar c = [nm charAtWithInt:index + 1];
    if ('x' == c || 'X' == c) {
      index += 2;
      base = 16;
    }
    else {
      index++;
      base = 8;
    }
  }
  else if ('#' == [nm charAtWithInt:index]) {
    index++;
    base = 16;
  }
  return JavaLangInteger_parseIntWithNSString_withInt_([nm substring:index], base) * sign;
}

jint RenameComAndroidInternalUtilXmlUtils_convertValueToUnsignedIntWithNSString_withInt_(NSString *value, jint defaultValue) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (nil == value) return defaultValue;
  return RenameComAndroidInternalUtilXmlUtils_parseUnsignedIntAttributeWithJavaLangCharSequence_(value);
}

jint RenameComAndroidInternalUtilXmlUtils_parseUnsignedIntAttributeWithJavaLangCharSequence_(id<JavaLangCharSequence> charSeq) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  NSString *value = [((id<JavaLangCharSequence>) nil_chk(charSeq)) description];
  jlong bits;
  jint index = 0;
  jint len = ((jint) [((NSString *) nil_chk(value)) length]);
  jint base = 10;
  if ('0' == [value charAtWithInt:index]) {
    if (index == (len - 1)) return 0;
    jchar c = [value charAtWithInt:index + 1];
    if ('x' == c || 'X' == c) {
      index += 2;
      base = 16;
    }
    else {
      index++;
      base = 8;
    }
  }
  else if ('#' == [value charAtWithInt:index]) {
    index++;
    base = 16;
  }
  return (jint) JavaLangLong_parseLongWithNSString_withInt_([value substring:index], base);
}

void RenameComAndroidInternalUtilXmlUtils_writeMapXmlWithJavaUtilMap_withJavaIoOutputStream_(id<JavaUtilMap> val, JavaIoOutputStream *outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  id<OrgXmlpullV1XmlSerializer> serializer = create_RenameComAndroidInternalUtilFastXmlSerializer_init();
  [serializer setOutputWithJavaIoOutputStream:outArg withNSString:@"utf-8"];
  [serializer startDocumentWithNSString:nil withJavaLangBoolean:JavaLangBoolean_valueOfWithBoolean_(true)];
  [serializer setFeatureWithNSString:@"http://xmlpull.org/v1/doc/features.html#indent-output" withBoolean:true];
  RenameComAndroidInternalUtilXmlUtils_writeMapXmlWithJavaUtilMap_withNSString_withOrgXmlpullV1XmlSerializer_(val, nil, serializer);
  [serializer endDocument];
}

void RenameComAndroidInternalUtilXmlUtils_writeListXmlWithJavaUtilList_withJavaIoOutputStream_(id<JavaUtilList> val, JavaIoOutputStream *outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  id<OrgXmlpullV1XmlSerializer> serializer = AndroidUtilXml_newSerializer();
  [((id<OrgXmlpullV1XmlSerializer>) nil_chk(serializer)) setOutputWithJavaIoOutputStream:outArg withNSString:@"utf-8"];
  [serializer startDocumentWithNSString:nil withJavaLangBoolean:JavaLangBoolean_valueOfWithBoolean_(true)];
  [serializer setFeatureWithNSString:@"http://xmlpull.org/v1/doc/features.html#indent-output" withBoolean:true];
  RenameComAndroidInternalUtilXmlUtils_writeListXmlWithJavaUtilList_withNSString_withOrgXmlpullV1XmlSerializer_(val, nil, serializer);
  [serializer endDocument];
}

void RenameComAndroidInternalUtilXmlUtils_writeMapXmlWithJavaUtilMap_withNSString_withOrgXmlpullV1XmlSerializer_(id<JavaUtilMap> val, NSString *name, id<OrgXmlpullV1XmlSerializer> outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (val == nil) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"null"];
    [outArg endTagWithNSString:nil withNSString:@"null"];
    return;
  }
  id<JavaUtilSet> s = [val entrySet];
  id<JavaUtilIterator> i = [((id<JavaUtilSet>) nil_chk(s)) iterator];
  [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"map"];
  if (name != nil) {
    [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
  }
  while ([((id<JavaUtilIterator>) nil_chk(i)) hasNext]) {
    id<JavaUtilMap_Entry> e = (id<JavaUtilMap_Entry>) cast_check([i next], JavaUtilMap_Entry_class_());
    RenameComAndroidInternalUtilXmlUtils_writeValueXmlWithId_withNSString_withOrgXmlpullV1XmlSerializer_([((id<JavaUtilMap_Entry>) nil_chk(e)) getValue], (NSString *) cast_chk([e getKey], [NSString class]), outArg);
  }
  [outArg endTagWithNSString:nil withNSString:@"map"];
}

void RenameComAndroidInternalUtilXmlUtils_writeListXmlWithJavaUtilList_withNSString_withOrgXmlpullV1XmlSerializer_(id<JavaUtilList> val, NSString *name, id<OrgXmlpullV1XmlSerializer> outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (val == nil) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"null"];
    [outArg endTagWithNSString:nil withNSString:@"null"];
    return;
  }
  [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"list"];
  if (name != nil) {
    [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
  }
  jint N = [val size];
  jint i = 0;
  while (i < N) {
    RenameComAndroidInternalUtilXmlUtils_writeValueXmlWithId_withNSString_withOrgXmlpullV1XmlSerializer_([val getWithInt:i], nil, outArg);
    i++;
  }
  [outArg endTagWithNSString:nil withNSString:@"list"];
}

void RenameComAndroidInternalUtilXmlUtils_writeByteArrayXmlWithByteArray_withNSString_withOrgXmlpullV1XmlSerializer_(IOSByteArray *val, NSString *name, id<OrgXmlpullV1XmlSerializer> outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (val == nil) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"null"];
    [outArg endTagWithNSString:nil withNSString:@"null"];
    return;
  }
  [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"byte-array"];
  if (name != nil) {
    [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
  }
  jint N = val->size_;
  [outArg attributeWithNSString:nil withNSString:@"num" withNSString:JavaLangInteger_toStringWithInt_(N)];
  JavaLangStringBuilder *sb = create_JavaLangStringBuilder_initWithInt_(val->size_ * 2);
  for (jint i = 0; i < N; i++) {
    jint b = IOSByteArray_Get(val, i);
    jint h = JreRShift32(b, 4);
    [sb appendWithInt:h >= 10 ? ('a' + h - 10) : ('0' + h)];
    h = b & (jint) 0xff;
    [sb appendWithInt:h >= 10 ? ('a' + h - 10) : ('0' + h)];
  }
  [outArg textWithNSString:[sb description]];
  [outArg endTagWithNSString:nil withNSString:@"byte-array"];
}

void RenameComAndroidInternalUtilXmlUtils_writeIntArrayXmlWithIntArray_withNSString_withOrgXmlpullV1XmlSerializer_(IOSIntArray *val, NSString *name, id<OrgXmlpullV1XmlSerializer> outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  if (val == nil) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"null"];
    [outArg endTagWithNSString:nil withNSString:@"null"];
    return;
  }
  [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"int-array"];
  if (name != nil) {
    [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
  }
  jint N = val->size_;
  [outArg attributeWithNSString:nil withNSString:@"num" withNSString:JavaLangInteger_toStringWithInt_(N)];
  for (jint i = 0; i < N; i++) {
    [outArg startTagWithNSString:nil withNSString:@"item"];
    [outArg attributeWithNSString:nil withNSString:@"value" withNSString:JavaLangInteger_toStringWithInt_(IOSIntArray_Get(val, i))];
    [outArg endTagWithNSString:nil withNSString:@"item"];
  }
  [outArg endTagWithNSString:nil withNSString:@"int-array"];
}

void RenameComAndroidInternalUtilXmlUtils_writeValueXmlWithId_withNSString_withOrgXmlpullV1XmlSerializer_(id v, NSString *name, id<OrgXmlpullV1XmlSerializer> outArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  NSString *typeStr;
  if (v == nil) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"null"];
    if (name != nil) {
      [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
    }
    [outArg endTagWithNSString:nil withNSString:@"null"];
    return;
  }
  else if ([v isKindOfClass:[NSString class]]) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"string"];
    if (name != nil) {
      [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
    }
    [outArg textWithNSString:[v description]];
    [outArg endTagWithNSString:nil withNSString:@"string"];
    return;
  }
  else if ([v isKindOfClass:[JavaLangInteger class]]) {
    typeStr = @"int";
  }
  else if ([v isKindOfClass:[JavaLangLong class]]) {
    typeStr = @"long";
  }
  else if ([v isKindOfClass:[JavaLangFloat class]]) {
    typeStr = @"float";
  }
  else if ([v isKindOfClass:[JavaLangDouble class]]) {
    typeStr = @"double";
  }
  else if ([v isKindOfClass:[JavaLangBoolean class]]) {
    typeStr = @"boolean";
  }
  else if ([v isKindOfClass:[IOSByteArray class]]) {
    RenameComAndroidInternalUtilXmlUtils_writeByteArrayXmlWithByteArray_withNSString_withOrgXmlpullV1XmlSerializer_((IOSByteArray *) cast_chk(v, [IOSByteArray class]), name, outArg);
    return;
  }
  else if ([v isKindOfClass:[IOSIntArray class]]) {
    RenameComAndroidInternalUtilXmlUtils_writeIntArrayXmlWithIntArray_withNSString_withOrgXmlpullV1XmlSerializer_((IOSIntArray *) cast_chk(v, [IOSIntArray class]), name, outArg);
    return;
  }
  else if ([JavaUtilMap_class_() isInstance:v]) {
    RenameComAndroidInternalUtilXmlUtils_writeMapXmlWithJavaUtilMap_withNSString_withOrgXmlpullV1XmlSerializer_((id<JavaUtilMap>) cast_check(v, JavaUtilMap_class_()), name, outArg);
    return;
  }
  else if ([JavaUtilList_class_() isInstance:v]) {
    RenameComAndroidInternalUtilXmlUtils_writeListXmlWithJavaUtilList_withNSString_withOrgXmlpullV1XmlSerializer_((id<JavaUtilList>) cast_check(v, JavaUtilList_class_()), name, outArg);
    return;
  }
  else if ([JavaLangCharSequence_class_() isInstance:v]) {
    [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:@"string"];
    if (name != nil) {
      [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
    }
    [outArg textWithNSString:[v description]];
    [outArg endTagWithNSString:nil withNSString:@"string"];
    return;
  }
  else {
    @throw create_JavaLangRuntimeException_initWithNSString_(JreStrcat("$@", @"writeValueXml: unable to write value ", v));
  }
  [((id<OrgXmlpullV1XmlSerializer>) nil_chk(outArg)) startTagWithNSString:nil withNSString:typeStr];
  if (name != nil) {
    [outArg attributeWithNSString:nil withNSString:@"name" withNSString:name];
  }
  [outArg attributeWithNSString:nil withNSString:@"value" withNSString:[v description]];
  [outArg endTagWithNSString:nil withNSString:typeStr];
}

JavaUtilHashMap *RenameComAndroidInternalUtilXmlUtils_readMapXmlWithJavaIoInputStream_(JavaIoInputStream *inArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  id<OrgXmlpullV1XmlPullParser> parser = [((OrgXmlpullV1XmlPullParserFactory *) nil_chk(OrgXmlpullV1XmlPullParserFactory_newInstance())) newPullParser];
  [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) setInputWithJavaIoInputStream:inArg withNSString:nil];
  return (JavaUtilHashMap *) cast_chk(RenameComAndroidInternalUtilXmlUtils_readValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, [IOSObjectArray arrayWithLength:1 type:NSString_class_()]), [JavaUtilHashMap class]);
}

JavaUtilArrayList *RenameComAndroidInternalUtilXmlUtils_readListXmlWithJavaIoInputStream_(JavaIoInputStream *inArg) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  id<OrgXmlpullV1XmlPullParser> parser = AndroidUtilXml_newPullParser();
  [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) setInputWithJavaIoInputStream:inArg withNSString:nil];
  return (JavaUtilArrayList *) cast_chk(RenameComAndroidInternalUtilXmlUtils_readValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, [IOSObjectArray arrayWithLength:1 type:NSString_class_()]), [JavaUtilArrayList class]);
}

JavaUtilHashMap *RenameComAndroidInternalUtilXmlUtils_readThisMapXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(id<OrgXmlpullV1XmlPullParser> parser, NSString *endTag, IOSObjectArray *name) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  JavaUtilHashMap *map = create_JavaUtilHashMap_init();
  jint eventType = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) getEventType];
  do {
    if (eventType == OrgXmlpullV1XmlPullParser_START_TAG) {
      id val = RenameComAndroidInternalUtilXmlUtils_readThisValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, name);
      if (IOSObjectArray_Get(nil_chk(name), 0) != nil) {
        [map putWithId:IOSObjectArray_Get(name, 0) withId:val];
      }
      else {
        @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Map value without name attribute: ", [parser getName]));
      }
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_END_TAG) {
      if ([((NSString *) nil_chk([parser getName])) isEqual:endTag]) {
        return map;
      }
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Expected ", endTag, @" end tag at: ", [parser getName]));
    }
    eventType = [parser next];
  }
  while (eventType != OrgXmlpullV1XmlPullParser_END_DOCUMENT);
  @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$", @"Document ended before ", endTag, @" end tag"));
}

JavaUtilArrayList *RenameComAndroidInternalUtilXmlUtils_readThisListXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(id<OrgXmlpullV1XmlPullParser> parser, NSString *endTag, IOSObjectArray *name) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  JavaUtilArrayList *list = create_JavaUtilArrayList_init();
  jint eventType = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) getEventType];
  do {
    if (eventType == OrgXmlpullV1XmlPullParser_START_TAG) {
      id val = RenameComAndroidInternalUtilXmlUtils_readThisValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, name);
      [list addWithId:val];
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_END_TAG) {
      if ([((NSString *) nil_chk([parser getName])) isEqual:endTag]) {
        return list;
      }
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Expected ", endTag, @" end tag at: ", [parser getName]));
    }
    eventType = [parser next];
  }
  while (eventType != OrgXmlpullV1XmlPullParser_END_DOCUMENT);
  @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$", @"Document ended before ", endTag, @" end tag"));
}

IOSIntArray *RenameComAndroidInternalUtilXmlUtils_readThisIntArrayXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(id<OrgXmlpullV1XmlPullParser> parser, NSString *endTag, IOSObjectArray *name) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  jint num;
  @try {
    num = JavaLangInteger_parseIntWithNSString_([((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) getAttributeValueWithNSString:nil withNSString:@"num"]);
  }
  @catch (JavaLangNullPointerException *e) {
    @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"Need num attribute in byte-array");
  }
  @catch (JavaLangNumberFormatException *e) {
    @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"Not a number in num attribute in byte-array");
  }
  IOSIntArray *array = [IOSIntArray arrayWithLength:num];
  jint i = 0;
  jint eventType = [parser getEventType];
  do {
    if (eventType == OrgXmlpullV1XmlPullParser_START_TAG) {
      if ([((NSString *) nil_chk([parser getName])) isEqual:@"item"]) {
        @try {
          *IOSIntArray_GetRef(array, i) = JavaLangInteger_parseIntWithNSString_([parser getAttributeValueWithNSString:nil withNSString:@"value"]);
        }
        @catch (JavaLangNullPointerException *e) {
          @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"Need value attribute in item");
        }
        @catch (JavaLangNumberFormatException *e) {
          @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"Not a number in value attribute in item");
        }
      }
      else {
        @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Expected item tag at: ", [parser getName]));
      }
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_END_TAG) {
      if ([((NSString *) nil_chk([parser getName])) isEqual:endTag]) {
        return array;
      }
      else if ([((NSString *) nil_chk([parser getName])) isEqual:@"item"]) {
        i++;
      }
      else {
        @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Expected ", endTag, @" end tag at: ", [parser getName]));
      }
    }
    eventType = [parser next];
  }
  while (eventType != OrgXmlpullV1XmlPullParser_END_DOCUMENT);
  @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$", @"Document ended before ", endTag, @" end tag"));
}

id RenameComAndroidInternalUtilXmlUtils_readValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(id<OrgXmlpullV1XmlPullParser> parser, IOSObjectArray *name) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  jint eventType = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) getEventType];
  do {
    if (eventType == OrgXmlpullV1XmlPullParser_START_TAG) {
      return RenameComAndroidInternalUtilXmlUtils_readThisValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(parser, name);
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_END_TAG) {
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Unexpected end tag at: ", [parser getName]));
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_TEXT) {
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Unexpected text: ", [parser getText]));
    }
    eventType = [parser next];
  }
  while (eventType != OrgXmlpullV1XmlPullParser_END_DOCUMENT);
  @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"Unexpected end of document");
}

id RenameComAndroidInternalUtilXmlUtils_readThisValueXmlWithOrgXmlpullV1XmlPullParser_withNSStringArray_(id<OrgXmlpullV1XmlPullParser> parser, IOSObjectArray *name) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  NSString *valueName = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) getAttributeValueWithNSString:nil withNSString:@"name"];
  NSString *tagName = [parser getName];
  id res;
  if ([((NSString *) nil_chk(tagName)) isEqual:@"null"]) {
    res = nil;
  }
  else if ([tagName isEqual:@"string"]) {
    NSString *value = @"";
    jint eventType;
    while ((eventType = [parser next]) != OrgXmlpullV1XmlPullParser_END_DOCUMENT) {
      if (eventType == OrgXmlpullV1XmlPullParser_END_TAG) {
        if ([((NSString *) nil_chk([parser getName])) isEqual:@"string"]) {
          IOSObjectArray_Set(nil_chk(name), 0, valueName);
          return value;
        }
        @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Unexpected end tag in <string>: ", [parser getName]));
      }
      else if (eventType == OrgXmlpullV1XmlPullParser_TEXT) {
        JreStrAppend(&value, "$", [parser getText]);
      }
      else if (eventType == OrgXmlpullV1XmlPullParser_START_TAG) {
        @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Unexpected start tag in <string>: ", [parser getName]));
      }
    }
    @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"Unexpected end of document in <string>");
  }
  else if ([tagName isEqual:@"int"]) {
    res = JavaLangInteger_valueOfWithInt_(JavaLangInteger_parseIntWithNSString_([parser getAttributeValueWithNSString:nil withNSString:@"value"]));
  }
  else if ([tagName isEqual:@"long"]) {
    res = JavaLangLong_valueOfWithNSString_([parser getAttributeValueWithNSString:nil withNSString:@"value"]);
  }
  else if ([tagName isEqual:@"float"]) {
    res = create_JavaLangFloat_initWithNSString_([parser getAttributeValueWithNSString:nil withNSString:@"value"]);
  }
  else if ([tagName isEqual:@"double"]) {
    res = create_JavaLangDouble_initWithNSString_([parser getAttributeValueWithNSString:nil withNSString:@"value"]);
  }
  else if ([tagName isEqual:@"boolean"]) {
    res = JavaLangBoolean_valueOfWithNSString_([parser getAttributeValueWithNSString:nil withNSString:@"value"]);
  }
  else if ([tagName isEqual:@"int-array"]) {
    [parser next];
    res = RenameComAndroidInternalUtilXmlUtils_readThisIntArrayXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(parser, @"int-array", name);
    IOSObjectArray_Set(nil_chk(name), 0, valueName);
    return res;
  }
  else if ([tagName isEqual:@"map"]) {
    [parser next];
    res = RenameComAndroidInternalUtilXmlUtils_readThisMapXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(parser, @"map", name);
    IOSObjectArray_Set(nil_chk(name), 0, valueName);
    return res;
  }
  else if ([tagName isEqual:@"list"]) {
    [parser next];
    res = RenameComAndroidInternalUtilXmlUtils_readThisListXmlWithOrgXmlpullV1XmlPullParser_withNSString_withNSStringArray_(parser, @"list", name);
    IOSObjectArray_Set(nil_chk(name), 0, valueName);
    return res;
  }
  else {
    @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$", @"Unknown tag: ", tagName));
  }
  jint eventType;
  while ((eventType = [parser next]) != OrgXmlpullV1XmlPullParser_END_DOCUMENT) {
    if (eventType == OrgXmlpullV1XmlPullParser_END_TAG) {
      if ([((NSString *) nil_chk([parser getName])) isEqual:tagName]) {
        IOSObjectArray_Set(nil_chk(name), 0, valueName);
        return res;
      }
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Unexpected end tag in <", tagName, @">: ", [parser getName]));
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_TEXT) {
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Unexpected text in <", tagName, @">: ", [parser getName]));
    }
    else if (eventType == OrgXmlpullV1XmlPullParser_START_TAG) {
      @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Unexpected start tag in <", tagName, @">: ", [parser getName]));
    }
  }
  @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$C", @"Unexpected end of document in <", tagName, '>'));
}

void RenameComAndroidInternalUtilXmlUtils_beginDocumentWithOrgXmlpullV1XmlPullParser_withNSString_(id<OrgXmlpullV1XmlPullParser> parser, NSString *firstElementName) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  jint type;
  while ((type = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) next]) != OrgXmlpullV1XmlPullParser_START_TAG && type != OrgXmlpullV1XmlPullParser_END_DOCUMENT) {
    ;
  }
  if (type != OrgXmlpullV1XmlPullParser_START_TAG) {
    @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(@"No start tag found");
  }
  if (![((NSString *) nil_chk([parser getName])) isEqual:firstElementName]) {
    @throw create_OrgXmlpullV1XmlPullParserException_initWithNSString_(JreStrcat("$$$$", @"Unexpected start tag: found ", [parser getName], @", expected ", firstElementName));
  }
}

void RenameComAndroidInternalUtilXmlUtils_nextElementWithOrgXmlpullV1XmlPullParser_(id<OrgXmlpullV1XmlPullParser> parser) {
  RenameComAndroidInternalUtilXmlUtils_initialize();
  jint type;
  while ((type = [((id<OrgXmlpullV1XmlPullParser>) nil_chk(parser)) next]) != OrgXmlpullV1XmlPullParser_START_TAG && type != OrgXmlpullV1XmlPullParser_END_DOCUMENT) {
    ;
  }
}

void RenameComAndroidInternalUtilXmlUtils_init(RenameComAndroidInternalUtilXmlUtils *self) {
  NSObject_init(self);
}

RenameComAndroidInternalUtilXmlUtils *new_RenameComAndroidInternalUtilXmlUtils_init() {
  J2OBJC_NEW_IMPL(RenameComAndroidInternalUtilXmlUtils, init)
}

RenameComAndroidInternalUtilXmlUtils *create_RenameComAndroidInternalUtilXmlUtils_init() {
  J2OBJC_CREATE_IMPL(RenameComAndroidInternalUtilXmlUtils, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RenameComAndroidInternalUtilXmlUtils)
