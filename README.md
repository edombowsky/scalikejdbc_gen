# scalikejdbc_gen
Reverse engineering of database tales for use in scalikejdbc projects

Much of what is here was taken from the section 9 of Japanese version of the github repository [scalikejdbc/cookbook](https://github.com/scalikejdbc/scalikejdbc-cookbook)

## Files needed for the reverse engineering to take place

### project/scalikejdbc-gen.sbt

This describes the sbt plug-in configuration. Please do not forget to specify the JDBC driver.

```sbt
// Do not forget to specify the JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "2.+"

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "2.2.+")
```

### project/scalikejdbc.properties

File name and location is fixed by copying the following model.

```java
# JDBC
jdbc.driver=org.hsqldb.jdbc.JDBCDriver
jdbc.url=jdbc:hsqldb:file:db/test
jdbc.username=sa
jdbc.password=
jdbc.schema=

 ---
# source code generator settings

# Package to place the class to generate
generator.packageName=com.abb.servicesuite.picklist_cl.dao
# generator.lineBreak: LF/CRLF
generator.lineBreak=LF
# generator.template: interpolation/queryDsl
generator.template=queryDsl
# generator.testTemplate: specs2unit/specs2acceptance/ScalaTestFlatSpec
generator.testTemplate=ScalaTestFlatSpec
generator.encoding=UTF-8
# When you're using Scala 2.11 or higher, you can use case classes for 22+ columns tables
generator.caseClassOnly=true
# Set AutoSession for implicit DBSession parameter's default value
generator.defaultAutoSession=false
# Use autoConstruct macro (default: false)
generator.autoConstruct=false
# joda-time (org.joda.time.DateTime) or JSR-310 (java.time.ZonedDateTime java.time.OffsetDateTime)
generator.dateTimeClass=java.time.ZonedDateTime
```

### build.sbt

By appending a "scalikejdbcSettings" it will enable scalikejdbc-gen command. Please do not forget to put a blank line before and after.

```
scalikejdbcSettings
```

## How to use

Using scalikejdbc-gen is very simple. Following the scalikejdbc-gen command, you specify the table name, specify the class name to be generated if necessary.

```shell
sbt "scalikejdbc-gen [table-name (class-name)]"
```

For example, if there is a table called `operation_history` to reverse engineer it use `scalikejdbc-gen operation_history`. This will create the files `src/main/scala/com/abb/servicesuite/dao/OperationHistory.scala` and `src/test/scala/com/ab/servicesuite/dao/OperationHistorySpec.scala`.

In the case of the table named `operation_histories` in table naming rules, such as Ruby's ActiveRecord it is generated with the same file name if you specify "scalikejdbc-gen operation_histories OperationHistory" this will specifu the filesname as "OperationHistories.scala" and "OperationHistoriesSpec.scala". By using the class name and by setting a function called tableNameToClassName of GeneratorSettings on build.sbt and Build.scala, it is also possible to set your own naming conventions at once.

## Generated code

It may take a little long but the code is actually generated. Since the test code is also generated, how to use the class I think is immediately apparent.

For the table

```sql
create table member (
  id int generated always as identity,
  name varchar(30) not null,
  description varchar(1000),
  birthday date,
  created_at timestamp not null,
  primary key(id)
)
```

When you run the "scalikejdbc-gen member" to generate the code you will get the following:

### src/main/scala/com/example/Member.scala

```scala
package package com.abb.servicesuite.dao

import scalikejdbc._
import org.joda.time.{LocalDate, DateTime}

case class Member(
  id: Int,
  name: String,
  description: Option[String] = None,
  birthday: Option[LocalDate] = None,
  createdAt: DateTime) {

  def save()(implicit session: DBSession = Member.autoSession): Member = Member.save(this)(session)

  def destroy()(implicit session: DBSession = Member.autoSession): Unit = Member.destroy(this)(session)

}


object Member extends SQLSyntaxSupport[Member] {

  override val tableName = "MEMBER"

  override val columns = Seq("ID", "NAME", "DESCRIPTION", "BIRTHDAY", "CREATED_AT")

  def apply(m: ResultName[Member])(rs: WrappedResultSet): Member = new Member(
    id = rs.int(m.id),
    name = rs.string(m.name),
    description = rs.stringOpt(m.description),
    birthday = rs.dateOpt(m.birthday).map(_.toLocalDate),
    createdAt = rs.timestamp(m.createdAt).toDateTime
  )

  val m = Member.syntax("m")

  h = AutoSession autoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Member] = {
    withSQL {
      select.from(Member as m).where.eq(m.id, id)
    }.map(Member(m.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Member] = {
    withSQL(select.from(Member as m)).map(Member(m.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Member as m)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Member] = {
    withSQL {
      select.from(Member as m).where.append(sqls"${where}")
    }.map(Member(m.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(Member as m).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): Member = {
    generatedKey withSQL h = {
      insert.into(Member).columns(
        column.name,
        column.description,
        column.birthday,
        column.createdAt
      ).values(
        name,
        description,
        birthday,
        createdAt
      )
    }.updateAndReturnGeneratedKey.apply()

    Member(
      id = generatedKey.toInt,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt)
  }

  def save(m: Member)(implicit session: DBSession = autoSession): Member = {
    withSQL {
      update(Member as m).set(
        m.id -> m.id,
        m.name -> m.name,
        m.description -> m.description,
        m.birthday -> m.birthday,
        m.createdAt -> m.createdAt
      ).where.eq(m.id, m.id)
    }.update.apply()
    m
  }

  def destroy(m: Member)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(Member).where.eq(column.id, m.id) }.update.apply()
  }

}
```

### src/test/scala/com/abb/servicesuite/dao/MemberSpec.scala

```scala
package com.abb.servicesuite.dao

import scalikejdbc.specs2.mutable.AutoRollback
import org.specs2.mutable._
import org.joda.time._

class MemberSpec extends Specification {

  "Member" should {
    "find by primary keys" in new AutoRollback {
      val maybeFound = Member.find(123)
      maybeFound.isDefined should beTrue
    }
    "find all records" in new AutoRollback {
      val allResults = Member.findAll()
      allResults.size should be_>(0)
    }
    "count all records" in new AutoRollback {
      val count = Member.countAll()
      count should be_>(0L)
    }
    "find by where clauses" in new AutoRollback {
      val results = Member.findAllBy(sqls.eq(m.id, 123))
      results.size should be_>(0)
    }
    "count by where clauses" in new AutoRollback {
      val count = Member.countBy(sqls.eq(m.id, 123))
      count should be_>(0L)
    }
    "create new record" in new AutoRollback {
      val created = Member.create(name = "MyString", createdAt = DateTime.now)
      created should not beNull
    }
    "save a record" in new AutoRollback {
      val entity = Member.findAll().head
      val updated = Member.save(entity)
      updated should not equalTo(entity)
    }
    "destroy a record" in new AutoRollback {
      val entity = Member.findAll().head
      Member.destroy(entity)
      val shouldBeNone = Member.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
```
