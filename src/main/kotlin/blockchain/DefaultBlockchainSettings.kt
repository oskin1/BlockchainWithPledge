package blockchain

object DefaultBlockchainSettings {
    const val depositDuration = 2
    const val depositAmount: Long = 11
    const val depositHoldingPoolAddr = "0000"

    object TxTypes {
        const val REG = 100
        const val DEP = 101
    }
}