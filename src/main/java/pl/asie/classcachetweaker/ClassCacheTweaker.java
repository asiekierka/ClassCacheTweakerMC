package pl.asie.classcachetweaker;

import com.google.common.collect.Lists;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by asie on 3/9/17.
 */
public class ClassCacheTweaker implements ITweaker {
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
			ClassCacheTransformer transformer = new ClassCacheTransformer(Lists.newArrayList(transformerList), gameDir);

			transformerList.clear();
			transformerList.add(transformer);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new String[0];
	}
}
