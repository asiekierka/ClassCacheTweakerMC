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
