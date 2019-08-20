package org.hashids

import java.lang.Long.toHexString
import kotlin.math.ceil
import kotlin.math.pow

/**
 * Hashids developed to generate short hashes from numbers (like YouTube).
 * Can be used as forgotten password hashes, invitation codes, store shard numbers.
 * This is implementation of http://hashids.org
 *
 * @author leprosus <korolyov.denis@gmail.com>
 * @author spuklo <piszdomniejeszcze@gmail.com>
 * @license MIT
 */

class Hashids(salt: String = defaultSalt, minHashLength: Int = defaultMinimalHashLength, alphabet: String = defaultAlphabet) {
    companion object {
        const val defaultSalt = ""
        const val defaultMinimalHashLength = 0
        const val defaultAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        const val defaultSeparators = "cfhistuCFHISTU"
        const val minimalAlphabetLength = 16
        const val separatorDiv = 3.5
        const val guardDiv = 12

        private const val emptyString = ""
        private const val space = " "
        private const val maxNumber = 9007199254740992
    }

    private val finalSalt = whatSalt(salt)
    private val finalHashLength = whatHashLength(minHashLength)
    private val alphabetSeparatorsAndGuards = calculateAlphabetAndSeparators(alphabet)
    private val finalAlphabet = alphabetSeparatorsAndGuards.alphabet
    private val finalSeparators = alphabetSeparatorsAndGuards.separators
    private val finalGuards = alphabetSeparatorsAndGuards.guards

    val version = "1.0.0"

    /**
     * Encodes numbers to string
     *
     * @param numbers the numbers to encode
     * @return The encoded string
     */
    fun encode(vararg numbers: Long): String = when {
        numbers.isEmpty() -> emptyString
        numbers.any { it > maxNumber } -> throw IllegalArgumentException("Number can not be greater than ${maxNumber}L")
        else -> {
            val numbersHash = numbers.indices
                    .map { (numbers[it] % (it + 100)).toInt() }
                    .sum()

            val initialCharacter = finalAlphabet.toCharArray()[numbersHash % finalAlphabet.length]
            val (encodedString, encodingAlphabet) = initialEncode(numbers.asList(), finalSeparators.toCharArray(), initialCharacter.toString(), 0, finalAlphabet, initialCharacter.toString())
            val tempReturnString = addGuardsIfNecessary(encodedString, numbersHash)

            val halfLength = finalAlphabet.length / 2
            ensureMinimalLength(halfLength, encodingAlphabet, tempReturnString)
        }
    }

    /**
     * Decodes string to numbers
     *
     * @param hash the encoded string
     * @return Decoded numbers
     */
    fun decode(hash: String): LongArray = when {
        hash.isEmpty() -> longArrayOf()
        else -> {
            val guardsRegex = "[$finalGuards]".toRegex()
            val hashWithSpacesInsteadOfGuards = hash.replace(guardsRegex, space)
            val initialSplit = hashWithSpacesInsteadOfGuards.split(space)

            val (lottery, hashBreakdown) = extractLotteryCharAndHashArray(initialSplit)
            val returnValue = unhashSubHashes(hashBreakdown.iterator(), lottery, mutableListOf(), finalAlphabet)

            when {
                encode(*returnValue) != hash -> longArrayOf()
                else -> returnValue
            }
        }
    }

    private fun guardIndex(numbersHash: Int, returnString: String, index: Int): Int = (numbersHash + returnString.toCharArray()[index].toInt()) % finalGuards.length

    /**
     * Encoded hex string to string
     *
     * @param hex the hex string to encode
     * @return The encoded string
     */
    fun encodeHex(hex: String): String = when {
        !hex.matches("^[0-9a-fA-F]+$".toRegex()) -> emptyString
        else -> {
            val toEncode = "[\\w\\W]{1,12}".toRegex().findAll(hex)
                    .map { it.groupValues }
                    .flatten()
                    .map { it.toLong(16) }
                    .toList()
                    .toLongArray()
            encode(*toEncode)
        }
    }

    /**
     * Decodes string to hex numbers string
     *
     * @param hash the encoded string
     * @return decoded hex numbers string
     */
    fun decodeHex(hash: String): String = decode(hash)
            .map { toHexString(it).substring(1) }
            .toString()

    private fun whatSalt(aSalt: String) = when {
        aSalt.isEmpty() -> defaultSalt
        else -> aSalt
    }

    private fun whatHashLength(aLength: Int) = when {
        aLength > 0 -> aLength
        else -> defaultMinimalHashLength
    }

