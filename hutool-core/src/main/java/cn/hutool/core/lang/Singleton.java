package cn.hutool.core.lang;

import cn.hutool.core.classloader.ClassLoaderUtil;
import cn.hutool.core.lang.func.Func0;
import cn.hutool.core.reflect.ConstructorUtil;
import cn.hutool.core.text.StrUtil;
import cn.hutool.core.util.ArrayUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 单例类<br>
 * 提供单例对象的统一管理，当调用get方法时，如果对象池中存在此对象，返回此对象，否则创建新对象返回<br>
 *
 * @author loolly
 */
public final class Singleton {

	private static final ConcurrentHashMap<String, Object> POOL = new ConcurrentHashMap<>();

	private Singleton() {
	}

	/**
	 * 获得指定类的单例对象<br>
	 * 对象存在于池中返回，否则创建，每次调用此方法获得的对象为同一个对象<br>
	 * 注意：单例针对的是类和参数，也就是说只有类、参数一致才会返回同一个对象
	 *
	 * @param <T>    单例对象类型
	 * @param clazz  类
	 * @param params 构造方法参数
	 * @return 单例对象
	 */
	public static <T> T get(final Class<T> clazz, final Object... params) {
		Assert.notNull(clazz, "Class must be not null !");
		final String key = buildKey(clazz.getName(), params);
		return get(key, () -> ConstructorUtil.newInstance(clazz, params));
	}

	/**
	 * 获得指定类的单例对象<br>
	 * 对象存在于池中返回，否则创建，每次调用此方法获得的对象为同一个对象<br>
	 * 注意：单例针对的是类和参数，也就是说只有类、参数一致才会返回同一个对象
	 *
	 * @param <T>      单例对象类型
	 * @param key      自定义键
	 * @param supplier 单例对象的创建函数
	 * @return 单例对象
	 * @since 5.3.3
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(final String key, final Func0<T> supplier) {
		//return (T) POOL.computeIfAbsent(key, (k)-> supplier.callWithRuntimeException());
		// issues#2349
		// ConcurrentHashMap.computeIfAbsent在某些情况下会导致死循环问题，此处采用Dubbo的解决方案

		Object value = POOL.get(key);
		if(null == value){
			POOL.putIfAbsent(key, supplier.callWithRuntimeException());
			value = POOL.get(key);
		}
		return (T) value;
	}

	/**
	 * 获得指定类的单例对象<br>
	 * 对象存在于池中返回，否则创建，每次调用此方法获得的对象为同一个对象<br>
	 *
	 * @param <T>       单例对象类型
	 * @param className 类名
	 * @param params    构造参数
	 * @return 单例对象
	 */
	public static <T> T get(final String className, final Object... params) {
		Assert.notBlank(className, "Class name must be not blank !");
		final Class<T> clazz = ClassLoaderUtil.loadClass(className);
		return get(clazz, params);
	}

	/**
	 * 将已有对象放入单例中，其Class做为键
	 *
	 * @param obj 对象
	 * @since 4.0.7
	 */
	public static void put(final Object obj) {
		Assert.notNull(obj, "Bean object must be not null !");
		put(obj.getClass().getName(), obj);
	}

	/**
	 * 将已有对象放入单例中，key做为键
	 *
	 * @param key 键
	 * @param obj 对象
	 * @since 5.3.3
	 */
	public static void put(final String key, final Object obj) {
		POOL.put(key, obj);
	}

	/**
	 * 判断某个类的对象是否存在
	 *
	 * @param clazz 类
	 * @param params 构造参数
	 * @return 是否存在
	 */
	public static boolean exists(final Class<?> clazz, final Object... params){
		if (null != clazz){
			final String key = buildKey(clazz.getName(), params);
			return POOL.containsKey(key);
		}
		return false;
	}

	/**
	 * 获取单例池中存在的所有类
	 *
	 * @return 非重复的类集合
	 */
	public static Set<Class<?>> getExistClass(){
		return POOL.values().stream().map(Object::getClass).collect(Collectors.toSet());
	}

	/**
	 * 移除指定Singleton对象
	 *
	 * @param clazz 类
	 */
	public static void remove(final Class<?> clazz) {
		if (null != clazz) {
			remove(clazz.getName());
		}
	}

	/**
	 * 移除指定Singleton对象
	 *
	 * @param key 键
	 */
	public static void remove(final String key) {
		POOL.remove(key);
	}

	/**
	 * 清除所有Singleton对象
	 */
	public static void destroy() {
		POOL.clear();
	}

	// ------------------------------------------------------------------------------------------- Private method start

	/**
	 * 构建key
	 *
	 * @param className 类名
	 * @param params    参数列表
	 * @return key
	 */
	private static String buildKey(final String className, final Object... params) {
		if (ArrayUtil.isEmpty(params)) {
			return className;
		}
		return StrUtil.format("{}#{}", className, ArrayUtil.join(params, "_"));
	}
	// ------------------------------------------------------------------------------------------- Private method end
}
