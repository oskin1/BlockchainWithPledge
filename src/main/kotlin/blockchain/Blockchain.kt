package blockchain

import utils.HASH.sha256
import blockchain.db.models.Block
import blockchain.db.models.Transaction
import network.NodeDelegate

class Blockchain(val addr: String, val sig: String? = null) : NodeDelegate {
    private val version = 1  // Version of the BC
    private var currentTransactions = mutableListOf<Transaction>()  // Txn Pool
    var chain = mutableListOf<Block>()

    private var difficulty = 3  // POW difficulty level

    private lateinit var miningThread: Thread

    var delegate: BlockchainDelegate? = null

    init {
        // Creates genesis block.
        createGenesisBlock()
    }

    override fun newTransactionReceived(transaction: Transaction) {
        val depositTx = currentTransactions.find { it.type == DefaultBlockchainSettings.TxTypes.DEP &&
                it.permForBlockHeight == transaction.permForBlockHeight && it.sender == transaction.sender &&
                it.recipient == DefaultBlockchainSettings.depositHoldingPoolAddr }
        if (depositTx != null) currentTransactions.remove(depositTx)

        currentTransactions.add(transaction)
        println("|v| New tx received from net")
    }

    override fun newBlockReceived(block: Block) {
        if (!commitBlock(block)) delegate?.blockDenied(block)
        println("|v| New block received from net")
    }

    private fun setupChain(): List<Block> {
        TODO("Not implemented")
    }

    fun createBlock(prevBlockHash: String = hash(chain.last()), commit: Boolean = true) {
        // Add coinbase txn with miner's reward.
        addTransaction("coinbase", addr, 12)

        // Push ready txs from current pool to Block.
        val readyPool = currentTransactions.filter {
            it.type == 100 || it.borderBlockHeight!! < getLastBlock().height
        }.toList()

        // Creates a new Block.
        val block = Block(
                version = version,
                height = chain.size + 1,
                timestamp = System.currentTimeMillis(),
                nonce = 0,  // Initial nonce
                prevBlockHash = prevBlockHash,
                merkleRootHash = merkle(prepareTrxsHashList(readyPool)),
                emitterAddr = addr
        )

        block.transactions = readyPool
        block.transactionsCount = readyPool.size

        // Increment nonce until the valid hash found.
        while (!validNonce(block)) block.nonce++

        delegate?.newBlockMined(block)

        if (commit) commitBlock(block)

//        miningThread = Thread(Runnable {
//            // Increment nonce until the valid hash found.
//            while (!validNonce(block)) block.nonce++
//
//            delegate?.newBlockMined(block)
//
//            if (commit) commitBlock(block)
//        }, "MiningThread")
//        miningThread.start()
    }

    fun commitBlock(block: Block): Boolean {
        // Commits the given Block to the Blockchain.
        if (block.height == getLastBlock().height) {
            if (validChain(chain) && validBlock(block)) chain[chain.size - 1] = block
            return true
        }
        if (validChain(chain) && validBlock(block)) {

            val unconfirmedDepPool = currentTransactions.filter {
                it.type == 101 && it.borderBlockHeight!! >= getLastBlock().height
            }
            if (unconfirmedDepPool.isNotEmpty()) {
                // Drop confirmed txs from cur pool.
                currentTransactions = unconfirmedDepPool.toMutableList()
            } else {
                currentTransactions.clear()
            }

            chain.add(block)

            // Save Block to db.
            block.save()

            print("\nCommitting new block:\n-> $block\n")

            return true
        }

        println("|x| Block <H: ${block.height}, TC: ${block.transactions.size}> is invalid!")
        return false
    }

    @JvmOverloads
    fun addTransaction(sender: String, recipient: String, amount: Long,
                       timestamp: Long = System.currentTimeMillis()): Int {
        // Creates a new transaction to go into the next mined Block.
        val tx = Transaction(sender, recipient, amount, timestamp)
        currentTransactions.add(tx)
        tx.save()

        println("Added tx: ${tx.sender} -> ${tx.recipient}: ${tx.amount}; tx is of type: ${tx.type}")
        delegate?.newTransactionCreated(tx)

        return getLastBlock().height + 1
    }

    fun createDepositTransaction(permForBlockHeight: Int) {
        // Creates a new deposit transaction to be held in the pool.
        val tx = Transaction(addr, DefaultBlockchainSettings.depositHoldingPoolAddr,
                DefaultBlockchainSettings.depositAmount, System.currentTimeMillis(),
                DefaultBlockchainSettings.TxTypes.DEP,
                permForBlockHeight,
                DefaultBlockchainSettings.depositDuration + getLastBlock().height)

        currentTransactions.add(tx)
        tx.save()

        println("Added tx: ${tx.sender} -> ${tx.recipient}: ${tx.amount}; tx is of " +
                "type: DEP; Border height: ${tx.permForBlockHeight}")
    }

    fun getLastBlock(): Block = chain.last()

    fun resolveConflicts(chain: List<Block>): Boolean {
        if (chain.size > this.chain.size && validChain(chain)) {
            this.chain = chain.toMutableList()
            return true
        }
        return false
    }