    private fun calculateAlphabetAndSeparators(userAlphabet: String): AlphabetAndSeparators {
        val uniqueAlphabet = unique(userAlphabet)
        when {
            uniqueAlphabet.length < minimalAlphabetLength -> throw IllegalArgumentException("alphabet must contain at least $minimalAlphabetLength unique characters")
            uniqueAlphabet.contains(space) -> throw IllegalArgumentException("alphabet cannot contains spaces")
            else -> {
                val legalSeparators = defaultSeparators.toSet().intersect(uniqueAlphabet.toSet())
                val alphabetWithoutSeparators = uniqueAlphabet.toSet().minus(legalSeparators).joinToString(emptyString)
                val shuffledSeparators = consistentShuffle(legalSeparators.joinToString(emptyString), finalSalt)
                val (adjustedAlphabet, adjustedSeparators) = adjustAlphabetAndSeparators(alphabetWithoutSeparators, shuffledSeparators)

                val guardCount = ceil(adjustedAlphabet.length.toDouble() / guardDiv).toInt()
                return if (adjustedAlphabet.length < 3) {
                    val guards = adjustedSeparators.substring(0, guardCount)
                    val seps = adjustedSeparators.substring(guardCount)
                    AlphabetAndSeparators(adjustedAlphabet, seps, guards)
                } else {
                    val guards = adjustedAlphabet.substring(0, guardCount)
                    val alphabet = adjustedAlphabet.substring(guardCount)
                    AlphabetAndSeparators(alphabet, adjustedSeparators, guards)
                }
            }
        }
    }

    private fun adjustAlphabetAndSeparators(alphabetWithoutSeparators: String, shuffledSeparators: String): AlphabetAndSeparators =
            if (shuffledSeparators.isEmpty() ||
                    (alphabetWithoutSeparators.length / shuffledSeparators.length).toFloat() > separatorDiv) {

                val sepsLength = calculateSeparatorsLength(alphabetWithoutSeparators)

                if (sepsLength > shuffledSeparators.length) {
                    val difference = sepsLength - shuffledSeparators.length
                    val seps = shuffledSeparators + alphabetWithoutSeparators.substring(0, difference)
                    val alpha = alphabetWithoutSeparators.substring(difference)
                    AlphabetAndSeparators(consistentShuffle(alpha, finalSalt), seps)
                } else {
                    val seps = shuffledSeparators.substring(0, sepsLength)
                    AlphabetAndSeparators(consistentShuffle(alphabetWithoutSeparators, finalSalt), seps)
                }
            } else {
                AlphabetAndSeparators(consistentShuffle(alphabetWithoutSeparators, finalSalt), shuffledSeparators)
            }

    private fun calculateSeparatorsLength(alphabet: String): Int = when (val s = ceil(alphabet.length / separatorDiv).toInt()) {
        1 -> 2
        else -> s
    }

    private fun unique(input: String) = input.toSet().joinToString(emptyString)

    private fun addGuardsIfNecessary(encodedString: String, numbersHash: Int): String =
            if (encodedString.length < finalHashLength) {
                val guard0 = finalGuards.toCharArray()[guardIndex(numbersHash, encodedString, 0)]
                val retString = guard0 + encodedString

                if (retString.length < finalHashLength) {
                    val guard2 = finalGuards.toCharArray()[guardIndex(numbersHash, retString, 2)]
                    retString + guard2
                } else {
                    retString
                }
            } else {
                encodedString
            }

    private fun extractLotteryCharAndHashArray(initialSplit: List<String>): Pair<Char, List<String>> {
        val separatorsRegex = "[$finalSeparators]".toRegex()
        val i = when {
            initialSplit.size == 2 || initialSplit.size == 3 -> 1
            else -> 0
        }
        val ithElementOfSplit = initialSplit[i]

        val lotteryChar = ithElementOfSplit.first()
        val finalBreakdown = ithElementOfSplit
                .substring(1)
                .replace(separatorsRegex, space)
                .split(space)
        return Pair(lotteryChar, finalBreakdown)
    }

    private tailrec fun unhashSubHashes(hashes: Iterator<String>, lottery: Char, currentReturn: MutableList<Long>, alphabet: String): LongArray {
        return when {
            hashes.hasNext() -> {
                val subHash = hashes.next()
                val buffer = "$lottery$finalSalt$alphabet"
                val newAlphabet = consistentShuffle(alphabet, buffer.substring(0, alphabet.length))
                currentReturn.add(unhash(subHash, newAlphabet))
                unhashSubHashes(hashes, lottery, currentReturn, newAlphabet)
            }
            else -> currentReturn.toLongArray()
        }
    }

