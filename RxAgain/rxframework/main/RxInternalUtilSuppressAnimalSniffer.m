//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/SuppressAnimalSniffer.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxInternalUtilSuppressAnimalSniffer.h"
#include "java/lang/annotation/Annotation.h"
#include "java/lang/annotation/Documented.h"
#include "java/lang/annotation/ElementType.h"
#include "java/lang/annotation/Retention.h"
#include "java/lang/annotation/RetentionPolicy.h"
#include "java/lang/annotation/Target.h"

@interface RxInternalUtilSuppressAnimalSniffer : NSObject

@end

__attribute__((unused)) static IOSObjectArray *RxInternalUtilSuppressAnimalSniffer__Annotations$0();

@implementation RxInternalUtilSuppressAnimalSniffer

+ (const J2ObjcClassInfo *)__metadata {
  static const void *ptrTable[] = { (void *)&RxInternalUtilSuppressAnimalSniffer__Annotations$0 };
  static const J2ObjcClassInfo _RxInternalUtilSuppressAnimalSniffer = { "SuppressAnimalSniffer", "rx.internal.util", ptrTable, NULL, NULL, 7, 0x2609, 0, 0, -1, -1, -1, -1, 0 };
  return &_RxInternalUtilSuppressAnimalSniffer;
}

@end

IOSObjectArray *RxInternalUtilSuppressAnimalSniffer__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_JavaLangAnnotationRetention(JreLoadEnum(JavaLangAnnotationRetentionPolicy, CLASS)), create_JavaLangAnnotationDocumented(), create_JavaLangAnnotationTarget([IOSObjectArray arrayWithObjects:(id[]){ JreLoadEnum(JavaLangAnnotationElementType, METHOD), JreLoadEnum(JavaLangAnnotationElementType, CONSTRUCTOR), JreLoadEnum(JavaLangAnnotationElementType, TYPE) } count:3 type:NSObject_class_()]) } count:3 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(RxInternalUtilSuppressAnimalSniffer)
