package pl.asie.classcachetweaker;

import com.google.common.io.Files;
import net.minecraft.launchwrapper.IClassTransformer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClassCacheTransformer implements IClassTransformer {
	private final List<IClassTransformer> transformerList;

	public ClassCacheTransformer(List<IClassTransformer> transformerList, File gameDir) {
		this.transformerList = transformerList;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		for (IClassTransformer transformer : transformerList) {
			basicClass = transformer.transform(name, transformedName, basicClass);
		}

		ClassCacheTweaker.cache.add(transformedName, basicClass);

		return basicClass;
	}
}
