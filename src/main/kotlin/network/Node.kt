package network

import blockchain.BlockchainDelegate
import java.io.IOException
import java.net.*

import blockchain.db.models.Block
import blockchain.db.models.Transaction

class Node() : BlockchainDelegate {

    var isReadyToSentSome = false
    //lateinit var messageToSent: Any
    lateinit var pool: HashMap<String, String>
    var messageToType: Char = 'b'

    private lateinit var socket: DatagramSocket
    private lateinit var nodeThreadListener: Thread
    //private lateinit var nodeThreadSender: Thread
    private lateinit var listeningThread: Thread
    //private lateinit var sendingThread: Thread

    private var active = false

    var delegate: NodeDelegate? = null

    override fun blockDenied(block: Block) {
        println("|x| Received block denied")
    }

    override fun newBlockMined(block: Block) {
        toPool(block)
        println("Сообщение о новом блоке отправлено")
    }

    override fun newTransactionCreated(transaction: Transaction) {
        toPool(transaction)
        println("Сообщение о новой транзакции отправлено")
    }

    override fun transactionDenied(transaction: Transaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startup(pool: HashMap<String, String>, portList: List<String>) {
        println("%%% ${pool.size}")
        this.pool = pool
        for (i in 0..(portList.size - 1)) {
            try {
                socket = DatagramSocket(portList[i].toInt())
            } catch (e: SocketException) {
                //e.printStackTrace()
                continue
            }
            break
        }

        nodeThreadListener = Thread(Runnable {
            active = true
            udpListen()
            println("Running OC node on ${socket.localAddress}:${socket.localPort}")
        }, "nodeThreadListener")
        nodeThreadListener.start()

//        nodeThreadSender = Thread(Runnable {
//            while (true) {
//                if (this.isReadyToSentSome) {
//                    println("***** Sending msg to pool")
//                    toPool()
//                    this.isReadyToSentSome = false
//                }
//            }
//        }, "nodeThreadSender")
//        nodeThreadSender.start()

    }

    fun shutDown() {
        active = false
    }


    //Блок методов отвечающих за прием "сообщения"

    private fun udpListen() {
        listeningThread = Thread(Runnable {
            while (active) {
                receive()
            }
        }, "udpListeningThread")
        listeningThread.start()
    }

    private fun receive() {
        val buffer = ByteArray(4095)
        val packet = DatagramPacket(buffer, buffer.size)

        try {
            socket.receive(packet)
        } catch (e: IOException) {
            //e.printStackTrace()
        }

        val str = String(packet.data)
        //println(str)
        process(packet)
    }

    private fun process(packet: DatagramPacket) {
        // Verifies the given packet
        // Defines the packet type and decides how to treat it.
        val data = packet.data.sliceArray(1..(packet.length - 1))
        val packetType = packet.data[0]
        val k = fromJson(packetType.toChar(), String(data))
        makeActionWithRP(k)
    }

    private fun makeActionWithRP(obj: Any){
        if(obj is Block){
            delegate?.newBlockReceived(obj)
            println(obj)
        }else if(obj is Transaction){
            delegate?.newTransactionReceived(obj)
        }
        print("\n**\n")
        println(obj)
    }

    // Конец блока

    //Блок методов отвечающих за связь с пулом узлов

//    fun newMessage(message : Any) {
//        this.isReadyToSentSome = true
//        messageToSent = message
//        println("** New MSG created: $message! ")
//        println("***isReadyToSentSome = $isReadyToSentSome")
//    }

    private fun toPool(msg: Any) {
        if(!pool.isEmpty()){
            println("&&& ${pool.size}")
            for((port, address) in pool.iterator()){
                println("*** Sending packet to $address:$port")
                sendTo(msg, address, port)
            }
        }
    }

    // Конец блока

    //Блок методов отвечающих за отправку сообщения

    fun sendTo(message : Any, ip : String, port : String) {
        val sentMessage = toJson(message)
        val packet = DatagramPacket(sentMessage.toByteArray(), sentMessage.toByteArray().size,
                InetAddress.getByName(ip), port.toInt())
        try {
            socket.send(packet)
        } catch (e: IOException) {
            println("** Node $ip:$port unreachable **")
        }
    }

    //Конец блока

    private fun wrapPacket() {
        // Signs the packet
    }

}