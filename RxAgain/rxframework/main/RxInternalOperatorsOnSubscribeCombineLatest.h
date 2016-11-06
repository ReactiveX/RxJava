//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeCombineLatest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest")
#ifdef RESTRICT_RxInternalOperatorsOnSubscribeCombineLatest
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest 1
#endif
#undef RESTRICT_RxInternalOperatorsOnSubscribeCombineLatest

#if !defined (RxInternalOperatorsOnSubscribeCombineLatest_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest || defined(INCLUDE_RxInternalOperatorsOnSubscribeCombineLatest))
#define RxInternalOperatorsOnSubscribeCombineLatest_

#define RESTRICT_RxObservable 1
#define INCLUDE_RxObservable_OnSubscribe 1
#include "RxObservable.h"

@class IOSObjectArray;
@class RxSubscriber;
@protocol JavaLangIterable;
@protocol RxFunctionsFuncN;

@interface RxInternalOperatorsOnSubscribeCombineLatest : NSObject < RxObservable_OnSubscribe > {
 @public
  IOSObjectArray *sources_;
  id<JavaLangIterable> sourcesIterable_;
  id<RxFunctionsFuncN> combiner_;
  jint bufferSize_;
  jboolean delayError_;
}

#pragma mark Public

- (instancetype)initWithJavaLangIterable:(id<JavaLangIterable>)sourcesIterable
                    withRxFunctionsFuncN:(id<RxFunctionsFuncN>)combiner;

- (instancetype)initWithRxObservableArray:(IOSObjectArray *)sources
                     withJavaLangIterable:(id<JavaLangIterable>)sourcesIterable
                     withRxFunctionsFuncN:(id<RxFunctionsFuncN>)combiner
                                  withInt:(jint)bufferSize
                              withBoolean:(jboolean)delayError;

- (void)callWithId:(RxSubscriber *)s;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeCombineLatest)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest, sources_, IOSObjectArray *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest, sourcesIterable_, id<JavaLangIterable>)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest, combiner_, id<RxFunctionsFuncN>)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeCombineLatest_initWithJavaLangIterable_withRxFunctionsFuncN_(RxInternalOperatorsOnSubscribeCombineLatest *self, id<JavaLangIterable> sourcesIterable, id<RxFunctionsFuncN> combiner);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest *new_RxInternalOperatorsOnSubscribeCombineLatest_initWithJavaLangIterable_withRxFunctionsFuncN_(id<JavaLangIterable> sourcesIterable, id<RxFunctionsFuncN> combiner) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest *create_RxInternalOperatorsOnSubscribeCombineLatest_initWithJavaLangIterable_withRxFunctionsFuncN_(id<JavaLangIterable> sourcesIterable, id<RxFunctionsFuncN> combiner);

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeCombineLatest_initWithRxObservableArray_withJavaLangIterable_withRxFunctionsFuncN_withInt_withBoolean_(RxInternalOperatorsOnSubscribeCombineLatest *self, IOSObjectArray *sources, id<JavaLangIterable> sourcesIterable, id<RxFunctionsFuncN> combiner, jint bufferSize, jboolean delayError);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest *new_RxInternalOperatorsOnSubscribeCombineLatest_initWithRxObservableArray_withJavaLangIterable_withRxFunctionsFuncN_withInt_withBoolean_(IOSObjectArray *sources, id<JavaLangIterable> sourcesIterable, id<RxFunctionsFuncN> combiner, jint bufferSize, jboolean delayError) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest *create_RxInternalOperatorsOnSubscribeCombineLatest_initWithRxObservableArray_withJavaLangIterable_withRxFunctionsFuncN_withInt_withBoolean_(IOSObjectArray *sources, id<JavaLangIterable> sourcesIterable, id<RxFunctionsFuncN> combiner, jint bufferSize, jboolean delayError);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeCombineLatest)

#endif

#if !defined (RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest || defined(INCLUDE_RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator))
#define RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_

#define RESTRICT_JavaUtilConcurrentAtomicAtomicInteger 1
#define INCLUDE_JavaUtilConcurrentAtomicAtomicInteger 1
#include "java/util/concurrent/atomic/AtomicInteger.h"

#define RESTRICT_RxProducer 1
#define INCLUDE_RxProducer 1
#include "RxProducer.h"

#define RESTRICT_RxSubscription 1
#define INCLUDE_RxSubscription 1
#include "RxSubscription.h"

