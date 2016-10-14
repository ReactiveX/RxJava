//
//  ViewController.m
//  ios
//
//  Created by Kevin Galligan on 6/11/16.
//  Copyright © 2016 Kevin Galligan. All rights reserved.
//

#import "ViewController.h"

#import "OneTest.h"
#import "java/lang/System.h"
#import "java/util/List.h"

@interface ViewController ()

@property (nonatomic, strong) NSArray *tableData;

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    NSLog(@"viewDidLoad");
    // Do any additional setup after loading the view, typically from a nib.
    
    _tableData = [ViewController nsArrayFromList:[OneTest allTestClassnames]];
}

- (IBAction)runClicked:(id)sender {
    
    [OneTest runTests];
//    [OneTest runTests];
    
}

+ (NSArray *)nsArrayFromList:(id<JavaUtilList>)list {
    NSMutableArray *result = [NSMutableArray array];
    for (id object in list) {
        [result addObject:object];
    }
    return result;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [_tableData count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *simpleTableIdentifier = @"SimpleTableCell";
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:simpleTableIdentifier];
    
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:simpleTableIdentifier];
    }
    
    cell.textLabel.text = [_tableData objectAtIndex:indexPath.row];
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
    
    NSString* testClassname =[_tableData objectAtIndex:indexPath.row];
    
    //Pushing next view
    [OneTest runNamedTestWithNSString:testClassname];
    
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
