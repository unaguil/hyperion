package serialization.binary;

import java.io.IOException;
import java.io.ObjectInput;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UnserializationUtils {

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
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, int value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setInt(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static <A, B> void fillMap(Map<A, B> map, A keys[], B values[]) {
		List<A> keyList = Arrays.asList(keys);
		List<B> valueList = Arrays.asList(values);
		
		fillMap(map, keyList, valueList);
	}

	public static <B, A> void fillMap(Map<A, B> map, List<A> keyList, List<B> valueList) {
		Iterator<A> itKeys = keyList.iterator();
		Iterator<B> itValues = valueList.iterator();
		
		while (itKeys.hasNext()) {
			map.put(itKeys.next(), itValues.next());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <A, B> void readMap(Map<A, B> map, ObjectInput in) throws ClassNotFoundException, IOException {
		A[] keys = (A[]) in.readObject();
		B[] values = (B[]) in.readObject();
		
		fillMap(map, keys, values);
	}
}
