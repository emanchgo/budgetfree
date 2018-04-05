/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2018 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */

package trove.core.infrastructure.persist

//ejf-fixMe: implement
class PersistenceServiceSpec {
/*
Persistence manager
===================
"listProjectNames" should "ignore directories"
it should "ignore lock files"
it should "ignores files whose names begin with dot"
it should "strip filename suffix"
it should "return a sorted list of project names"

"openProject" should "create a project lock"
it should "return failure if unable to obtain project lock"
it should "return failure and clean up project lock if unable to open database"
it should "create all tables if creating a new database"
it should "populate the version table if creating a new database"
it should "create a project if all setup actions succeed"
it should "fail with a PersistenceError if the wrong database version exists and clean up the project lock"
it should "fail with a PersistenceError if there are too many rows in the database version table and clean up the project lock"
it should "set the current project upon successful project opening"
it should "open the database with all the right settings"
it should "add a shutdown hook to close the database and delete the project lock if the process were to shut down"

"closeCurrentProject" should "clear the current project upon successful project closing"
it should "close the database, release the project lock, and remove the shutdown hook upon successful project closing"
it should "return a PersistenceError if the database cannot be closed"
it should "return a PersistenceError if it cannot release the project lock"
it should "return a PersistenceError if it cannot remove the shutdown hook"

"shutdown hook" should "close the database and release the project lock if invoked"
it should "not try to remove itself from the jvm shutdown hooks"
*/
}
