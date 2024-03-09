package com.urosjarc.dbmessiah.data

import com.urosjarc.dbmessiah.serializers.AllTS
import org.junit.jupiter.api.BeforeEach
import java.sql.JDBCType
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals

class Test_TableInfo {

    private lateinit var primaryColumn: PrimaryColumn
    private lateinit var foreignColumn: ForeignColumn
    private lateinit var otherColumn: OtherColumn
    private lateinit var tableInfo: TableInfo

    data class Entity(var pk: Int? = null, val fk: String, val col: String)

    @Suppress("UNCHECKED_CAST")
    @BeforeEach
    fun init() {
        this.otherColumn = OtherColumn(
            unique = true,
            kprop = Entity::col as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )
        this.foreignColumn = ForeignColumn(
            unique = true,
            kprop = Entity::fk as KProperty1<Any, Any?>,
            dbType = "VARCHAR",
            jdbcType = JDBCType.VARCHAR,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) },
            cascadeUpdate = false,
            cascadeDelete = false
        )
        this.primaryColumn = PrimaryColumn(
            kprop = Entity::pk as KMutableProperty1<Any, Any?>,
            dbType = "INT",
            jdbcType = JDBCType.INTEGER,
            decoder = { rs, i, _ -> rs.getString(i) },
            encoder = { ps, i, x -> ps.setString(i, x.toString()) }
        )

        this.tableInfo = TableInfo(
            schema = "Schema",
            kclass = Entity::class,
            primaryColumn = primaryColumn,
            foreignColumns = listOf(foreignColumn),
            otherColumns = listOf(otherColumn),
            typeSerializers = AllTS.basic
        )

    }

    @Test
    fun `test userControlledColumns`() {
        assertEquals(
            actual = this.tableInfo.getColumns, expected = listOf(
                foreignColumn, otherColumn
            )
        )
    }

    @Test
    fun `test toString()`() {
        assertEquals(actual = this.tableInfo.toString(), expected = "Schema.Entity")
    }

    @Test
    fun `test sqlInsertColumns()`() {
        assertEquals(actual = this.tableInfo.sqlInsertColumns { "`$it`"}, expected = "`fk`, `col`")
    }

    @Test
    fun `test sqlInsertQuestions()`() {
        assertEquals(actual = this.tableInfo.sqlInsertQuestions(), expected = "?, ?")
    }

    @Test
    fun `test sqlUpdateColumns()`() {
        assertEquals(actual = this.tableInfo.sqlUpdateColumns {"`$it`"}, expected = "`fk` = ?, `col` = ?")
    }

    @Test
    fun `test jdbcTypes`() {
        assertEquals(actual = this.tableInfo.jdbcTypes, expected = mutableListOf(JDBCType.VARCHAR, JDBCType.VARCHAR))
    }

    @Test
    fun `test encoders`() {
        assertEquals(actual = this.tableInfo.encoders, expected = mutableListOf(foreignColumn.encoder, otherColumn.encoder))
    }

    @Test
    fun `test queryValues()`() {
        val entity = Entity(pk = 1, fk = "2", col = "3")
        val queryValues = this.tableInfo.queryValues(entity)
        assertEquals(expected = 2, queryValues.size)
        assertEquals(
            actual = queryValues[0],
            expected = QueryValue(name = "fk", value = "2", jdbcType = JDBCType.VARCHAR, encoder = foreignColumn.encoder)
        )
        assertEquals(
            actual = queryValues[1],
            expected = QueryValue(name = "col", value = "3", jdbcType = JDBCType.VARCHAR, encoder = otherColumn.encoder)
        )
    }
}
