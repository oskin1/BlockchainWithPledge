package blockchain.db.models

import blockchain.DefaultBlockchainSettings as Dbs
import blockchain.db.pultusORM
import ninja.sakib.pultusorm.annotations.AutoIncrement
import ninja.sakib.pultusorm.annotations.PrimaryKey
import ninja.sakib.pultusorm.exceptions.PultusORMException

import com.fasterxml.jackson.annotation.JsonAnyGetter

class Transaction(
        // Header.
        val sender: String,
        var recipient: String,
        val amount: Long,
        val timestamp: Long,
        val type: Int = Dbs.TxTypes.REG,
        val permForBlockHeight: Int? = null,
        val borderBlockHeight: Int? = null,
        var frozen: Boolean = true
) : Savable {

    // Db representation.
    @PrimaryKey
    @AutoIncrement
    val id: Int = 0
    val blockId: Int? = null

    override fun save(){
        try {
            pultusORM.save(this)
        } catch (e: PultusORMException) {
            e.printStackTrace()
        }

    }

    fun unfroze() {
        frozen = false
    }

    override fun toString(): String {
        return "<T: $type> $sender -> $recipient: $amount, frozen: $frozen"
    }
}