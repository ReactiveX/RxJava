/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
//
//  SQLitePreparedStatement.m
//  squidb-ios
//

#import "SQLitePreparedStatement.h"

@implementation SQLitePreparedStatement
@synthesize statement;

- (id) initWithStatement:(sqlite3_stmt *)_statement {
    if (self = [super init])  {
        self.statement = _statement;
    }
    return self;
}

- (void) dealloc {
    [super dealloc];
}

@end
