fun main() {
    val balancer = LoadBalancerRoundRobin()

    for (id in 1..MAX_PROVIDERS) {
        balancer.register(Provider(id.toString()))
    }

    val heartBeatChecker = HeartBeatChecker(balancer)
    heartBeatChecker.start()

    Thread {
        println("Started request Thread")
        println(balancer.get())
    }.start()
    Thread.sleep(100)
    Thread {
        println("Started request Thread")
        println(balancer.get())
    }.start()

    Thread.sleep(5000)
    balancer.shutDown()

    heartBeatChecker.stop()
}