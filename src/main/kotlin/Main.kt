fun main() {
    val balancer = LoadBalancerRoundRobin(2)

    for (id in 1..10) {
        balancer.register(id.toString())
    }

    val heartBeatChecker = HeartBeatChecker(balancer, 2, 1000L)
    heartBeatChecker.start()

    val requests = listOf(Thread {
        println(balancer.get())
    }, Thread {
        println(balancer.get())
    }, Thread {
        println(balancer.get())
    }, Thread {
        println(balancer.get())
    }, Thread {
        println(balancer.get())
    })


    requests.forEach { it.start() }

    requests.forEach { it.join() }
    balancer.shutDown()
    heartBeatChecker.stop()
}