@class IOSObjectArray;
@class JavaUtilConcurrentAtomicAtomicLong;
@class JavaUtilConcurrentAtomicAtomicReference;
@class RxInternalUtilAtomicSpscLinkedArrayQueue;
@class RxSubscriber;
@protocol JavaUtilQueue;
@protocol RxFunctionsFuncN;

@interface RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator : JavaUtilConcurrentAtomicAtomicInteger < RxProducer, RxSubscription > {
 @public
  __unsafe_unretained RxSubscriber *actual_;
  id<RxFunctionsFuncN> combiner_;
  IOSObjectArray *subscribers_;
  jint bufferSize_;
  IOSObjectArray *latest_;
  RxInternalUtilAtomicSpscLinkedArrayQueue *queue_;
  jboolean delayError_;
  volatile_jboolean cancelled_;
  volatile_jboolean done_;
  JavaUtilConcurrentAtomicAtomicLong *requested_;
  JavaUtilConcurrentAtomicAtomicReference *error_;
  jint active_;
  jint complete_;
}

#pragma mark Public

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                withRxFunctionsFuncN:(id<RxFunctionsFuncN>)combiner
                             withInt:(jint)count
                             withInt:(jint)bufferSize
                         withBoolean:(jboolean)delayError;

- (jboolean)isUnsubscribed;

- (void)requestWithLong:(jlong)n;

- (void)subscribeWithRxObservableArray:(IOSObjectArray *)sources;

- (void)unsubscribe;

#pragma mark Package-Private

- (void)cancelWithJavaUtilQueue:(id<JavaUtilQueue>)q;

- (jboolean)checkTerminatedWithBoolean:(jboolean)mainDone
                           withBoolean:(jboolean)queueEmpty
                      withRxSubscriber:(RxSubscriber *)childSubscriber
                     withJavaUtilQueue:(id<JavaUtilQueue>)q
                           withBoolean:(jboolean)delayError;

- (void)combineWithId:(id)value
              withInt:(jint)index;

- (void)drain;

- (void)onErrorWithNSException:(NSException *)e;

@end

J2OBJC_STATIC_INIT(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, combiner_, id<RxFunctionsFuncN>)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, subscribers_, IOSObjectArray *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, latest_, IOSObjectArray *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, queue_, RxInternalUtilAtomicSpscLinkedArrayQueue *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, requested_, JavaUtilConcurrentAtomicAtomicLong *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, error_, JavaUtilConcurrentAtomicAtomicReference *)

inline id RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_get_MISSING();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT id RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_MISSING;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator, MISSING, id)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_initWithRxSubscriber_withRxFunctionsFuncN_withInt_withInt_withBoolean_(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *self, RxSubscriber *actual, id<RxFunctionsFuncN> combiner, jint count, jint bufferSize, jboolean delayError);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *new_RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_initWithRxSubscriber_withRxFunctionsFuncN_withInt_withInt_withBoolean_(RxSubscriber *actual, id<RxFunctionsFuncN> combiner, jint count, jint bufferSize, jboolean delayError) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *create_RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_initWithRxSubscriber_withRxFunctionsFuncN_withInt_withInt_withBoolean_(RxSubscriber *actual, id<RxFunctionsFuncN> combiner, jint count, jint bufferSize, jboolean delayError);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator)

#endif

#if !defined (RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest || defined(INCLUDE_RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber))
#define RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber_

#define RESTRICT_RxSubscriber 1
#define INCLUDE_RxSubscriber 1
#include "RxSubscriber.h"

@class RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator;

@interface RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber : RxSubscriber {
 @public
  RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *parent_;
  jint index_;
  jboolean done_;
}

#pragma mark Public

- (instancetype)initWithRxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator:(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *)parent
                                                                              withInt:(jint)index;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)t;

- (void)onNextWithId:(id)t;

- (void)requestMoreWithLong:(jlong)n;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber, parent_, RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber_initWithRxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_withInt_(RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber *self, RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *parent, jint index);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber *new_RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber_initWithRxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_withInt_(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *parent, jint index) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber *create_RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber_initWithRxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator_withInt_(RxInternalOperatorsOnSubscribeCombineLatest_LatestCoordinator *parent, jint index);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeCombineLatest_CombinerSubscriber)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeCombineLatest")
