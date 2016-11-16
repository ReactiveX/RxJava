//
//  ViewController.m
//  RxAgain
//
//  Created by Kevin Galligan on 11/2/16.
//  Copyright © 2016 Kevin Galligan. All rights reserved.
//

#import "ViewController.h"
#import "OneTest.h"

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad {
    
//    [OneTest runTestsWithNSString:@"rx.internal.operators.OperatorDelayTest#testBackpressureWithSelectorDelayAndSubscriptionDelay"];
    [OneTest runTestsWithInt:230 withInt:0];
    
    
    //Don't forget. These are the problem tests
//    [OneTest runTestsWithNSString:@"rx.internal.operators.OperatorDelayTest"];
//    [OneTest runTestsWithNSString:@"rx.schedulers.ComputationSchedulerTests"];

    //Currently Failing
//    [OneTest runTestsWithNSString:@"rx.internal.operators.OnSubscribeGroupJoinTest#behaveAsJoin"
//                     withNSString:@"rx.internal.operators.OnSubscribeGroupJoinTest#rightThrows"
//                     withNSString:@"rx.SchedulerWorkerTest#testOnBackpressureDropWithAction"];

}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end


//    [OneTest runTestsWithNSString:@"rx.BackpressureTests"];
//rx.internal.operators.OperatorFlatMapTest#flatMapRangeMixedAsyncLoop
//    [OneTest runMethodWithNSString:@"rx.internal.operators.OperatorMergeTest" withNSString:@"mergeManyAsyncSingle"];

//        [OneTest runTestsWithNSString:@"rx.BackpressureTests#testOnBackpressureDropWithAction"];

//    [OneTest runTestsWithNSString:@"rx.subjects.AsyncSubjectTest#testSubscribeCompletionRaceCondition"
//                     withNSString:@"rx.schedulers.NewThreadSchedulerTest#testUnSubscribeForScheduler"
//                     withNSString:@"rx.schedulers.IoSchedulerTest#testUnSubscribeForScheduler"
//                     withNSString:@"rx.schedulers.ComputationSchedulerTests#testUnSubscribeForScheduler"];

//    [OneTest runTestsWithNSString:@"rx.internal.schedulers.ExecutorSchedulerTest#testUnSubscribeForScheduler"
//                     withNSString:@"rx.internal.operators.OnSubscribeGroupJoinTest#behaveAsJoin"
//                     withNSString:@"rx.internal.operators.OnSubscribeGroupJoinTest#rightThrows"
//                     withNSString:@"rx.SchedulerWorkerTest#testCurrentTimeDriftForwards"];

//    [OneTest runTestsWithNSString:@"rx.schedulers.ImmediateSchedulerTest#testSequenceOfDelayedActions"];
//    [OneTest runTestsWithNSString:@"rx.schedulers.ImmediateSchedulerTest#testNestedActions"];

//    [OneTest runTestsWithNSString:@"rx.schedulers.NewThreadSchedulerTest" withNSString:@"rx.schedulers.IoSchedulerTest" withNSString:@"rx.schedulers.ImmediateSchedulerTest"];
//rx.observables.BlockingObservableTest
//        [OneTest runTestsWithNSString:@"rx.internal.operators.OperatorGroupByTest"];
//    [OneTest runSingleClassWithNSString:@"rx.doppl.memory.SubscriberAutomaticRemovalTest"];

