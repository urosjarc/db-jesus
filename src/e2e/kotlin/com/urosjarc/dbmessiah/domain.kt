package com.urosjarc.dbmessiah

import com.urosjarc.dbmessiah.domain.Table
import kotlin.random.Random

data class TestProcedure(
    val parent_pk: Int
)

class TestProcedureEmpty

data class Input(
    val child_pk: Int,
    val parent_pk: Int,
)

data class Output(
    val child_pk: Int,
    val parent_pk: Int,
)

data class Child(
    var pk: Int? = null,
    val fk: Int,
    val col: String
) {
    companion object {
        fun get(pk: Int? = null, fk: Int, seed: Int): Child {
            val random = Random(seed = seed)
            return Child(
                pk = pk,
                fk = fk,
                col = random.nextInt().toString()
            )
        }
    }
}

data class Parent(
    var pk: Int? = null,
    var col: String
) {
    companion object {
        fun get(pk: Int? = null, seed: Int): Parent {
            val random = Random(seed = seed)
            return Parent(pk = pk, col = random.nextInt().toString())
        }
    }
}


fun testSchema(name: String) = Schema(
    name = name, tables = listOf(
        Table(primaryKey = Parent::pk),
        Table(
            primaryKey = Child::pk, foreignKeys = listOf(
                Child::fk to Parent::class
            )
        )
    ),
    procedures = listOf(TestProcedure::class, TestProcedureEmpty::class)
)
