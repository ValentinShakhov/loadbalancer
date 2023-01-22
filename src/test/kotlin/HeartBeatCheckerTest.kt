import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HeartBeatCheckerTest {

    private val loadBalancer: LoadBalancer = LoadBalancerRoundRobin(2)

    @Test
    fun testActiveProvidersCheck() {
        val numOfProviders = 2

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }
        loadBalancer.getActiveProviders()[0].setEnabled(false)
        assertEquals(numOfProviders, loadBalancer.getActiveProviders().size)

        HeartBeatChecker(loadBalancer, 2, 50L).checkActiveProviders()

        assertEquals(numOfProviders - 1, loadBalancer.getActiveProviders().size)
        assertEquals(1, loadBalancer.getInactiveProviders().size)
    }

    @Test
    fun testInactiveProvidersCheck() {
        val heartBeatChecker = HeartBeatChecker(loadBalancer, 2, 50L)
        val numOfProviders = 2

        for (id in 1..numOfProviders) {
            loadBalancer.register(id.toString())
        }
        loadBalancer.getActiveProviders()[0].setEnabled(false)
        heartBeatChecker.checkActiveProviders()
        assertEquals(numOfProviders - 1, loadBalancer.getActiveProviders().size)
        assertEquals(1, loadBalancer.getInactiveProviders().size)

        heartBeatChecker.checkInactiveProviders()
        assertEquals(numOfProviders - 1, loadBalancer.getActiveProviders().size)
        assertEquals(1, loadBalancer.getInactiveProviders().size)

        loadBalancer.getInactiveProviders()[0].setEnabled(true)
        heartBeatChecker.checkInactiveProviders()
        assertEquals(numOfProviders - 1, loadBalancer.getActiveProviders().size)
        assertEquals(1, loadBalancer.getInactiveProviders().size)

        heartBeatChecker.checkInactiveProviders()
        assertEquals(numOfProviders, loadBalancer.getActiveProviders().size)
        assertEquals(0, loadBalancer.getInactiveProviders().size)
    }
}