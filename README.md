# Hashids.kt

A Kotlin class to generate YouTube-like hashes from one or many numbers.

Ported from Java [Hashids.java](https://github.com/jiecao-fm/hashids-java/blob/master/src/main/java/org/hashids/Hashids.java) by [fanweixiao](https://github.com/fanweixiao) (is port of javascript [hashids.js](https://github.com/ivanakimov/hashids.js/blob/master/lib/hashids.js) by [Ivan Akimov](https://github.com/ivanakimov))

## What is it?

Hashids (Hash ID's) creates short, unique, decryptable hashes from unsigned (long) integers.

This algorithm tries to satisfy the following requirements:

1. Hashes must be unique and decryptable.
2. They should be able to contain more than one integer (so you can use them in complex or clustered systems).
3. You should be able to specify minimum hash length.
4. Hashes should not contain basic English curse words (since they are meant to appear in public places - like the URL).

Instead of showing items as `1`, `2`, or `3`, you could show them as `U6dc`, `u87U`, and `HMou`.
You don't have to store these hashes in the database, but can encrypt + decrypt on the fly.

All (long) integers need to be greater than or equal to zero.

## Usage

#### Import the package

```java
import org.hashids;
```

#### Encrypting one number

You must pass a unique salt string so your hashes differ from everyone. I use "this is my salt" as an example.

```kotlin

val hashids = Hashids("this is my salt")
val hash: String = hashids.encode(12345)
```

`hash` is now going to be: NkK9

#### Decrypting

Notice: during decryption, the same salt value has to be used:

```kotlin

val hashids = Hashids("this is my salt")
val numbers: LongArray = hashids.decode("NkK9")
val numver: Int = numbers[0]
```

`numbers` is now going to be: [12345]
`number` is: 12345

#### Decrypting with different salt

Decryption will not work if salt is changed:

```kotlin

val hashids = Hashids("this is my pepper")
val numbers: LongArray = hashids.decode("NkK9")
```

`numbers` is now going to be: []

#### Encrypting several numbers

```kotlin

val hashids = Hashids("this is my salt")
val hash: String = hashids.encode(683L, 94108L, 123L, 5L)
```

`hash` is now going to be: aBMswoO2UB3Sj

#### Decrypting is done the same way

```kotlin

val hashids = Hashids("this is my salt")
val numbers: String = hashids.decode("aBMswoO2UB3Sj")
```

`numbers` is now going to be: [683, 94108, 123, 5]

#### Encrypting and specifying minimum hash length

Here we encode integer 1, and set the minimum hash length to **8** (by default it's **0** -- meaning hashes will be the shortest possible length).

```kotlin

val hashids = Hashids("this is my salt", 8)
val hash: String = hashids.encode(1)
```

`hash` is now going to be: gB0NV05e

#### Decrypting

```kotlin

val hashids = Hashids("this is my salt", 8)
val numbers: String = hashids.decode("gB0NV05e")
```

`numbers` is now going to be: [1]

#### Specifying custom hash alphabet

Let's set the alphabet that consist of only four letters: "0123456789abcdef"

```kotlin

val hashids = Hashids("this is my salt", 0, "0123456789abcdef")
val hash: String = hashids.encode(1234567)
```

`hash` is now going to be: b332db5

#### Repeating numbers

```kotlin

val hashids = Hashids("this is my salt")
val hash: String = hashids.encode(5, 5, 5, 5);
```

You don't see any repeating patterns that might show there's 4 identical numbers in the hash: 1Wc8cwcE

Same with incremented numbers:

```kotlin

val hashids = Hashids("this is my salt")
val hash: String = hashids.encode(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
```

`hash` will be: kRHnurhptKcjIDTWC3sx

### Incrementing number hashes:

```kotlin

val hashids = Hashids("this is my salt")
val hash1: String = hashids.encode(1) /* NV */
val hash2: String = hashids.encode(2) /* 6m */
val hash3: String = hashids.encode(3) /* yD */
val hash4: String = hashids.encode(4) /* 2l */
val hash5: String = hashids.encode(5) /* rD */
```

## Contact

Follow me [@spuklo](https://twitter.com/spuklo), [@leprosus](https://twitter.com/leprosus_ru), [@IvanAkimov](http://twitter.com/ivanakimov), [@fanweixiao](https://twitter.com/fanweixiao)

## License

MIT License. See the `LICENSE` file.
