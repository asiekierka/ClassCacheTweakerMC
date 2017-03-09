package pl.asie.classcachetweaker;

import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.Map;

public class ClassCache implements Serializable {
	private transient static final Field PACKAGES;
	private transient static final Field CACHED_CLASSES;
	private transient static final MethodHandle DEFINE_CLASS;

	static {
		Field PACKAGES_TMP = null;

		try {
			PACKAGES_TMP = ClassLoader.class.getDeclaredField("packages");
		} catch (Exception e) {
			e.printStackTrace();
		}

		PACKAGES = PACKAGES_TMP;
		PACKAGES.setAccessible(true);

		Field CACHED_CLASSES_TMP = null;

		try {
			CACHED_CLASSES_TMP = LaunchClassLoader.class.getDeclaredField("cachedClasses");
		} catch (Exception e) {
			e.printStackTrace();
		}

		CACHED_CLASSES = CACHED_CLASSES_TMP;
		CACHED_CLASSES.setAccessible(true);

		MethodHandle DEFINE_CLASS_TMP = null;

		try {
			Method m = SecureClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, CodeSource.class);
			m.setAccessible(true);
			DEFINE_CLASS_TMP = MethodHandles.lookup().unreflect(m);
		} catch (Exception e) {
			e.printStackTrace();
		}

		DEFINE_CLASS = DEFINE_CLASS_TMP;
	}

	private transient File classCacheFile;
	private transient LaunchClassLoader classLoader;

	//private Map<String, Package> packageMap = new HashMap<>();
	private Map<String, byte[]> classMap = new HashMap<>();
	private transient Thread saveThread;
	private transient boolean dirty;

	public static ClassCache load(LaunchClassLoader classLoader, File gameDir) throws IOException, IllegalAccessException, ClassNotFoundException {
		File classCacheFile = new File(gameDir, "classCache.dat");
		ClassCache cache;
		if (!classCacheFile.exists()) {
			cache = new ClassCache();
		} else {
			FileInputStream fileInputStream = new FileInputStream(classCacheFile);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
			cache = (ClassCache) objectInputStream.readObject();
			objectInputStream.close();
			fileInputStream.close();

			HashMap<String, Package> packageHashMap = (HashMap<String, Package>) PACKAGES.get(classLoader);
			//packageHashMap.putAll(cache.packageMap);

			Map<String, Class<?>> cachedClasses = (Map<String, Class<?>>) CACHED_CLASSES.get(classLoader);

			try {
				for (Map.Entry<String, byte[]> entry : cache.classMap.entrySet()) {
					if (!cachedClasses.containsKey(entry.getKey()) && entry.getValue() != null) {
						Class<?> c = (Class<?>) DEFINE_CLASS.invokeExact((SecureClassLoader) classLoader, entry.getKey(), entry.getValue(), 0, entry.getValue() == null ? 0 : entry.getValue().length, (CodeSource) null);
						cachedClasses.put(entry.getKey(), c);
					}
				}
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}

		cache.classCacheFile = classCacheFile;
		cache.classLoader = classLoader;
		cache.saveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {

					}

					synchronized (cache) {
						if (cache.dirty) {
							cache.save();
							cache.dirty = false;
						}
					}
				}
			}
		});
		cache.saveThread.start();

		return cache;
	}

	public synchronized void add(String transformedName, byte[] data) {
		System.out.println("Adding " + transformedName);
		classMap.put(transformedName, data);

		int lastDot = transformedName.lastIndexOf('.');
		if (lastDot > -1 && !transformedName.startsWith("net.minecraft.")) {
			String packageName = transformedName.substring(0, lastDot);
			//packageMap.put(packageName, Package.getPackage(packageName));
		}

		dirty = true;
	}

	private void save() {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(classCacheFile);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(this);
			objectOutputStream.close();
			fileOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
