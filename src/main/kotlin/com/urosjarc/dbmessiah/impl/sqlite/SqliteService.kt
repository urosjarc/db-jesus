package com.urosjarc.dbmessiah.impl.sqlite

import com.urosjarc.dbmessiah.Engine
import com.urosjarc.dbmessiah.Serializer
import com.urosjarc.dbmessiah.impl.DbMessiahService

class SqliteService(
    eng: Engine,
    ser: Serializer,
) : DbMessiahService(eng = eng, ser = ser) {

    /**
     * Because sqlite C driver does not support auto generated keys :)
     */
    override fun <T : Any> insert(obj: T): Boolean {
        //Prepare insert query
        val T = this.ser.mapper.getTableInfo(obj = obj)
        val query = this.ser.insertQuery(obj = obj)

        //GET ID by force
        val id = this.eng.executeInsert(query = query, primaryKey = T.primaryKey.kprop, onGeneratedKeysFail = "select last_insert_rowid();") { rs, i ->
            rs.getInt(i)
        }

        //UPDATE OBJECT AUTOMATICLY
        if (id != null) {
            T.primaryKey.setValue(obj = obj, value = id)
            return true
        }

        return false
    }
}
