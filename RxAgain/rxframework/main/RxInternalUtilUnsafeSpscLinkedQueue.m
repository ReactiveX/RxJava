//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/unsafe/SpscLinkedQueue.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxInternalUtilAtomicLinkedQueueNode.h"
#include "RxInternalUtilUnsafeBaseLinkedQueue.h"
#include "RxInternalUtilUnsafeSpscLinkedQueue.h"
#include "java/lang/NullPointerException.h"

@implementation RxInternalUtilUnsafeSpscLinkedQueue

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilUnsafeSpscLinkedQueue_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (jboolean)offerWithId:(id)nextValue {
  if (nextValue == nil) {
    @throw create_JavaLangNullPointerException_initWithNSString_(@"null elements not allowed");
  }
  RxInternalUtilAtomicLinkedQueueNode *nextNode = create_RxInternalUtilAtomicLinkedQueueNode_initWithId_(nextValue);
  [((RxInternalUtilAtomicLinkedQueueNode *) nil_chk(producerNode_)) soNextWithRxInternalUtilAtomicLinkedQueueNode:nextNode];
  JreStrongAssign(&producerNode_, nextNode);
  return true;
}

- (id)poll {
  RxInternalUtilAtomicLinkedQueueNode *nextNode = [((RxInternalUtilAtomicLinkedQueueNode *) nil_chk(consumerNode_)) lvNext];
  if (nextNode != nil) {
    id nextValue = [nextNode getAndNullValue];
    JreStrongAssign(&consumerNode_, nextNode);
    return nextValue;
  }
  return nil;
}

- (id)peek {
  RxInternalUtilAtomicLinkedQueueNode *nextNode = [((RxInternalUtilAtomicLinkedQueueNode *) nil_chk(consumerNode_)) lvNext];
  if (nextNode != nil) {
    return [nextNode lpValue];
  }
  else {
    return nil;
  }
}

- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len {
  return JreDefaultFastEnumeration(self, state, stackbuf, len);
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "LNSObject;", 0x1, -1, -1, -1, 3, -1, -1 },
    { NULL, "LNSObject;", 0x1, -1, -1, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(offerWithId:);
  methods[2].selector = @selector(poll);
  methods[3].selector = @selector(peek);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "offer", "LNSObject;", "(TE;)Z", "()TE;", "<E:Ljava/lang/Object;>Lrx/internal/util/unsafe/BaseLinkedQueue<TE;>;" };
  static const J2ObjcClassInfo _RxInternalUtilUnsafeSpscLinkedQueue = { "SpscLinkedQueue", "rx.internal.util.unsafe", ptrTable, methods, NULL, 7, 0x11, 4, 0, -1, -1, -1, 4, -1 };
  return &_RxInternalUtilUnsafeSpscLinkedQueue;
}

@end

void RxInternalUtilUnsafeSpscLinkedQueue_init(RxInternalUtilUnsafeSpscLinkedQueue *self) {
  RxInternalUtilUnsafeBaseLinkedQueue_init(self);
  [self spProducerNodeWithRxInternalUtilAtomicLinkedQueueNode:create_RxInternalUtilAtomicLinkedQueueNode_init()];
  [self spConsumerNodeWithRxInternalUtilAtomicLinkedQueueNode:self->producerNode_];
  [((RxInternalUtilAtomicLinkedQueueNode *) nil_chk(self->consumerNode_)) soNextWithRxInternalUtilAtomicLinkedQueueNode:nil];
}

RxInternalUtilUnsafeSpscLinkedQueue *new_RxInternalUtilUnsafeSpscLinkedQueue_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilUnsafeSpscLinkedQueue, init)
}

RxInternalUtilUnsafeSpscLinkedQueue *create_RxInternalUtilUnsafeSpscLinkedQueue_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilUnsafeSpscLinkedQueue, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilUnsafeSpscLinkedQueue)
