package com.urosjarc.dbmessiah.tests

import com.urosjarc.dbmessiah.Mapper
import com.urosjarc.dbmessiah.Schema
import com.urosjarc.dbmessiah.data.*
import com.urosjarc.dbmessiah.domain.Table
import com.urosjarc.dbmessiah.exceptions.MapperException
import com.urosjarc.dbmessiah.exceptions.SerializerException
import com.urosjarc.dbmessiah.serializers.AllTS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.sql.JDBCType
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertContains

class Test_MapperTests {

    private lateinit var otherPArg: ProcedureArg
    private lateinit var pArg: ProcedureArg
    private lateinit var primaryColumnBad: PrimaryColumn
    private lateinit var foreignColumn2: ForeignColumn
    private lateinit var otherColumn2: OtherColumn
    private lateinit var primaryColumn2: PrimaryColumn
    private lateinit var repo: Mapper
    private lateinit var primaryColumn: PrimaryColumn
    private lateinit var foreignColumn: ForeignColumn
    private lateinit var otherColumn: OtherColumn
    private lateinit var entity: Entity

    private data class Entity(var pk: Int, val fk: Int, val col: Float)
    private data class Entity2(var pk: Int, var text: String?)
    private data class Input(val value: String)
    private data class Output(val value: Int)
    private data class TestProcedure(val value: String)

    @BeforeEach
    fun init() {

        repo = Mapper(
            schemas = listOf(
                Schema(
                    name = "main", tables = listOf(
                        Table(
                            primaryKey = Entity::pk,
                            foreignKeys = listOf(
                                Entity::fk to Entity2::class
                            )
                        ),
                        Table(primaryKey = Entity2::pk)
                    )
                )
            ),
            globalInputs = listOf(Input::class),
            globalOutputs = listOf(Output::class),
            globalSerializers = AllTS.basic,
            globalProcedures = listOf(TestProcedure::class)
        )

        entity = Entity(pk = 23, fk = 12, col = 2.34f)

        otherColumn = OtherColumn(
            unique = true,
            kprop = Entity::col as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
        foreignColumn = ForeignColumn(
            unique = true,
            kprop = Entity::fk as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) },
            cascadeDelete = false,
            cascadeUpdate = false
        )
        primaryColumn = PrimaryColumn(
            kprop = Entity::pk as KMutableProperty1<Any, Any?>,
            dbType = "INT",
            jdbcType = JDBCType.INTEGER,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
        primaryColumnBad = PrimaryColumn(
            kprop = Entity::pk as KMutableProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.INTEGER,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
        primaryColumn2 = PrimaryColumn(
            kprop = Entity2::pk as KMutableProperty1<Any, Any?>,
            dbType = "INT",
            jdbcType = JDBCType.INTEGER,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )

        otherColumn2 = OtherColumn(
            unique = true,
            kprop = Entity2::text as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
        foreignColumn2 = ForeignColumn(
            unique = true,
            kprop = Entity2::text as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) },
            cascadeUpdate = false,
            cascadeDelete = false
        )
        pArg = ProcedureArg(
            kprop = TestProcedure::value as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
        otherPArg = ProcedureArg(
            kprop = Entity::pk as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
    }

    @Test
    fun `test 1-th()`() {
        repo.tableInfos = listOf()
        val e = assertThrows<MapperException> {
            repo.testMapper()
        }
        assertContains(charSequence = e.message.toString(), other = "No table info was created", message = e.toString())
    }

    @Test
    fun `test 3-th()`() {
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(foreignColumn),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            ),
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(foreignColumn),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            )
        )
        val e = assertThrows<MapperException> {
            repo.testMapper()
        }
        assertContains(
            charSequence = e.message.toString(),
            other = "Following tables have been created multiple times: [Schema.Entity]",
            message = e.toString()
        )
    }

    @Test
    fun `test 4-th()`() {
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(),
                otherColumns = listOf(otherColumn, otherColumn),
                serializers = listOf()
            )
        )
        val e = assertThrows<MapperException> {
            repo.testMapper()
        }
        assertContains(
            charSequence = e.message.toString(),
            other = "Table 'Schema.Entity' does not have unique columns: [Column(name='col', dbType='VARCHAR', jdbcType='VARCHAR')]",
            message = e.toString()
        )
    }

    @Test
    fun `test 5-th()`() {
        /**
         * PRIMARY
         */
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(foreignColumn),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            ),
            TableInfo(
                schema = "Schema",
                kclass = Entity2::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(),
                otherColumns = listOf(),
                serializers = listOf()
            )
        )

        repo.tableInfos[0].foreignKeys[0].foreignTable = repo.tableInfos[1]

        val e = assertThrows<MapperException> {
            repo.testMapper()
        }
        assertContains(
            charSequence = e.message.toString(),
            other = "Table 'Schema.Entity2' does own primary key: Column(name='pk', dbType='INT', jdbcType='INTEGER')",
            message = e.toString()
        )


        /**
         * FOREIGN KEY
         */

        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(foreignColumn, foreignColumn2),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            ),
            TableInfo(
                schema = "Schema",
                kclass = Entity2::class,
                primaryKey = primaryColumn2,
                foreignKeys = listOf(),
                otherColumns = listOf(),
                serializers = listOf()
            )
        )

        repo.tableInfos[0].foreignKeys[0].foreignTable = repo.tableInfos[1]

        val e2 = assertThrows<MapperException> {
            repo.testMapper()
        }
        assertContains(
            charSequence = e2.message.toString(),
            other = "Table 'Schema.Entity' does own foreign key: Column(name='text', dbType='VARCHAR', jdbcType='VARCHAR')",
            message = e2.toString()
        )

        /**
         * OTHER KEY
         */

        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(),
                otherColumns = listOf(otherColumn, otherColumn2),
                serializers = listOf()
            )
        )

        val e3 = assertThrows<MapperException> {
            repo.testMapper()
        }
        assertContains(
            charSequence = e3.message.toString(),
            other = "Table 'Schema.Entity' does own column: Column(name='text', dbType='VARCHAR', jdbcType='VARCHAR')",
            message = e3.toString()
        )
    }

    @Test
    fun `test 6-th()`() {

        /**
         * IS INITED
         */
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(foreignColumn),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            )
        )

        val e2 = assertThrows<MapperException> {
            repo.testMapper()
        }

        assertContains(
            charSequence = e2.message.toString(),
            other = "Foreign key 'Schema.Entity.fk' is not initialized and connected to foreign table",
            message = e2.toString()
        )
        /**
         * POINTING TO RIGHT TABLE
         */
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(foreignColumn),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            )
        )

        repo.tableInfos[0].foreignKeys[0].foreignTable = TableInfo(
            schema = "Schema", kclass = String::class, primaryKey = primaryColumn,
            foreignKeys = listOf(), otherColumns = listOf(), serializers = listOf()
        )

        val e = assertThrows<MapperException> {
            repo.testMapper()
        }

        assertContains(
            charSequence = e.message.toString(),
            other = "Foreign key Column(name='fk', dbType='VARCHAR', jdbcType='VARCHAR') of table 'Schema.Entity' does not points to registered table: 'Schema.String'",
            message = e.toString()
        )
    }

    @Test
    fun `test 7-th()`() {
        val otherTable = TableInfo(
            schema = "Schema", kclass = String::class, primaryKey = primaryColumn,
            foreignKeys = listOf(), otherColumns = listOf(), serializers = listOf()
        )
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            )
        )

        /**
         * Primary key
         */
        repo.tableInfos[0].primaryKey.table = otherTable
        val e = assertThrows<MapperException> { repo.testMapper() }
        assertContains(
            charSequence = e.message.toString(),
            other = "Column 'Schema.String.pk' have parent 'Schema.String' but it should have parent: 'Schema.Entity'",
            message = e.toString()
        )

        /**
         * FOREIGN COLUMS
         */
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema", kclass = Entity::class, primaryKey = primaryColumn,
                foreignKeys = listOf(
                    ForeignColumn(
                        unique = true, kprop = Entity::fk as KProperty1<Any, Any?>, dbType = "VARCHAR", jdbcType = JDBCType.VARCHAR,
                        decoder = { rs, i, _ -> rs.getString(i) },
                        encoder = { ps, i, x -> ps.setString(i, x.toString()) },
                        cascadeUpdate = false,
                        cascadeDelete = false
                    )
                ), otherColumns = listOf(otherColumn), serializers = listOf()
            )
        )
        repo.tableInfos[0].foreignKeys[0].foreignTable = repo.tableInfos[0]
        repo.tableInfos[0].foreignKeys[0].table = otherTable
        val e2 = assertThrows<MapperException> { repo.testMapper() }
        assertContains(
            charSequence = e2.message.toString(),
            other = "Column 'Schema.String.fk' have parent 'Schema.String' but it should have parent: 'Schema.Entity'",
            message = e2.toString()
        )

        /**
         * OTHER COLUMNS
         */
        repo.tableInfos = listOf(
            TableInfo(
                schema = "Schema",
                kclass = Entity::class,
                primaryKey = primaryColumn,
                foreignKeys = listOf(),
                otherColumns = listOf(otherColumn),
                serializers = listOf()
            )
        )
        repo.tableInfos[0].otherColumns[0].table = otherTable
        val e3 = assertThrows<MapperException> { repo.testMapper() }
        assertContains(
            charSequence = e3.message.toString(),
            other = "Column 'Schema.String.col' have parent 'Schema.String' but it should have parent: 'Schema.Entity'",
            message = e3.toString()
        )
    }

    @Test
    fun `test 9-th()`() {

        /**
         * IS INITED
         */
        repo.procedures = listOf(
            Procedure(
                schema = "main",
                kclass = TestProcedure::class,
                args = listOf(pArg)
            )
        )

        repo.procedures[0].args[0].procedure = Procedure(schema = null, kclass = TestProcedure::class, args = listOf())

        val e2 = assertThrows<MapperException> {
            repo.testMapper()
        }

        assertContains(
            charSequence = e2.message.toString(),
            other = " Argument 'TestProcedure.value' have parent 'TestProcedure()' but it should have parent: 'TestProcedure(value: String)",
            message = e2.toString()
        )
    }

    @Test
    fun `test 10-th()`() {

        /**
         * IS INITED
         */
        repo.procedures = listOf(
            Procedure(
                schema = "main",
                kclass = TestProcedure::class,
                args = listOf(otherPArg)
            )
        )

        val e2 = assertThrows<MapperException> {
            repo.testMapper()
        }

        assertContains(
            charSequence = e2.message.toString(),
            other = "Procedure TestProcedure(pk: Int) does own argument: Arg(name='pk', dbType='VARCHAR', jdbcType='VARCHAR')",
            message = e2.toString()
        )
    }

}
