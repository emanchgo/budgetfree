/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!ng.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */

-- Database create script for Trove

create table "DatabaseVersion" (
  "version" integer not null
);

create table "Account" (
  "id" integer primary key,
  "name" varchar(128) not null,
  "description" varchar(128) not null,
  "accountType" integer not null,
  "parentId" bigint,
   constraint "Account_Account_FK" foreign key ("parentId") references "id" ("Account")
);

create table "Transaction" (
  "id" integer primary key,
  "description" varchar(128) not null,
  "amount" decimal(20,16) not null,
  "date" varchar(10) not null,
  "note" varchar(128) not null
);

create table "TransactionEntry" (
  "id" integer primary key,
  "number" bigint not null,
  "description" varchar(128) not null,
  "accountId" bigint not null,
  "amount" decimal(20,16) not null,
  "note" varchar(128) not null,
  "transactionId" bigint not null,
  constraint "TransactionEntry_Account_FK" foreign key ("accountId") references "id" ("Account"),
  constraint "TransactionEntry_Transaction_FK" foreign key ("transactionId") references "id" ("Transaction") on delete cascade
);

create unique index "Account_name_IDX" on "Account" ("name");
create index "Account_parentId_IDX" on "Account" ("parentId");
create index "TransactionEntry_accountId_IDX" on "TransactionEntry" ("accountId");
create index "TransactionEntry_transactionId_IDX" on "TransactionEntry" ("transactionId");

insert into "DatabaseVersion" values (1);

