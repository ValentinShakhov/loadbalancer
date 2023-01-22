import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LoadBalancerRoundRobinTest : LoadBalancerTest(LoadBalancerRoundRobin(2)) {

    @Test
    fun testExecutionOrder() {
        val numOfProviders = 2

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }

        assertEquals("1", loadBalancer.get())
        assertEquals("2", loadBalancer.get())
        assertEquals("1", loadBalancer.get())
    }

    @Test
    fun testExecutionOrderWhenExcludedInBetween() {
        val numOfProviders = 4

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }

        assertEquals("1", loadBalancer.get())
        assertEquals("2", loadBalancer.get())
        loadBalancer.exclude("3")
        assertEquals("4", loadBalancer.get())
    }

    @Test
    fun testExecutionOrderWhenExcludedFirst() {
        val numOfProviders = 4

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }

        assertEquals("1", loadBalancer.get())
        assertEquals("2", loadBalancer.get())
        assertEquals("3", loadBalancer.get())
        assertEquals("4", loadBalancer.get())
        loadBalancer.exclude("1")
        assertEquals("2", loadBalancer.get())
    }

    @Test
    fun testExecutionOrderWhenExcludedLast() {
        val numOfProviders = 4

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }

        assertEquals("1", loadBalancer.get())
        assertEquals("2", loadBalancer.get())
        assertEquals("3", loadBalancer.get())
        loadBalancer.exclude("4")
        assertEquals("1", loadBalancer.get())
    }
}