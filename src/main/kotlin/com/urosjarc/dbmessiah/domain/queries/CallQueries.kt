package com.urosjarc.dbmessiah.domain.queries

import com.urosjarc.dbmessiah.Driver
import com.urosjarc.dbmessiah.Serializer
import com.urosjarc.dbmessiah.exceptions.SerializerException
import kotlin.reflect.KClass

class CallQueries(val ser: Serializer, val driver: Driver) {
    fun <IN : Any> call(procedure: IN, vararg outputs: KClass<*>): List<List<Any>> {
        val query = this.ser.callQuery(obj = procedure)

        val results = this.driver.execute(query = query) { i, rs ->
            this.ser.mapper.decodeMany(resultSet = rs, i = i, outputs = outputs)
        }

        if (results.size != outputs.size)
            throw SerializerException("Number of results '${results.size}' does not match with number of output classes '${outputs.size}'")

        return results
    }
}
