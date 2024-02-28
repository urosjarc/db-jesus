package com.urosjarc.dbmessiah

import com.urosjarc.dbmessiah.data.BatchQuery
import com.urosjarc.dbmessiah.data.Encoder
import com.urosjarc.dbmessiah.data.Query
import com.urosjarc.dbmessiah.data.QueryValue
import com.urosjarc.dbmessiah.exceptions.DriverException
import com.urosjarc.dbmessiah.exceptions.base.IssueException
import org.apache.logging.log4j.kotlin.logger
import java.sql.*

/**
 * A class representing a database driver.
 *
 * @property conn The connection to the database.
 */
public open class Driver(private val conn: Connection) {

    /** @suppress */
    private val log = this.logger()

    /**
     * Prepares the given query by replacing question marks in the SQL template with the corresponding values
     * and setting the values to the provided PreparedStatement.
     *
     * @param ps The [PreparedStatement] object where the values are set.
     * @param query The [Query] object representing the SQL query and its values.
     */
    private fun prepareQuery(ps: PreparedStatement, query: Query) {
        var rawSql = query.sql
        //Apply values to prepared statement
        (query.values).forEachIndexed { i, queryValue: QueryValue ->
            rawSql = rawSql.replaceFirst("?", queryValue.escapped)
            if (queryValue.value == null) ps.setNull(i + 1, queryValue.jdbcType.ordinal) //If value is null encoding is done with setNull function !!!
            else (queryValue.encoder as Encoder<Any>)(
                ps, i + 1, queryValue.value
            ) //If value is not null encoding is done over user defined encoder !!!
        }
        this.log.info("Prepare query: ${rawSql.replace("\\s+".toRegex(), " ")}")
    }

    /**
     * Tries to close the provided PreparedStatement and ResultSet objects.
     *
     * @param ps The [PreparedStatement] object to be closed.
     * @param rs The [ResultSet] object to be closed.
     */
    private fun closeAll(ps: PreparedStatement? = null, rs: ResultSet? = null) {
        try {
            ps?.close()
        } catch (e: Throwable) {
            this.log.error("Closing prepared statement failed", e)
        }
        try {
            rs?.close()
        } catch (e: Throwable) {
            this.log.error("Closing resultSet failed", e)
        }
    }

    /**
     * Creates batch groups of 1000 and then execute them one by one on the database connection.
     * Groups are created executed separately in order to not block database process.
     *
     * @param batchQuery The [BatchQuery] object containing the SQL query string and value matrix.
     * @return The number of updates executed.
     * @throws DriverException If there is an error processing the batch query.
     */
    internal fun batch(batchQuery: BatchQuery): Int {
        var ps: PreparedStatement? = null
        var numUpdates = 0

        try {
            //Prepare statement and query
            ps = conn.prepareStatement(batchQuery.sql)

            var i = 0
            for (values in batchQuery.valueMatrix) {
                this.prepareQuery(ps = ps, query = Query(sql = batchQuery.sql, values = values.toTypedArray()))
                ps.addBatch()
                if (++i % 1_000 == 0) {
                    val exeCount = ps.executeBatch()
                    numUpdates += exeCount.sum()
                    ps.clearParameters()
                    i = 0
                }
            }
            if (i > 0) {
                val exeCount = ps.executeBatch()
                numUpdates += exeCount.sum()
            }

            //Close everything
            this.closeAll(ps = ps)

            //Return result
            return numUpdates

        } catch (e: Throwable) {
            this.closeAll(ps = ps)
            throw DriverException(msg = "Failed to process batch results for: $batchQuery", cause = e)
        }
    }

    /**
     * Updates the database with the given query.
     *
     * @param query The [Query] object representing the SQL query and its values.
     * @return The number of rows affected by the update.
     * @throws DriverException If there is an error processing the update query.
     */
    internal fun update(query: Query): Int {
        var ps: PreparedStatement? = null

        try {
            //Prepare statement
            ps = conn.prepareStatement(query.sql)
            this.prepareQuery(ps = ps, query = query)

            //Get info
            val count: Int = ps.executeUpdate()

            //Close all
            this.closeAll(ps = ps)

            //Return count
            return count
        } catch (e: Throwable) {
            this.closeAll(ps = ps)
            throw DriverException(msg = "Failed to process update results for: $query", cause = e)
        }
    }

