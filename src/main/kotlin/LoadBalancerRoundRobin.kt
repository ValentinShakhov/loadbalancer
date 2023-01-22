import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LoadBalancerRoundRobin(providerPoolSize: Int) : LoadBalancer(providerPoolSize) {

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
        } ?: throw NoActiveProvidersException()
    }

    override fun exclude(id: String) {
        collectionsLock.write {
            findProviderById(id)?.let {
                if (firstProvider == it) {
                    firstProvider = it.next
                } else {
                    it.prev!!.next = it.next
                }
                if (nextProvider == it) {
                    nextProvider = if (it.next == null) {
                        firstProvider
                    } else {
                        it.next
                    }
                }
                it.next?.prev = it.prev

                it.next = null
                it.prev = null
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
                if (lastProvider == null) {
                    nextProvider = newProviderWrap
                    firstProvider = newProviderWrap
                } else {
                    lastProvider.next = newProviderWrap
                    newProviderWrap.prev = lastProvider
                }

                inactiveProviders.remove(id)
                println("Included Provider $id")
            } ?: println("Provider $id is not excluded")
        }
    }

    override fun register(providerId: String) {
        collectionsLock.write {
            with(ProviderWrap(Provider(providerId, providerPoolSize))) {
                val lastProvider: ProviderWrap? = findLastProvider()
                if (firstProvider == null && lastProvider == null) {
                    firstProvider = this
                    nextProvider = this
                    println("Registered first Provider ${provider.getId()}")
                } else {
                    this.prev = lastProvider
                    lastProvider!!.next = this
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

    private fun findLastProvider(): ProviderWrap? {
        var provider: ProviderWrap? = firstProvider
        while (provider?.next != null) {
            provider = provider.next
        }
        return provider
    }

    private fun checkNumberOfSessions() {
        collectionsLock.read {
            if (numOfSessions.get() >= getNumberOfActiveProviders() * providerPoolSize) {
                throw ProvidersCapacityReached()
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
