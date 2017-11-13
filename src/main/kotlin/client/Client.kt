package client

import blockchain.Blockchain
import blockchain.DefaultBlockchainSettings as Dbs
import crypto.KeyManager
import network.Node
import utils.toHexString
import java.util.*
import kotlin.collections.HashMap

class Client {
    private val addr: String = "ME"  //KeyManager().pubKey.encoded.toHexString()
    private val blockchain: Blockchain = Blockchain(addr)
    private val node: Node = Node()

    val remoteNodeList: HashMap<String, String> = hashMapOf("8080" to "127.0.0.1", "8081" to "127.0.0.1",
            "8082" to "127.0.0.1", "8083" to "127.0.0.1", "8084" to "127.0.0.1")

    val portList = mutableListOf("8080", "8081", "8082", "8083", "8084")

    init {
        blockchain.delegate = node
        node.delegate = blockchain
        println(remoteNodeList)
        node.startup(remoteNodeList, portList)
    }

    fun sendCoinsTo(amount: Long, recipient: String) {
        blockchain.addTransaction(addr, recipient, amount)
    }

    fun createDeposit() {
        blockchain.createDepositTransaction(blockchain.getLastBlock().height + 1)
    }

    fun mine() {
        blockchain.createBlock()
    }

    fun printChain() {
        print("\n")
        for (block in blockchain.chain) print("BH: ${block.height}; Emitter: ${block.emitterAddr} :: ")
    }

    fun punish(nodeAddr: String) {
        blockchain.punish(nodeAddr)
    }
}