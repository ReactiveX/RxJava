//
//  ViewController.h
//  ios
//
//  Created by Kevin Galligan on 6/11/16.
//  Copyright © 2016 Kevin Galligan. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "java/util/List.h"

@interface ViewController : UIViewController <UITableViewDelegate, UITableViewDataSource>

+ (NSArray *)nsArrayFromList:(id<JavaUtilList>)list;
@end

