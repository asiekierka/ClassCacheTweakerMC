package pl.asie.classcachetweaker;

import net.minecraft.launchwrapper.LaunchClassLoader;

import java.security.SecureClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClassCacheConcurrentHashMap extends ConcurrentHashMap<String, Class<?>> {
	private final Lock lock = new ReentrantLock();
	private final ClassCache cache;
	private final LaunchClassLoader classLoader;
	private boolean isInside;

	public ClassCacheConcurrentHashMap(ClassCache cache, LaunchClassLoader classLoader) {
		this.cache = cache;
		this.classLoader = classLoader;
	}

	@Override
	public Class<?> get(Object key) {
		if (isInside) {
			return super.get(key);
		}

		lock.lock();
		isInside = true;
		Class<?> output = super.get(key);
		if (output == null) {
			byte[] data = cache.get(key);
			if (data != null) {
				Class<?> c = null;
				try {
					c = (Class<?>) ClassCache.DEFINE_CLASS.invokeExact((SecureClassLoader) classLoader, (String) key, data, 0, data.length, cache.codeSourceMap.get(key));
					super.put((String) key, c);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				output = c;
			}

			if (output == null && key instanceof String) {
				try {
					output = classLoader.findClass((String) key);
				} catch (Exception e) {

				}
				if (output != null) {
					super.put((String) key, output);
				}
			}
		}

		isInside = false;
		lock.unlock();
		return output;
	}

	public Class<?> getReal(String key) {
		return super.get(key);
	}
}
