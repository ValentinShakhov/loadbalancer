import java.util.Timer
import java.util.TimerTask

class HeartBeatChecker(
    private val loadBalancer: LoadBalancer,
    private val heartBeatsToInclude: Int,
    private val heartBeatCheckDelayMillis: Long
) {

    private val timer = Timer()
    private val heartBeats: MutableMap<String, Int> = HashMap()

    fun start() {
        println("Starting heart beat checker...")
        timer.schedule(object : TimerTask() {
            override fun run() {
                checkActiveProviders()
                checkInactiveProviders()
            }
        }, heartBeatCheckDelayMillis)
    }

    fun stop() = timer.cancel().also { println("Heart beat checker stopped") }

    fun checkActiveProviders() {
        loadBalancer.getActiveProviders().filter { !it.check() }.forEach {
            println("Provider ${it.getId()} is unavailable. Disabling...")
            loadBalancer.exclude(it.getId())
        }
    }

    fun checkInactiveProviders() {
        loadBalancer.getInactiveProviders().forEach {
            val prevCount = heartBeats[it.getId()] ?: 0
            val newCount = prevCount + (if (it.check()) 1 else 0)

            if (newCount >= heartBeatsToInclude) {
                println("Disabled Provider ${it.getId()} is fully responsive again. Enabling...")
                loadBalancer.include(it.getId())
                heartBeats.remove(it.getId())
            } else {
                println("Disabled Provider ${it.getId()} responded, will check again later")
                heartBeats[it.getId()] = newCount
            }
        }
    }
}