import blockchain.Blockchain
import blockchain.db.models.Block
import blockchain.db.models.Transaction
import client.Client
import network.Node
import network.fromJson
import network.toJson
//import sun.security.tools.jarsigner.SignatureFile
import utils.toHexString
import java.net.InetAddress
import java.nio.charset.Charset

fun main(args: Array<String>) {

    val client = Client()

    client.sendCoinsTo(100, "Ivan")
    client.createDeposit()
    client.mine()

}
