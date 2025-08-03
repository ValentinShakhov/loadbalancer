## Load balancer
A load balancer is a component that, once
invoked, it distributes incoming requests to a list of
registered providers and return the value obtained
from one of the registered providers to the original
caller. For simplicity we will consider both the load
balancer and the provider having a public method
named get()

## Functions

- Generate provider
- Register a list of providers
- Random invocation
- Round Robin invocation
- Manual node exclusion / inclusion
- Heart beat checker
- Heart beat failure - disable provider
- Heart beat ressurection - re-enable provider
- Cluster Capacity Limit
