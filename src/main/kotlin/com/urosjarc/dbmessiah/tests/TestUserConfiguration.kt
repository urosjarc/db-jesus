package com.urosjarc.dbmessiah.tests

import com.urosjarc.dbmessiah.DbMessiahRepository
import com.urosjarc.dbmessiah.domain.columns.C
import com.urosjarc.dbmessiah.domain.schema.Schema
import com.urosjarc.dbmessiah.domain.table.Table
import com.urosjarc.dbmessiah.exceptions.SerializerException
import com.urosjarc.dbmessiah.extend.ext_notUnique
import kotlin.reflect.KClass

class TestUserConfiguration(val repo: DbMessiahRepository) {
    /**
     * CHECK FOR EMPTYNESS
     */
    fun `1-th Test - If at least one table exist`() {
        this.repo.schemas.getOrNull(0)?.tables?.getOrNull(0) ?: throw SerializerException("Missing schema or it has no registered table")
    }

    /**
     * SCHEMA OBJECTS REGISTERING MULTIPLE TIMES
     */
    fun `2-th Test - If schema registered multiple times`() {
        val notUnique = this.repo.schemas.ext_notUnique
        if (notUnique.isNotEmpty()) throw SerializerException("Schemas registered multiple times: ${notUnique.keys}")
    }

    fun `3-th Test - If schemas table registered multiple times`() {
        this.repo.schemas.forEach {
            val notUnique = it.tables.ext_notUnique
            if (notUnique.isNotEmpty()) throw SerializerException("Schema $it has tables ${notUnique.keys} registered multiple times")
        }
    }

    fun `4-th Test - If schemas table foreign key registered multiple times`() {
        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                val notUnique = table.foreignKeys.map { "'${it.first.name}'" }.ext_notUnique
                if (notUnique.isNotEmpty()) throw SerializerException("Table ${schema}.${table} has foreign keys ${notUnique.keys} registered multiple times")
            }
        }
    }

    fun `5-th Test - If schemas table constraints registered multiple times`() {
        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                val notUnique = table.constraints.map { "'${it.first.name}'" }.ext_notUnique
                if (notUnique.isNotEmpty()) throw SerializerException("Table ${schema}.${table} has constraints ${notUnique.keys} registered multiple times")
            }
        }
    }

    /**
     * SERIALIZERS REGISTERED MULTIPLE TIMES
     */
    fun `6-th Test - If schema serializers registered multiple times`() {
        this.repo.schemas.forEach {
            val notUnique = it.serializers.ext_notUnique
            if (notUnique.isNotEmpty()) throw SerializerException("Schema ${it} has serializers ${notUnique.keys} registered multiple times")
        }
    }

    fun `7-th Test - If schemas table serializers registered multiple times`() {
        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                val notUnique = table.serializers.ext_notUnique
                if (notUnique.isNotEmpty()) throw SerializerException("Table ${schema}.${table} has serializers ${notUnique.keys} registered multiple times")
            }
        }
    }

    /**
     * MAP VALUES ARE CORRECT
     */
    fun `8-th Test - If schemas table foreign keys are pointing to wrong table`() {
        val schemas_tables = mutableListOf<Pair<Schema, Table<*>>>()

        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                schemas_tables.add(Pair(first = schema, second = table))
            }
        }
        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                table.foreignKeys.forEach { fk ->

                    val schema_fkTable = schemas_tables.firstOrNull { fk.second == it.second.kclass }
                        ?: throw SerializerException("Foreign key ${schema}.${table}.'${fk.first.name}' points to unregistered class '${fk.second.simpleName}'")

                    val fkKClass = fk.first.returnType.classifier as KClass<*>
                    if (schema_fkTable.second.primaryKey.returnType.classifier != fkKClass)
                        throw SerializerException(
                            "Foreign key ${schema}.${table}.'${fk.first.name}' is type of '${fkKClass.simpleName}' " +
                                    "but it points to incompatible primary key ${schema_fkTable.first}.${schema_fkTable.second}.'${schema_fkTable.second.primaryKey.name}'"
                        )
                }
            }
        }
    }

    fun `9-th Test - If schemas table constraints are valid`() {
        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                table.constraints.forEach { prop_const ->

                    val prop = prop_const.first

                    //Primary key
                    if (prop == table.primaryKey) {
                        prop_const.second.forEach {
                            when (it) {
                                C.AUTO_INC -> {}
                                C.UNIQUE -> throw SerializerException("Primary key ${schema}.${table}.'${table.primaryKey.name}' does not need '${C.UNIQUE}' constraint")
                            }
                        }
                    }

                    //Foreign constraints
                    if (table.foreignKeys.map { it.first }.contains(prop)) {
                        prop_const.second.forEach {
                            when (it) {
                                C.AUTO_INC -> throw SerializerException("Foreign key ${schema}.${table}.'${prop.name}' does not need '${C.AUTO_INC}' constraint")
                                C.UNIQUE -> {}
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Every input objects has registered serializers
     */
    fun `10-th Test - If all primary keys that are autoincrement have integer dbType`() {
        this.repo.schemas.forEach { schema ->
            schema.tables.forEach { table ->
                if (table.primaryKeyConstraints.contains(C.AUTO_INC)) {
                    val pkKClass = table.primaryKey.returnType.classifier as KClass<*>
                    if (pkKClass != Int::class) {
                        throw SerializerException("Primary key '${schema.name}.${table.name}.${table.primaryKey.name}' of type '${pkKClass.simpleName}' has constrain 'AUTO_INC' but then it should be of type 'Int'")
                    }
                }

            }
        }
    }

    fun `11-th Test - If all input classes have imutable and not null properties`() {
        this.repo.globalInputs.forEach { input ->
            this.repo.getKProps(kclass = input).forEach { kp ->
                if (kp.returnType.isMarkedNullable) {
                    throw SerializerException("Input property '${input.simpleName}'.'${kp.name}' can be null which is not allowed on any input class!")
                }
            }
        }
    }

    fun `12-th Test - If all output classes have no default values`() {
        this.repo.globalOutputs.forEach { input ->
            val constructor = this.repo.getConstructor(kclass = input)
            val defaultParams = constructor.parameters.filter { p -> p.isOptional }.map { "'${it.name}'" }
            if (defaultParams.isNotEmpty())
                throw SerializerException("Output class '${input.simpleName}' have primary constructor with optional arguments $defaultParams, which is not allowed on any output class!")
        }
    }

}