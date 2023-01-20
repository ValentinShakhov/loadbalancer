import java.util.Timer
import java.util.TimerTask

class HeartBeatChecker(private val loadBalancer: LoadBalancer) {

    private val timer = Timer()
    private val heartBeats: MutableMap<String, Int> = HashMap()

    fun start() {
        println("Starting heart beat checker...")
        timer.schedule(object : TimerTask() {
            override fun run() {
                checkActiveProviders()
                checkInactiveProviders()
            }
        }, HEART_BEAT_CHECK_DELAY_MILLIS)
    }

    fun stop() = timer.cancel().also { println("Heart beat checker stopped") }

    private fun checkActiveProviders() {
        loadBalancer.getActiveProviders().filter { !it.check() }.forEach {
            println("Provider ${it.getId()} is unavailable. Disabling...")
            loadBalancer.exclude(it.getId())
        }
    }

    private fun checkInactiveProviders() {
        loadBalancer.getInactiveProviders().filter { it.check() }.forEach {
            val prevCount = heartBeats[it.getId()]
            val newCount = calcNewCount(prevCount, it)

            if (newCount >= HEART_BEATS_TO_INCLUDE) {
                println("Disabled Provider ${it.getId()} is fully responsive again. Enabling...")
                loadBalancer.include(it.getId())
                heartBeats.remove(it.getId())
            } else {
                println("Disabled Provider ${it.getId()} responded, will check again later")
                heartBeats[it.getId()] = newCount
            }
        }
    }

    private fun calcNewCount(prevCount: Int?, it: Provider) = if (prevCount != null && it.check()) {
        prevCount + 1
    } else {
        if (it.check()) {
            1
        } else {
            0
        }
    }
}