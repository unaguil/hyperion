package serialization.binary;

import java.io.IOException;
import java.lang.reflect.Field;

public class FinalFieldSetter {

	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final Object value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, long value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setLong(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
}
