package com.bootcloud.core.factory;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.PreDestroy;
import com.bootcloud.core.annotation.Scope;
import com.bootcloud.core.bean.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Spring IOC 容器默认实现类
 * 核心职责：Bean 定义注册、Bean 创建、依赖注入、生命周期管理、单例缓存
 * 实现了 Spring 最核心的 Bean 工厂功能
 * 
 * 【并发安全设计】
 * 1. 使用 ConcurrentHashMap 保证缓存读写的线程安全
 * 2. 使用 ReentrantLock 保证"查询 + 创建"的原子性
 * 3. 每个 Bean 名称对应一把锁，避免全局锁的性能问题
 * 4. 三级缓存解决循环依赖，但不解决并发安全问题
 * 
 * 【面试考点】
 * 1. 为什么 ConcurrentHashMap 不能保证并发安全？
 *    - 只能保证单个 put/get 操作原子，不能保证"查询 + 创建"复合操作原子
 *    - 例如：getBean() 时可能多个线程同时发现 Bean 不存在，然后同时创建
 * 2. 为什么需要 per-bean-lock（每 Bean 锁）而不是全局锁？
 *    - 全局锁会导致所有 Bean 创建串行化，性能差
 *    - 每 Bean 锁只锁住当前创建的 Bean，其他 Bean 可并发创建
 * 3. singletonsCurrentlyInCreation 的作用？
 *    - 仅用于标记 Bean 是否正在创建，防止循环依赖死循环
 *    - 不是并发控制，需要配合锁使用
 */
