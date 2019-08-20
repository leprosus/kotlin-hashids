
import org.hashids.Hashids
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Arrays

class HashidsTest {

    private var hashids: Hashids = Hashids()

    @BeforeEach
    fun beforeEach() {
        hashids = Hashids("this is my salt")
    }

    @Test
    fun `should follow examples from main project page`() {
        val noSaltHashids = Hashids()
        val examples = mapOf("o2fXhV" to longArrayOf(1, 2, 3),
                "pYfzhG" to longArrayOf(1, 2, 4),
                "qxfOhN" to longArrayOf(1, 2, 5),
                "rkfAhN" to longArrayOf(1, 2, 6),
                "v2fWhy" to longArrayOf(1, 2, 7))

        examples.map { it.key to noSaltHashids.encode(*it.value) }.forEach { assertEquals(it.first, it.second) }
        examples.map { noSaltHashids.decode(it.key) to it.value }.forEach { assertTrue(Arrays.equals(it.first, it.second)) }
    }

    @Test
    fun `should encode large numbers`() {
        val numberToHash = 9007199254740992L
        val encode = hashids.encode(numberToHash)
        val decoded = hashids.decode(encode)
        assertEquals(numberToHash, decoded[0])
    }

    @Test
    fun `should not encode number bigger than supported max`() {
        assertThrows(IllegalArgumentException::class.java) {
            val numberToHash = 9007199254740993L
            hashids.encode(numberToHash)
        }
    }

    @Test
    fun `should return empty array if fails to decode hash`() {
        val pepperedHashids = Hashids("this is my pepper")
        val decoded = pepperedHashids.decode("NkK9")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `should encode and decode back single number`() {
        val numberToHash = 12345L
        val expectedHashid = "NkK9"

        val encoded = hashids.encode(numberToHash)
        assertEquals(expectedHashid, encoded)

        val decoded = hashids.decode(expectedHashid)
        assertEquals(1, decoded.size)
        assertEquals(numberToHash, decoded[0])
    }

    @Test
    fun `should encode and decode back multiple numbers`() {
        val numbersToHash = longArrayOf(683L, 94108L, 123L, 5L)
        val expectedHashid = "aBMswoO2UB3Sj"

        val encoded = hashids.encode(*numbersToHash)
        assertEquals(expectedHashid, encoded)

        val decoded = hashids.decode(expectedHashid)
        assertEquals(numbersToHash.size, decoded.size)
        assertTrue(Arrays.equals(decoded, numbersToHash))
    }

    @Test
    fun `should allow to specify custom alphabet`() {
        val hashids = Hashids("this is my salt", alphabet = "0123456789abcdef")
        val expectedHashid = "b332db5"
        val numberToHash = 1234567L

        val encoded = hashids.encode(numberToHash)
        assertEquals(expectedHashid, encoded)

        val decoded = hashids.decode(expectedHashid)
        assertEquals(numberToHash, decoded[0])
    }

    @Test
    fun `should allow to specify custom hash length`() {
        val expectedHashid = "gB0NV05e"
        val numberToHash = 1L
        val hashids = Hashids("this is my salt", 8)

        val encoded = hashids.encode(numberToHash)
        assertEquals(expectedHashid, encoded)

        val decoded = hashids.decode(expectedHashid)
        assertEquals(1, decoded.size)
        assertEquals(numberToHash, decoded[0])
    }

    @Test
    fun `should encode and decode the same numbers`() {
        val expected = "1Wc8cwcE"
        val numberToHash = longArrayOf(5L, 5L, 5L, 5L)

        val encoded = hashids.encode(*numberToHash)
        assertEquals(expected, encoded)

        val decoded = hashids.decode(expected)
        assertEquals(numberToHash.size, decoded.size)
        assertTrue(Arrays.equals(decoded, numberToHash))
    }

    @Test
    fun `should encode and decode array of incrementing numbers`() {
        val expected = "kRHnurhptKcjIDTWC3sx"
        val numberToHash = longArrayOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)

        val encoded = hashids.encode(*numberToHash)
        assertEquals(encoded, expected)

        val decoded = hashids.decode(expected)
        assertEquals(numberToHash.size, decoded.size)
        assertTrue(Arrays.equals(decoded, numberToHash))
    }

    @Test
    fun `should encode and decode incrementing numbers`() {
        assertEquals("NV", hashids.encode(1L))
        assertEquals("6m", hashids.encode(2L))
        assertEquals("yD", hashids.encode(3L))
        assertEquals("2l", hashids.encode(4L))
        assertEquals("rD", hashids.encode(5L))
    }

    @Test
    fun `should encode numbers bigger than max integer value`() {
        assertEquals("Y8r7W1kNN", hashids.encode(9876543210123L))
    }
}