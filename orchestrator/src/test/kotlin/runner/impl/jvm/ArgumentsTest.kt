package runner.impl.jvm

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import technology.idlab.RDFCException
import technology.idlab.runner.impl.jvm.Arguments

class ArgumentsTest {
  @Test
  fun delegation() {
    val args =
        Arguments(
            mapOf(
                "words" to listOf("the", "a"),
                "present" to listOf(true),
            ))
    val obj =
        object {
          val words: List<String> by args
          val present: Boolean? by args
          val nulled: Boolean? by args
        }
    assertEquals(listOf("the", "a"), obj.words)
    assertEquals(true, obj.present)
    assertEquals(null, obj.nulled)
  }

  @Test
  fun single() {
    val args = Arguments(mapOf("key" to listOf("value")))
    assertEquals("value", args.get<String>("key"))
  }

  @Test
  fun notSingle() {
    val args = Arguments(mapOf("key" to listOf("value1", "value2")))
    assertThrows<RDFCException> { args.get<String>("key") }
  }

  @Test
  fun singleList() {
    val args = Arguments(mapOf("key" to listOf("value")))
    assertEquals(listOf("value"), args.get<List<String>>("key"))
  }

  @Test
  fun longList() {
    val args = Arguments(mapOf("key" to listOf("value1", "value2")))
    assertEquals(listOf("value1", "value2"), args.get<List<String>>("key"))
  }

  @Test
  fun longListWrong() {
    val args = Arguments(mapOf("key" to listOf("value1", "value2")))

    assertThrows<RDFCException> { args.get<List<Int>>("key", strict = true) }
  }

  @Test
  fun nullable() {
    val args = Arguments(mapOf())
    assertEquals(null, args.get<String?>("key"))
  }

  @Test
  fun nonNullable() {
    val args = Arguments(mapOf())
    assertThrows<RDFCException> { args.get<String>("key") }
  }

  @Test
  fun invalidCast() {
    val args = Arguments(mapOf("key" to listOf("value")))
    assertThrows<RDFCException> { args.get<Int>("key") }
  }

  @Test
  fun pairs() {
    val args = Arguments(mapOf("first" to listOf(Pair(1, "a")), "second" to listOf(Pair(2, "b"))))

    // Get first pair correctly.
    val first = args.get<Pair<Int, String>>("first")
    assertEquals(1, first.first)
    assertEquals("a", first.second)

    // Get second pair correctly, use operator syntax.
    val second: Pair<Int, String> = args["second"]
    assertEquals(2, second.first)
    assertEquals("b", second.second)

    // Get first pair as a list.
    val firstList = args.get<List<Pair<Int, String>>>("first")
    assertEquals(1, firstList[0].first)
    assertEquals("a", firstList[0].second)

    // Same for second, use operator syntax.
    val secondList: List<Pair<Int, String>> = args["second"]
    assertEquals(2, secondList[0].first)
    assertEquals("b", secondList[0].second)

    // Attempt to get integer as double, in strict mode.
    assertThrows<RDFCException> { args.get<Pair<Double, String>>("first", strict = true) }

    // Attempt to get string as integer.
    assertThrows<RDFCException> { args.get<Pair<Int, Int>>("first") }
  }

  @Test
  fun nested() {
    val args = Arguments.from(mapOf("root" to listOf(mapOf("leaf" to listOf("Hello, World!")))))

    val value = args.get<Arguments>("root").get<String>("leaf")
    assertEquals("Hello, World!", value)
  }

  @Test
  fun inheritance() {
    // The base class.
    open class A

    // The extended class.
    open class B : A()

    // The extended, extended class.
    class C : B()

    // Create three arguments, each with the lists.
    val args =
        Arguments(mapOf("a" to listOf(A(), A()), "b" to listOf(B(), B()), "c" to listOf(C(), C())))

    assertEquals(2, args.get<List<A>>("a", strict = true).size)
    assertEquals(2, args.get<List<A>>("b", strict = true).size)
    assertEquals(2, args.get<List<A>>("c", strict = true).size)

    assertThrows<RDFCException> { args.get<List<B>>("a", strict = true) }
    assertEquals(2, args.get<List<B>>("b", strict = true).size)
    assertEquals(2, args.get<List<B>>("c", strict = true).size)

    assertThrows<RDFCException> { args.get<List<C>>("a", strict = true) }
    assertThrows<RDFCException> { args.get<List<C>>("b", strict = true) }
    assertEquals(2, args.get<List<C>>("c", strict = true).size)
  }
}
