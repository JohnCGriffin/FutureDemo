# FutureDemo
Quick show of handling possibly slow backend service

## Basic Service Definition
An interface TodayService describes a method ```todayAsString``` that
delivers the current date string.

## Progressive Implementations of the Interface

#### BackendService
```BackendService``` takes up to ten seconds to return the date, simulating an expensive backend service such as a database or MFServer call.

#### CachingBackendService
```CachingBackendService``` decorates ```BackendService``` with a cache, avoiding needless and expensive underlying backend service requests.

#### AsynchronousTodayService
```AsynchronousTodayService``` decorates the caching backend service with a timeout.  The answer will arrive in time or fail in the specified duration.

#### AsynchronousTodayServiceWithFallback
```AsynchronousTodayServiceWithFallback``` extends ```AsynchronousTodayService``` so that if all else fails, it returns the previous result.  This would likely be the place where someone should be notified that a problem exists.  That problem, however, will be hidden from the end user.  The only time an TimeoutException can occur with an ```AsynchronousTodayServiceWithFallback``` is before it successfully arrives at a result the first time.
