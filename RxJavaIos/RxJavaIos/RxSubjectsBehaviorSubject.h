//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/subjects/BehaviorSubject.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxSubjectsBehaviorSubject")
#ifdef RESTRICT_RxSubjectsBehaviorSubject
#define INCLUDE_ALL_RxSubjectsBehaviorSubject 0
#else
#define INCLUDE_ALL_RxSubjectsBehaviorSubject 1
#endif
#undef RESTRICT_RxSubjectsBehaviorSubject

#if !defined (RxSubjectsBehaviorSubject_) && (INCLUDE_ALL_RxSubjectsBehaviorSubject || defined(INCLUDE_RxSubjectsBehaviorSubject))
#define RxSubjectsBehaviorSubject_

#define RESTRICT_RxSubjectsSubject 1
#define INCLUDE_RxSubjectsSubject 1
#include "RxSubjectsSubject.h"

@class IOSObjectArray;
@class RxSubjectsSubjectSubscriptionManager;
@protocol RxObservable_OnSubscribe;

@interface RxSubjectsBehaviorSubject : RxSubjectsSubject

#pragma mark Public

+ (RxSubjectsBehaviorSubject *)create;

+ (RxSubjectsBehaviorSubject *)createWithId:(id)defaultValue;

- (NSException *)getThrowable;

- (id)getValue;

- (IOSObjectArray *)getValues;

- (IOSObjectArray *)getValuesWithNSObjectArray:(IOSObjectArray *)a;

- (jboolean)hasCompleted;

- (jboolean)hasObservers;

- (jboolean)hasThrowable;

- (jboolean)hasValue;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)v;

#pragma mark Protected

- (instancetype)initWithRxObservable_OnSubscribe:(id<RxObservable_OnSubscribe>)onSubscribe
        withRxSubjectsSubjectSubscriptionManager:(RxSubjectsSubjectSubscriptionManager *)state;

#pragma mark Package-Private

- (jint)subscriberCount;

@end

J2OBJC_STATIC_INIT(RxSubjectsBehaviorSubject)

FOUNDATION_EXPORT RxSubjectsBehaviorSubject *RxSubjectsBehaviorSubject_create();

FOUNDATION_EXPORT RxSubjectsBehaviorSubject *RxSubjectsBehaviorSubject_createWithId_(id defaultValue);

FOUNDATION_EXPORT void RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(RxSubjectsBehaviorSubject *self, id<RxObservable_OnSubscribe> onSubscribe, RxSubjectsSubjectSubscriptionManager *state);

FOUNDATION_EXPORT RxSubjectsBehaviorSubject *new_RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(id<RxObservable_OnSubscribe> onSubscribe, RxSubjectsSubjectSubscriptionManager *state) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxSubjectsBehaviorSubject *create_RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(id<RxObservable_OnSubscribe> onSubscribe, RxSubjectsSubjectSubscriptionManager *state);

J2OBJC_TYPE_LITERAL_HEADER(RxSubjectsBehaviorSubject)

#endif

#pragma pop_macro("INCLUDE_ALL_RxSubjectsBehaviorSubject")
