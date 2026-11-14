package com.iafenvoy.iceandfire.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 兼容垫片：把已改名的 {@link IafMobEffects} 以旧类名 {@code IafStatusEffects} 暴露。
 *
 * <p>背景：Ice and Fire 2.1.x 把注册类从 {@code IafStatusEffects} 改名为 {@code IafMobEffects}，
 * 而第三方附加模组 {@code ice_and_fire_spellbooks 2.3.3} 仍以 {@code getstatic IafStatusEffects.FROZEN}
 * 引用，执行到该指令即抛 {@code NoClassDefFoundError}（装备冰护之戒时触发）。</p>
 *
 * <p>做法：字段描述符保持 {@code Ldev/architectury/registry/registries/RegistrySupplier;}，
 * 实例用动态代理转发到 {@code IafMobEffects.FROZEN}，调用方无需改动，也不复制注册逻辑。</p>
 */
public final class IafStatusEffects {

    /** 真实持有类（Ice and Fire 2.1.x 的新名字）。 */
    private static final String HOLDER_CLASS = "com.iafenvoy.iceandfire.registry.IafMobEffects";
    /** 真实持有字段名。 */
    private static final String HOLDER_FIELD = "FROZEN";

    /** 真实持有者对象（DeferredHolder，同时实现 Holder）。 */
    private static final Object HOLDER = resolveHolder();

    /** 旧类名暴露的 FROZEN：描述符必须与调用方字节码一致。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final RegistrySupplier FROZEN = (RegistrySupplier) Proxy.newProxyInstance(
            IafStatusEffects.class.getClassLoader(),
            new Class<?>[]{RegistrySupplier.class},
            new ForwardingHandler());

    private IafStatusEffects() {
    }

    /** 反射取 IafMobEffects.FROZEN，避免编译期依赖 neoforge 的 DeferredHolder 类型。 */
    private static Object resolveHolder() {
        try {
            Class<?> holderClass = Class.forName(HOLDER_CLASS);
            Field field = holderClass.getField(HOLDER_FIELD);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "无法取得 " + HOLDER_CLASS + "." + HOLDER_FIELD + "（Ice and Fire 版本不匹配？）", e);
        }
    }

    /** 把接口调用整体转发到真实持有者。 */
    private static final class ForwardingHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "IafStatusEffects.FROZEN (compat shim)";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            Method target = findTarget(method);
            if (target == null) {
                // 明确抛出，不静默返回 null：让问题在日志里可见。
                throw new UnsupportedOperationException(
                        "IafStatusEffects 垫片未覆盖的方法: " + method.getName());
            }
            return target.invoke(HOLDER, args);
        }

        /** 在持有者的类与它实现的接口上寻找同名同参方法。 */
        private Method findTarget(Method method) {
            try {
                return HOLDER.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException notOnClass) {
                for (Class<?> itf : HOLDER.getClass().getInterfaces()) {
                    try {
                        return itf.getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException notOnInterface) {
                        // 继续找下一个接口；全部找不到时由调用方抛 UnsupportedOperationException。
                        continue;
                    }
                }
                return null;
            }
        }
    }
}
