/**
 * This file is part of ClassCacheTweaker.
 *
 * ClassCacheTweaker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClassCacheTweaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClassCacheTweaker.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the Minecraft game engine, the Mojang Launchwrapper,
 * the Mojang AuthLib and the Minecraft Realms library (and/or modified
 * versions of said software), containing parts covered by the terms of
 * their respective licenses, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 */
package pl.asie.classcachetweaker;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.net.URL;
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
					for (IClassTransformer transformer : classLoader.getTransformers()) {
						if (!(transformer instanceof ClassCacheTweaker))
							data = transformer.transform((String) key, (String) key, data);
					}
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
