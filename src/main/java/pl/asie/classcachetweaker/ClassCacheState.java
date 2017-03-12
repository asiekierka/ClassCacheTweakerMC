package pl.asie.classcachetweaker;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

public class ClassCacheState {
	private static class FileState {
		private static final ByteBuffer buffer = ByteBuffer.allocate(16);
		private String path;
		private long time;
		private long size;

		private FileState(File file) {
			try {
				this.path = file.getCanonicalPath();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.time = file.lastModified();
			this.size = file.length();

			System.out.println("- " + path);
		}

		public void append(MessageDigest md) {
			md.update(path.getBytes());

			buffer.putLong(0, time);
			buffer.putLong(8, size);
			md.update(buffer.array());
		}
	}

	private static byte[] data = null;

	public static byte[] generate(File gameDir) {
		if (data != null)
			return data;

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			List<FileState> stateList = Lists.newArrayList();
			addFile(stateList, new File(gameDir, "mods"), 0);
			addFile(stateList, new File(gameDir, "modList.json"), 0);
			stateList.sort(Comparator.comparing(state -> state.path));
			for (FileState state : stateList) {
				state.append(md);
			}
			data = md.digest();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return data;
	}

	private static void addFile(List<FileState> stateList, File file, int depth) {
		if (depth > 16) {
			throw new RuntimeException("depth too high");
		}
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] ff = file.listFiles();
			if (ff != null) {
				for (File f : ff) {
					addFile(stateList, f, depth + 1);
				}
			}
		} else {
			stateList.add(new FileState(file));
		}
	}
}
