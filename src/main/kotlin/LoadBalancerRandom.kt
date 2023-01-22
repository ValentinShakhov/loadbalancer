import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LoadBalancerRandom(providerPoolSize: Int) : LoadBalancer(providerPoolSize) {

    private val lock = ReentrantReadWriteLock()

    private val activeProviders: MutableMap<String, Provider> = HashMap()

    override fun get(): String {
        lock.read {
            checkActiveProvidersSize()

            with(activeProviders.values.random()) {
                if (!this.check()) {
                    return excludeAndRetry(this)
                }

                checkNumberOfSessions()

                return execute(this)
            }
        }
    }

    override fun exclude(id: String) {
        lock.write {
            activeProviders.remove(id)?.let {
                inactiveProviders[it.getId()] = it
                println("Excluded Provider $id")
            } ?: println("Provider $id is not included")
        }
    }

    override fun include(id: String) {
        lock.write {
            inactiveProviders.remove(id)?.let {
                activeProviders[it.getId()] = it
                println("Included Provider $id")
            } ?: println("Provider $id is not excluded")
        }
    }

    override fun register(providerId: String) {
        lock.write {
            activeProviders[providerId] = Provider(providerId, providerPoolSize)
            println("Registered Provider $providerId")
        }
    }

    override fun getActiveProviders() = activeProviders.values.toList()

    override fun shutdownActiveProviders() {
        activeProviders.values.forEach { it.shutdown() }
    }

    private fun checkActiveProvidersSize() {
        if (activeProviders.isEmpty()) {
            throw NoActiveProvidersException()
        }
    }

    private fun checkNumberOfSessions() {
        lock.read {
            if (numOfSessions.get() >= activeProviders.size * providerPoolSize) {
                throw ProvidersCapacityReached()
            }
        }
    }
}
