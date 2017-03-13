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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Created by asie on 3/9/17.
 */
public class ClassCacheTweaker implements ITweaker {
	private static final Set<String> INCOMPATIBLE_TRANSFORMER_PREFIXES = ImmutableSet.of("elec332.");
	private static final Set<String> INCOMPATIBLE_TRANSFORMER_SUFFIXES = ImmutableSet.of("fml.common.asm.transformers.ModAPITransformer");

	public static ClassCache cache;
	private LaunchClassLoader classLoader;
	private File gameDir;

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
		this.gameDir = gameDir;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public String getLaunchTarget() {
		return null;
	}

	@Override
	public String[] getLaunchArguments() {
		try {
			cache = ClassCache.load(classLoader, gameDir);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			// This is a good point in time.
			Field transformersField = LaunchClassLoader.class.getDeclaredField("transformers");
			transformersField.setAccessible(true);

			List<IClassTransformer> transformerList = (List<IClassTransformer>) transformersField.get(classLoader);

			List<IClassTransformer> capturedTransformerList = Lists.newArrayList();
			List<IClassTransformer> preservedTransformerList = Lists.newArrayList();

			for (IClassTransformer transformer : transformerList) {
				String className = transformer.getClass().getName();
				boolean found = false;
				for (String prefix : INCOMPATIBLE_TRANSFORMER_PREFIXES) {
					if (className.startsWith(prefix)) {
						found = true;
						break;
					}
				}
				if (!found) {
					for (String suffix : INCOMPATIBLE_TRANSFORMER_SUFFIXES) {
						if (className.endsWith(suffix)) {
							found = true;
							break;
						}
					}
				}
				if (found) {
					preservedTransformerList.add(transformer);
				} else {
					capturedTransformerList.add(transformer);
				}
			}

			ClassCacheTransformer transformer = new ClassCacheTransformer(capturedTransformerList, gameDir);

			transformerList.clear();
			transformerList.add(transformer);
			transformerList.addAll(preservedTransformerList);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new String[0];
	}
}