public class DefaultListableBeanFactory implements BeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(DefaultListableBeanFactory.class);

    // 存储所有 Bean 的定义信息（类信息、作用域、依赖、初始化/销毁方法等）
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    // 一级缓存：完全初始化好的单例 Bean（成品 Bean）
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    // 二级缓存：提前暴露的早期 Bean（解决循环依赖）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();

    // 三级缓存：单例 Bean 工厂（解决循环依赖，支持 AOP）
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();

    // 标记当前正在创建中的 Bean（防止循环依赖引发死循环）
    private final Map<String, Boolean> singletonsCurrentlyInCreation = new ConcurrentHashMap<>();

    // 【关键】每 Bean 锁映射表，确保每个 Bean 的创建过程是线程安全的
    // 使用 ConcurrentHashMap + ReentrantLock，避免全局锁的性能瓶颈
    private final Map<String, ReentrantLock> beanLocks = new ConcurrentHashMap<>();

    /**
     * 根据Bean名称获取Bean实例
     */
    @Override
    public Object getBean(String name) {
        return doGetBean(name, Object.class);
    }

    /**
     * 根据Bean类型获取Bean实例
     * 解析策略：
     * 1. 精确匹配（类型完全相同）优先
     * 2. 找到多个 → 按名称回退匹配（byName）
     * 3. 找不到 → 报错
     */
    @Override
    public <T> T getBean(Class<T> type) {
        // 第一轮：收集所有类型匹配的 Bean 名称
        List<String> matchedNames = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())) {
                matchedNames.add(entry.getKey());
            }
        }

        if (matchedNames.isEmpty()) {
            throw new RuntimeException("No bean of type " + type.getName() + " found");
        }

        // 精确匹配优先：找到类型完全一致的 Bean
        List<String> exactMatches = matchedNames.stream()
                .filter(name -> beanDefinitionMap.get(name).getBeanClass().equals(type))
                .collect(java.util.stream.Collectors.toList());

        if (exactMatches.size() == 1) {
            return (T) doGetBean(exactMatches.get(0), type);
        }
        if (exactMatches.size() > 1) {
            // 多个精确匹配：无法自动决定，抛出异常
            throw new RuntimeException("Expected single bean of type " + type.getName()
                    + ", but found: " + exactMatches);
        }

        // 没有精确匹配但有多个候选：按名称回退（使用类型简单名称作为候选名称）
        String expectedName = type.getSimpleName();
        expectedName = Character.toLowerCase(expectedName.charAt(0)) + expectedName.substring(1);
        if (matchedNames.contains(expectedName)) {
            return (T) doGetBean(expectedName, type);
        }

        // 多个候选但名称也不匹配：报错
        throw new RuntimeException("Expected single bean of type " + type.getName()
                + ", but found: " + matchedNames + ". Use @Qualifier or field name to disambiguate.");
    }

    /**
     * 根据Bean名称 + 类型 获取Bean实例
     */
    @Override
    public <T> T getBean(String name, Class<T> type) {
        return doGetBean(name, type);
    }

    /**
     * 判断容器中是否包含指定名称的Bean
     */
    @Override
    public boolean containsBean(String name) {
        return beanDefinitionMap.containsKey(name);
    }

    /**
     * 判断Bean是否为单例
     */
    @Override
    public boolean isSingleton(String name) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        return beanDefinition != null && beanDefinition.isSingleton();
    }

    /**
     * 获取指定名称的Bean定义信息
     */
    @Override
    public BeanDefinition getBeanDefinition(String name) {
        return beanDefinitionMap.get(name);
    }

    /**
     * 获取所有Bean定义（返回副本，防止外部修改原容器）
     */
    @Override
    public Map<String, BeanDefinition> getBeanDefinitionMap() {
        return new ConcurrentHashMap<>(beanDefinitionMap);
    }

    /**
     * 向容器注册Bean定义
     * @param name Bean名称
     * @param beanDefinition Bean定义对象
     */
    public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(name, beanDefinition);
        logger.debug("Registered bean definition: {} -> {}", name, beanDefinition.getBeanClass().getName());
    }

    /**
     * 【核心方法】获取 Bean 的真正逻辑
     * 1. 先从缓存中获取
     * 2. 缓存不存在则创建 Bean
     * 
     * 【并发安全保证】
     * - 单例 Bean：使用 ReentrantLock 确保"查询 + 创建"的原子性
     * - 原型 Bean：无锁，每次都创建新实例
     */
    private <T> T doGetBean(String name, Class<T> type) {
        // 尝试从单例缓存中获取 Bean（无锁读，性能优化）
        Object sharedInstance = getSingleton(name);
        if (sharedInstance != null) {
            return (T) sharedInstance;
        }

        // 获取 Bean 定义信息
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        if (beanDefinition == null) {
            throw new RuntimeException("Bean definition not found: " + name);
        }

        // 单例 Bean：走单例创建逻辑（需要加锁确保并发安全）
        if (beanDefinition.isSingleton()) {
            return (T) getSingleton(name, () -> createBean(name, beanDefinition));
        } else {
            // 原型 Bean：每次都创建新实例（无锁，线程安全）
            return (T) createBean(name, beanDefinition);
        }
    }

    /**
     * 从三级缓存中获取单例 Bean（解决循环依赖）
     * 查找顺序：一级缓存 → 二级缓存 → 三级缓存
     * 
     * 【注意】此方法仅用于循环依赖场景，不负责并发控制
     * 并发控制由 getSingleton(String, ObjectFactory) 方法负责
     */
    private Object getSingleton(String name) {
        Object singletonObject = singletonObjects.get(name);
        if (singletonObject == null) {
            singletonObject = earlySingletonObjects.get(name);
            if (singletonObject == null) {
                ObjectFactory<?> singletonFactory = singletonFactories.get(name);
                if (singletonFactory != null) {
                    // 从工厂获取早期 Bean，放入二级缓存，删除三级缓存
                    singletonObject = singletonFactory.getObject();
                    earlySingletonObjects.put(name, singletonObject);
                    singletonFactories.remove(name);
                }
            }
        }
        return singletonObject;
    }

    /**
     * 创建单例 Bean 的模板方法（使用双重检查锁定 DCL）
     * 负责：获取锁 → 双重检查 → 标记创建中 → 执行创建 → 放入一级缓存 → 清理缓存
     * 
     * 【并发安全设计】
     * 1. 第一次检查：无锁检查缓存，避免不必要的锁竞争（性能优化）
     * 2. 获取锁：使用 per-bean-lock，只锁当前 Bean，不影响其他 Bean 创建
     * 3. 第二次检查：有锁检查缓存，防止其他线程已创建完成
     * 4. 执行创建：在锁保护下创建 Bean
     * 5. 清理：释放锁，清理临时标记
     * 
     * 【为什么需要双重检查锁定？】
     * - 第一次检查（无锁）：如果 Bean 已存在，直接返回，避免锁开销
     * - 第二次检查（有锁）：防止多线程同时通过第一次检查后重复创建
     * 
     * 【面试考点】
     * 1. 为什么不直接用 synchronized？
     *    - synchronized 是重量级锁，性能差
     *    - ReentrantLock 更灵活，支持可中断、超时等特性
     * 2. 为什么不用 volatile？
     *    - volatile 只能保证可见性，不能保证复合操作原子性
     *    - 需要配合锁使用
     */
    private Object getSingleton(String name, ObjectFactory<?> objectFactory) {
        // 【第一次检查】无锁检查缓存（性能优化）
        Object singletonObject = singletonObjects.get(name);
        if (singletonObject != null) {
            return singletonObject;
        }

        // 【获取锁】获取当前 Bean 的专属锁
        ReentrantLock lock = beanLocks.computeIfAbsent(name, k -> new ReentrantLock());
        lock.lock();
        try {
            // 【第二次检查】有锁检查缓存（防止重复创建）
            singletonObject = singletonObjects.get(name);
            if (singletonObject != null) {
                return singletonObject;
            }

            // 【防止循环依赖】标记 Bean 正在创建
            singletonsCurrentlyInCreation.put(name, true);
            try {
                // 【执行创建】调用工厂创建 Bean
                singletonObject = objectFactory.getObject();
                
                // 【放入缓存】放入一级缓存
                singletonObjects.put(name, singletonObject);
                
                // 【清理缓存】清理二、三级缓存
                earlySingletonObjects.remove(name);
                singletonFactories.remove(name);
                
                return singletonObject;
            } finally {
                // 【清理标记】移除创建标记
                singletonsCurrentlyInCreation.remove(name);
            }
        } finally {
            // 【释放锁】释放锁
            lock.unlock();
        }
    }

    /**
     * 创建Bean的入口方法
     * 包含：创建实例 → 依赖注入 → 初始化 → 注册销毁方法
     */
    private Object createBean(String name, BeanDefinition beanDefinition) {
        final String beanName = name;
        try {
            // 执行Bean创建流程
            Object bean = doCreateBean(beanName, beanDefinition);

            // 注册@PreDestroy销毁方法（JVM关闭时执行）
            for (Method method : beanDefinition.getPreDestroyMethods()) {
                method.setAccessible(true);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        method.invoke(bean);
                        logger.debug("Executed @PreDestroy method: {} on bean: {}", method.getName(), beanName);
                    } catch (Exception e) {
                        logger.error("Failed to execute @PreDestroy method", e);
                    }
                }));
            }

            return bean;
        } catch (Exception e) {
            logger.error("Failed to create bean: " + name, e);
            throw new RuntimeException("Failed to create bean: " + name, e);
        }
    }

    /**
     * 真正执行 Bean 创建的核心方法
     * 1. 创建实例
     * 2. 提前暴露 Bean（解决循环依赖）
     * 3. 依赖注入
     * 4. 初始化 Bean
     * 
     * 【作用域处理】
     * - singleton：放入三级缓存，提前暴露实例，解决循环依赖
     * - prototype：不放入缓存，每次创建新实例，不支持循环依赖
     */
    private Object doCreateBean(String name, BeanDefinition beanDefinition) throws Exception {
        Object instance;
        
        // 【单例 Bean】缓存实例，避免重复创建
        // 【原型 Bean】每次都创建新实例，不缓存
        if (beanDefinition.isSingleton()) {
            instance = beanDefinition.getInstance();
            if (instance == null) {
                instance = createInstance(beanDefinition);
                beanDefinition.setInstance(instance);
            }
        } else {
            // 原型 Bean：直接创建新实例
            instance = createInstance(beanDefinition);
        }

        final String beanName = name;
        final BeanDefinition beanDef = beanDefinition;
        final Object beanInstance = instance;

        // 【单例 Bean】放入三级缓存，提前暴露实例，解决循环依赖
        // 【原型 Bean】不放入缓存，不支持循环依赖
        if (beanDefinition.isSingleton()) {
            if (singletonsCurrentlyInCreation.containsKey(name)) {
                singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, beanDef, beanInstance));
            }
        }

        // 依赖注入（@Autowired）
        populateBean(beanDefinition, instance);

        // 执行初始化（@PostConstruct）
        initializeBean(beanDefinition, instance);

        return instance;
    }

    /**
     * 获取早期Bean引用（预留扩展点，可用于AOP代理）
     */
    private Object getEarlyBeanReference(String name, BeanDefinition beanDefinition, Object bean) {
        return bean;
    }

    /**
     * 反射创建Bean实例
     * 支持：构造函数依赖注入
     */
    private Object createInstance(BeanDefinition beanDefinition) throws Exception {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Constructor<?> constructor = beanDefinition.getConstructor();

        // 有参构造：自动注入依赖
        if (constructor != null) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = getBean(parameterTypes[i]);
            }
            // newInstance()：反射创建对象
            return constructor.newInstance(args);
        }

        // 无参构造：默认实例化
        return beanClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Bean属性填充（依赖注入）
     * 处理@Autowired注解的字段和Setter方法
     */
    private void populateBean(BeanDefinition beanDefinition, Object bean) throws Exception {
        // 1. 字段注入
        for (Field field : beanDefinition.getAutowiredFields()) {
            Class<?> fieldType = field.getType();
            Autowired autowired = field.getAnnotation(Autowired.class);
            Object dependency = resolveDependency(fieldType, field.getName(), autowired.required());

            if (dependency != null) {
                field.setAccessible(true);
                field.set(bean, dependency);
                logger.debug("Autowired field: {} in bean: {}", field.getName(), beanDefinition.getBeanName());
            }
        }

        // 2. Setter 方法注入
        for (Method method : beanDefinition.getAutowiredMethods()) {
            Class<?> paramType = method.getParameterTypes()[0];
            Autowired autowired = method.getAnnotation(Autowired.class);
            String paramName = method.getName().replaceFirst("^(set|is)", "");
            paramName = Character.toLowerCase(paramName.charAt(0)) + paramName.substring(1);
            Object dependency = resolveDependency(paramType, paramName, autowired.required());

            if (dependency != null) {
                method.setAccessible(true);
                method.invoke(bean, dependency);
                logger.debug("Autowired setter: {} in bean: {}", method.getName(), beanDefinition.getBeanName());
            }
        }
    }

    /**
     * 解析单个依赖项
     * 策略：先按类型查找 → 找到多个则按名称回退
     */
    private Object resolveDependency(Class<?> type, String name, boolean required) {
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())) {
                candidates.add(entry.getKey());
            }
        }

        if (candidates.isEmpty()) {
            if (required) {
                throw new RuntimeException("No bean of type " + type.getName() + " found for injection");
            }
            return null;
        }

        // 精确匹配优先
        List<String> exactMatches = candidates.stream()
                .filter(n -> beanDefinitionMap.get(n).getBeanClass().equals(type))
                .collect(java.util.stream.Collectors.toList());

        if (exactMatches.size() == 1) {
            return doGetBean(exactMatches.get(0), type);
        }
        if (exactMatches.size() > 1) {
            // 多个精确匹配：尝试按名称匹配
            if (exactMatches.contains(name)) {
                return getBean(name, type);
            }
            throw new RuntimeException("Expected single bean of type " + type.getName()
                    + ", but found: " + exactMatches + ". Use field/setter name to disambiguate.");
        }

        // 没有精确匹配但有候选：按名称回退
        if (candidates.contains(name)) {
            return getBean(name, type);
        }

        // 单个候选直接返回
        if (candidates.size() == 1) {
            return getBean(candidates.get(0), type);
        }

        if (required) {
            throw new RuntimeException("Expected single bean of type " + type.getName()
                    + ", but found: " + candidates + ". Use field/setter name to disambiguate.");
        }
        return null;
    }

    /**
     * 初始化Bean
     * 执行@PostConstruct标注的方法
     */
    private void initializeBean(BeanDefinition beanDefinition, Object bean) throws Exception {
        for (Method method : beanDefinition.getPostConstructMethods()) {
            method.setAccessible(true);
            method.invoke(bean);
            logger.debug("Executed @PostConstruct method: {} on bean: {}", method.getName(), beanDefinition.getBeanName());
        }
    }

    /**
     * 获取所有指定类型的 Bean 实例
     * 返回 beanName -> beanInstance 的映射
     */
    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        Map<String, T> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition bd = entry.getValue();
            if (type.isAssignableFrom(bd.getBeanClass())) {
                try {
                    T bean = getBean(beanName, type);
                    result.put(beanName, bean);
                } catch (Exception e) {
                    logger.warn("Failed to get bean of type {} with name {}: {}", type.getName(), beanName, e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     预先实例化所有单例Bean
     项目启动时执行，初始化所有单例Bean
     */
    public void preInstantiateSingletons() {
        for (String beanName : beanDefinitionMap.keySet()) {
            if (isSingleton(beanName)) {
                getBean(beanName);
            }
        }
    }

    /**
     * 关闭容器
     * 执行所有Bean的销毁方法，清空缓存
     */
    public void close() {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            BeanDefinition beanDefinition = entry.getValue();
            Object bean = singletonObjects.get(entry.getKey());
            if (bean != null) {
                // 执行@PreDestroy销毁方法
                for (Method method : beanDefinition.getPreDestroyMethods()) {
                    try {
                        method.setAccessible(true);
                        method.invoke(bean);
                        logger.debug("Executed @PreDestroy method: {} on bean: {}", method.getName(), entry.getKey());
                    } catch (Exception e) {
                        logger.error("Failed to execute @PreDestroy method", e);
                    }
                }
            }
        }
        // 清空所有缓存
        singletonObjects.clear();
        earlySingletonObjects.clear();
        singletonFactories.clear();
    }

    /**
     * 对象工厂接口（函数式接口）
     * 用于延迟创建Bean实例
     */
    @FunctionalInterface
    public interface ObjectFactory<T> {
        T getObject();
    }
}