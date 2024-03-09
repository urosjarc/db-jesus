package com.urosjarc.dbmessiah.data

import com.urosjarc.dbmessiah.builders.RowBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Represents the database table.
 * This class is final representation of database table defined by the user.
 * This class will be used internally by the system.
 *
 * @param schema The schema that contains this table.
 * @param kclass The class representing the table.
 * @param primaryColumn The [PrimaryColumn] for this table.
 * @param foreignColumns The list of [ForeignColumn] for this table.
 * @param otherColumns The list of [OtherColumn] for this table.
 * @param typeSerializers The list of [TypeSerializer] which will help in serialization process.
 */
public data class TableInfo(
    val schema: String,
    val kclass: KClass<*>,
    val primaryColumn: PrimaryColumn,
    val foreignColumns: List<ForeignColumn>,
    val otherColumns: List<OtherColumn>,
    val typeSerializers: List<TypeSerializer<*>>
) {

    /**
     * Represents the unescaped table name.
     */
    val name: String = this.kclass.simpleName!!

    /**
     * Full path where this table is located.
     */
    public val path: String = listOf(this.schema, this.name).joinToString(".")

    /**
     * Assign [TableInfo] instance to all their children.
     */
    init {
        (listOf(this.primaryColumn) + this.foreignColumns + this.otherColumns).forEach {
            it.table = this
        }
    }

    /**
     * Retrieves the [Column] corresponding to the given [KProperty1].
     *
     * @param kprop The [KProperty1] representing the property.
     * @return The corresponding [Column] or null if not found.
     */
    public fun getColumn(kprop: KProperty1<*, *>): Column? =
        (listOf(this.primaryColumn) + this.foreignColumns + this.otherColumns).firstOrNull { it.kprop == kprop }

    /**
     * Returns a [RowBuilder] object that is responsible for generating SQL strings and query values based on a list of columns.
     * The method constructs the [RowBuilder] object by combining the foreign columns and other columns of the [TableInfo] instance.
     * If the primary column has an auto-increment or auto-UUID property, the foreign columns and other columns are used as is.
     * Otherwise, the primary column is positioned at the beginning of the list followed by the foreign columns and other columns.
     *
     * @return The [RowBuilder] object.
     */
    public fun getInsertRowBuilder(): RowBuilder {
        val columns = this.foreignColumns + this.otherColumns
        return if (this.primaryColumn.autoInc || this.primaryColumn.autoUUID)
            RowBuilder(columns = columns)
        else
            RowBuilder(columns = listOf(this.primaryColumn) + columns)
    }

    /**
     * Returns a [RowBuilder] object that is responsible for generating SQL strings and query values based on a list of columns.
     *
     * The method constructs the [RowBuilder] object by combining the foreign columns and other columns of the [TableInfo] instance.
     * If the primary column has an auto-increment or auto-UUID property, the foreign columns and other columns are used as is.
     * Otherwise, the primary column is positioned at the beginning of the list followed by the foreign columns and other columns.
     *
     * @return The [RowBuilder] object.
     */
    public fun getUpdateRowBuilder(): RowBuilder = RowBuilder(columns = this.foreignColumns + this.otherColumns)

    /** @suppress */
    override fun hashCode(): Int = path.hashCode()//OK

    /** @suppress */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TableInfo
        return path == other.path
    }

    /** @suppress */
    override fun toString(): String = this.path
}
