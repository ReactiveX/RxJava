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
//    [OneTest runTests];
//    [OneTest runSingleClassWithNSString:@"rx.BackpressureTests"];
//    [OneTest runSingleClassWithNSString:@"rx.internal.operators.OperatorMergeTest"];
        [OneTest runSingleClassWithNSString:@"rx.internal.operators.OperatorGroupByTest"];
//    [OneTest runSingleClassWithNSString:@"rx.doppl.memory.SubscriberAutomaticRemovalTest"];
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end