    private fun hash(input: Long, alphabet: String): String =
            doHash(input, alphabet.toCharArray(), HashData(emptyString, input)).hash

    private tailrec fun doHash(number: Long, alphabet: CharArray, data: HashData): HashData = when {
        data.current > 0 -> {
            val newHashCharacter = alphabet[(data.current % alphabet.size.toLong()).toInt()]
            val newCurrent = data.current / alphabet.size
            doHash(number, alphabet, HashData("$newHashCharacter${data.hash}", newCurrent))
        }
        else -> data
    }

    private fun unhash(input: String, alphabet: String): Long =
            doUnhash(input.toCharArray(), alphabet, alphabet.length.toDouble(), 0, 0)

    private tailrec fun doUnhash(input: CharArray, alphabet: String, alphabetLengthDouble: Double, currentNumber: Long, currentIndex: Int): Long =
            when {
                currentIndex < input.size -> {
                    val position = alphabet.indexOf(input[currentIndex])
                    val newNumber = currentNumber + (position * alphabetLengthDouble.pow((input.size - currentIndex - 1))).toLong()
                    doUnhash(input, alphabet, alphabetLengthDouble, newNumber, currentIndex + 1)
                }
                else -> currentNumber
            }

    private fun consistentShuffle(alphabet: String, salt: String) = when {
        salt.isEmpty() -> alphabet
        else -> {
            val initial = ShuffleData(alphabet.toList(), salt, 0, 0)
            shuffle(initial, alphabet.length - 1, 1).alphabet.joinToString(emptyString)
        }
    }

    private tailrec fun shuffle(data: ShuffleData, currentPosition: Int, limit: Int): ShuffleData = when {
        currentPosition < limit -> data
        else -> {
            val currentAlphabet = data.alphabet.toCharArray()
            val saltReminder = data.saltReminder % data.salt.length
            val asciiValue = data.salt[saltReminder].toInt()
            val cumulativeValue = data.cumulative + asciiValue
            val positionToSwap = (asciiValue + saltReminder + cumulativeValue) % currentPosition
            currentAlphabet[positionToSwap] = currentAlphabet[currentPosition].also {
                currentAlphabet[currentPosition] = currentAlphabet[positionToSwap]
            }
            shuffle(ShuffleData(currentAlphabet.toList(), data.salt, cumulativeValue, saltReminder + 1), currentPosition - 1, limit)
        }
    }

    private tailrec fun initialEncode(numbers: List<Long>,
                                      separators: CharArray,
                                      bufferSeed: String,
                                      currentIndex: Int,
                                      alphabet: String,
                                      currentReturnString: String): Pair<String, String> = when {
        currentIndex < numbers.size -> {
            val currentNumber = numbers[currentIndex]
            val buffer = bufferSeed + finalSalt + alphabet
            val nextAlphabet = consistentShuffle(alphabet, buffer.substring(0, alphabet.length))
            val last = hash(currentNumber, nextAlphabet)

            val newReturnString = if (currentIndex + 1 < numbers.size) {
                val nextNumber = currentNumber % (last.toCharArray()[0].toInt() + currentIndex)
                val sepsIndex = (nextNumber % separators.size).toInt()
                currentReturnString + last + separators[sepsIndex]
            } else {
                currentReturnString + last
            }
            initialEncode(numbers, separators, bufferSeed, currentIndex + 1, nextAlphabet, newReturnString)
        }
        else -> Pair(currentReturnString, alphabet)
    }

    private tailrec fun ensureMinimalLength(halfLength: Int, alphabet: String, returnString: String): String = when {
        returnString.length < finalHashLength -> {
            val newAlphabet = consistentShuffle(alphabet, alphabet)
            val tempReturnString = newAlphabet.substring(halfLength) + returnString + newAlphabet.substring(0, halfLength)
            val excess = tempReturnString.length - finalHashLength
            val newReturnString = if (excess > 0) {
                val position = excess / 2
                tempReturnString.substring(position, position + finalHashLength)
            } else {
                tempReturnString
            }
            ensureMinimalLength(halfLength, newAlphabet, newReturnString)
        }
        else -> returnString
    }

}

private data class AlphabetAndSeparators(val alphabet: String, val separators: String, val guards: String = "")

private data class ShuffleData(val alphabet: List<Char>, val salt: String, val cumulative: Int, val saltReminder: Int)

private data class HashData(val hash: String, val current: Long)