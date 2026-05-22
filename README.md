# usage-quotas

This project was created using the [Ktor Project Generator](https://start.ktor.io).

## Design choices
### Token bucket
#### Rationale
For actually implementing the usage quotas I went with a token bucket.

One of its strengths is that it's efficient to implement since only two values needs to be stored per api key, the time the last "refill" was done and how many tokens are available.

It's also possible to make a burst of requests and spend your entire quota without being completely locked out from the system for an hour since it slowly refills over time rather than at set points.

Given the specification mentioned this might be used for billing, if we were to implement a daily or monthly quota I may have gone with a different solution. When companies budget for things they usually think in calendar time rather than a sliding 24h timespan.
But with an hourly usage quota it seems more like a cost control to limit a buggy process from spending an infinite amount.

#### Implementation

I went with a `ConcurrentHashMap` for storage since it's protected for concurrent requests because the `compute` method executes atomically.

I used kotlin.time.TimeSource.Monotonic because the calculation of the new number of tokens depends on the difference between timestamps and a negative value would mess up the calculation. From my understanding it's not guaranteed to be monotonic but tries to be if such an option is available on the system where things are running.
The fact that it doesn't really represent any actual value on a clock doesn't matter since we only care about duration since the last request.

I make all calculations in nanoseconds since that's the smallest unit available, and it simplifies the logic around how the "refill" gets calculated.

Available tokens are stored as a double rather than an integer. This is again to keep the logic around the "refill" simple since it means we can top up the value with fractional tokens.

All this keeps the refill rate nice and consistent.

The solution is also quite simple to extend if we wanted to add support for multiple different quotas per api key. It would just be a matter of including an additional value in the key to the map.

### Multi unit consumption per request
#### Rationale
I made the api allow for specifying how many units you want to consume to allow for potentially having different actions with different costs.
For example if some bulk version of an action was available a single request could be used to check against the quota instead of having to make one for each item in the bulk. This is assuming you wanted to either accept or reject the whole bulk request in one go rather than potentially allowing some of them to succeed and some to fail.

#### Implementation
I used a post request because I wanted to try out json request serialization in Ktor :)

This could easily be implemented differently depending on the needs of the services calling this api.

### StatusPages plugin
#### Rationale
I noticed internal implementation details were leaking in the responses when the request didn't match the expected content type or structure.

I consider it good practice to have control over what your APIs return in either case so it was a good excuse to implement this.

I would assume there are other controls available in Ktor to turn off leaking of internal information but since I wanted to do this anyway and this is an MVP I didn't look into it.

### Logging or basic metrics
#### Rationale
Skipped this since it's just an exercise and I didn't see the point. It's basic stuff to implement anyway assuming this was a real project.
#### Implementation
Nope

## Changes for a real system

If this was a real production system then obviously an in memory solution would not be sufficient, **_especially_** since it was indicated this might be connected to billing.

For the example an hourly quota of 100 was used which doesn't exactly indicate a high traffic system, but that part is obviously easily adjusted.

Assuming really high throughput requirements we would probably run multiple instances of the service which would then need to share state storage. Something like redis with atomic execution of some lua script to handle the "refill" directly in the database would probably be used.

The API as it's currently written is extremely simple. If it was extended with support for requests with more complex structures in the request body and the inputs kept as json, rather than something like protobuf where validation can happen clientside as well, I would probably skip letting Ktor parse the request and do the steps myself just to be able to return better error messages for malformed requests.