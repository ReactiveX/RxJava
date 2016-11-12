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
    [super viewDidLoad];
    [OneTest runTests];
//    [OneTest runTestsWithNSString:@"rx.BackpressureTests"];
    //rx.internal.operators.OperatorFlatMapTest#flatMapRangeMixedAsyncLoop
//    [OneTest runMethodWithNSString:@"rx.internal.operators.OperatorFlatMapTest" withNSString:@"flatMapRangeMixedAsyncLoop"];
//    [OneTest runTestsWithNSString:@"rx.schedulers.TrampolineSchedulerTest"];
//        [OneTest runTestsWithNSString:@"rx.internal.operators.OperatorGroupByTest"];
//    [OneTest runSingleClassWithNSString:@"rx.doppl.memory.SubscriberAutomaticRemovalTest"];
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end
