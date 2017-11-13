package network

import blockchain.db.models.Block
import blockchain.db.models.Transaction
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

fun toJson(block: Any) : String{

    val mapper = jacksonObjectMapper()
    var typyOfBlock = 'b'

    if(block is Block){
        typyOfBlock = 'b'
    }else if(block is Transaction){
        typyOfBlock = 't'
    }

    val jsonStr =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(block)
    return typyOfBlock + jsonStr
}

fun fromJson(blockType: Char, data: String) : Any{
    val mapper = jacksonObjectMapper()
    when(blockType){
        'b' -> { //Это блок
            return mapper.readValue<Block>(data)
        }
        't' -> { //Транзакция
            return mapper.readValue<Transaction>(data)
        }
    }

    return Unit
}