    fun adjustDiffRate(): Unit {
        // Not implemented.
    }

    fun hash(block: Block): String {
        // Creates the SHA-256 hash of Block header.
        var header = block.version.toString(16) + block.height.toString(16) +
                block.timestamp.toString(16) + block.nonce.toString(16) + block.prevBlockHash + block.emitterAddr
        if (block.transactions.isNotEmpty()) header += merkle(prepareTrxsHashList(block.transactions))

        return sha256(sha256(header))
    }

    fun hash(transaction: Transaction): String {
        // Creates the SHA-256 hash of Transaction header.
        val header = transaction.sender +
                transaction.recipient +
                transaction.amount.toString(16) +
                transaction.timestamp.toString(16) +
                transaction.type.toString(16)

        return sha256(sha256(header))
    }

    fun merkle(hashList: List<String>): String {
        if (hashList.isEmpty()) return ""
        if (hashList.size == 1) return hashList[0]
        val newHashList = mutableListOf<String>()
        for (i in 0..(hashList.size - 2) step 2) newHashList.add(hash2(hashList[i], hashList[i+1]))
        if (hashList.size % 2 == 1) newHashList.add(hash2(hashList.last(), hashList.last()))

        return merkle(newHashList)
    }

    private fun hash2(a: String, b: String): String {
        return sha256(sha256(a.reversed() + b.reversed())).reversed()
    }

    private fun prepareTrxsHashList(transactions: List<Transaction>): List<String> {
        // Returns the list of Transactions hashes.
        return transactions.map { hash(it) }
    }

    fun validNonce(block: Block): Boolean {
        // Validates the given nonce.
        return hash(block).slice(0..(difficulty - 1)) == "0".repeat(difficulty)
    }

    fun validTransaction(): Boolean {
        // Not implemented.
        return true
    }

    fun validBlock(block: Block): Boolean {
        // Validates the given block.
        val prevBlock: Block? = if (block.height > 1) chain[block.height - 2] else null
        if (prevBlock != null) {
            // Check emitter's permission.
            if (!validEmitPerm(block)) return false
            // Test the Block.
            if ((block.height - prevBlock.height) != 1) return false
            if (block.version < prevBlock.version) return false
            if (block.prevBlockHash != hash(prevBlock)) return false
            if (block.merkleRootHash != merkle(prepareTrxsHashList(block.transactions))) return false
            if (!validNonce(block)) return false
        } else {
            if (chain.isNotEmpty() && hash(block) != hash(chain[0])) return false
        }
        return true
    }

    private fun validEmitPerm(block: Block): Boolean {
        // Tries to find valid DEP transaction in current or past DEP tx pool.
        print("CUR TX POOL: ")
        for (tx in currentTransactions) {
            print("-> ${tx.type} ${tx.sender} - ${tx.recipient}, ${tx.permForBlockHeight} ;")
            if (tx.type == DefaultBlockchainSettings.TxTypes.DEP &&
                    tx.sender == block.emitterAddr &&
                    tx.recipient == DefaultBlockchainSettings.depositHoldingPoolAddr &&
                    block.height == tx.permForBlockHeight && tx.frozen) return true
        }

        print("\n")
        print("PAST TX POOL: ")

        for (bl in chain) {
            for (tx in bl.transactions) {
                print("-> ${tx.type} ${tx.sender} - ${tx.recipient}, ${tx.permForBlockHeight} ")
                if (tx.type == DefaultBlockchainSettings.TxTypes.DEP &&
                        tx.sender == block.emitterAddr &&
                        tx.recipient == DefaultBlockchainSettings.depositHoldingPoolAddr &&
                        block.height == tx.permForBlockHeight && tx.frozen) return true
            }
        }

        print("\n")

        println("|x| INVALID PERM FOR BLOCK H: ${block.height}")

        return false
    }

    fun validChain(chain: List<Block>): Boolean {
        // Validates the given chain.
        var currentIndex = 1

        while (currentIndex < chain.size) {
            val block = chain[currentIndex]

            if (!validBlock(block)) return false

            currentIndex++
        }

        return true
    }

    ///

    private fun createGenesisBlock() {
        // Temporary
        val block = Block(
                version = version,
                height = chain.size + 1,
                timestamp = System.currentTimeMillis(),
                nonce = 0,  // Initial nonce
                prevBlockHash = "100",
                merkleRootHash = "",
                emitterAddr = addr
        )

        while (!validNonce(block)) block.nonce++

        chain.add(block)
    }

    fun punish(nodeAddr: String) {
        val deposit = currentTransactions.find {
            it.type == DefaultBlockchainSettings.TxTypes.DEP && it.sender == nodeAddr &&
                    it.recipient == DefaultBlockchainSettings.depositHoldingPoolAddr &&
                    it.permForBlockHeight!! <= getLastBlock().height }
        if (deposit != null) {
            deposit.unfroze()
            delegate?.newTransactionCreated(deposit)

            println("Депозит <NodeAddr: $nodeAddr> отчуждён")
            return
        }

        println("|x| Депозит <NodeAddr: $nodeAddr> не найден")
    }

    private fun printCurTxPool() {
        for (tx in currentTransactions) print("$tx; ")
    }

}