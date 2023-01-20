import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class LoadBalancer {

    protected val inactiveProviders: MutableMap<String, Provider> = HashMap()
    protected val numOfSessions = AtomicInteger()
    private val shuttingDown = AtomicBoolean(false)

    fun shutDown() {
        println("Balancer stopping...")
        shuttingDown.set(true)
        shutdownActiveProviders()
        shutdownInactiveProviders()
        println("Balancer stopped")
    }

    abstract fun register(provider: Provider)

    abstract fun exclude(id: String)

    abstract fun include(id: String)

    abstract fun get(): String

    abstract fun getActiveProviders(): List<Provider>

    protected abstract fun shutdownActiveProviders()

    fun getInactiveProviders() = inactiveProviders.values.toList()

    protected fun execute(provider: Provider): String {
        if (shuttingDown.get()) {
            throw RuntimeException("Balancer is out of service")
        }

        println("Using Provider ${provider.getId()}")
        return try {
            numOfSessions.incrementAndGet()
            val result = provider.get()
            numOfSessions.decrementAndGet()

            return result
        } catch (e: RejectedExecutionException) {
            println("Provider ${provider.getId()} is busy, trying another one...")
            numOfSessions.decrementAndGet()
            get()
        }
    }

    protected fun excludeAndRetry(provider: Provider): String {
        println("Provider ${provider.getId()} is disabled")
        exclude(provider.getId())
        println("Retrying execution...")
        return get()
    }
    private fun shutdownInactiveProviders() = inactiveProviders.values.forEach { it.shutdown() }
}
