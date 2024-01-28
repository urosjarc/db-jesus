package com.urosjarc.dbmessiah.impl

import com.urosjarc.dbmessiah.Engine
import com.urosjarc.dbmessiah.domain.queries.PreparedQuery
import com.urosjarc.dbmessiah.domain.queries.Query
import com.urosjarc.dbmessiah.domain.queries.QueryValue
import com.urosjarc.dbmessiah.domain.serialization.Encoder
import com.urosjarc.dbmessiah.exceptions.EngineException
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.logging.log4j.kotlin.logger
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KProperty1


class DbMessiahEngine(config: HikariConfig) : Engine {

    val dataSource = HikariDataSource(config)
    val log = this.logger()

    init {
        if (this.dataSource.isClosed && !this.dataSource.isRunning) {
            throw Exception("Database source is closed or not running!")
        }
    }

    private val conn
        get(): Connection {
            try {
                return this.dataSource.connection
            } catch (e: SQLException) {
                throw EngineException(msg = "Could not get connection!", cause = e)
            }
        }

    override fun prepareQuery(query: Query, autoGeneratedKey: KProperty1<out Any, Any?>?): PreparedQuery {
        this.log.info("Preparing query: \n\n${query}")

        //Get prepare statement
        val ps =
            if (autoGeneratedKey == null)
                this.conn.prepareStatement(query.sql)
            else
                this.conn.prepareStatement(query.sql, arrayOf(autoGeneratedKey.name))

        val pQuery = PreparedQuery(query = query, ps = ps)

        //Apply values to prepared statement
        (query.queryValues).forEachIndexed { i, queryValue: QueryValue ->
            if (queryValue.value == null) ps.setNull(i + 1, queryValue.jdbcType.ordinal) //If value is null encoding is done with setNull function !!!
            else (queryValue.encoder as Encoder<Any>)(ps, i + 1, queryValue.value) //If value is not null encoding is done over user defined encoder !!!
        }

        this.log.info("Prepared statement: ${ps}")
        return pQuery
    }


    override fun <T> executeQuery(pQuery: PreparedQuery, decodeResultSet: (rs: ResultSet) -> T): List<T> {
        val objs = mutableListOf<T>()
        try {
            val rs = pQuery.ps.executeQuery()
            while (rs.next()) {
                objs.add(decodeResultSet(rs))
            }
            rs.close()
        } catch (e: SQLException) {
            throw EngineException(msg = "Could not execute select statement!", cause = e)
        }
        return objs
    }

    override fun executeUpdate(pQuery: PreparedQuery): Int {
        try {
            return pQuery.ps.executeUpdate()
        } catch (e: SQLException) {
            throw EngineException(msg = "Could not execute update statement!", cause = e)
        }
    }

    override fun <T> executeInsert(pQuery: PreparedQuery, onGeneratedKeysFail: String?, decodeIdResultSet: ((rs: ResultSet, i: Int) -> T)): T? {
        val pstmnt = pQuery.ps

        //Execute query
        try {
            val numUpdates = pstmnt.executeUpdate()
            if (numUpdates == 0) return null
        } catch (e: SQLException) {
            throw EngineException(msg = "Could not execute insert statement!", cause = e)
        }

        //Try fetching ids normaly
        try {
            val rs = pstmnt.generatedKeys
            if (rs.next()) return decodeIdResultSet(rs, 1)
        } catch (e: SQLException) {
            this.log.warn(e)
        }

        //Try fetching ids with force
        if (onGeneratedKeysFail != null) {
            try {
                val rs = pstmnt.connection.prepareStatement(onGeneratedKeysFail).executeQuery()
                if (rs.next()) return decodeIdResultSet(rs, 1)
            } catch (e: SQLException) {
                this.log.warn(e)
            }
        }

        throw EngineException(msg = "Could not retrieve inserted id!")
    }

    override fun executeQueries(pQuery: PreparedQuery, decodeResultSet: (i: Int, rs: ResultSet) -> Unit) {
        val ps = pQuery.ps
        try {
            var isResultSet = ps.execute()

            var count = 0
            while (true) {
                if (isResultSet) {
                    val rs = ps.resultSet
                    decodeResultSet(count, rs)
                } else if (ps.updateCount == -1) break
                count++
                isResultSet = ps.moreResults
            }

        } catch (e: SQLException) {
            throw EngineException(msg = "Could not execute statement!", cause = e)
        }
    }

}
