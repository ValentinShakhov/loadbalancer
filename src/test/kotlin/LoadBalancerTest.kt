import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class LoadBalancerTest(protected val loadBalancer: LoadBalancer) {

    @Test
    fun testRegistrationMultithreaded() {
        val threads: MutableList<Thread> = ArrayList()
        for (id in 1..10) {
            val thread = Thread {
                loadBalancer.register(id.toString())
            }
            threads.add(thread)
            thread.start()
        }

        threads.forEach {
            it.join()
        }
        assertEquals(10, loadBalancer.getActiveProviders().size)
    }

    @Test
    fun testExclusionMultithreaded() {
        val numOfThreads = 10

        for (id in 1..numOfThreads) {
            loadBalancer.register(id.toString())
        }

        val threads: MutableList<Thread> = ArrayList()
        for (id in 2..numOfThreads) {
            val thread = Thread { loadBalancer.exclude(id.toString()) }
            threads.add(thread)
            thread.start()
        }

        threads.forEach {
            it.join()
        }
        assertEquals(1, loadBalancer.getActiveProviders().size)
        assertEquals(9, loadBalancer.getInactiveProviders().size)
    }

    @Test
    fun testInclusionMultithreaded() {
        val numOfThreads = 10

        for (id in 1..numOfThreads) {
            loadBalancer.register(id.toString())
            loadBalancer.exclude(id.toString())
        }

        val threads: MutableList<Thread> = ArrayList()
        for (id in 2..numOfThreads) {
            val thread = Thread { loadBalancer.include(id.toString()) }
            threads.add(thread)
            thread.start()
        }

        threads.forEach {
            it.join()
        }
        assertEquals(9, loadBalancer.getActiveProviders().size)
        assertEquals(1, loadBalancer.getInactiveProviders().size)
    }

    @Test
    fun testExclusionAndInclusionMultithreaded() {
        val numOfThreads = 10

        for (id in 1..numOfThreads) {
            loadBalancer.register(id.toString())
        }
        for (id in 1..numOfThreads step 2) {
            loadBalancer.exclude(id.toString())
        }

        val threads: MutableList<Thread> = ArrayList()
        for (id in 1..numOfThreads step 2) {
            val thread = Thread { loadBalancer.include(id.toString()) }
            threads.add(thread)
            thread.start()
        }
        for (id in 2..numOfThreads step 2) {
            val thread = Thread { loadBalancer.exclude(id.toString()) }
            threads.add(thread)
            thread.start()
        }

        threads.forEach {
            it.join()
        }
        assertEquals(numOfThreads / 2, loadBalancer.getActiveProviders().size)
        assertEquals(numOfThreads / 2, loadBalancer.getInactiveProviders().size)
    }

    @Test
    fun testGetWhileExclusionAndInclusionMultithreaded() {
        val numOfProviders = 10

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }
        for (id in 1..numOfProviders step 2) {
            loadBalancer.exclude(id.toString())
        }

        val results: MutableList<String> = ArrayList()
        val threads: MutableList<Thread> = ArrayList()
        for (id in 1..numOfProviders step 2) {
            val thread = Thread { loadBalancer.include(id.toString()) }
            threads.add(thread)
            thread.start()
        }
        for (id in 1..numOfProviders) {
            val thread = Thread {
                val result = loadBalancer.get()
                synchronized(results) { results.add(result) }
            }
            threads.add(thread)
            thread.start()
        }
        for (id in 2..numOfProviders step 2) {
            val thread = Thread { loadBalancer.exclude(id.toString()) }
            threads.add(thread)
            thread.start()
        }

        threads.forEach {
            it.join()
        }
        assertEquals(numOfProviders / 2, loadBalancer.getActiveProviders().size)
        assertEquals(numOfProviders / 2, loadBalancer.getInactiveProviders().size)
        assertEquals(numOfProviders, results.size)
    }

    @Test
    fun testGetAboveCapacityMultithreaded() {
        val numOfProviders = 2
        val numOfRequests = numOfProviders * loadBalancer.providerPoolSize + 1

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }

        val results: MutableList<String> = ArrayList()
        val threads: MutableList<Thread> = ArrayList()
        for (id in 1..numOfRequests) {
            val thread = Thread {
                val result = loadBalancer.get()
                synchronized(results) { results.add(result) }
            }
            threads.add(thread)
            thread.start()
        }

        threads.forEach {
            it.join()
        }

        assertEquals(numOfRequests - 1, results.size)
    }

    @Test
    fun testGetFailOnSingleDisabledProvider() {
        loadBalancer.register("providerId")
        loadBalancer.getActiveProviders()[0].setEnabled(false)
        assertThrows<NoActiveProvidersException> { loadBalancer.get() }
    }

    @Test
    fun testGetSucceedsOnOneOfProvidersDisabled() {
        val numOfProviders = 2

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }
        loadBalancer.getActiveProviders()[0].setEnabled(false)
        assertEquals(numOfProviders, loadBalancer.getActiveProviders().size)

        while (loadBalancer.getActiveProviders().size != numOfProviders - 1) {
            assertDoesNotThrow { loadBalancer.get() }
        }
        assertEquals(1, loadBalancer.getInactiveProviders().size)
    }
}