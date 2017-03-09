package pl.asie.classcachetweaker;

import com.google.common.io.Files;
import net.minecraft.launchwrapper.IClassTransformer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClassCacheTransformer implements IClassTransformer {
	private final List<IClassTransformer> transformerList;
	private final File cacheDir;

	public ClassCacheTransformer(List<IClassTransformer> transformerList, File gameDir) {
		this.transformerList = transformerList;
		this.cacheDir = new File(gameDir, "classCache");
		if (!this.cacheDir.exists()) {
			this.cacheDir.mkdirs();
		}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		File file = new File(cacheDir, transformedName + ".class");
		if (file.exists()) {
			try {
				return Files.toByteArray(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (IClassTransformer transformer : transformerList) {
			basicClass = transformer.transform(name, transformedName, basicClass);
		}

		try {
			Files.write(basicClass, file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return basicClass;
	}
}
