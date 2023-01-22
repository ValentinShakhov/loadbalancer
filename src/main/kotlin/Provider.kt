import java.util.concurrent.Callable
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class Provider(private val id: String, poolSize: Int) {

    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        poolSize,
        poolSize,
        0L,
        TimeUnit.MILLISECONDS,
        SynchronousQueue(),
        AbortPolicy()
    )

    private val enabled = AtomicBoolean(true)

    fun get(): String {
        return executor.submit(Callable {
            println("Provider $id executing...")
            Thread.sleep(Random.nextLong(20, 200))
            return@Callable id
        }).get()
    }

    fun getId() = id

    fun check() = enabled.get()

    fun setEnabled(value: Boolean) = enabled.set(value)

    fun shutdown() = executor.shutdown()
}