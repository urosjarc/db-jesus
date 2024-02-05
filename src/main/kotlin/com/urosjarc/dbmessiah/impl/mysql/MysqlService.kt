import com.urosjarc.dbmessiah.Driver
import com.urosjarc.dbmessiah.Serializer
import com.urosjarc.dbmessiah.Service
import com.urosjarc.dbmessiah.TransConn
import com.urosjarc.dbmessiah.domain.queries.BatchQueries
import com.urosjarc.dbmessiah.domain.queries.RowQueries
import com.urosjarc.dbmessiah.domain.queries.RunOneQueries
import com.urosjarc.dbmessiah.domain.queries.TableQueries
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.util.IsolationLevel
import java.sql.Connection

class MysqlService(conf: HikariConfig, val ser: Serializer) {
    val service = Service(conf = conf)

    open class QueryConn(conn: Connection, ser: Serializer) {
        private val driver = Driver(conn = conn)
        val table = TableQueries(ser = ser, driver = driver)
        val row = RowQueries(ser = ser, driver = driver)
        val batch = BatchQueries(ser = ser, driver = driver)
        val run = RunOneQueries(ser = ser, driver = driver)
    }

    fun query(readOnly: Boolean = false, body: (conn: QueryConn) -> Unit) =
        this.service.query(readOnly = readOnly) { body(QueryConn(conn = it, ser = this.ser)) }

    class TransConn(conn: Connection, ser: Serializer) : QueryConn(conn = conn, ser = ser) {
        val roolback = TransConn(conn = conn)
    }

    fun transaction(isolationLevel: IsolationLevel? = null, body: (tr: TransConn) -> Unit) =
        this.service.transaction(isoLevel = isolationLevel) { body(TransConn(conn = it, ser = this.ser)) }
}
