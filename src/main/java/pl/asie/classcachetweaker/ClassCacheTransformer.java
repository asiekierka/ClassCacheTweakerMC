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

import java.io.File;
import java.util.List;

public class ClassCacheTransformer implements IClassTransformer {
	private final List<IClassTransformer> transformerList;

	public ClassCacheTransformer(List<IClassTransformer> transformerList, File gameDir) {
		this.transformerList = transformerList;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		byte[] data = ClassCacheTweaker.cache.get(transformedName);
		if (data != null) {
			return data;
		}

		for (IClassTransformer transformer : transformerList) {
			basicClass = transformer.transform(name, transformedName, basicClass);
		}

		ClassCacheTweaker.cache.add(transformedName, basicClass);

		return basicClass;
	}
}
