package pl.asie.classcachetweaker;

import net.minecraft.launchwrapper.LaunchClassLoader;
import org.lwjgl.Sys;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

public class ClassCache implements Serializable {
	private transient static final Field PACKAGES;
	private transient static final Field CACHED_CLASSES;
	private transient static final Field MANIFESTS;
	private transient static final Field SEAL_BASE;
	private transient static final MethodHandle DEFINE_CLASS;
	private transient static final MethodHandle DEFINE_PACKAGE;

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

		Field MANIFESTS_TMP = null;

		try {
			MANIFESTS_TMP = LaunchClassLoader.class.getDeclaredField("packageManifests");
		} catch (Exception e) {
			e.printStackTrace();
		}

		MANIFESTS = MANIFESTS_TMP;
		MANIFESTS.setAccessible(true);

		Field SEAL_BASE_TMP = null;

		try {
			SEAL_BASE_TMP = Package.class.getDeclaredField("sealBase");
		} catch (Exception e) {
			e.printStackTrace();
		}

		SEAL_BASE = SEAL_BASE_TMP;
		SEAL_BASE.setAccessible(true);

		MethodHandle DEFINE_PACKAGE_TMP = null;

		try {
			Method m = URLClassLoader.class.getDeclaredMethod("definePackage", String.class, Manifest.class, URL.class);
			m.setAccessible(true);
			DEFINE_PACKAGE_TMP = MethodHandles.lookup().unreflect(m);
		} catch (Exception e) {
			e.printStackTrace();
		}

		DEFINE_PACKAGE = DEFINE_PACKAGE_TMP;

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
	private transient File classCacheFileTmp;
	private transient LaunchClassLoader classLoader;
	private transient Thread saveThread;
	private transient boolean dirty;

	private Set<String> packageNames = new HashSet<>();
	private Map<String, byte[]> classMap = new HashMap<>();

	public static ClassCache load(LaunchClassLoader classLoader, File gameDir) throws IOException, IllegalAccessException, ClassNotFoundException {
		File classCacheFile = new File(gameDir, "classCache.dat");
		ClassCache cache = new ClassCache();
		if (classCacheFile.exists()) {
			boolean loaded = false;

			try {
				FileInputStream fileInputStream = new FileInputStream(classCacheFile);
				DataInputStream dataInputStream = new DataInputStream(fileInputStream);
				int version = dataInputStream.readUnsignedByte();
				if (version != 1) {
					throw new IOException("Invalid ClassCache.dat version!");
				}

				Map<Package, Manifest> packageManifestMap = (Map<Package, Manifest>) MANIFESTS.get(classLoader);

				int packageCount = dataInputStream.readInt();
				for (int i = 0; i < packageCount; i++) {
					boolean present = dataInputStream.readBoolean();
					if (present) {
						String name = dataInputStream.readUTF();
						Manifest manifest = new Manifest();
						manifest.read(dataInputStream);
						URL url = dataInputStream.readBoolean() ? new URL(dataInputStream.readUTF()) : null;
						Package out = (Package) DEFINE_PACKAGE.invokeExact((URLClassLoader) classLoader, name, manifest, url);
						cache.packageNames.add(name);
						packageManifestMap.put(out, manifest);
					}
				}

				int classCount = dataInputStream.readInt();
				for (int i = 0; i < classCount; i++) {
					String name = dataInputStream.readUTF();
					int dataLen = dataInputStream.readInt();
					byte[] data = new byte[dataLen];
					dataInputStream.read(data);
					cache.classMap.put(name, data);
				}

				dataInputStream.close();
				fileInputStream.close();

				loaded = true;
			} catch (Throwable t) {
				t.printStackTrace();
				cache = new ClassCache();
			}

			if (loaded) {
				//HashMap<String, Package> packageHashMap = (HashMap<String, Package>) PACKAGES.get(classLoader);
				//packageHashMap.putAll(cache.packageMap);

				Map<String, Class<?>> cachedClasses = (Map<String, Class<?>>) CACHED_CLASSES.get(classLoader);

				cache.classMap.entrySet().forEach((entry) -> {
					try {
						if (!cachedClasses.containsKey(entry.getKey())) {
							Class<?> c = (Class<?>) DEFINE_CLASS.invokeExact((SecureClassLoader) classLoader, entry.getKey(), entry.getValue(), 0, entry.getValue() == null ? 0 : entry.getValue().length, (CodeSource) null);
							cachedClasses.put(entry.getKey(), c);
						}
					} catch (Throwable t) {
						throw new RuntimeException(t);
					}
				});
			}
		}

		cache.classCacheFile = classCacheFile;
		cache.classCacheFileTmp = new File(classCacheFile.getAbsolutePath() + "_tmp");
		cache.classLoader = classLoader;

		if (cache.classCacheFileTmp.exists()) {
			cache.classCacheFileTmp.delete();
		}

		final ClassCache cache1 = cache;
		cache.saveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {

					}

					synchronized (cache1) {
						if (cache1.dirty) {
							cache1.save();
							cache1.dirty = false;
						}
					}
				}
			}
		});
		cache.saveThread.start();

		return cache;
	}

	public synchronized void add(String transformedName, byte[] data) {
		if (data == null) return;

		System.out.println("Adding " + transformedName);
		classMap.put(transformedName, data);

		int lastDot = transformedName.lastIndexOf('.');
		if (lastDot > -1 && !transformedName.startsWith("net.minecraft.")) {
			String packageName = transformedName.substring(0, lastDot);
			packageNames.add(packageName);
		}

		dirty = true;
	}

	private synchronized void save() {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(classCacheFileTmp);
			DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
			Map<Package, Manifest> packageManifestMap = (Map<Package, Manifest>) MANIFESTS.get(classLoader);

			dataOutputStream.writeByte(1); // version
			dataOutputStream.writeInt(packageNames.size());
			for (String s : packageNames) {
				Package pkg = Package.getPackage(s);
				if (pkg == null) {
					// System.out.println("!? " + s);
					dataOutputStream.writeBoolean(false);
					continue;
				}
				Manifest manifest = packageManifestMap.get(pkg);
				dataOutputStream.writeBoolean(manifest != null);
				if (manifest != null) {
					dataOutputStream.writeUTF(s);
					manifest.write(dataOutputStream);
					URL sealBase = (URL) SEAL_BASE.get(pkg);
					dataOutputStream.writeBoolean(sealBase != null);
					if (sealBase != null) {
						dataOutputStream.writeUTF(sealBase.toString());
					}
				}
			}

			dataOutputStream.writeInt(classMap.size());
			for (Map.Entry<String, byte[]> entry : classMap.entrySet()) {
				dataOutputStream.writeUTF(entry.getKey());
				dataOutputStream.writeInt(entry.getValue().length);
				dataOutputStream.write(entry.getValue());
			}

			dataOutputStream.flush();
			dataOutputStream.close();
			fileOutputStream.flush();
			fileOutputStream.close();

			classCacheFile.delete();
			classCacheFileTmp.renameTo(classCacheFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
