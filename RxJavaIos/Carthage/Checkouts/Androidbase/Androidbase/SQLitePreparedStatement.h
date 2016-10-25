/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
//
//  SQLitePreparedStatement.h
//  squidb-ios
//

#import <Foundation/Foundation.h>
#import <sqlite3.h>
#include "J2ObjC_header.h"

@interface SQLitePreparedStatement : NSObject

@property sqlite3_stmt *statement;

- (id) initWithStatement:(sqlite3_stmt *)_statement;

@end
