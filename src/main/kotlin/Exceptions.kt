class OutOfServiceException : RuntimeException("Load Balancer out of service")
class NoActiveProvidersException : RuntimeException("No active providers found")
class ProvidersCapacityReached : RuntimeException("Providers' capacity reached")