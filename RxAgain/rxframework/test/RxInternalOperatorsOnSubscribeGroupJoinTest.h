//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OnSubscribeGroupJoinTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest")
#ifdef RESTRICT_RxInternalOperatorsOnSubscribeGroupJoinTest
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOnSubscribeGroupJoinTest

#if !defined (RxInternalOperatorsOnSubscribeGroupJoinTest_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest || defined(INCLUDE_RxInternalOperatorsOnSubscribeGroupJoinTest))
#define RxInternalOperatorsOnSubscribeGroupJoinTest_

@class RxObservable;
@protocol RxFunctionsFunc1;
@protocol RxFunctionsFunc2;
@protocol RxObserver;

@interface RxInternalOperatorsOnSubscribeGroupJoinTest : NSObject {
 @public
  id<RxObserver> observer_;
  id<RxFunctionsFunc2> add_;
  id<RxFunctionsFunc2> add2_;
}

#pragma mark Public

- (instancetype)init;

- (void)before;

- (void)behaveAsJoin;

- (void)leftDurationSelectorThrows;

- (void)leftDurationThrows;

- (void)leftThrows;

- (void)normal1;

- (void)resultSelectorThrows;

- (void)rightDurationSelectorThrows;

- (void)rightDurationThrows;

- (void)rightThrows;

- (void)setup;

#pragma mark Package-Private

- (id<RxFunctionsFunc1>)justWithRxObservable:(RxObservable *)observable;

- (id<RxFunctionsFunc1>)just2WithRxObservable:(RxObservable *)observable;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeGroupJoinTest)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest, observer_, id<RxObserver>)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest, add_, id<RxFunctionsFunc2>)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest, add2_, id<RxFunctionsFunc2>)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeGroupJoinTest_init(RxInternalOperatorsOnSubscribeGroupJoinTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest *new_RxInternalOperatorsOnSubscribeGroupJoinTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest *create_RxInternalOperatorsOnSubscribeGroupJoinTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeGroupJoinTest)

#endif

#if !defined (RxInternalOperatorsOnSubscribeGroupJoinTest_Person_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest || defined(INCLUDE_RxInternalOperatorsOnSubscribeGroupJoinTest_Person))
#define RxInternalOperatorsOnSubscribeGroupJoinTest_Person_

@class RxInternalOperatorsOnSubscribeGroupJoinTest;

@interface RxInternalOperatorsOnSubscribeGroupJoinTest_Person : NSObject {
 @public
  jint id__;
  NSString *name_;
}

#pragma mark Public

- (instancetype)initWithRxInternalOperatorsOnSubscribeGroupJoinTest:(RxInternalOperatorsOnSubscribeGroupJoinTest *)outer$
                                                            withInt:(jint)id_
                                                       withNSString:(NSString *)name;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeGroupJoinTest_Person)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest_Person, name_, NSString *)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeGroupJoinTest_Person_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withInt_withNSString_(RxInternalOperatorsOnSubscribeGroupJoinTest_Person *self, RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, jint id_, NSString *name);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest_Person *new_RxInternalOperatorsOnSubscribeGroupJoinTest_Person_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withInt_withNSString_(RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, jint id_, NSString *name) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest_Person *create_RxInternalOperatorsOnSubscribeGroupJoinTest_Person_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withInt_withNSString_(RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, jint id_, NSString *name);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeGroupJoinTest_Person)

#endif

#if !defined (RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest || defined(INCLUDE_RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit))
#define RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit_

@class RxInternalOperatorsOnSubscribeGroupJoinTest;

@interface RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit : NSObject {
 @public
  jint personId_;
  NSString *fruit_;
}

#pragma mark Public

- (instancetype)initWithRxInternalOperatorsOnSubscribeGroupJoinTest:(RxInternalOperatorsOnSubscribeGroupJoinTest *)outer$
                                                            withInt:(jint)personId
                                                       withNSString:(NSString *)fruit;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit, fruit_, NSString *)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withInt_withNSString_(RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit *self, RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, jint personId, NSString *fruit);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit *new_RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withInt_withNSString_(RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, jint personId, NSString *fruit) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit *create_RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withInt_withNSString_(RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, jint personId, NSString *fruit);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeGroupJoinTest_PersonFruit)

#endif

#if !defined (RxInternalOperatorsOnSubscribeGroupJoinTest_PPF_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest || defined(INCLUDE_RxInternalOperatorsOnSubscribeGroupJoinTest_PPF))
#define RxInternalOperatorsOnSubscribeGroupJoinTest_PPF_

@class RxInternalOperatorsOnSubscribeGroupJoinTest;
@class RxInternalOperatorsOnSubscribeGroupJoinTest_Person;
@class RxObservable;

@interface RxInternalOperatorsOnSubscribeGroupJoinTest_PPF : NSObject {
 @public
  RxInternalOperatorsOnSubscribeGroupJoinTest_Person *person_;
  RxObservable *fruits_;
}

#pragma mark Public

- (instancetype)initWithRxInternalOperatorsOnSubscribeGroupJoinTest:(RxInternalOperatorsOnSubscribeGroupJoinTest *)outer$
             withRxInternalOperatorsOnSubscribeGroupJoinTest_Person:(RxInternalOperatorsOnSubscribeGroupJoinTest_Person *)person
                                                   withRxObservable:(RxObservable *)fruits;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeGroupJoinTest_PPF)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest_PPF, person_, RxInternalOperatorsOnSubscribeGroupJoinTest_Person *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeGroupJoinTest_PPF, fruits_, RxObservable *)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeGroupJoinTest_PPF_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withRxInternalOperatorsOnSubscribeGroupJoinTest_Person_withRxObservable_(RxInternalOperatorsOnSubscribeGroupJoinTest_PPF *self, RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, RxInternalOperatorsOnSubscribeGroupJoinTest_Person *person, RxObservable *fruits);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest_PPF *new_RxInternalOperatorsOnSubscribeGroupJoinTest_PPF_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withRxInternalOperatorsOnSubscribeGroupJoinTest_Person_withRxObservable_(RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, RxInternalOperatorsOnSubscribeGroupJoinTest_Person *person, RxObservable *fruits) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeGroupJoinTest_PPF *create_RxInternalOperatorsOnSubscribeGroupJoinTest_PPF_initWithRxInternalOperatorsOnSubscribeGroupJoinTest_withRxInternalOperatorsOnSubscribeGroupJoinTest_Person_withRxObservable_(RxInternalOperatorsOnSubscribeGroupJoinTest *outer$, RxInternalOperatorsOnSubscribeGroupJoinTest_Person *person, RxObservable *fruits);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeGroupJoinTest_PPF)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeGroupJoinTest")