    /**
     * Inserts a record into the database based on the provided query and returns the generated primary ID.
     * Method will try to fetch generated primary ID normally over JDBC or by creating new database call with SQL provided by [onGeneratedKeysFail].
     *
     * @param query The [Query] object representing the SQL query and its values.
     * @param onGeneratedKeysFail The SQL statement to execute if retrieving the generated primary ID fails.
     * @param decodeIdResultSet The function to decode primary ID from the ResultSet.
     * @return The generated primary ID if the insert was successful, or null if no rows were affected.
     * @throws DriverException If there is an error processing the insert query.
     * @throws IssueException If the inserted primary ID couldn't be retrieved normally or with force.
     */
    internal fun <T> insert(query: Query, onGeneratedKeysFail: String? = null, decodeIdResultSet: ((rs: ResultSet, i: Int) -> T)): T? {
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        var rs2: ResultSet? = null

        //Execute query
        try {
            //Prepare statement
            ps = conn.prepareStatement(query.sql, Statement.RETURN_GENERATED_KEYS)
            this.prepareQuery(ps = ps, query = query)

            //Get info
            val numUpdates = ps.executeUpdate()

            //If no updates happend close all
            if (numUpdates == 0) {
                this.closeAll(ps = ps)
                return null
            }
            //Continue with getting ids for inserts
        } catch (e: Throwable) {
            this.closeAll(ps = ps)
            throw DriverException(msg = "Failed to process insert results from: $query", cause = e)
        }

        if (onGeneratedKeysFail != null) {
            //Try fetching ids normaly
            try {
                rs = ps.generatedKeys
                if (rs.next()) {
                    val data = decodeIdResultSet(rs, 1)
                    this.closeAll(ps = ps, rs = rs)
                    return data
                }
            } catch (e: SQLException) {
                //Auto reurn id is probably not supported
                //Continue with execution
                rs?.close()
            } catch (e: Throwable) {
                this.closeAll(ps = ps, rs = rs)
                throw DriverException(msg = "Failed to process id results from: $query", cause = e)
            }

            //Try fetching ids with force
            try {
                rs2 = ps.connection.prepareStatement(onGeneratedKeysFail).executeQuery()
                if (rs2.next()) {
                    val data = decodeIdResultSet(rs2, 1)
                    this.closeAll(ps = ps, rs = rs2)
                    return data
                }
            } catch (e: Throwable) {
                this.closeAll(ps = ps, rs = rs2)
                throw DriverException(msg = "Failed to process id results with '$onGeneratedKeysFail' for: $query", cause = e)
            } finally {
                this.closeAll(ps = ps, rs = rs2)
            }
        }

        throw IssueException(msg = "Could not retrieve inserted id normally nor with force from: $query")
    }

    /**
     * Executes custom user defined queries in single database call.
     *
     * @param query The [Query] object representing the SQL query and its values.
     * @param decodeResultSet A user defined function that decodes results set to list of objects (rows).
     *                        It accepts index of a query that was executed in database call, and result set for that specific query.
     * @return The result of the query as a mutable list of lists, where each inner list represents a row of the result set.
     * @throws DriverException If there is an error executing the query.
     */
    internal fun execute(query: Query, decodeResultSet: (i: Int, rs: ResultSet) -> List<Any>): MutableList<List<Any>> {
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        val returned = mutableListOf<List<Any>>()

        try {
            ps = conn.prepareStatement(query.sql)
            this.prepareQuery(ps = ps, query = query)
            var isResultSet = ps.execute()

            var count = 0
            do {
                if (isResultSet) {
                    rs = ps.resultSet
                    returned.add(decodeResultSet(count, rs))
                    rs.close()
                } else {
                    if (ps.updateCount == -1) break
                } //If there is no more result finish

                count++
                isResultSet = ps.moreResults //Get next result
            } while (isResultSet)

            return returned
        } catch (e: Throwable) {
            this.closeAll(ps = ps, rs = rs)
            throw DriverException(msg = "Failed to return query results from: $query", cause = e)
        }
    }

    /**
     * Executes a database query with the given query and a function to decode the result set.
     *
     * @param query The query to execute.
     * @param decodeResultSet The function used to decode each row of the result set.
     *                        It takes a ResultSet as input and returns an instance of type T.
     * @return A list of objects of type T, obtained by decoding each row of the result set.
     * @throws DriverException if there is an error executing the query or decoding the result set.
     */
    internal fun <T> query(query: Query, decodeResultSet: (rs: ResultSet) -> T): List<T> {
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null

        try {
            //Prepare statement and query
            ps = conn.prepareStatement(query.sql)
            this.prepareQuery(ps = ps, query = query)

            //Define results
            val objs = mutableListOf<T>()

            //Create result set and fill element
            rs = ps.executeQuery()
            while (rs.next()) {
                objs.add(decodeResultSet(rs))
            }

            //Close everything
            this.closeAll(ps = ps, rs = rs)

            //Return objects
            return objs

        } catch (e: Throwable) {
            this.closeAll(ps = ps, rs = rs)
            throw DriverException(msg = "Failed to return query results from: $query", cause = e)
        }
    }

    internal fun call(query: Query, decodeResultSet: (i: Int, rs: ResultSet) -> List<Any>): MutableList<List<Any>> {
        var ps: CallableStatement? = null
        var rs: ResultSet? = null
        val returned = mutableListOf<List<Any>>()

        try {
            //Prepare statement and query
            ps = conn.prepareCall(query.sql)
            this.prepareQuery(ps = ps, query = query)
            var isResultSet = ps.execute()

            var count = 0
            do {
                if (isResultSet) {
                    rs = ps.resultSet
                    returned.add(decodeResultSet(count, rs))
                    rs.close()
                } else {
                    if (ps.updateCount == -1) break
                } //If there is no more result finish

                count++
                isResultSet = ps.moreResults //Get next result
            } while (isResultSet)

            return returned
        } catch (e: Throwable) {
            this.closeAll(ps = ps, rs = rs)
            throw DriverException(msg = "Failed to return query results from: $query", cause = e)
        }
    }

}
