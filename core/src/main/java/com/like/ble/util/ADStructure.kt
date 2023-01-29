package com.like.ble.util

/**
 * 广播数据结构体
 * 包括：长度(1 byte)+类型(1 byte)+内容(N byte)，即长度=1+N
 */
data class ADStructure(val length: Int, val type: Int, val data: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ADStructure) return false

        if (length != other.length) return false
        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = length
        result = 31 * result + type
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        /**
         * 解析
         * 一个广播数据包最长37个字节：MAC地址(6 byte)+数据(31 byte。其中包括若干个广播数据结构体，长度不足自动补0。)
         * @param rawData   原始广播数据
         * @return 广播数据结构体集合
         */
        fun parse(rawData: ByteArray): List<ADStructure> {
            val aDStructures = mutableListOf<ADStructure>()
            var currentPos = 0
            while (currentPos < rawData.size) {
                val length: Int = rawData[currentPos++].toInt() and 0xFF
                if (length == 0) {
                    break
                }
                val dataLength = length - 1
                val fieldType = rawData[currentPos++].toInt() and 0xFF

                val data = ByteArray(dataLength)
                System.arraycopy(rawData, currentPos, data, 0, dataLength)
                aDStructures.add(ADStructure(length, fieldType, data))
                currentPos += dataLength
            }
            return aDStructures
        }
    }

}