import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LoadBalancerRoundRobin : LoadBalancer() {

    private val collectionsLock = ReentrantReadWriteLock()
    private val nextProviderLock = ReentrantReadWriteLock()

    private var firstProvider: ProviderWrap? = null
    private var nextProvider: ProviderWrap? = null

    override fun get(): String = collectionsLock.read<String> {
        nextProvider?.let {
            nextProviderLock.write {
                if (!it.provider.check()) {
                    nextProvider = it.next ?: firstProvider
                    return excludeAndRetry(it.provider)
                }

                checkNumberOfSessions()
                nextProvider = it.next ?: firstProvider
            }

            return execute(it.provider)
        } ?: throw RuntimeException("No active providers found")
    }

    override fun exclude(id: String) {
        collectionsLock.write {
            findProviderById(id)?.let {
                it.prev!!.next = it.next
                it.next?.let { next -> next.prev = it.prev }

                inactiveProviders[it.provider.getId()] = it.provider
                println("Excluded Provider $id")
            } ?: println("Provider $id is not included")
        }
    }

    override fun include(id: String) {
        collectionsLock.write {
            inactiveProviders[id]?.let {
                val newProviderWrap = ProviderWrap(it)
                val lastProvider = findLastProvider()
                lastProvider.next = newProviderWrap
                newProviderWrap.prev = lastProvider

                inactiveProviders.remove(id)
                println("Included Provider $id")
            } ?: println("Provider $id is not excluded")
        }
    }

    override fun register(provider: Provider) {
        collectionsLock.write {
            with(ProviderWrap(provider)) {
                if (firstProvider == null) {
                    firstProvider = this
                    nextProvider = this
                    println("Registered first Provider ${provider.getId()}")
                } else {
                    val lastProvider: ProviderWrap = findLastProvider()
                    this.prev = lastProvider
                    lastProvider.next = this
                    println("Registered Provider ${provider.getId()}")
                }
            }
        }
    }

    override fun getActiveProviders(): List<Provider> {
        val result: MutableList<Provider> = ArrayList()

        var curProvider = firstProvider
        while (curProvider != null) {
            result.add(curProvider.provider)
            curProvider = curProvider.next
        }

        return result
    }

    override fun shutdownActiveProviders() {
        var curProvider = firstProvider
        do {
            curProvider?.provider?.shutdown()
            curProvider = curProvider?.next
        } while (curProvider?.next != null)
    }

    private fun findProviderById(id: String): ProviderWrap? {
        var curProvider = firstProvider
        while (curProvider != null && curProvider.provider.getId() != id) {
            curProvider = curProvider.next
        }
        return curProvider
    }

    private fun findLastProvider(): ProviderWrap {
        var lastProvider: ProviderWrap? = nextProvider
        while (lastProvider!!.next != null) {
            lastProvider = lastProvider.next
        }
        return lastProvider
    }

    private fun checkNumberOfSessions() {
        collectionsLock.read {
            if (numOfSessions.get() >= getNumberOfActiveProviders() * MAX_TASKS_PER_PROVIDER) {
                throw RuntimeException("Providers' capacity is reached")
            }
        }
    }

    private fun getNumberOfActiveProviders(): Int {
        var result = 0

        var curProvider = firstProvider
        while (curProvider != null) {
            result++
            curProvider = curProvider.next
        }

        return result
    }
}

private class ProviderWrap(val provider: Provider) {
    var next: ProviderWrap? = null
    var prev: ProviderWrap? = null
}
