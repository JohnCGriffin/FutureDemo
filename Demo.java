package futuredemo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

interface TodayService {
	String todayAsString() throws TimeoutException; // .e.g. "2020-01-23"
}

// Basic implementation can take a few seconds
class BackendService implements TodayService {

	static void someExpensiveOperation() {
		// Sleep up to 10 seconds
		long millisDelay = ThreadLocalRandom.current().nextLong(10000);
		try {
			Thread.sleep(millisDelay);
		} catch (InterruptedException e) {
		}
	}

	public String todayAsString() {
		System.out.println("BACKEND Service working");
		someExpensiveOperation();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return formatter.format(date);
	}
}

// Caching TodayService Decorator
class CachingBackendService implements TodayService {

	private static class CacheEntry {
		final String value;
		final long expiration;

		private CacheEntry(String value, long expiration) {
			this.value = value;
			this.expiration = expiration;
		}
	}

	private CacheEntry cache;
	final private TodayService underlyingService;
	final private long millisDuration;

	public CachingBackendService(TodayService underlyingService, long millis) {
		this.underlyingService = underlyingService;
		this.millisDuration = millis;
	}

	public String todayAsString() throws TimeoutException {
		CacheEntry c = this.cache;
		if (c != null && c.expiration > System.currentTimeMillis()) {
			System.out.println("CACHE success");
			return c.value;
		}
		String result = this.underlyingService.todayAsString();
		System.out.println("STORING result into cache");
		this.cache = new CacheEntry(result, System.currentTimeMillis() + millisDuration);
		return result;
	}
}

// Asynchronous TodayService Decorator guarantees time-bound success or Exception
class AsynchronousTodayService implements TodayService {

	// For instance, MFServer has 2 instances available to answer requests
	final static ExecutorService executor = Executors.newFixedThreadPool(2);

	final private TodayService underlying;
	final private long millisAcceptableDelay;

	AsynchronousTodayService(long millisAcceptableDelay) {
		this.underlying = new CachingBackendService(new BackendService(), 5000);
		this.millisAcceptableDelay = millisAcceptableDelay;
	}

	public String todayAsString() throws TimeoutException {
		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			public String call() throws Exception {
				return underlying.todayAsString();
			}
		});
		executor.submit(task);
		try {
			return task.get(millisAcceptableDelay, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new TimeoutException();
		}
	}

}

//Asynchronous TodayService Decorator ALMOST guarantees success
class AsynchronousTodayServiceWithFallback extends AsynchronousTodayService {
	
	String fallback;

	AsynchronousTodayServiceWithFallback(long millisAcceptableDelay) {
		super(millisAcceptableDelay);
		this.fallback = null;
	}

	public String todayAsString() throws TimeoutException {
		try {
			return (this.fallback = super.todayAsString());
		} catch (TimeoutException e) {
			if(this.fallback != null) {
				System.out.println("*** SAVED BY FALLBACK ***");
				return this.fallback;
			}
			throw new TimeoutException();
		}
	}

}

public class Demo {
	
	static void demo(TodayService service)
	{
		for (;;) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			long start = System.currentTimeMillis();
			try {
				System.out.printf("RESULT: %s after %d ms\n", service.todayAsString(),
						System.currentTimeMillis() - start);
			} catch (TimeoutException e) {
				long duration = System.currentTimeMillis() - start;
				System.err.printf("**** REQUEST timed out after %d ms\n", duration);
			}
			System.out.flush();
		}		
	}

	public static void main(String[] args) {
		TodayService theService = new BackendService();
		//TodayService theService = new CachingBackendService(new BackendService(), 5000);
		//TodayService theService = new AsynchronousTodayService(7000);
		//TodayService theService = new AsynchronousTodayServiceWithFallback(7000);
		demo(theService);
	}

}
